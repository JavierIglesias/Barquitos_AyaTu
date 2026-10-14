# Barquitos AyaTu

Juego de hundir la flota para Android: el de lápiz y papel de toda la vida. Diez barcos (uno de 4,
dos de 3, tres de 2 y cuatro de 1) que no pueden tocarse ni en diagonal, dos reglas de disparo,
contra la máquina (tres niveles) o contra otro móvil con el mismo WiFi. Sin anuncios, sin compras,
sin
cuentas y sin conexión a ningún servidor.

![versión](https://img.shields.io/badge/versi%C3%B3n-1.0.0-blue) ![minSdk](https://img.shields.io/badge/minSdk-24-green)

## Documentación

| Documento | Para qué |
|---|---|
| [`Docs/manual_usuario.md`](Docs/manual_usuario.md) | Manual de usuario (se ve dentro de la app) |
| [`Docs/novedades.md`](Docs/novedades.md) | Qué cambia en cada versión, en lenguaje normal (se ve dentro de la app) |
| [`Docs/historial.md`](Docs/historial.md) | Historial técnico: cambios, causa de los fallos y ficheros implicados |
| [`store/PLAY_STORE.md`](store/PLAY_STORE.md) | Ficha de Google Play: textos, permisos, datos, gráficos, comprobaciones |
| [`privacidad.html`](privacidad.html) · [`privacy.html`](privacy.html) | Política de privacidad, para servir con GitHub Pages |

Los tres documentos de `Docs/` se empaquetan como assets con la tarea `syncDocs` de Gradle, así que
lo que se lee dentro de la app es exactamente el fichero de este repositorio.

## Cómo está montado

```
app/src/main/kotlin/com/ayatu/barquitos/
├── core/              Motor del juego. Kotlin puro, sin Android: se prueba con pruebas unitarias.
│   ├── Model.kt       Coord, ShipShape, PlacedShip, PlayerBoard
│   ├── Fleets.kt      La flota: 1×4, 2×3, 3×2, 4×1, con separación obligatoria
│   ├── Rules.kt       Las dos reglas de disparo y la configuración de partida
│   ├── Placement.kt   Qué colocaciones son legales y reparto al azar
│   ├── Game.kt        Turnos, disparos y victoria
│   ├── GameSave.kt    Guardado de la partida en texto plano
│   └── ai/ShipAi.kt   La IA (fácil / normal / difícil por mapa de probabilidad)
├── data/              Ajustes, estadísticas, partida guardada y medallas
├── log/               Registro de eventos en fichero, con rotación
├── net/               Dos jugadores con el mismo WiFi: descubrimiento NSD y conexión TCP
├── sound/             Efectos sintetizados (no hay ficheros de audio)
└── ui/                Pantallas en Jetpack Compose
```

Dos detalles de los que depende todo lo visual:

- **No hay imágenes de barcos**. `ui/common/ShipPainter.kt` los dibuja con Canvas a partir de su
  forma, y `ui/common/BoardView.kt` pinta el tablero entero igual: agua, retícula, impactos y
  animaciones.
- **No hay ficheros de audio**. `sound/SfxSynth.kt` sintetiza los efectos —sin nada de Android, para
  poder medirlos en las pruebas— y `sound/SoundEngine.kt` los escribe como WAV en la caché la
  primera vez que arranca la app y los reproduce con un `SoundPool`. Están pensados para el
  altavoz de un móvil, que no da graves: `SoundTest` mide con una FFT que cada efecto llega con
  nivel suficiente y que su energía cae donde ese altavoz sí reproduce.

## Compilar

Hace falta JDK 17 y el SDK de Android (`local.properties` → `sdk.dir`).

```bash
./gradlew testDebugUnitTest   # 46 pruebas del motor, la IA, el protocolo de red y el sonido
./gradlew assembleDebug       # APK de depuración
./gradlew bundleRelease       # .aab firmado para Google Play (necesita keystore.properties)
```

## Licencia y origen

El juego reproduce las reglas del clásico de hundir la flota, que no son objeto de derechos de
autor. Todo el código, el dibujo, los sonidos y los textos son propios.
