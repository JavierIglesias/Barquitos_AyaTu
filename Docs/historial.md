# Historial de Cambios

## 20-10-2026 [1.1.1]

### Cambiado

- **Los aplausos pasan a ser una grabación de verdad** (`app/src/main/res/raw/aplausos.wav`), en
  lugar de sintetizarse. **Causa**: los de 1.1.0 no sonaban creíbles —«no parecen reales», dijo
  Javier—. Y es un límite de la síntesis, no un fallo de los números: medían bien (76 % de la
  energía en banda, 101 transitorios), pero todas las palmadas salían del mismo filtro y del mismo
  ruido, así que al oído era **lluvia**, no público. Lo que delata a un aplauso real no es cuántos
  golpes hay sino que cada palmada suene distinta de la anterior, y eso no se arregla subiendo el
  número de palmadas.
  - Origen: `277021_sandermotions_applause-2.wav` de Wikimedia Commons, de Sandermotions, con
    licencia **CC0** (dominio público: no exige atribución ni ata la ficha de Play a nadie).
  - Preparación del fichero: mezcla a mono, **paso-bajo de ventana a 9,5 kHz** antes de bajar de
    48 000 a 22 050 Hz (sin filtrar, el siseo de las palmadas se plegaría sobre la banda audible),
    recorte de 3,0 s desde el segundo 0,9 —el principio de la grabación está casi vacío—,
    desvanecido de entrada de 30 ms y de salida cuadrático de 600 ms, y normalización al pico.
  - Medido sobre el fichero que va en la app: **3,00 s, pico −0,3 dBFS, rms −19,1 dBFS, 99 % de la
    energía entre 300 Hz y 8 kHz y 162 palmadas distinguibles** (el sintetizado: 76 % y 101).
  - `Sfx` gana `sintetizado` y `SfxSynth.render()` rechaza `APLAUSOS`; `SoundEngine` lo carga con
    `pool.load(context, R.raw.aplausos, 1)`. El `aplausos_v2.wav` de la caché lo borra el propio
    `prepare()`, que ya limpiaba lo que no está en la lista de efectos sintetizados.
  - La app deja de ser «sin un solo fichero de audio»: son 132 KB sobre 1,5 MB. El resto de
    efectos siguen sintetizados.
- **El Registro sale del puerto y se abre con tres dedos desde «Acerca de»**
  (`ui/common/MultiFingerTap.kt`, nuevo; `ui/about/AboutScreen.kt`, `ui/home/HomeScreen.kt`,
  `MainActivity.kt`). Es el mismo gesto y el mismo sitio que en Rutas AyaTu y Audio AyaTu, a
  petición de Javier: el pie del puerto se queda en Manual, Novedades y Acerca de, y quien necesita
  el registro no tiene que acordarse de un gesto distinto en cada app. Al abrirse deja apuntado
  «Registro abierto con 3 dedos», para saber luego por qué se abrió.
  - La regla es **exactamente tres dedos a la vez**, no «al menos tres»: con «al menos» un roce con
    la palma al sujetar el móvil abriría el registro. Y se cuenta el máximo **durante** el gesto,
    no al levantar, porque al soltar nunca se van los tres dedos a la vez.
- **`versionCode` 3 → 4 y `versionName` 1.1.0 → 1.1.1.** La 1.1.0 no llegó a subirse a Play (la
  1.0.0 seguía en revisión), así que queda descartada junto con la 1.0.1.

### Comprobado

- 62 pruebas sin fallos. Dos clases nuevas:
  - `AplausosTest` mide **el fichero del repositorio**: que sea PCM de 16 bits, mono y a 22 050 Hz
    (la misma frecuencia que los sintetizados, para que el `SoundPool` no tenga que remuestrear),
    que dure entre 2 y 4 s, el pico y el rms, que se distingan más de 60 palmadas —si alguien lo
    sustituye por un siseo, salta— y que no empiece ni acabe de golpe.
  - `GestosTest` prueba el contador de dedos: tres dedos abren, uno o dos no, y cuatro tampoco.
- `SoundTest` ya solo recorre los efectos con `Sfx.sintetizado`.

## 18-10-2026 [1.1.0]

### Añadido

