# Mis gastos: cómo instalarla

Cada persona entra con su cuenta de Google y ve solo sus propios gastos. Los avisos de Yape y del banco llegan desde el celular, y los correos del banco se leen desde Gmail.

Archivos de esta carpeta:

- `index.html`: la app completa.
- `firestore.rules`: las reglas de seguridad (quién puede ver qué).

---

## Parte 1. Lo que haces tú una sola vez (en la PC)

### 1. Inicio de sesión con Google

En Firebase (proyecto **mis gastos**): **Seguridad → Authentication → Método de acceso → Google →** activa **Habilitar**, elige tu correo de asistencia y toca **Guardar**.

### 2. Reglas de seguridad

**Firestore → Reglas**: borra lo que haya, pega **todo** el contenido de `firestore.rules` y toca **Publicar**.

### 3. Permitir leer Gmail

1. Abre **console.cloud.google.com**. Arriba, elige el proyecto **mis gastos**.
2. En el buscador de arriba escribe **Gmail API**, ábrela y toca **Habilitar**.
3. En el buscador escribe **Pantalla de consentimiento de OAuth** (también aparece como "Google Auth Platform").
   - Nombre de la app: **Mis gastos**. Correo de asistencia: el tuyo.
   - Público: **Externo**.
   - Cuando termines, en **Público**, toca **Publicar app** para que quede **En producción**.

> **Por qué "En producción":** si la dejas "En prueba", solo pueden usar Gmail las personas que anotes a mano, y el permiso se vence cada 7 días.
> Como Google no ha revisado la app, cada persona verá el aviso "Google no verificó esta app" al conectar Gmail, y solo pueden usarla hasta 100 personas. Para familia y amigos está bien.

### 4. Publicar la app en internet (GitHub Pages)

1. En GitHub crea un repositorio **público** llamado `Mis-gastos`.
2. Sube `index.html` (puedes arrastrarlo en **Add file → Upload files**).
3. En el repositorio: **Settings → Pages →** Branch: `main` (o `master`) → carpeta `/ (root)` → **Save**.
4. En un par de minutos, la app queda en **https://ivanhinistroza-oss.github.io/Mis-gastos/**.

### 5. Autorizar esa dirección en Firebase

**Authentication → Configuración → Dominios autorizados → Agregar dominio**: escribe `ivanhinistroza-oss.github.io`.

---

## Parte 2. Cada persona (incluido tú)

1. Abre **https://ivanhinistroza-oss.github.io/Mis-gastos/** en el celular y toca **Entrar con Google**.
2. Ponla en la pantalla de inicio:
   - **Android (Chrome):** menú ⋮ → **Agregar a la pantalla principal**.
   - **iPhone (Safari):** botón **Compartir** → **Agregar a inicio**.
3. **Correos del banco (opcional):** **Ajustes → Conectar Gmail**. Acepta el aviso de Google: toca **Configuración avanzada → Ir a Mis gastos** y marca la casilla de Gmail. Desde ahí, cada vez que abras la app se traen los gastos que llegaron por correo.
   - El permiso de Google dura una hora. Si pasa más tiempo, en **Agregar** aparece el botón **Revisar correos**: tócalo y listo.

### Android: app «Mis gastos avisos»

Es una app pequeña que lee las notificaciones de Yape y de tus bancos y las manda a tu cuenta. No está en la Play Store; se instala desde el enlace.

1. En el celular, abre la app web → **Ajustes → Avisos desde el celular** → **Descargar app para Android**.
2. Abre el archivo descargado (`mis-gastos-avisos.apk`). Android pedirá permiso para **instalar apps desconocidas** desde Chrome: actívalo y toca **Instalar**. Si sale un aviso de Play Protect, toca **Más detalles → Instalar de todas formas**.
3. Vuelve a la app web y toca **Copiar dirección**. Abre **Mis gastos avisos** y sigue sus 4 pasos:
   1. **Dar permiso:** activa **Mis gastos avisos** en "Acceso a notificaciones".
      - Si sale **"Ajuste restringido"** y no te deja activarlo (pasa en Android 13 o más nuevo con apps que no vienen de la Play Store): ve a **Ajustes del celular → Aplicaciones → Mis gastos avisos →** menú **⋮** arriba a la derecha **→ Permitir ajustes restringidos**. Luego vuelve a **Dar permiso**.
   2. **Pegar** la dirección y tocar **Probar**: en unos segundos aparece un gasto de S/ 1.00 en Movimientos.
   3. Revisa que estén marcados **Yape** y las apps de tus bancos (se marcan solos la primera vez).
   4. **Ajustes de batería:** elige **Sin restricciones** o **No optimizar**, para que Android no la apague.
