# Historial de Cambios

## 10-10-2026 14:40 [1.0.0]

### Corregido

- **Mantener pulsado el tablero para colocar un barco no hacía nada.** Causa: `BoardView` resolvía
  los gestos de colocación con `detectTapGestures`, donde una pulsación sostenida se clasifica como
  *long press* y nunca llega a ser un *tap*; el `onLongPress` de la pantalla de colocación estaba
  conectado a «quitar un barco ya puesto», así que sobre agua vacía no se ejecutaba ninguna acción
  y además no había ninguna señal en pantalla de que el gesto se hubiera recibido. Reproducido en
  el emulador: toque corto coloca, pulsación sostenida no.

### Cambiado

- **Colocación por arrastre** (`ui/common/BoardView.kt`, `ui/match/PlacementScreen.kt`,
  `ui/match/MatchViewModel.kt`). El tablero acepta ahora dos callbacks nuevos, `onAim(Coord)` y
  `onDrop(Coord?)`, resueltos con `awaitEachGesture` en vez de `detectTapGestures`, de modo que
  tocar, mantener pulsado y arrastrar son **el mismo gesto**: mientras el dedo está apoyado se
  previsualiza la casilla bajo él y al levantarlo se coloca.
  - `MatchViewModel.aimAt()`: si el dedo baja sobre un barco ya colocado, lo levanta y pasa a ser el
    que se arrastra, así que mover un barco ya no obliga a quitarlo primero.
  - `MatchViewModel.dropAt()`: soltar **fuera** del tablero (`onDrop(null)`) devuelve el barco a la
    bandeja y borra su sombra, que es lo que sustituye a la antigua pulsación larga para quitarlo.
  - `removeAt()` desaparece: ya no la usa nadie.
  - El tablero de la partida no cambia: sigue con `onTap` y `detectTapGestures`.
- **Textos**: `placement_hint` (es/en) y el apartado «Colocar tu flota» del manual explican el gesto
  nuevo.

### Comprobado

- En el emulador (API 36), con la flota Clásica: pulsación sostenida sobre agua → aparece la sombra
  verde; arrastre → la sombra sigue al dedo; al soltar → barco colocado; dedo sobre un barco puesto
  → se levanta; soltar fuera del tablero → vuelve a la bandeja y no queda sombra pintada. La
  pantalla de batalla sigue disparando con normalidad (tres disparos, impacto y fallos
  registrados). Pruebas unitarias en verde y APK de publicación sin errores en `logcat -b crash`.
- Sin cambio de versión: la 1.0.0 todavía no está publicada en Google Play, así que no hace falta
  subir el `versionCode`.

## 10-10-2026 13:20 [1.0.0]

### Cambiado

- **Creada la clave de firma de publicación** (`barquitos-release.jks`, alias `barquitos`) y su
  `keystore.properties`, los dos fuera de git. Causa: sin almacén de claves, `assembleRelease` dejaba
  el APK **sin firmar** y no se podía instalar en un móvil ni subir a Google Play. Huellas y aviso de
  copia de seguridad anotados en `store/PLAY_STORE.md` («Clave de firma»).
- **`app/build.gradle.kts`**: la ruta del almacén se resuelve ahora con `rootProject.file(...)` en vez
  de `file(...)`. Causa: dentro del bloque `android {}`, `file()` resuelve sobre `app/`, así que una
  ruta relativa escrita en `keystore.properties` (lo natural, al vivir ese fichero en la raíz) no se
  encontraba.
- **`.gitignore`**: añadidos `/*.jks` y `/*.keystore`, para que el almacén de claves no se pueda subir
  al repositorio por descuido.
- **`store/PLAY_STORE.md`**: tamaño real del APK de publicación (1,49 MB en vez de la estimación de
  ~5 MB) y comando `assembleRelease` para instalar a mano.

### Comprobado

- APK de publicación (R8, recursos encogidos, 1,49 MB) firmado con la clave nueva e **instalado en un
  Pixel 9 Pro XL con Android 17**; arranca sin errores ni en `logcat -b crash` ni en el registro de la
  propia app.

## 10-10-2026 11:30 [1.0.0]

Primera versión del proyecto. Juego de hundir la flota para Android, pensado a partir del conjunto
de reglas y modos de *Fleet Battle* (Smuttlewerk Interactive) —las reglas de un juego de tablero no
son de nadie—, pero con **código, dibujo, sonido y textos propios**: no se ha copiado ni un recurso
de la app de referencia, ni su nombre, ni su marca, ni sus gráficos.

### Añadido