- **Aplausos al ganar** (`Sfx.APLAUSOS` en `sound/SfxSynth.kt`), sintetizados como el resto: no hay
  ficheros de audio. Son **190 palmadas sueltas** sorteadas a lo largo de 2,6 s, cada una un
  chasquido corto de ruido de banda centrado en 2 kHz (que es donde suena una palmada de verdad)
  con una caída de 50 ms, más una capa de murmullo de sala por debajo. Los instantes se sortean
  **una sola vez** y se recorren ordenados: con ruido continuo no sonaría a aplauso sino a lluvia,
  porque lo que lo hace reconocible son los transitorios. Entran a los 450 ms del arpegio de
  victoria (`MatchViewModel.finish()`), como en una entrega de premios. Medido sobre el WAV
  generado en el propio Android: pico −0,4 dBFS, rms −20,2 dBFS, **76 %** de la energía entre
  300 Hz y 8 kHz y **101 transitorios** distinguibles.
- **Pantalla «Acerca de»** (`ui/about/AboutScreen.kt`), debajo de Manual / Novedades / Registro en
  la pantalla de inicio: nombre, versión y `versionCode`, marca y modelo del móvil, versión de
  Android con su API, y la invitación a un café con el mismo enlace de Stripe que Rutas AyaTu y
  Sudoku AyaTu. La taza va **centrada** sobre el texto (en Rutas está a la izquierda), que es como
  lo pidió Javier. Ruta `about` en `MainActivity`. El enlace se abre con un `Intent` de navegador
  y, si el móvil no tuviera ninguno, no revienta: se traga la `ActivityNotFoundException`.
- **Novedades estrena icono** en el pie de la pantalla de inicio (`Icons.Default.NewReleases`), que
  era el único de los tres que iba sin él.

### Cambiado

- **«Al puerto» pasa a «Al puerto (reiniciar)»** (`result_home`, es e in), para que se vea que ese
  botón de la pantalla de resultado deja la partida y vuelve al principio.
- **`versionCode` 2 → 3 y `versionName` 1.0.1 → 1.1.0**, por ser funciones nuevas y no solo
  correcciones. El `.aab` 1.0.1 (`versionCode` 2) **no llegó a subirse a Play** —la 1.0.0 seguía en
  revisión—, así que queda descartado y se sube este.

### Comprobado

- 52 pruebas sin fallos. `SoundTest` recorre `Sfx.entries`, así que los aplausos pasan por los
  mismos tres límites que el resto (pico, rms útil y fracción en banda) sin tocar nada.
- En el emulador: la pantalla «Acerca de» con la taza centrada, y el pie de inicio con los cuatro
  accesos.

## 17-10-2026 23:30 [1.0.1]

### Corregido

- **Una partida creada en un móvil no la veía el otro, y al revés sí.** Javier lo probó con dos
  móviles de verdad: publicaba la partida, el contrincante no la veía nunca, y en cambio sí podía
  unirse a la que creaba el contrincante. Dos causas, una encima de la otra:
  - **Todo el descubrimiento dependia de NSD, y NSD falla distinto en cada móvil y cada router.**
    Es mDNS por multicast: basta con que un móvil no reciba el multicast (ahorro de energía del
    WiFi, banda distinta, aislamiento de clientes del router) para que no vea nada, aunque el otro
    lado esté publicando bien. Ahora hay **un segundo camino en paralelo**, `net/LanBeacon.kt`:
    el anfitrión manda un paquete UDP de **difusión** (no multicast) cada segundo a todas las
    redes del móvil, con el identificador de la partida y el puerto; el que busca escucha en el
    puerto 48101 y se queda con la dirección del remitente, así que **no hay que resolver nada**
    (se salta también `resolveService`, que era otro punto de fallo). Con que funcione uno de los
    dos caminos, la partida aparece. No hace falta ningún permiso nuevo: la difusión UDP va con el
    `INTERNET` que ya estaba.
  - **La app no se enteraba de nada de lo que le decía NSD.** Las seis respuestas de
    `NsdManager.RegistrationListener` y `DiscoveryListener` en `net/LanDiscovery.kt` estaban
    escritas como `= Unit`: si `registerService` fallaba, `onRegistrationFailed` se tragaba el
    código de error y la pantalla seguía diciendo «Esperando a que alguien se una…» para siempre,
    sin un aviso ni una línea en el registro. Ahora **todas** escriben en el registro, con el
    código de error traducido. Comprobado en el emulador: al publicar, NSD **no llama nunca** a
    `onServiceRegistered` —la publicación no llega a completarse y nadie se enteraba— mientras que
    la baliza sale puntualmente cada segundo.
  - Además, cada partida lleva ahora un **identificador corto propio** (`BarquitosAyaTu#a3f1 ...`),
    que sirve para no repetir nombre entre dos móviles, para agrupar la misma partida vista por los
    dos caminos y para seguirla en el registro.
