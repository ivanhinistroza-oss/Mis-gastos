package pe.misgastos.avisos;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/** Lo que la app recuerda en el celular. */
final class Prefs {
    private Prefs() {}

    static SharedPreferences de(Context c) {
        return c.getSharedPreferences("avisos", Context.MODE_PRIVATE);
    }

    /** Dirección del buzón, copiada desde Ajustes en la app web. */
    static String url(Context c) { return de(c).getString("url", ""); }
    static void guardarUrl(Context c, String url) { de(c).edit().putString("url", url).commit(); }

    /** Apps de las que se leen los avisos (por su nombre de paquete). */
    static Set<String> apps(Context c) { return new HashSet<>(de(c).getStringSet("apps", new HashSet<>())); }
    static boolean appsElegidas(Context c) { return de(c).contains("apps"); }
    static void guardarApps(Context c, Set<String> apps) { de(c).edit().putStringSet("apps", apps).commit(); }

    /** Avisos que aún no se pudieron mandar (por ejemplo, sin internet). */
    static JSONArray pendientes(Context c) { return leer(c, "pendientes"); }
    static void guardarPendientes(Context c, JSONArray a) { de(c).edit().putString("pendientes", a.toString()).commit(); }

    /** Últimos avisos mandados, para mostrarlos en pantalla. */
    static JSONArray registro(Context c) { return leer(c, "registro"); }
    static void anotar(Context c, String texto) {
        JSONArray viejo = registro(c), nuevo = new JSONArray();
        nuevo.put(texto);
        for (int i = 0; i < viejo.length() && i < 14; i++) nuevo.put(viejo.optString(i));
        de(c).edit().putString("registro", nuevo.toString()).commit();
    }

    static String error(Context c) { return de(c).getString("error", ""); }
    static void guardarError(Context c, String e) { de(c).edit().putString("error", e).commit(); }

    static JSONObject aviso(String texto, String fuente) {
        JSONObject o = new JSONObject();
        try { o.put("texto", texto); o.put("fuente", fuente); } catch (JSONException ignorado) {}
        return o;
    }

    private static JSONArray leer(Context c, String clave) {
        try { return new JSONArray(de(c).getString(clave, "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }
}