- **Motor de juego** (`core/`, Kotlin puro sin dependencias de Android, probado con pruebas
  unitarias):
  - `Model.kt`: `Coord`, `Orientation` (cuatro giros, porque las formas de la flota Estándar no son
    simétricas), `ShipShape` (forma como lista de casillas relativas, normalizada, con constructor a
    partir de un dibujo en texto), `ShipDef`, `PlacedShip`, `PlayerBoard` (la flota más los disparos
    recibidos; es el estado «de verdad», que el rival nunca ve).
  - `Fleets.kt`: las tres flotas. **Estándar** (portaaviones de 6 con isla de mando, acorazado 4,
    submarino de 4 con torreta, crucero 3, destructor 2, patrullera 2 = 21 casillas), **Clásica**
    (5-4-3-3-2 = 17) y **Rusa** (4, 3×2, 2×3, 1×4 = 20, con separación obligatoria de una casilla,
    diagonales incluidas).
    - Nota: la documentación pública de Fleet Battle dice que en su flota Estándar «el portaaviones y
      el submarino tienen formas especiales», pero no publica cuáles. Las formas de aquí son una
      reconstrucción razonable; están en dos constantes (`PORTAAVIONES_SHAPE`, `SUBMARINO_SHAPE`) y
      se cambian en una línea si se quiere otra cosa.
  - `Rules.kt`: `ShotRule` con las cinco reglas (UN_DISPARO, RACHA, SALVA, FUEGO_EN_CADENA,
    MULTIDISPARO), `Difficulty`, `GameMode` y `GameConfig`.
  - `Placement.kt`: validación de colocación (dentro del tablero, sin solapar y —flota Rusa— sin
    tocarse ni en diagonal) y reparto al azar con reintentos, colocando primero los barcos grandes
    porque son los que menos sitios tienen.
  - `Game.kt`: turnos y aplicación de las cinco reglas. Las secuenciales van con `fire()`; la SALVA
    es simultánea y va con `toggleAim()` / `confirmAim()` / `resolveSalvo()`, de forma que los
    impactos de uno no cambian la salva del otro en la misma ronda y las dos flotas pueden hundirse
    a la vez (empate, `Game.DRAW`). `viewFor(player)` devuelve un `BoardView` con **solo** lo que un
    atacante puede saber, que es lo único que recibe la IA.
  - `GameSave.kt`: guardado y recuperación de la partida en texto plano, sin JSON ni Android, para
    poder probarlo con pruebas unitarias y para que se pueda leer a simple vista al depurar.
- **IA** (`core/ai/ShipAi.kt`), tres niveles, sin acceso al tablero rival:
  - *Fácil*: casi a ciegas, con un 40 % de probabilidad de rematar alrededor de un impacto.
  - *Normal*: búsqueda al tresbolillo y, al tocar, deducción de la línea del barco.
  - *Difícil*: mapa de probabilidad. Para cada casilla sin disparar cuenta de cuántas maneras podría
    estar ahí alguno de los barcos que le quedan al rival, descartando las colocaciones que pisan
    agua conocida o barcos hundidos (y, en la flota Rusa, las que quedarían pegadas a un hundido), y
    multiplicando por 60 por cada impacto sin hundir que la colocación explique. Esto le hace rematar
    los barcos tocados sin necesidad de una lógica aparte y aprovecha las formas raras de la flota
    Estándar.
  - `placeFleet()` reparte la flota de la IA; en Difícil prueba 24 repartos y se queda con el menos
    agrupado (barcos poco pegados y poco arrimados al borde).
- **Registro de eventos** (`log/BarquitosLogger.kt`): fichero de texto con rotación cada 25 KB,
  mismo esquema que `RutasLogger` (Rutas Ayatu) y `AguedaLogger` (Agueda). `BarquitosApplication`
  engancha además un manejador de excepciones no capturadas, para que un cierre inesperado deje
  rastro. Visor en `ui/docs/LogScreen.kt`: autorrefresco, navegación por los ficheros rotados,
  selección de texto, copiar todo y compartir vía `FileProvider`.
- **Datos** (`data/`): `Settings` y `Stats` sobre un único DataStore; `MatchStore` guarda la partida
  a medias en `filesDir/partida.sav` (las de WiFi no se guardan: al salir se corta la conexión);
  `Catalog.kt` con las 14 armadas (bandera descrita como geometría), las 100 pinturas (5 patrones ×
  20 colores), los 12 emblemas, los 22 planos de buques históricos y las 14 medallas.
- **Interfaz** (Jetpack Compose, Material 3): puerto, «Nueva partida», colocación, batalla, traspaso
  del móvil, resultado, astillero, estadísticas, ajustes, manual, novedades y registro.
  - `ui/common/BoardView.kt`: el tablero entero dibujado con Canvas (agua, retícula, rótulos A-J /
    1-10, barcos, impactos, agua deducida, objetivos de salva y animaciones), el mismo componente
    para colocar, jugar y ver el resultado.
  - `ui/common/ShipPainter.kt`: los barcos no son imágenes. Se dibujan por **tramos seguidos** de
    casillas (cada tramo una cápsula, más una punta de proa en el tramo principal), de modo que un
    barco recto sale de una pieza y las casillas que sobresalen quedan pegadas al casco; encima van
    cubierta, torretas y el patrón de pintura, recortados a la silueta.
  - `ui/common/Widgets.kt`: banderas y emblemas también dibujados con Canvas a partir de los datos
    del catálogo.
- **Sonido** (`sound/SoundEngine.kt`): los efectos se **sintetizan** (ruido filtrado con envolvente
  para cañonazos y explosiones, senoidales para los avisos), se escriben como WAV en la caché la
  primera vez y se cargan en un `SoundPool`. El APK no lleva ningún fichero de audio.
