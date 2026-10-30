package pe.misgastos.avisos;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * La pantalla principal muestra la app web Mis gastos (tus movimientos, el resumen y los gráficos).
 * La web habla con el celular por el puente "MisGastosAndroid": así recibe la dirección del buzón
 * sola y puede abrir la configuración de avisos.
 */
public class MainActivity extends Activity {
    private static final int GUARDAR_ARCHIVO = 1, ELEGIR_ARCHIVO = 2;

    private WebView web;
    private View sinInternet;
    private ValueCallback<Uri[]> eligiendo;
    private String porGuardar;

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        setContentView(R.layout.principal);
        web = findViewById(R.id.web);
        sinInternet = findViewById(R.id.sinInternet);
        findViewById(R.id.btnReintentarWeb).setOnClickListener(v -> cargar());

        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true);  // para revisar la página desde la PC
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Puente(), "MisGastosAndroid");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                // Solo la app web se abre aquí; cualquier otro enlace va al navegador.
                if (!r.isForMainFrame() || r.getUrl().toString().startsWith(BuildConfig.WEB)) return false;
                abrirFuera(r.getUrl());
                return true;
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) sinInternet.setVisibility(View.VISIBLE);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            // Para «Cargar copia»: deja elegir un archivo del celular.
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (eligiendo != null) eligiendo.onReceiveValue(null);
                eligiendo = cb;
                try { startActivityForResult(p.createIntent(), ELEGIR_ARCHIVO); }
                catch (ActivityNotFoundException e) { eligiendo = null; return false; }
                return true;
            }
        });

        if (guardado != null) web.restoreState(guardado);
        else cargar();
        recibirLogin(getIntent());
    }

    private void cargar() {
        sinInternet.setVisibility(View.GONE);
        // Sin internet, usa la copia que quedó guardada de la última vez.
        web.getSettings().setCacheMode(hayInternet() ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK);
        web.loadUrl(BuildConfig.WEB);
    }

    private boolean hayInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities nc = cm == null ? null : cm.getNetworkCapabilities(cm.getActiveNetwork());
        return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.evaluateJavascript("window.alVolverApp && alVolverApp()", null);
        Envio.reintentar(this, null);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (sinInternet.getVisibility() == View.VISIBLE) { finish(); return; }
        web.evaluateJavascript("window.atrasApp ? atrasApp() : false", r -> { if (!"true".equals(r)) finish(); });
    }

    // ---------- Entrar con Google ----------
    // Google no deja elegir la cuenta dentro de una app, así que se abre el navegador.
    // La web vuelve con misgastos://login, trayendo el permiso y el mismo código "estado".
    private void entrarConGoogle() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        StringBuilder estado = new StringBuilder();
        for (byte x : b) estado.append(String.format("%02x", x));
        Prefs.guardarEstadoLogin(this, estado.toString());
        abrirFuera(Uri.parse(BuildConfig.WEB + "#app-login=" + estado));
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        recibirLogin(i);
    }

    private void recibirLogin(Intent i) {
        Uri d = i == null ? null : i.getData();
        if (d == null || !"misgastos".equals(d.getScheme())) return;
        setIntent(new Intent());  // que no se repita al girar la pantalla
        String esperado = Prefs.estadoLogin(this);
        Prefs.guardarEstadoLogin(this, "");
        String token = i.getStringExtra("token");
        if (token == null || esperado.isEmpty() || !esperado.equals(i.getStringExtra("estado"))) {
            avisar("No se pudo entrar. Toca «Entrar con Google» otra vez.");
            return;
        }
        pasarToken(token, 0);
    }

    /** La página puede estar cargando todavía: reintenta hasta que esté lista. */
    private void pasarToken(String token, int intentos) {
        String js = "typeof entrarDesdeApp === 'function' ? (entrarDesdeApp(" + JSONObject.quote(token) + "), 'ok') : 'no'";
        web.evaluateJavascript(js, r -> {
            if (!"\"ok\"".equals(r) && intentos < 40) web.postDelayed(() -> pasarToken(token, intentos + 1), 500);
        });
    }

    // ---------- Archivos (copias de seguridad) ----------
    @Override
    protected void onActivityResult(int pedido, int resultado, Intent datos) {
        super.onActivityResult(pedido, resultado, datos);
        if (pedido == ELEGIR_ARCHIVO && eligiendo != null) {
            eligiendo.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultado, datos));
            eligiendo = null;
        } else if (pedido == GUARDAR_ARCHIVO) {
            String texto = porGuardar;
            porGuardar = null;
            if (resultado != RESULT_OK || datos == null || datos.getData() == null || texto == null) return;
            try (OutputStream o = getContentResolver().openOutputStream(datos.getData())) {
                o.write(texto.getBytes(StandardCharsets.UTF_8));
                avisar("Archivo guardado");
            } catch (Exception e) {
                avisar("No se pudo guardar el archivo");
            }
        }
    }

    private void abrirFuera(Uri u) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); }
        catch (ActivityNotFoundException e) { avisar("No hay un navegador para abrir el enlace"); }
    }

    private void avisar(String t) { Toast.makeText(this, t, Toast.LENGTH_LONG).show(); }

    /** Lo que la página web puede pedirle al celular. Corre fuera de la pantalla: lo visual va con runOnUiThread. */
    private class Puente {
        @JavascriptInterface
        public void entrarConGoogle() { runOnUiThread(MainActivity.this::entrarConGoogle); }

        /** La web pasa la dirección del buzón de la cuenta que entró ("" al salir). */
        @JavascriptInterface
        public void guardarBuzon(String url) {
            Context c = MainActivity.this;
            if (url == null) url = "";
            if (!url.isEmpty() && !(url.startsWith("https://firestore.googleapis.com/") && url.contains("/buzon/"))) return;
            if (url.equals(Prefs.url(c))) return;
            Prefs.guardarUrl(c, url);
            if (!url.isEmpty()) Envio.reintentar(c, null);
        }

        @JavascriptInterface
        public String estado() {
            Context c = MainActivity.this;
            try {
                return new JSONObject()
                    .put("permiso", ConfigActivity.tienePermiso(c))
                    .put("bateria", ConfigActivity.bateriaLibre(c))
                    .put("apps", Prefs.apps(c).size())
                    .put("pendientes", Prefs.pendientes(c).length())
                    .toString();
            } catch (JSONException e) {
                return "{}";
            }
        }

        @JavascriptInterface
        public void abrirConfiguracion() {
            runOnUiThread(() -> startActivity(new Intent(MainActivity.this, ConfigActivity.class)));
        }

        /** «Descargar copia» y «Descargar para Excel»: la persona elige dónde guardarlo. */
        @JavascriptInterface
        public void guardarArchivo(String nombre, String texto, String tipo) {
            runOnUiThread(() -> {
                porGuardar = texto;
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(tipo == null || tipo.isEmpty() ? "*/*" : tipo)
                    .putExtra(Intent.EXTRA_TITLE, nombre);
                try { startActivityForResult(i, GUARDAR_ARCHIVO); }
                catch (ActivityNotFoundException e) { porGuardar = null; avisar("No se pudo guardar el archivo"); }
            });
        }
    }
}
