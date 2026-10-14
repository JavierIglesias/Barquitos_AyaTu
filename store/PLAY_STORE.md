# Ficha de Google Play — Barquitos AyaTu

Documento vivo: **hay que actualizarlo con cada cambio que afecte a la ficha** (funciones, permisos,
datos que se tratan, versión). Al final está la lista de comprobaciones antes de enviar.

---

## 1. Datos técnicos

| Campo | Valor |
|---|---|
| `applicationId` | `com.ayatu.barquitos` |
| `versionName` / `versionCode` | `1.0.0` / `1` |
| `minSdk` | 24 (Android 7.0) |
| `targetSdk` / `compileSdk` | 36 |
| Formato de subida | Android App Bundle (`.aab`) |
| Firma | Almacén `barquitos-release.jks`, alias `barquitos` (ver «Clave de firma») |
| Idiomas | Español (por defecto) e inglés |
| Tamaño real del APK de publicación | **1,47 MB** (no lleva imágenes ni audio: todo se dibuja y se sintetiza) |

Compilar la versión de publicación:

```
./gradlew bundleRelease     # app/build/outputs/bundle/release/app-release.aab  -> Google Play
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk     -> instalar a mano
```

### Clave de firma

| Campo | Valor |
|---|---|
| Almacén | `barquitos-release.jks`, en la raíz del repositorio |
| Alias | `barquitos` |
| Algoritmo | RSA 2048, SHA256withRSA, validez 10.000 días |
| Titular | `CN=Javier Iglesias, OU=AyaTu, O=AyaTu, L=Madrid, C=ES` |
| Huella SHA-256 | `9A:A3:AB:C3:1C:47:B3:3C:1C:72:8E:39:00:40:23:3A:34:1E:E0:91:EC:AB:E3:A9:0F:23:04:05:34:B5:AC:21` |
| Huella SHA-1 | `75:B2:16:E0:56:A7:05:4F:20:D7:82:22:C6:F3:CD:88:D4:8D:24:E1` |

El almacén y sus contraseñas están en `barquitos-release.jks` y `keystore.properties`, los dos
**ignorados por git**: no se suben al repositorio y, por tanto, **no hay copia de seguridad
automática**. Hay que guardar los dos ficheros fuera del equipo. Esta es la clave de **subida**; si se
activa la firma de apps de Google Play, la clave con la que se distribuye la app la custodia Google y
esta solo sirve para subir, de modo que una pérdida se puede resolver pidiendo un restablecimiento de
clave de subida en la consola.

---

## 2. Ficha de la tienda

### Nombre de la app (máx. 30)

```
Barquitos AyaTu
```

### Descripción breve (máx. 80)

```
Hunde la flota de siempre: contra la máquina o contra otro móvil por el mismo WiFi.
```

### Descripción completa (máx. 4000)

```
Barquitos AyaTu es el juego de hundir la flota de toda la vida, el de lápiz y papel, sin nada de lo
que sobra: no hay anuncios, ni compras, ni cuentas, ni rastreo. Lo que juegas se queda en tu móvil.

LA FLOTA DE SIEMPRE

Tablero de 10 x 10 y diez barcos: un portaaviones de 4 casillas, dos acorazados de 3, tres
destructores de 2 y cuatro submarinos de 1. Veinte casillas en total.

Y la regla que lo cambia todo: dos barcos nunca pueden tocarse, tampoco en diagonal. Así que cuando
hundes uno sabes seguro que todo lo que lo rodea es agua, y la app te lo marca para que no gastes
disparos ahí.

DOS REGLAS DE DISPARO

• Un disparo — Un disparo por turno y se pasa, aciertes o no. Es la regla clásica y la que viene
  puesta.
• Continuo — Sigues disparando mientras aciertes. El primer fallo acaba tu turno.

CONTRA QUIÉN

• Contra la máquina, sin conexión de ningún tipo, en tres niveles. En Difícil calcula en cada
  tirada de cuántas maneras podría estar cada barco que te queda en cada casilla y dispara a la más
  probable. No hace trampas: solo ve lo mismo que verías tú.
• Dos jugadores con el mismo WiFi: los dos móviles se encuentran solos en la red de casa y
  juegan directamente, sin servidor, sin cuenta y sin salir a internet.

COLOCAR LA FLOTA ES ARRASTRAR

Toca, mantén pulsado o arrastra, da lo mismo: mientras el dedo está sobre el tablero ves la sombra
del barco, verde si cabe y roja si no, y se coloca al levantarlo. Arrastra un barco ya puesto para
moverlo, o sácalo del tablero para devolverlo a la bandeja. Y si tienes prisa, el botón "Al azar"
reparte la flota entera.

Y ADEMÁS

• La partida se guarda sola: si cierras la app, al volver la sigues donde estaba.
• Estadísticas y 14 medallas.
• Manual de usuario, novedades y registro de la app, dentro de la propia app.
• Aspecto claro u oscuro, con letras y números en el tablero si los quieres.
• Pesa menos de 2 MB: no lleva ni una imagen ni un fichero de sonido, todo lo dibuja y lo sintetiza
  el propio programa.
```

