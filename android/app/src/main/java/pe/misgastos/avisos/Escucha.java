package pe.misgastos.avisos;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Android avisa aquí de cada notificación. Solo se mandan las de las apps elegidas
 * que traen un monto (S/ o US$); el resto se ignora y no sale del celular.
 */
public class Escucha extends NotificationListenerService {
    private static final Pattern MONTO = Pattern.compile("(S/|US\\$|PEN)\\s*\\d");
    private static final long REPETIDO_MS = 10 * 60 * 1000;
    // Yape y los bancos a veces vuelven a publicar la misma notificación; se manda una sola vez.
    private final Map<String, Long> recientes = new HashMap<>();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification n = sbn.getNotification();
        if (n == null || (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        if (!Prefs.apps(this).contains(sbn.getPackageName())) return;

        Bundle x = n.extras;
        CharSequence titulo = x.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence cuerpo = x.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (cuerpo == null) cuerpo = x.getCharSequence(Notification.EXTRA_TEXT);
        // El título va en su propia línea para no mezclarlo con el nombre de quien pagó.
        String texto = ((titulo != null ? titulo + "\n" : "") + (cuerpo != null ? cuerpo : "")).trim();
        if (!MONTO.matcher(texto).find()) return;

        long ahora = System.currentTimeMillis();
        synchronized (recientes) {
            recientes.values().removeIf(t -> ahora - t > REPETIDO_MS);
            String clave = sbn.getPackageName() + "|" + texto;
            if (recientes.containsKey(clave)) return;
            recientes.put(clave, ahora);
        }
        // La fecha y hora del aviso van al final; la app web las reconoce.
        String cuando = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(new Date(sbn.getPostTime()));
        Envio.enviar(this, texto + "\n" + cuando, "android");
    }
}
