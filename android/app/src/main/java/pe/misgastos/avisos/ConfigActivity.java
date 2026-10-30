package pe.misgastos.avisos;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Pantalla para dejar lista la lectura de avisos: permiso, cuenta, apps y batería. */
public class ConfigActivity extends Activity {
    // Apps que se marcan solas la primera vez, por su nombre.
    private static final Pattern SUGERIDAS = Pattern.compile(
        "yape|plin|bcp|interbank|bbva|scotiabank|banbif|pichincha|banco de la naci|caja |mibanco",
        Pattern.CASE_INSENSITIVE);

    private boolean verTodas;

    static boolean tienePermiso(Context c) {
        String activos = Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        return activos != null && activos.contains(new ComponentName(c, Escucha.class).flattenToString());
    }

    static boolean bateriaLibre(Context c) {
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName());
    }

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        setContentView(R.layout.configuracion);
        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.btnPermiso).setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        findViewById(R.id.btnProbar).setOnClickListener(v -> probar());
        findViewById(R.id.btnBateria).setOnClickListener(v -> pedirBateria());
        findViewById(R.id.btnTodas).setOnClickListener(v -> { verTodas = !verTodas; pintarApps(); });
        findViewById(R.id.btnReintentar).setOnClickListener(v ->
            Envio.reintentar(this, () -> runOnUiThread(this::actualizar)));
        marcarSugeridas();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizar();
        pintarApps();
        Envio.reintentar(this, () -> runOnUiThread(this::actualizar));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void actualizar() {
        boolean permiso = tienePermiso(this);
        boolean cuenta = !Prefs.url(this).isEmpty();
        int apps = Prefs.apps(this).size();
        boolean bateria = bateriaLibre(this);

        etiqueta(R.id.etqPermiso, permiso, "Activado");
        texto(R.id.txtPermiso, permiso
            ? "La app ya puede ver los avisos de Yape y de tus bancos."
            : "Toca «Activar» y prende «Mis gastos» en la lista. Si Android dice «ajuste restringido», "
              + "ve a Ajustes del celular → Aplicaciones → Mis gastos → menú ⋮ → «Permitir ajustes restringidos».");
        ((Button) findViewById(R.id.btnPermiso)).setText(permiso ? "Revisar" : "Activar");
        findViewById(R.id.btnPermiso).setBackgroundResource(permiso ? R.drawable.boton_claro : R.drawable.boton);
        ((Button) findViewById(R.id.btnPermiso)).setTextColor(getColor(permiso ? R.color.morado : android.R.color.white));

        etiqueta(R.id.etqCuenta, cuenta, "Conectada");
        texto(R.id.txtCuenta, cuenta
            ? "Los avisos se anotan en tu cuenta de Mis gastos."
            : "Vuelve a la pantalla principal y entra con tu cuenta de Google.");
        findViewById(R.id.btnProbar).setEnabled(cuenta);
        findViewById(R.id.btnProbar).setAlpha(cuenta ? 1f : .5f);

        etiqueta(R.id.etqApps, apps > 0, apps == 1 ? "1 app" : apps + " apps");

        etiqueta(R.id.etqBateria, bateria, "Listo");
        texto(R.id.txtBateria, bateria
            ? "Android no apagará la app para ahorrar batería."
            : "Si no, Android puede apagarla y algunos avisos no se anotarían. Toca «Permitir» y acepta.");
        findViewById(R.id.btnBateria).setVisibility(bateria ? View.GONE : View.VISIBLE);

        int listos = (permiso ? 1 : 0) + (cuenta ? 1 : 0) + (apps > 0 ? 1 : 0) + (bateria ? 1 : 0);
        texto(R.id.resumenPasos, listos == 4 ? "¡Todo listo!" : listos + " de 4 pasos listos");
        texto(R.id.resumenTexto, listos == 4
            ? "Tus yapeos y compras se anotan solos. Puedes cerrar esta pantalla."
            : "Completa los pasos que faltan para que tus yapeos y compras se anoten solos.");

        int pendientes = Prefs.pendientes(this).length();
        texto(R.id.estadoEnvio, pendientes == 0 ? "" : pendientes + (pendientes == 1 ? " aviso por enviar. " : " avisos por enviar. ") + Prefs.error(this));
        findViewById(R.id.estadoEnvio).setVisibility(pendientes == 0 ? View.GONE : View.VISIBLE);
        findViewById(R.id.btnReintentar).setVisibility(pendientes == 0 ? View.GONE : View.VISIBLE);
        JSONArray r = Prefs.registro(this);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < r.length(); i++) sb.append(r.optString(i)).append("\n\n");
        texto(R.id.registro, r.length() == 0 ? "Todavía no se ha mandado ningún aviso." : sb.toString().trim());
    }

    private void etiqueta(int id, boolean ok, String textoOk) {
        TextView t = findViewById(id);
        t.setText(ok ? "✓ " + textoOk : "Falta");
        t.setTextColor(getColor(ok ? R.color.verde : R.color.naranja));
        t.setBackgroundResource(ok ? R.drawable.etiqueta_ok : R.drawable.etiqueta_falta);
    }

    private void texto(int id, String t) { ((TextView) findViewById(id)).setText(t); }

    private void probar() {
        Envio.enviar(this, "Aviso de prueba desde Android: Yapeaste S/ 1.00 a Prueba de Mis gastos", "prueba");
        avisar("Enviado. En unos segundos aparece en Movimientos.");
        findViewById(R.id.registro).postDelayed(this::actualizar, 3000);
    }

    private void pedirBateria() {
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    /** Apps instaladas que se pueden abrir, ordenadas por nombre (nombre + "\0" + paquete). */
    private TreeMap<String, ResolveInfo> instaladas() {
        PackageManager pm = getPackageManager();
        Intent lanzables = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        TreeMap<String, ResolveInfo> apps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ResolveInfo ri : pm.queryIntentActivities(lanzables, 0)) {
            String paquete = ri.activityInfo.packageName;
            if (!paquete.equals(getPackageName())) apps.put(ri.loadLabel(pm) + "\u0000" + paquete, ri);
        }
        return apps;
    }

    /** La primera vez, marca solas Yape y las apps de bancos. */
    private void marcarSugeridas() {
        if (Prefs.appsElegidas(this)) return;
        Set<String> elegidas = Prefs.apps(this);
        List<String> nombres = new ArrayList<>();
        for (String clave : instaladas().keySet()) {
            String nombre = clave.substring(0, clave.indexOf('\u0000'));
            if (SUGERIDAS.matcher(nombre).find()) { elegidas.add(clave.substring(clave.indexOf('\u0000') + 1)); nombres.add(nombre); }
        }
        Prefs.guardarApps(this, elegidas);
        if (!nombres.isEmpty()) avisar("Marqué: " + String.join(", ", nombres));
    }

    /** Muestra las apps marcadas (o todas), cada una con su ícono y su casilla. */
    private void pintarApps() {
        PackageManager pm = getPackageManager();
        TreeMap<String, ResolveInfo> apps = instaladas();
        Set<String> elegidas = Prefs.apps(this);
        boolean todas = verTodas || elegidas.isEmpty();
        LinearLayout lista = findViewById(R.id.listaApps);
        lista.removeAllViews();
        int alto = Math.round(48 * getResources().getDisplayMetrics().density);
        int icono = Math.round(32 * getResources().getDisplayMetrics().density);
        for (String clave : apps.keySet()) {
            String paquete = clave.substring(clave.indexOf('\u0000') + 1);
            if (!todas && !elegidas.contains(paquete)) continue;

            LinearLayout fila = new LinearLayout(this);
            fila.setGravity(Gravity.CENTER_VERTICAL);
            fila.setMinimumHeight(alto);
            ImageView img = new ImageView(this);
            img.setImageDrawable(apps.get(clave).loadIcon(pm));
            LinearLayout.LayoutParams pi = new LinearLayout.LayoutParams(icono, icono);
            pi.setMarginEnd(icono / 2);
            fila.addView(img, pi);
            TextView nombre = new TextView(this);
            nombre.setText(clave.substring(0, clave.indexOf('\u0000')));
            nombre.setTextSize(15);
            nombre.setTextColor(getColor(R.color.texto));
            fila.addView(nombre, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            CheckBox cb = new CheckBox(this);
            cb.setChecked(elegidas.contains(paquete));
            cb.setOnCheckedChangeListener((b, marcada) -> {
                Set<String> s = Prefs.apps(this);
                if (marcada) s.add(paquete); else s.remove(paquete);
                Prefs.guardarApps(this, s);
                actualizar();
            });
            fila.addView(cb);
            fila.setOnClickListener(v -> cb.toggle());
            lista.addView(fila);
        }
        Button btn = findViewById(R.id.btnTodas);
        btn.setVisibility(elegidas.isEmpty() ? View.GONE : View.VISIBLE);
        btn.setText(verTodas ? "Ver solo las marcadas" : "Elegir otras apps (" + apps.size() + ")");
    }

    private void avisar(String t) { Toast.makeText(this, t, Toast.LENGTH_LONG).show(); }
}