### Textos de la versión (notas de la versión 1.0.0)

```
Primera versión. El barquitos de toda la vida: diez barcos (uno de 4, dos de 3, tres de 2 y cuatro
de 1) que no pueden tocarse, dos reglas de disparo, partidas contra la máquina en tres niveles y
para dos jugadores con el mismo WiFi. Estadísticas y medallas. Sin anuncios, sin compras y sin
conexión.
```

### Categoría y etiquetas

- **Categoría**: Juegos → Tablero (Board).
- **Etiquetas sugeridas**: juego de mesa, estrategia, por turnos, multijugador local, sin conexión, hundir la flota.

---

## 3. Permisos declarados y por qué

| Permiso | Para qué | Si se quita |
|---|---|---|
| `android.permission.INTERNET` | Abrir el socket TCP entre los dos móviles en el modo **Dos Jugadores (con mismo WIFI)**. No se usa para salir a internet: no hay servidor, ni analítica, ni anuncios. | El modo de dos jugadores deja de funcionar. |
| `android.permission.ACCESS_NETWORK_STATE` | Saber si hay red local al publicar o buscar partidas por NSD. | Igual que el anterior. |
| `android.permission.VIBRATE` | Toque corto al acertar y largo al hundir un barco. Se puede desactivar en Ajustes. | Solo se pierde la vibración. |

No se piden: ubicación, cámara, micrófono, contactos, almacenamiento ni identificadores de
publicidad.

---

## 4. Seguridad de los datos (formulario de la consola)

Respuestas que hay que marcar:

- **¿La app recopila o comparte datos de usuario?** → **No**.
- **¿Hay recopilación obligatoria?** → No aplica.
- **¿Los datos se cifran en tránsito?** → No aplica (no se envían datos a ningún servidor).
- **¿Se pueden eliminar los datos?** → Sí: desinstalando la app o, para las estadísticas, con
  «Borrar estadísticas» dentro de la propia app.

Qué guarda la app, todo **solo en el móvil** y sin salir de él:

| Dato | Dónde | Para qué |
|---|---|---|
| Ajustes (sonido, vibración, aspecto…) | DataStore de la app | Preferencias |
| Nombre del jugador y nombre del segundo jugador | DataStore de la app | Mostrarlos en la partida |
| Estadísticas y desbloqueos | DataStore de la app | Marcador y progresión |
| Partida a medias | `filesDir/partida.sav` | Poder seguir la partida |
| Registro de eventos | `filesDir/logs/*.log` | Depuración; el usuario decide si lo comparte |

En el modo de dos jugadores, lo único que viaja entre los dos móviles es: el nombre que hayas puesto, las
reglas de la partida, la colocación de tu flota y tus disparos. Va directamente de un móvil a otro
por la red local.

---

## 5. Clasificación de contenido (cuestionario IARC)

- **Violencia**: ninguna representación realista. Son fichas sobre una cuadrícula; hay explosiones
  esquemáticas dibujadas con figuras geométricas y no hay personas, ni sangre, ni armas realistas.
- **Lenguaje soez, sexo, drogas, juego de azar/apuestas, compras**: no.
- **Interacción con otros usuarios**: **sí, limitada** — en el modo de dos jugadores, dos jugadores de la
  misma red intercambian jugadas. **No hay chat, ni texto libre, ni perfiles, ni contenido
  compartido**; lo único que el rival puede ver es el nombre que el jugador se haya puesto.
- **Comparte ubicación**: no.
- Clasificación esperada: **PEGI 3 / ESRB Everyone**.

---

## 6. Público objetivo y contenido

- **Público objetivo**: todas las edades (de 5 años en adelante).
- Al incluir menores en el público objetivo, hay que confirmar en la consola que la app **no muestra
  anuncios** (es cierto) y que **cumple la política de Familias**: no hay publicidad, ni compras, ni
  recopilación de datos personales, ni enlaces externos dentro del juego.

---

## 7. Gráficos que hay que subir

Todos están hechos y en **`store/graficos/`**. El icono y el gráfico destacado se generan por
programa con la paleta de la app (`Docs/` no lleva imágenes: el script está en el historial del
repositorio); las capturas salen del emulador Pixel 9 Pro con la barra de estado en modo
demostración, en español.