4. Haz un yapeo pequeño de prueba.

Solo salen del celular las notificaciones de las apps marcadas **que traen un monto** (S/ o US$). Si no hay internet, se guardan y se mandan después.

> **Importante para Iván:** la firma de la app está en `C:\Users\IVAN\herramientas-android\firma\` (archivo `mis-gastos.jks` y la clave en `android\firma.properties`). Guarda una copia en un lugar seguro: sin ella, las actualizaciones no se podrán instalar encima de la app y cada persona tendría que desinstalarla primero.

### Android, otra opción: MacroDroid

Si prefieres no instalar la app, MacroDroid hace lo mismo:

1. Instala **MacroDroid** desde la Play Store.
2. En la app de gastos, entra a **Ajustes → Avisos desde el celular**. Ahí usarás dos botones: **Copiar dirección** y **Copiar cuerpo para MacroDroid**.
3. En MacroDroid, toca **Agregar macro**:
   - **Disparador:** Notificaciones → **Notificación recibida** → elige **Yape** (y la app de tu banco si quieres) → contenido: **cualquiera**. Dale el permiso para leer notificaciones.
   - **Acción:** Conectividad → **Solicitud HTTP**:
     - Método: **POST**
     - URL: pega la **dirección**
     - Cuerpo / contenido, tipo **application/json**: pega el **cuerpo**
   - Guarda la macro.
4. El cuerpo trae palabras entre corchetes, como `[notification]` o `[hour]`, que MacroDroid cambia por el texto del aviso y la hora. Si alguna no se cambia, bórrala y vuelve a ponerla con el botón de **texto mágico** de MacroDroid.
5. En los ajustes del celular, entra a **Batería** y quita a MacroDroid del ahorro de batería.
6. Prueba con un yapeo pequeño. Debe aparecer en **Movimientos** en unos segundos.

### iPhone: Yape con la app Atajos

En iPhone ninguna app puede leer las notificaciones de Yape. El atajo lee una captura de la constancia.

1. Abre **Atajos → +** y llámalo **Registrar gasto**. Agrega estas acciones, en orden:
   1. **Obtener las últimas capturas de pantalla** (cantidad 1).
   2. **Extraer texto de** la captura de pantalla.
   3. **Obtener contenido de la URL**:
      - URL: la **dirección** copiada en Ajustes
      - Método: **POST**
      - Cuerpo de la solicitud: **JSON**. Agrega un campo `fields` de tipo **Diccionario**. Dentro, pon:
        - `texto` (Diccionario) → dentro, `stringValue` (Texto) = **Texto extraído**
        - `fuente` (Diccionario) → dentro, `stringValue` (Texto) = `iphone`
   4. **Mostrar notificación**: "Gasto enviado".
2. Para que funcione con un toque: **Ajustes del iPhone → Accesibilidad → Tocar → Tocar atrás → Doble toque →** elige **Registrar gasto**.

Desde ahí, cada vez que yapees: toma una captura de la constancia y da dos golpecitos en la parte de atrás del iPhone.

**SMS del banco, automático:** en **Atajos → Automatización → Nueva → Mensaje**, en "El mensaje contiene" escribe `S/` y elige **Ejecutar inmediatamente**. Usa la misma acción **Obtener contenido de la URL**, con `stringValue` = **Mensaje**.

### Para comprobar que todo está bien

En **Ajustes → Mandar un aviso de prueba** llega un gasto de S/ 1.00 a "Prueba de Mis gastos". Si aparece en Movimientos, el buzón funciona y puedes borrarlo.

---

## Bueno saber

- **Revisa las categorías:** la app las adivina. Si una está mal, tócala en Movimientos y cámbiala; la próxima vez que pagues al mismo lugar, la recordará.
- **Avisos repetidos:** si el mismo gasto llega por la notificación y por el correo, la app guarda uno solo.
- **Sin internet** puedes anotar gastos igual; se suben solos cuando vuelve la conexión.
- **Si alguien conoce tu dirección:** en **Ajustes → Cambiar dirección**, la anterior deja de funcionar. Luego pon la nueva en MacroDroid o Atajos.
- **Quitar el permiso de Gmail:** en **Ajustes → Desconectar Gmail**. Para quitarlo del todo, entra a **myaccount.google.com/permissions**.
- **Abrir el archivo directo** (doble clic en `index.html`): funciona sin cuenta y guarda los datos solo en ese navegador.
