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
| Firma | `keystore.properties` en la raíz del repo (fuera de git) |
| Idiomas | Español (por defecto) e inglés |
| Tamaño aproximado | ~5 MB (no lleva imágenes ni audio: todo se dibuja y se sintetiza) |

Compilar la versión de publicación:

```
./gradlew bundleRelease     # app/build/outputs/bundle/release/app-release.aab
```

---

## 2. Ficha de la tienda

### Nombre de la app (máx. 30)

```
Barquitos AyaTu
```

### Descripción breve (máx. 80)

```
Hunde la flota: 3 flotas, 5 reglas, contra la máquina, a dos o por WiFi local.
```

### Descripción completa (máx. 4000)

```
Barquitos AyaTu es el juego de hundir la flota de toda la vida, con todo lo que hace falta para
echar partidas de verdad y sin nada de lo que sobra: no hay anuncios, ni compras, ni cuentas, ni
publicidad, ni rastreo. Lo que juegas se queda en tu móvil.

TRES FLOTAS

• Estándar — Seis barcos, y dos de ellos no son líneas rectas: el portaaviones lleva isla de mando y
  el submarino torreta, así que giran en cuatro posiciones y cuesta mucho más encontrarlos.
• Clásica — La de siempre: barcos de 5, 4, 3, 3 y 2 casillas.
• Rusa — Diez barcos que tienen que dejar siempre una casilla de separación, también en diagonal.
  Cuando hundes uno sabes seguro que todo lo que lo rodea es agua.

CINCO REGLAS DE DISPARO

• Un disparo — Un disparo por turno, turnos alternos.
• Racha — Sigues disparando mientras aciertes.
• Salva — Los dos disparáis a la vez, un disparo por cada barco que os quede a flote.
• Fuego en cadena — Un disparo por barco a flote, pero de uno en uno.
• Multidisparo — De 2 a 6 disparos fijos por turno.

CONTRA QUIÉN

• Contra la máquina, sin conexión, en tres niveles. En Difícil calcula en cada tirada dónde es más
  probable que te queden barcos. No hace trampas: solo ve lo mismo que verías tú.
• A dos en el mismo móvil, con una pantalla de traspaso entre turnos para que nadie vea la flota del
  otro.
• Por WiFi local: dos móviles en la misma red se encuentran solos y juegan directamente, sin
  servidor y sin internet.

Y ADEMÁS

• La partida se guarda sola: si cierras la app, al volver la sigues donde estaba.
• Astillero con 14 armadas, 100 pinturas de casco, 12 emblemas y 22 planos de buques históricos que
  se desbloquean hundiendo barcos.
• Estadísticas completas y 14 medallas.
• Manual de usuario y registro de la app, dentro de la propia app.
• Aspecto claro u oscuro, con letras y números en el tablero si los quieres.
```

### Textos de la versión (notas de la versión 1.0.0)

```
Primera versión. Tres flotas (Estándar, Clásica y Rusa), cinco reglas de disparo, partidas contra la
máquina en tres niveles, a dos en el mismo móvil y por WiFi local. Astillero con pinturas, emblemas
y planos de buques históricos, estadísticas y medallas. Sin anuncios ni compras.
```

### Categoría y etiquetas

- **Categoría**: Juegos → Tablero (Board).
- **Etiquetas sugeridas**: juego de mesa, estrategia, por turnos, multijugador local, sin conexión.

---

## 3. Permisos declarados y por qué

| Permiso | Para qué | Si se quita |
|---|---|---|
| `android.permission.INTERNET` | Abrir el socket TCP entre los dos móviles en el modo **WiFi local**. No se usa para salir a internet: no hay servidor, ni analítica, ni anuncios. | El modo WiFi local deja de funcionar. |
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

En el modo WiFi local, lo único que viaja entre los dos móviles es: el nombre que hayas puesto, las
reglas de la partida, la colocación de tu flota y tus disparos. Va directamente de un móvil a otro
por la red local.

---

## 5. Clasificación de contenido (cuestionario IARC)

- **Violencia**: ninguna representación realista. Son fichas sobre una cuadrícula; hay explosiones
  esquemáticas dibujadas con figuras geométricas y no hay personas, ni sangre, ni armas realistas.
- **Lenguaje soez, sexo, drogas, juego de azar/apuestas, compras**: no.
- **Interacción con otros usuarios**: **sí, limitada** — en el modo WiFi local dos jugadores de la
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

| Recurso | Tamaño | Estado |
|---|---|---|
| Icono de la app | 512 × 512 PNG, 32 bits | **Pendiente**: exportar desde `app/src/main/res/drawable/ic_launcher_foreground.xml` sobre el fondo `#071A2B` |
| Gráfico destacado | 1024 × 500 PNG | **Pendiente** |
| Capturas de teléfono | mín. 2, máx. 8; 16:9 o 9:16, lado corto ≥ 320 px | **Pendiente**: puerto, colocación de la flota, batalla con impactos, resultado, astillero |
| Capturas de tablet de 7" y 10" | opcionales | Pendiente |
| Vídeo promocional | opcional (YouTube) | No previsto |

Para las capturas, el emulador Pixel 9 Pro (1280 × 2856) da la proporción correcta:

```
adb exec-out screencap -p > captura.png
```

---

## 8. Política de privacidad

Google Play exige una URL de política de privacidad aunque la app no recoja datos. Texto mínimo que
sirve (hay que publicarlo en una página accesible, p. ej. en GitHub Pages del repositorio):

```
Barquitos AyaTu no recopila, almacena ni comparte ningún dato personal. No usa cuentas, publicidad,
analítica ni servicios de terceros. Los ajustes, las estadísticas, la partida en curso y el registro
de la aplicación se guardan únicamente en el dispositivo y se eliminan al desinstalarla. En el modo
«WiFi local», el nombre elegido por el jugador, las reglas de la partida y las jugadas se envían
directamente al otro dispositivo de la misma red local, sin pasar por ningún servidor.
Contacto: javier.iglesias10@gmail.com
```

- **URL de la política**: **pendiente de publicar**.

---

## 9. Comprobaciones antes de enviar

- [ ] `versionCode` mayor que el de la última subida.
- [ ] `keystore.properties` presente y `./gradlew bundleRelease` termina bien.
- [ ] `Docs/historial.md`, `Docs/novedades.md` y `Docs/manual_usuario.md` al día (entran en el APK
      como assets; la app los enseña).
- [ ] Las notas de la versión de esta ficha coinciden con `Docs/novedades.md`.
- [ ] Probado en un móvil real además del emulador, incluido el modo WiFi local con dos aparatos.
- [ ] Permisos del manifiesto = los de la tabla del apartado 3.
- [ ] Formulario de seguridad de los datos revisado si ha cambiado algo de lo que guarda la app.
- [ ] Capturas rehechas si ha cambiado la interfaz.
- [ ] URL de política de privacidad accesible.

---

## 10. Nota legal

El juego reproduce **las reglas** del clásico de hundir la flota y el conjunto de modos y variantes
que popularizó *Fleet Battle* (Smuttlewerk Interactive). Las reglas de un juego no son objeto de
derechos de autor, pero **el nombre, la marca, los gráficos, los textos y el código de esa app sí lo
son**, y aquí no se ha utilizado nada de eso: todo el código, el dibujo (hecho por programa, sin
imágenes), los sonidos (sintetizados) y los textos son propios. No se debe usar en la ficha el
nombre «Fleet Battle» ni sugerir relación alguna con ese producto o su editor.