| Recurso | Tamaño | Fichero |
|---|---|---|
| Icono de la app | 512 × 512 PNG, sin transparencia | `store/graficos/icono_512.png` |
| Gráfico destacado | 1024 × 500 PNG, sin transparencia | `store/graficos/destacado_1024x500.png` |
| Captura 1 — pantalla de inicio | 1280 × 2856 | `store/graficos/capturas/01_inicio.png` |
| Captura 2 — nueva partida | 1280 × 2856 | `store/graficos/capturas/02_nueva_partida.png` |
| Captura 3 — colocar la flota | 1280 × 2856 | `store/graficos/capturas/03_colocar_flota.png` |
| Captura 4 — batalla con impactos y hundidos | 1280 × 2856 | `store/graficos/capturas/04_batalla.png` |
| Captura 5 — dos jugadores | 1280 × 2856 | `store/graficos/capturas/05_dos_jugadores.png` |
| Captura 6 — ajustes | 1280 × 2856 | `store/graficos/capturas/06_ajustes.png` |
| Capturas de tablet de 7" y 10" | opcionales | No hechas |
| Vídeo promocional | opcional (YouTube) | No previsto |

Google Play pide **de 2 a 8 capturas de teléfono**, con el lado corto de 320 px como mínimo y el
largo de 3840 px como máximo, y una proporción entre 1:2,4 y 2,4:1. Las seis cumplen (1280 × 2856 es
1:2,23).

Para rehacer las capturas, con el emulador Pixel 9 Pro arrancado:

```
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1000
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
adb shell cmd locale set-app-locales com.ayatu.barquitos --locales es-ES
adb exec-out screencap -p > captura.png
```

---

## 8. Política de privacidad

Google Play exige una URL de política de privacidad aunque la app no recoja ningún dato. La política
está escrita y lista para publicar, en los dos idiomas de la ficha:

| Fichero | Idioma |
|---|---|
| `privacidad.html` (raíz del repositorio) | Español — es la que hay que poner en la consola |
| `privacy.html` (raíz del repositorio) | Inglés, enlazada desde la anterior |

Dice, en resumen: la app no recoge, no envía y no comparte ningún dato personal; lo que guarda
(ajustes, estadísticas, partida a medias, nombre del jugador y registro de la app) se queda en el
almacenamiento privado del móvil; no hay servidores, analítica, publicidad ni bibliotecas de
terceros; en el modo de dos jugadores viajan **directamente de un móvil al otro** el nombre, las
reglas, la colocación de la flota y los disparos; se detallan los tres permisos y para qué sirven; y
se advierte de que la copia de seguridad de Android puede llevar los ajustes al Drive del propio
usuario.

### Cómo publicar la URL (hay que hacerlo en GitHub, una sola vez)

Los dos ficheros están en la **raíz** del repositorio a propósito: GitHub Pages solo puede servir
desde la raíz o desde una carpeta llamada exactamente `docs`, y este proyecto ya tiene una carpeta
`Docs` (con mayúscula), que en Windows sería la misma.

1. En `https://github.com/JavierIglesias/Barquitos_AyaTu` → **Settings** → **Pages**.
2. En *Build and deployment*, **Source: Deploy from a branch**; *Branch:* **`main`**, carpeta
   **`/ (root)`**. **Save**.
3. Al cabo de un par de minutos la política queda publicada en:

```
https://javieriglesias.github.io/Barquitos_AyaTu/privacidad.html
```

- **URL de la política para la consola**:
  `https://javieriglesias.github.io/Barquitos_AyaTu/privacidad.html`
  (pendiente del paso 2: hasta que no se active Pages, la dirección da 404).

---

## 9. Comprobaciones antes de enviar

- [ ] `versionCode` mayor que el de la última subida.
- [ ] `keystore.properties` presente y `./gradlew bundleRelease` termina bien.
- [ ] `Docs/historial.md`, `Docs/novedades.md` y `Docs/manual_usuario.md` al día (entran en el APK
      como assets; la app los enseña).
- [ ] Las notas de la versión de esta ficha coinciden con `Docs/novedades.md`.
- [ ] Probado en un móvil real además del emulador, incluido el modo de dos jugadores con dos
      aparatos.
- [ ] Permisos del manifiesto = los de la tabla del apartado 3.
- [ ] Formulario de seguridad de los datos revisado si ha cambiado algo de lo que guarda la app.
- [ ] Capturas rehechas si ha cambiado la interfaz.
- [ ] URL de política de privacidad accesible (GitHub Pages activado; ver apartado 8).
- [ ] Sonido comprobado en un móvil real con «Probar el sonido», en Ajustes.

---

## 10. Nota legal

El juego reproduce **las reglas** del clásico de hundir la flota y el conjunto de modos y variantes
que popularizó *Fleet Battle* (Smuttlewerk Interactive). Las reglas de un juego no son objeto de
derechos de autor, pero **el nombre, la marca, los gráficos, los textos y el código de esa app sí lo
son**, y aquí no se ha utilizado nada de eso: todo el código, el dibujo (hecho por programa, sin
imágenes), los sonidos (sintetizados) y los textos son propios. No se debe usar en la ficha el
nombre «Fleet Battle» ni sugerir relación alguna con ese producto o su editor.
