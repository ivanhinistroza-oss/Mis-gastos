package pe.misgastos.avisos;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manda los avisos al buzón de Firestore, el mismo que usaría MacroDroid.
 * Todo pasa por una sola fila: primero se guarda el aviso como pendiente y luego se intenta mandar,
 * así no se pierde nada si no hay internet.
 */
final class Envio {
    private Envio() {}

    private static final ExecutorService FILA = Executors.newSingleThreadExecutor();

    static void enviar(Context c, String texto, String fuente) {
        Context app = c.getApplicationContext();
        FILA.execute(() -> {
            JSONArray p = Prefs.pendientes(app);
            p.put(Prefs.aviso(texto, fuente));
            Prefs.guardarPendientes(app, p);
            vaciar(app);
        });
    }

    static void reintentar(Context c, Runnable alTerminar) {
        Context app = c.getApplicationContext();
        FILA.execute(() -> {
            vaciar(app);
            if (alTerminar != null) alTerminar.run();
        });
    }

    /** Manda los pendientes en orden. Se detiene en el primer fallo para reintentar después. */
    private static void vaciar(Context c) {
        String url = Prefs.url(c);
        JSONArray p = Prefs.pendientes(c);
        if (p.length() == 0) return;
        if (url.isEmpty()) { Prefs.guardarError(c, "Falta pegar la dirección del buzón."); return; }
        while (p.length() > 0) {
            JSONObject a = p.optJSONObject(0);
            int codigo = a == null ? 200 : mandar(url, a.optString("texto"), a.optString("fuente", "android"));
            if (codigo < 200 || codigo >= 300) {
                Prefs.guardarError(c, codigo == -1
                    ? "Sin internet. Se mandará cuando vuelva la conexión."
                    : "La dirección del buzón no funciona (error " + codigo + "). Cópiala otra vez desde la app web.");
                return;
            }
            p.remove(0);
            Prefs.guardarPendientes(c, p);
            Prefs.guardarError(c, "");
            if (a != null) Prefs.anotar(c, hora() + "  " + a.optString("texto"));
        }
    }

    /** Devuelve el código HTTP, o -1 si no hubo conexión. */
    static int mandar(String url, String texto, String fuente) {
        HttpURLConnection con = null;
        try {
            JSONObject cuerpo = new JSONObject()
                .put("fields", new JSONObject()
                    .put("texto", new JSONObject().put("stringValue", texto))
                    .put("fuente", new JSONObject().put("stringValue", fuente)));
            byte[] bytes = cuerpo.toString().getBytes(StandardCharsets.UTF_8);
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (OutputStream o = con.getOutputStream()) { o.write(bytes); }
            return con.getResponseCode();
        } catch (IOException | JSONException e) {
            return -1;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    static String hora() {
        return new SimpleDateFormat("dd/MM HH:mm", Locale.US).format(new Date());
    }
}