- **Volverse atrás mientras se esperaba un rival escribía un ERROR en el registro** («fallo al
  publicar la partida: Socket closed»). Causa: cancelar cierra el `ServerSocket` y eso hace que
  `accept()` lance, y el fallo se trataba igual que uno de verdad. `LanHost` expone ahora
  `cancelled` y `WifiViewModel` distingue las dos cosas.

### Añadido

- **Aviso en pantalla si al cabo de 20 segundos no aparece nadie** (`wifi_waiting_slow`), tanto
  esperando como buscando: que los dos móviles tienen que estar en la misma red WiFi —no uno con
  datos móviles ni en la red de invitados— y que en Ajustes → Registro queda apuntado lo que ha
  pasado.
- 5 pruebas nuevas de la baliza (`BeaconTest` en `NetTest.kt`): ida y vuelta del anuncio, nombres
  con la barra separadora, y que **cualquier basura que llegue al puerto se descarta sin reventar**,
  que es obligado en un puerto UDP abierto a toda la red. Total: **52 pruebas**.

### Comprobado

- La difusión UDP llega de verdad: mandada desde el PC (192.168.1.135) a 192.168.1.255 y a
  255.255.255.255, y recibida íntegra en el Pixel 9 Pro XL (192.168.1.23) escuchando con
  `toybox nc -u -l -p 48101`.
- En el emulador, con la app: al buscar aparece «escuchando balizas en el puerto 48101» y una
  partida inyectada a mano sale en la lista en dos segundos; al crear, la app manda
  `BQ1|<id>|<nombre>|<puerto>` cada segundo a dos destinos de difusión.

### Cambiado

- **`versionCode` 1 → 2 y `versionName` 1.0.0 → 1.0.1.** Causa: el bundle con el `versionCode` 1
  ya estaba en manos de Javier para subirlo a Play, y un `versionCode` que se ha subido a una
  pista no se puede reutilizar. Actualizado también el apartado 1 de `store/PLAY_STORE.md`.

### Pendiente de comprobar

- Falta la prueba de punta a punta **entre los dos móviles de Javier**, que es la que fallaba. Aquí
  solo se puede probar cada mitad por separado: el emulador no comparte red con el móvil.

## 15-10-2026 [1.0.0]

### Documentación

- **`store/PLAY_STORE.md`: añadido al apartado 2 un mapa de en qué pantalla de Play Console va cada
  texto.** Causa: el documento tenía los textos listos pero no dónde pegarlos, y están repartidos
  entre tres sitios que no se parecen —la ficha de Play principal (nombre, descripciones,
  gráficos), la versión que se sube (notas de la versión) y Contenido de la app (URL de la
  política)—, más la configuración de la ficha para la categoría y las traducciones. Recogidos
  también el «Guardar» + «Enviar para revisión», que la ficha es por idioma, y el formato
  `<es-ES>...</es-ES>` de las notas de la versión.
- **Compilada y verificada la versión firmada 1.0.0 (`versionCode` 1) para subir a Play.** El
  `.aab` y el `.apk` se dejan con nombre de versión en `store/publicar/`, carpeta añadida a
  `.gitignore` porque son binarios que se regeneran con `./gradlew bundleRelease`. Comprobado:
  47 pruebas sin fallos, `jarsigner -verify` da «jar verified» sobre el bundle, el APK verifica con
  el esquema v2 y la huella SHA-256 del certificado coincide con la documentada en el apartado 1 de
  `store/PLAY_STORE.md` (`9A:A3:AB:...:AC:21`), y el manifiesto declara `versionName` 1.0.0,
  `minSdk` 24 y `targetSdk` 36.

## 14-10-2026 22:30 [1.0.0]

### Corregido