- **WiFi local** (`net/`): `LanDiscovery.kt` publica y busca partidas con NSD
  (`_barquitosayatu._tcp`) y `LanLink.kt` lleva la conexión TCP con un protocolo de texto de una
  línea por mensaje (LAYOUT, SHOT, SALVO, SURRENDER, BYE), precedido de un HELLO del anfitrión con
  las reglas. Sin servidor, sin cuenta y sin salir a internet.
- **Documentos vivos**: `Docs/historial.md`, `Docs/novedades.md` y `Docs/manual_usuario.md` se
  empaquetan como assets con la tarea `syncDocs` de Gradle, así que lo que se lee dentro de la app es
  exactamente el fichero del repositorio.
- **Ficha de Google Play**: `store/PLAY_STORE.md`.
- **Pruebas unitarias** (34): formas y giros, composición de las flotas, reglas de colocación
  (incluida la separación obligatoria de la Rusa), las cinco reglas de disparo, detección de
  victoria y de empate en salva, ida y vuelta del guardado, y escalonado de los tres niveles de la
  IA (Difícil barre la flota Clásica en menos de 60 disparos de media y es al menos 10 disparos
  mejor que Fácil).

### Corregido durante el desarrollo

- **La IA reventaba con `Comparison method violates its general contract!`** (`ShipAi.bestByDensity`).
  Causa: el desempate al azar estaba *dentro* del comparador (`thenBy { rng.nextInt() }`), así que el
  comparador no era consistente consigo mismo y TimSort lo detectaba. Arreglado barajando antes y
  ordenando después con un orden estable.
- **`Game.viewFor()` decía que seguían a flote barcos que ya estaban hundidos** cuando el tablero no
  coincidía con la flota de la configuración (caso de las pruebas). Causa: la lista de barcos que
  quedan se sacaba de `config.fleetDef` en vez del propio tablero. Ahora sale de
  `enemyBoard.aliveShips`, que además es lo correcto conceptualmente (no filtra nada: la composición
  de la flota se pacta al empezar).
- **El tablero se quedaba muerto después del turno de la máquina**: no se podía disparar. Causa: en
  `MatchViewModel.maybeRunAi()` se soltaba `busy = false` pero no se volvía a llamar a `refresh()`,
  y de ahí salía el `enabled` del tablero, que se quedaba con el valor viejo. Arreglado llamando a
  `refresh()` al entrar y al salir del turno de la IA y, además, calculando `enabled` en
  `GameScreen` a partir del estado recién llegado, para que esa clase de desfase no pueda repetirse.
- **Con la regla SALVA contra la máquina, si el sorteo daba el inicio al rival el jugador no podía
  apuntar nunca**: el tablero no admitía toques y la partida se quedaba parada. Causa: en SALVA el
  campo `Game.current` no es «de quién es el turno» sino «quién apunta ahora», y `MatchViewModel`
  lo dejaba en el jugador que decía «Quién empieza». En SALVA disparan los dos a la vez, así que
  «quién empieza» solo tiene sentido para decidir quién apunta primero **cuando se comparte el
  móvil**; `beginGame()` fuerza ahora el jugador local como primero en apuntar en un jugador y en
  WiFi local.
- **«1 ships sunk» / «1 impactos»** en el resumen de la salva. Causa: el texto se montaba
  concatenando el número con una cadena fija. Pasado a `plurals` (`salvo_hits`, `salvo_sunk`) en
  español e inglés.
- **Las tablas del manual se veían como una ristra de barras verticales** dentro de la app. Causa:
  Markwon sin la extensión de tablas. Añadida `io.noties.markwon:ext-tables` y registrada en
  `MarkdownView`.
- **El título se metía debajo de la barra de estado** en las pantallas que no usan `Scaffold` (puerto,
  traspaso, espera). Causa: la app va a pantalla completa (`enableEdgeToEdge`) y esas pantallas no
  dejaban sitio a las barras del sistema. Añadido `windowInsetsPadding(WindowInsets.safeDrawing)`.
- **Los barcos se veían como cuentas sueltas** en vez de como un casco. Causa: el casco se dibujaba
  casilla a casilla, cada una con su margen y sus esquinas redondeadas. Rehecho por tramos seguidos
  (ver `ShipPainter`), con la isla de mando y la torreta alargadas medio hueco hacia el casco para
  que queden pegadas a él.
- **El SDK no se encontraba al compilar** (`java.io.IOException: El nombre de archivo... no son
  correctos`). Causa: las barras invertidas escapadas de `local.properties`. Pasado a barras
  normales.

### Pendiente / fuera de alcance de esta versión

- **PvP por internet** (partida rápida con desconocidos, clasificaciones mundiales, torneos y chat):
  haría falta un servidor y una cuenta de usuario, cosa que esta app no tiene. Lo que sí está es el
  juego a dos por WiFi local, que cubre jugar con otra persona sin ningún servicio de por medio.
- **Interfaz solo en español e inglés**; sin traducir a más idiomas.
