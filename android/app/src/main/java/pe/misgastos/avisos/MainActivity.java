package pe.misgastos.avisos;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    // Apps que se marcan solas la primera vez, por su nombre.
    private static final Pattern SUGERIDAS = Pattern.compile(
        "yape|plin|bcp|interbank|bbva|scotiabank|banbif|pichincha|banco de la naci|caja |mibanco",
        Pattern.CASE_INSENSITIVE);

    private TextView estadoPermiso, estadoEnvio, registro;
    private Button btnPermiso;
    private EditText url;

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        setContentView(R.layout.principal);
        estadoPermiso = findViewById(R.id.estadoPermiso);
        estadoEnvio = findViewById(R.id.estadoEnvio);
        registro = findViewById(R.id.registro);
        btnPermiso = findViewById(R.id.btnPermiso);
        url = findViewById(R.id.url);
        url.setText(Prefs.url(this));

        btnPermiso.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        findViewById(R.id.btnPegar).setOnClickListener(v -> pegar());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardarUrl());
        findViewById(R.id.btnPrueba).setOnClickListener(v -> probar());
        findViewById(R.id.btnBateria).setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
        findViewById(R.id.btnReintentar).setOnClickListener(v ->
            Envio.reintentar(this, () -> runOnUiThread(this::actualizar)));
        pintarApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizar();
        Envio.reintentar(this, () -> runOnUiThread(this::actualizar));
    }

    private void actualizar() {
        boolean permiso = tienePermiso();
        estadoPermiso.setText(permiso
            ? "Activado. La app ya puede ver los avisos de Yape y de tus bancos."
            : "Falta el permiso. Toca «Dar permiso» y activa «Mis gastos avisos». "
              + "Si Android no te deja, mira el paso de «ajustes restringidos» en la guía.");
        btnPermiso.setText(permiso ? "Revisar permiso" : "Dar permiso");

        int pendientes = Prefs.pendientes(this).length();
        String error = Prefs.error(this);
        estadoEnvio.setText(pendientes == 0 ? "" : pendientes + " por enviar. " + error);

        JSONArray r = Prefs.registro(this);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < r.length(); i++) sb.append(r.optString(i)).append("\n\n");
        registro.setText(r.length() == 0 ? "Todavía no se ha mandado ningún aviso." : sb.toString().trim());
    }

    private boolean tienePermiso() {
        String activos = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return activos != null && activos.contains(new ComponentName(this, Escucha.class).flattenToString());
    }

    private void pegar() {
        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = cb == null ? null : cb.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0 || clip.getItemAt(0).getText() == null) {
            avisar("No hay nada copiado. Copia la dirección en la app web.");
            return;
        }
        url.setText(clip.getItemAt(0).getText().toString().trim());
        guardarUrl();
    }

    private void guardarUrl() {
        String u = url.getText().toString().trim();
        if (!u.startsWith("https://firestore.googleapis.com/") || !u.contains("/buzon/")) {
            avisar("Esa no es la dirección del buzón. Cópiala desde Ajustes en la app web.");
            return;
        }
        Prefs.guardarUrl(this, u);
        avisar("Dirección guardada");
        Envio.reintentar(this, () -> runOnUiThread(this::actualizar));
    }

    private void probar() {
        if (Prefs.url(this).isEmpty()) { avisar("Primero pega y guarda la dirección"); return; }
        Envio.enviar(this, "Aviso de prueba desde Android: Yapeaste S/ 1.00 a Prueba de Mis gastos", "prueba");
        avisar("Enviando… debe aparecer en Movimientos en unos segundos");
        url.postDelayed(this::actualizar, 3000);
    }

    /** Lista de apps instaladas, ordenadas por nombre, cada una con su casilla. */
    private void pintarApps() {
        PackageManager pm = getPackageManager();
        Intent lanzables = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        TreeMap<String, String> apps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ResolveInfo ri : pm.queryIntentActivities(lanzables, 0)) {
            String paquete = ri.activityInfo.packageName;
            if (paquete.equals(getPackageName())) continue;
            apps.put(ri.loadLabel(pm) + "\u0000" + paquete, paquete);
        }
        boolean primeraVez = !Prefs.appsElegidas(this);
        Set<String> elegidas = Prefs.apps(this);
        List<String> sugeridas = new ArrayList<>();
        LinearLayout lista = findViewById(R.id.listaApps);
        lista.removeAllViews();
        for (String clave : apps.keySet()) {
            String nombre = clave.substring(0, clave.indexOf('\u0000'));
            String paquete = apps.get(clave);
            if (primeraVez && SUGERIDAS.matcher(nombre).find()) { elegidas.add(paquete); sugeridas.add(nombre); }
            CheckBox cb = new CheckBox(this);
            cb.setText(nombre);
            cb.setChecked(elegidas.contains(paquete));
            cb.setOnCheckedChangeListener((b, marcada) -> {
                Set<String> s = Prefs.apps(this);
                if (marcada) s.add(paquete); else s.remove(paquete);
                Prefs.guardarApps(this, s);
            });
            lista.addView(cb);
        }
        if (primeraVez) {
            Prefs.guardarApps(this, elegidas);
            if (!sugeridas.isEmpty()) avisar("Marqué: " + String.join(", ", sugeridas));
        }
    }

    private void avisar(String t) { Toast.makeText(this, t, Toast.LENGTH_LONG).show(); }
}