- **No se oía ningún sonido.** No era que no sonara: sonaba, pero a nada. Medidos los WAV que la app
  genera en la caché del móvil (`SoundEngine.prepare()` → `cache/sfx/*.wav`), los efectos salían
  entre **-31 y -18 dBFS** de valor eficaz y, peor aún, con casi toda su energía **por debajo de
  300 Hz**, que es donde el altavoz de un móvil no reproduce nada: el cuerpo del cañonazo estaba en
  90-45 Hz, el del impacto en 70 Hz y el del hundimiento en 55 Hz. Encima, los dos efectos que más
  suenan se reproducían atenuados: el agua (cada disparo fallado) al 30 % y el toque de interfaz al
  40 %. En conjunto, el agua llegaba al altavoz a unos **-42 dBFS**, alrededor del 1 % de la escala:
  inaudible.
  - Resintetizados los siete efectos para el altavoz de un móvil. El cuerpo de los golpes se sube a
    la zona de 170-520 Hz, el ruido se tiñe con un filtro de banda (variable de estado de
    Chamberlin) en vez de repartirse por todo el espectro, y **cada efecto se normaliza a un pico
    fijo**, que es lo que evita que uno acabe veinte decibelios por debajo de otro según cómo se
    mezclen sus componentes. Medido sobre los ficheros generados en el propio móvil, ahora van de
    **-24 a -13 dBFS** y entre el **80 y el 100 %** de su energía cae en la banda que el altavoz
    reproduce (antes, el cañonazo tenía ahí el 18 % y el impacto el 33 %).
  - Subido el volumen en los puntos de llamada: el agua y el toque pasan de 0,3 y 0,4 a 1,0.
  - `GENERATION` pasa a 2 para regenerar la caché, y `prepare()` borra los WAV de generaciones
    anteriores en vez de dejarlos ocupando sitio.
- **El cañonazo (`Sfx.DISPARO`) estaba sintetizado y no sonaba nunca.** No había ni una llamada a
  `play(Sfx.DISPARO)` en todo el proyecto. Ahora suena en cada disparo, con el resultado encima.
- **Los ajustes de sonido y vibración no se aplicaban hasta reiniciar la app.** Causa:
  `MatchViewModel` leía los ajustes **una sola vez** con `settingsStore.settings.first()` en su
  `init`. Ahora el motor de sonido vive en `BarquitosApplication` y sigue el flujo de ajustes con
  `collect`, así que el interruptor vale en cuanto se toca. Lo mismo para «Marcar agua al hundir» y
  «Confirmar disparo» dentro de `MatchViewModel`.
- **Publicación insegura del mapa de efectos entre hilos.** `SoundEngine.ids` era un `HashMap`
  mutable escrito desde el hilo de disco y leído desde el hilo principal, sin barrera de memoria.
  Ahora es un mapa inmutable detrás de una referencia `@Volatile`.

### Añadido

- **`sound/SfxSynth.kt`**: la síntesis se separa de Android para poder **medirla en las pruebas**.
  `SoundEngine` se queda solo con el `SoundPool`, el vibrador y la caché.
- **`SoundTest`** (6 pruebas): mide con una FFT propia el pico, el valor eficaz y qué fracción de la
  energía cae entre 300 Hz y 8 kHz, y falla si un efecto baja de -26 dBFS útiles o deja menos del
  40 % de su energía en esa banda. Es la prueba que habría cazado este fallo.
- **Botón «Probar el sonido»** en Ajustes → Sonido y vibración: suena un impacto sin tener que
  empezar una partida, con un aviso de que si no se oye es el volumen multimedia del móvil.
- **Política de privacidad** publicable: `privacidad.html` y `privacy.html` en la raíz del
  repositorio, para servirlas con GitHub Pages.
- **Gráficos de Google Play** en `store/graficos/`: icono de 512×512, gráfico destacado de 1024×500
  (generados por programa con la paleta de la app) y seis capturas de 1280×2856 tomadas del emulador
  con la barra de estado en modo demostración.

### Cambiado

- **La regla «Racha» pasa a llamarse «Continuo»**, y las dos reglas cambian de orden y de valor por
  defecto: ahora se ofrece primero **«Un disparo»**, que además es la que viene puesta
  (`GameConfig.shotRule`), y el panel «Regla de disparo» de Nueva partida arranca **contraído**. El
  orden de las constantes de `ShotRule` es el orden en que se ofrecen, así que basta con ponerlas en
  ese orden: `UN_DISPARO` y luego `CONTINUO`. Renombradas también la constante del enum
  (`RACHA` → `CONTINUO`) y las cadenas (`rule_streak` → `rule_continuous`), para que el código no
  siga llamando a las cosas por un nombre que ya no se ve en pantalla. Como el nombre del enum es lo
  que se guarda en los ajustes y en las estadísticas, un `last_rule` o un récord de la versión
  anterior con el valor `RACHA` se ignora y se vuelve al valor por defecto; en una app sin publicar
  (`versionCode 1`) no afecta a nadie.
