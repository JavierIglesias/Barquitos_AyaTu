# Barquitos AyaTu

Juego de hundir la flota para Android. Tres flotas, cinco reglas de disparo, contra la máquina
(tres niveles), a dos en el mismo móvil o por WiFi local. Sin anuncios, sin compras, sin cuentas y
sin conexión a ningún servidor.

![versión](https://img.shields.io/badge/versi%C3%B3n-1.0.0-blue) ![minSdk](https://img.shields.io/badge/minSdk-24-green)

## Documentación

| Documento | Para qué |
|---|---|
| [`Docs/manual_usuario.md`](Docs/manual_usuario.md) | Manual de usuario (se ve dentro de la app) |
| [`Docs/novedades.md`](Docs/novedades.md) | Qué cambia en cada versión, en lenguaje normal (se ve dentro de la app) |
| [`Docs/historial.md`](Docs/historial.md) | Historial técnico: cambios, causa de los fallos y ficheros implicados |
| [`store/PLAY_STORE.md`](store/PLAY_STORE.md) | Ficha de Google Play: textos, permisos, datos, comprobaciones |

Los tres documentos de `Docs/` se empaquetan como assets con la tarea `syncDocs` de Gradle, así que
lo que se lee dentro de la app es exactamente el fichero de este repositorio.

## Cómo está montado

```
app/src/main/kotlin/com/ayatu/barquitos/
├── core/              Motor del juego. Kotlin puro, sin Android: se prueba con pruebas unitarias.
│   ├── Model.kt       Coord, ShipShape, PlacedShip, PlayerBoard
│   ├── Fleets.kt      Las tres flotas (Estándar, Clásica, Rusa)
│   ├── Rules.kt       Las cinco reglas de disparo y la configuración de partida
│   ├── Placement.kt   Qué colocaciones son legales y reparto al azar
│   ├── Game.kt        Turnos, disparos, salvas simultáneas, victoria y empate
│   ├── GameSave.kt    Guardado de la partida en texto plano
│   └── ai/ShipAi.kt   La IA (fácil / normal / difícil por mapa de probabilidad)
├── data/              Ajustes, estadísticas, partida guardada y catálogo de desbloqueos
├── log/               Registro de eventos en fichero, con rotación
├── net/               WiFi local: descubrimiento NSD y conexión TCP
├── sound/             Efectos sintetizados (no hay ficheros de audio)
└── ui/                Pantallas en Jetpack Compose
```

Dos detalles de los que depende todo lo visual:

- **No hay imágenes de barcos**. `ui/common/ShipPainter.kt` los dibuja con Canvas a partir de su
  forma y de la pintura elegida; `ui/common/Widgets.kt` hace lo mismo con las banderas y los
  emblemas.
- **No hay ficheros de audio**. `sound/SoundEngine.kt` sintetiza los efectos y los escribe como WAV
  en la caché la primera vez que arranca la app.

## Compilar

Hace falta JDK 17 y el SDK de Android (`local.properties` → `sdk.dir`).

```bash
./gradlew testDebugUnitTest   # 34 pruebas del motor y de la IA
./gradlew assembleDebug       # APK de depuración
./gradlew bundleRelease       # .aab firmado para Google Play (necesita keystore.properties)
```

## Licencia y origen

El juego reproduce las reglas del clásico de hundir la flota y el conjunto de modos y variantes que
popularizó *Fleet Battle* (Smuttlewerk Interactive). Las reglas de un juego no son objeto de derechos
de autor; el nombre, la marca, los gráficos y el código de esa app sí lo son, y aquí no se usa nada
de eso: todo el código, el dibujo, los sonidos y los textos son propios.