- **«WiFi local» pasa a llamarse «Dos Jugadores (con mismo WIFI)»** en la pantalla de inicio, a dos
  líneas, con «Uno crea la partida y el otro se une» debajo. El título de la pantalla queda en «Dos
  jugadores», que es lo que cabe en la barra superior. Cadenas `home_wifi`, `home_wifi_desc`,
  `mode_wifi` y `wifi_title`, en español y en inglés.

### Pendiente de comprobar

- El arreglo del WiFi del 10-10-2026 sigue sin poder probarse aquí de punta a punta (dos emuladores
  no se ven por NSD). Necesita dos móviles reales.

## 10-10-2026 22:15 [1.0.0]

### Corregido

- **Por WiFi local no se podía disparar una vez empezada la partida.** Causa: «quién empieza»
  viajaba en el saludo como «al azar» (`startingPlayer = null`, que es el valor por defecto de la
  pantalla de nueva partida), y **cada móvil echaba su propio dado** en
  `MatchViewModel.beginGame()`. Como los dos montan su propia partida a partir de la misma
  configuración, el sorteo salía distinto la mitad de las veces: en una cuarta parte de las
  partidas los dos creían que le tocaba al otro y ningún tablero admitía toques, y en otra cuarta
  parte creían los dos que les tocaba a ellos y los disparos se cruzaban.
  - Arreglado sorteando **una sola vez y en el anfitrión**, antes de publicar la partida:
    `GameConfig.resolvedForHosting()` (en `core/Rules.kt`, función pura para poder probarla) y
    `WifiViewModel.startHosting()`. Lo que sale a la red lleva ya un 0 o un 1.
  - Red de seguridad en `beginGame()`: si por lo que fuera llegara sin decidir en una partida de
    WiFi, empieza el anfitrión en vez de sortear, que es preferible a desincronizarse.
  - El registro de la app anota ahora quién abre fuego en los dos móviles, para poder comprobarlo
    desde dentro si vuelve a pasar algo parecido.
- **En un plegable abierto el tablero se comía la pantalla** y había que bajar a buscar la bandeja
  y los botones (visto en un Samsung Fold 5). Causa: el tablero era `fillMaxWidth().aspectRatio(1f)`
  sin tope, así que en una pantalla ancha y casi cuadrada su lado era el ancho entero y dejaba
  fuera todo lo demás. Añadido `BoardView(maxSize = ...)`: la pantalla de colocación lo limita al
  48 % de la altura y la de batalla al 50 %, y el tablero se centra. En móviles normales no cambia
  nada, porque ahí manda el ancho.

### Añadido

- **Pruebas del WiFi local** (`app/src/test/.../NetTest.kt`, 6 pruebas): que el anfitrión nunca
  publica un «quién empieza» sin decidir, que no lo toca si ya estaba elegido, que **los dos
  móviles acaban creyendo lo mismo** tras el saludo, y el ida y vuelta del protocolo (saludo,
  disparo, flota, rendición y despedida), incluido que un saludo roto no revienta. Total: 40.

### Pendiente de comprobar

- El WiFi local no se puede probar de verdad aquí: dos emuladores no se ven entre sí por NSD y el
  móvil está en otra red. El arreglo está razonado y cubierto por pruebas, pero **hay que
  confirmarlo con los dos móviles**.

## 10-10-2026 21:00 [1.0.0]

### Cambiado

- **La colocación se hace arrastrando desde la bandeja**, que es el gesto que pedía Javier:
  mantener pulsado el botón del barco, arrastrar hasta el tablero y soltar. Ficheros implicados:
  - `ui/common/Widgets.kt` → `ShipChip` pasa de ser un botón por **barco** a un botón por **clase**,
    con el número de los que quedan por colocar, y coge el gesto con
    `detectDragGesturesAfterLongPress`. Informa de la posición del dedo en coordenadas de la
    ventana (`positionInRoot` del propio botón más la posición local del toque).
  - `ui/common/BoardView.kt` → nueva clase `BoardHitTest` y parámetro `onGeometry`: el tablero
    dice dónde ha quedado en pantalla para que la pantalla de colocación pueda traducir un dedo
    que **viene de fuera** a una casilla. Hacía falta porque el gesto empieza en la bandeja, que
    es otro componente, y `pointerInput` solo ve lo que pasa dentro del suyo.
  - `ui/match/PlacementScreen.kt` → un `Box` envuelve la pantalla y dibuja el **barco fantasma**
    pegado al dedo por encima de todo lo demás, un poco por encima del punto de contacto para que
    no lo tape el propio dedo.
- **El barco recién colocado se queda elegido y se pinta en rojo** (`BoardRender.selectedShipId`,
  `ShipPainter.drawShip(selected = ...)`), que es la señal de «este es el que va a girar».
- **`rotate()` gira el barco colocado en su sitio**, sobre su propio centro, en vez de limitarse a
  cambiar la orientación del siguiente que se coloque. Si girado no cabe, intenta meterlo dentro
  del tablero y, si tampoco, avisa con el sonido de agua y lo deja como estaba.
- **Gestos del tablero repartidos en tres** (`onPress` / `onMove` / `onRelease`, antes `onAim` y
  `onDrop`), que es lo que permite distinguir **tocar** un barco puesto (lo elige, para girarlo) de
  **arrastrarlo** (lo mueve). El barco solo se levanta cuando el dedo cambia de casilla.
- Al empezar a colocar **no hay nada preseleccionado** (`selectedShipId = null` en `startNew`):
  antes el primer barco salía con el recuadro de elegido sin que nadie lo hubiera tocado, y ya no
  significa nada porque el barco se elige al sacarlo.
- Textos de `placement_hint` (es/en) y apartado «Colocar tu flota» del manual, reescritos para el
  gesto nuevo.

### Comprobado

En el emulador (API 36), con la app recién instalada y sin datos: bandeja con los cuatro botones y
sus contadores (1, 2, 3, 4); arrastre del Acorazado hasta D5-F5 con barco pegado al dedo y sombra
verde; al soltar, barco en rojo y contador de 2 a 1; «Girar» lo pone vertical en E4-E6, sobre su
centro; arrastre de un barco ya puesto de B1-E1 a F7-I7; un toque en él lo vuelve a poner en rojo
sin moverlo. 33 pruebas en verde.

## 10-10-2026 19:30 [1.0.0]

Poda grande: el juego pasa de «todo lo que hace Fleet Battle» a **el barquitos de toda la vida**,
a petición de Javier. De 8220 líneas de Kotlin a 6539, y el APK de publicación de 1,49 MB a
1,47 MB.

### Quitado

- **Las flotas Estándar y Clásica**, y con ellas el enumerado `FleetType`, las formas no rectas
  (`shapeOf`, `ShipShape.isStraight`) y la elección de flota en la pantalla de nueva partida, en
  el guardado (`GameSave`), en el saludo de red (`NetCodec.encodeHello`) y en las estadísticas.
  Queda una sola flota, la que era «Rusa»: 1×4, 2×3, 3×2 y 4×1, veinte casillas, con separación
  obligatoria. `Fleet` pasa de `data class` con tres instancias a `object` con constantes
  (`BOARD_SIZE`, `MUST_SEPARATE`), de modo que `Placement` y `ShipAi` ya no reciben la flota por
  parámetro.
- **Los nombres de barco que sobraban**: `ShipKind` se queda en PORTAAVIONES (4), ACORAZADO (3),
  DESTRUCTOR (2) y SUBMARINO (1). Fuera CRUCERO, PATRULLERA y LANCHA.
- **Tres reglas de disparo**: SALVA, FUEGO_EN_CADENA y MULTIDISPARO. Con SALVA se va **toda la
  maquinaria de turnos simultáneos**, que era la parte más enrevesada del motor: `Game.toggleAim`,
  `setAim`, `confirmAim`, `resolveSalvo`, `salvoSize`, `SalvoRound`, `Game.DRAW` y el empate,
  `ShotRule.isSimultaneous`/`scalesWithFleet`, `GameConfig.multiShotCount`, `Game.shotsLeft`,
  `ShipAi.chooseShots`, `NetMessage.Salvo`, el diálogo de resumen de salva y los `plurals`
  `salvo_hits`/`salvo_sunk`. También desaparece `MatchResult.EMPATADA` y el contador de empates.
- **El modo de dos jugadores en el mismo móvil** (`GameMode.DOS_JUGADORES`): con él se van la fase
  `MatchPhase.TRASPASO`, `HandoffScreen`, `handoffDone()`, `handleTurnChange()`, `placingPlayer`,
  el segundo nombre de jugador y el contador `hotseatPlayed` de las estadísticas.
- **El astillero entero** (`ui/shipyard/`, 318 líneas) y su catálogo: 14 armadas con sus banderas
  geométricas, 100 pinturas, 12 emblemas y 22 planos históricos. De `Catalog.kt` (359 líneas)
  quedan 60: los colores de los barcos y las medallas. Con ello se van `FlagView`, `EmblemView` y
  `ShipPainter.drawPattern`, y el parámetro `skin` desaparece de `BoardView`, `ShipPainter`,
  `ShipChip` y las cuatro pantallas de partida.
- **`Orientation`** pasa de cuatro valores (NORTE/ESTE/SUR/OESTE) a dos (HORIZONTAL/VERTICAL):
  todos los barcos son líneas rectas, así que la mitad de los giros daba el mismo dibujo.

### Añadido

- **`OptionGroup`** (`ui/common/Widgets.kt`): grupo de opciones plegable con icono, título,
  resumen de lo elegido y chevron. Es el mismo patrón de `SettingsSection` de Radio AyaTu
  (`com.ayatu.audio`) y Rutas AyaTu, para que las tres apps se manejen igual. Lo usan la pantalla
  de **Nueva partida** (reglas, nivel, quién empieza, nombre) y la de **Ajustes** (partida,
  tablero, sonido, aspecto, nombre, acerca de).
- **Medallas recalibradas** para esta flota: con 20 casillas de barco sobre 100, el 50 % y el 65 %
  de acierto de antes eran inalcanzables. Ahora son 35 % y 45 %, y «victoria limpia» pasa de 40 a
  50 disparos. Fuera «Las tres flotas», entra «Cazaflotas» (100 barcos hundidos), y «Todas las
  reglas» pasa a ser «Las dos reglas».

### Corregido

- **La IA en nivel Normal jugaba peor que en Fácil** con esta flota: 90,1 disparos de media frente
  a 83,8 en una tanda de doce partidas. Causa: `parityCells` elegía el paso del tresbolillo a
  partir del barco más pequeño que le quedaba al rival, y aquí hay submarinos de **una** casilla,
  con lo que el paso era 1 y el filtro no descartaba nada; Normal acababa buscando tan a ciegas
  como Fácil, sin ninguna ventaja que compensara. Arreglado dándole a Normal la deducción que hace
  cualquier humano con esta flota: `ShipAi.knownWater()` descarta las ocho casillas que rodean a
  un barco hundido, porque la regla de separación garantiza que son agua. `parityCells` devuelve
  además lista vacía cuando al rival solo le quedan barcos de una casilla, que es cuando el
  tresbolillo no vale para nada. El nivel Fácil se queda como estaba, sin deducir nada.
- **Texto desactualizado** en «Marcar agua al hundir» («En la flota Rusa…») y en el aviso de la
  pantalla de colocación. Causa: hablaban de una flota que ya no existe.

### Cambiado

- `GameSave.VERSION` pasa de 1 a 2: el formato pierde los campos `fleet`, `multi` y `shotsLeft`.
  Las partidas guardadas con la versión anterior se descartan en silencio, que es lo correcto
  porque además la flota ha cambiado.
- Pruebas unitarias rehechas: 33 en verde (antes 34). Cubren la composición exacta de la flota
  (1×4, 2×3, 3×2, 4×1), la separación obligatoria en los cuatro sentidos y en diagonal, que el
  reparto al azar la respeta con cincuenta semillas distintas, las dos reglas de disparo, el ida y
  vuelta del guardado, y que la IA Difícil barre la flota en menos de 80 disparos de media y queda
  por delante de Normal, que a su vez queda por delante de Fácil.

### Comprobado

- En el emulador (API 36): puerto, nueva partida con los grupos plegables, colocación manual y al
  azar con la flota de diez barcos, batalla completa con impactos, hundimientos y marcado
  automático del agua alrededor de los submarinos hundidos, y Ajustes con los seis grupos. Sin
  nada en `logcat -b crash`.
- Sin cambio de versión: la 1.0.0 sigue sin publicarse en Google Play.

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
