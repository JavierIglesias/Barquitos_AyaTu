package com.ayatu.barquitos.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import com.ayatu.barquitos.core.Coord
import com.ayatu.barquitos.core.PlacedShip
import com.ayatu.barquitos.data.Skin
import com.ayatu.barquitos.data.SkinPattern
import kotlin.math.min
import kotlin.random.Random

/**
 * Dibuja los barcos. No hay ni una imagen en el APK: cada barco se pinta con Canvas a partir de
 * su forma ([PlacedShip.cells]) y de la pintura elegida en el Astillero ([Skin]).
 *
 * El casco es la unión de sus casillas redondeadas; encima van la cubierta, las torretas y el
 * patrón de pintura (camuflaje, rayas de guerra, desgastado o aniversario), todo recortado a la
 * silueta del barco para que no se salga de sus casillas.
 */
object ShipPainter {

    /**
     * @param origin esquina superior izquierda del tablero en píxeles.
     * @param cell lado de una casilla en píxeles.
     * @param sunk si el barco está hundido se pinta apagado y con las vías de agua.
     */
    fun DrawScope.drawShip(
        ship: PlacedShip,
        origin: Offset,
        cell: Float,
        skin: Skin,
        sunk: Boolean = false,
        alpha: Float = 1f,
    ) {
        val cells = ship.cells
        val hull = if (sunk) skin.hull.darken(0.45f) else skin.hull
        val deck = if (sunk) skin.deck.darken(0.45f) else skin.deck
        val accent = if (sunk) skin.accent.darken(0.5f) else skin.accent
        val inset = cell * 0.08f

        val horizontal = (cells.maxOf { it.x } - cells.minOf { it.x }) >=
            (cells.maxOf { it.y } - cells.minOf { it.y })
        // El casco no se dibuja casilla a casilla (saldrían cuentas sueltas), sino por tramos
        // seguidos: cada tramo es una cápsula, así un barco recto queda de una pieza y las piezas
        // que sobresalen (la isla del portaaviones, la torreta del submarino) quedan encima.
        val runs = contiguousRuns(cells, horizontal)
        val mainRun = runs.maxByOrNull { it.length } ?: return

        val silhouette = Path().apply {
            runs.forEach { run ->
                // Los tramos que sobresalen (isla de mando, torreta) se alargan medio hueco hacia
                // el casco para que queden pegados a él y no floten sueltos.
                val rect = run.rect(origin, cell, horizontal, inset)
                    .extendedTowards(mainRun, run, cell, horizontal)
                val r = if (run.length == 1) cell * 0.3f else cell * 0.42f
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = rect.left, top = rect.top, right = rect.right, bottom = rect.bottom,
                        radiusX = r, radiusY = r,
                    ),
                )
            }
            // Proa en punta, en el extremo del tramo más largo.
            if (mainRun.length >= 2) addPath(bowPath(mainRun, origin, cell, horizontal, inset))
        }

        drawPath(silhouette, hull, alpha = alpha)

        clipPath(silhouette) {
            drawPattern(ship, origin, cell, skin, alpha * if (sunk) 0.5f else 1f)

            // Cubierta: una banda a lo largo del tramo principal.
            val main = mainRun.rect(origin, cell, horizontal, inset)
            if (horizontal) {
                drawRect(
                    color = deck,
                    topLeft = Offset(main.left, main.top + (main.bottom - main.top) * 0.3f),
                    size = Size(main.right - main.left, (main.bottom - main.top) * 0.4f),
                    alpha = alpha * 0.8f,
                )
            } else {
                drawRect(
                    color = deck,
                    topLeft = Offset(main.left + (main.right - main.left) * 0.3f, main.top),
                    size = Size((main.right - main.left) * 0.4f, main.bottom - main.top),
                    alpha = alpha * 0.8f,
                )
            }

            // Torretas en el tramo principal; las casillas que sobresalen van en color de acento
            // (la isla de mando y la torreta del submarino).
            val mainCells = mainRun.cells(horizontal).toSet()
            cells.forEach { c ->
                val center = Offset(origin.x + (c.x + 0.5f) * cell, origin.y + (c.y + 0.5f) * cell)
                if (c in mainCells) {
                    drawCircle(hull.darken(0.3f), radius = cell * 0.15f, center = center, alpha = alpha * 0.9f)
                } else {
                    drawCircle(accent, radius = cell * 0.22f, center = center, alpha = alpha)
                }
            }
        }

        // Contorno, para que se despegue del agua.
        drawPath(
            silhouette,
            hull.darken(0.45f),
            alpha = alpha * 0.9f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = cell * 0.05f),
        )
    }

    /** Un tramo seguido de casillas del barco: fila (o columna) fija y de dónde a dónde llega. */
    private data class Run(val fixed: Int, val from: Int, val to: Int) {
        val length: Int get() = to - from + 1

        fun cells(horizontal: Boolean): List<Coord> = (from..to).map {
            if (horizontal) Coord(it, fixed) else Coord(fixed, it)
        }

        fun rect(origin: Offset, cell: Float, horizontal: Boolean, inset: Float): Rect =
            if (horizontal) {
                Rect(
                    origin.x + from * cell + inset,
                    origin.y + fixed * cell + inset,
                    origin.x + (to + 1) * cell - inset,
                    origin.y + (fixed + 1) * cell - inset,
                )
            } else {
                Rect(
                    origin.x + fixed * cell + inset,
                    origin.y + from * cell + inset,
                    origin.x + (fixed + 1) * cell - inset,
                    origin.y + (to + 1) * cell - inset,
                )
            }
    }

    /** Alarga un tramo secundario medio hueco hacia el tramo principal, para que se peguen. */
    private fun Rect.extendedTowards(main: Run, run: Run, cell: Float, horizontal: Boolean): Rect {
        if (run == main) return this
        val grow = cell * 0.55f
        return when {
            run.fixed < main.fixed && horizontal -> copy(bottom = bottom + grow)
            run.fixed > main.fixed && horizontal -> copy(top = top - grow)
            run.fixed < main.fixed -> copy(right = right + grow)
            else -> copy(left = left - grow)
        }
    }

    /** Parte las casillas en tramos seguidos a lo largo del eje mayor. */
    private fun contiguousRuns(cells: List<Coord>, horizontal: Boolean): List<Run> {
        val groups = cells.groupBy { if (horizontal) it.y else it.x }
        val runs = mutableListOf<Run>()
        groups.forEach { (fixed, group) ->
            val values = group.map { if (horizontal) it.x else it.y }.sorted()
            var start = values.first()
            var previous = start
            for (v in values.drop(1)) {
                if (v != previous + 1) {
                    runs += Run(fixed, start, previous)
                    start = v
                }
                previous = v
            }
            runs += Run(fixed, start, previous)
        }
        return runs
    }

    /** Punta de proa en el extremo «final» del tramo principal. */
    private fun bowPath(
        run: Run,
        origin: Offset,
        cell: Float,
        horizontal: Boolean,
        inset: Float,
    ): Path {
        val r = run.rect(origin, cell, horizontal, inset)
        return Path().apply {
            if (horizontal) {
                moveTo(r.right - cell * 0.1f, r.top)
                lineTo(r.right + inset, (r.top + r.bottom) / 2f)
                lineTo(r.right - cell * 0.1f, r.bottom)
            } else {
                moveTo(r.left, r.bottom - cell * 0.1f)
                lineTo((r.left + r.right) / 2f, r.bottom + inset)
                lineTo(r.right, r.bottom - cell * 0.1f)
            }
            close()
        }
    }

    private fun DrawScope.drawPattern(
        ship: PlacedShip,
        origin: Offset,
        cell: Float,
        skin: Skin,
        alpha: Float,
    ) {
        val cells = ship.cells
        val bounds = Rect(
            left = origin.x + cells.minOf { it.x } * cell,
            top = origin.y + cells.minOf { it.y } * cell,
            right = origin.x + (cells.maxOf { it.x } + 1) * cell,
            bottom = origin.y + (cells.maxOf { it.y } + 1) * cell,
        )
        // Semilla fija por barco: el patrón sale siempre igual, no baila entre fotogramas.
        val rng = Random(ship.id * 7919 + cells.size * 31)
        when (skin.pattern) {
            SkinPattern.LISO -> Unit

            SkinPattern.CAMUFLAJE -> repeat(5) {
                val w = bounds.width * rng.nextDouble(0.18, 0.4).toFloat()
                val h = bounds.height * rng.nextDouble(0.3, 0.75).toFloat()
                val x = bounds.left + bounds.width * rng.nextDouble(0.0, 0.8).toFloat()
                val y = bounds.top + bounds.height * rng.nextDouble(0.0, 0.5).toFloat()
                drawRect(
                    color = if (it % 2 == 0) skin.deck else skin.hull.darken(0.3f),
                    topLeft = Offset(x, y),
                    size = Size(w, h),
                    alpha = alpha * 0.6f,
                )
            }

            SkinPattern.DAZZLE -> {
                val stripe = cell * 0.3f
                var x = bounds.left - bounds.height
                while (x < bounds.right + bounds.height) {
                    val path = Path().apply {
                        moveTo(x, bounds.bottom)
                        lineTo(x + bounds.height, bounds.top)
                        lineTo(x + bounds.height + stripe, bounds.top)
                        lineTo(x + stripe, bounds.bottom)
                        close()
                    }
                    drawPath(path, skin.hull.darken(0.45f), alpha = alpha * 0.7f)
                    x += stripe * 2.4f
                }
            }

            SkinPattern.DESGASTADO -> {
                repeat(14) {
                    val r = cell * rng.nextDouble(0.06, 0.18).toFloat()
                    drawCircle(
                        color = Color(0xFF7A4A2A),
                        radius = r,
                        center = Offset(
                            bounds.left + bounds.width * rng.nextFloat(),
                            bounds.top + bounds.height * rng.nextFloat(),
                        ),
                        alpha = alpha * 0.55f,
                    )
                }
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(bounds.left, bounds.bottom - bounds.height * 0.22f),
                    size = Size(bounds.width, bounds.height * 0.22f),
                    alpha = alpha * 0.22f,
                )
            }

            SkinPattern.ANIVERSARIO -> {
                drawRect(
                    color = skin.accent,
                    topLeft = Offset(bounds.left, bounds.top + bounds.height * 0.12f),
                    size = Size(bounds.width, bounds.height * 0.1f),
                    alpha = alpha * 0.9f,
                )
                drawRect(
                    color = skin.accent,
                    topLeft = Offset(bounds.left, bounds.bottom - bounds.height * 0.22f),
                    size = Size(bounds.width, bounds.height * 0.1f),
                    alpha = alpha * 0.9f,
                )
            }
        }
    }

    /**
     * Dibuja un barco suelto (sin tablero) dentro de [area]: lo usa el Astillero para la vista
     * previa y la partida para la lista de barcos de cada flota.
     */
    fun DrawScope.drawShipPreview(ship: PlacedShip, area: Rect, skin: Skin, sunk: Boolean = false) {
        val cells = ship.cells
        val w = cells.maxOf { it.x } - cells.minOf { it.x } + 1
        val h = cells.maxOf { it.y } - cells.minOf { it.y } + 1
        val cell = min(area.width / w, area.height / h)
        val origin = Offset(
            area.left + (area.width - cell * w) / 2f - cells.minOf { it.x } * cell,
            area.top + (area.height - cell * h) / 2f - cells.minOf { it.y } * cell,
        )
        drawShip(ship, origin, cell, skin, sunk)
    }

    /** Una explosión sencilla: núcleo claro, llamas y esquirlas. [progress] va de 0 a 1. */
    fun DrawScope.drawExplosion(center: Offset, maxRadius: Float, progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        val r = maxRadius * (0.35f + 0.65f * p)
        val fade = 1f - p
        drawCircle(Color(0xFFFFE9A8), radius = r * 0.45f, center = center, alpha = fade)
        drawCircle(Color(0xFFE8562A), radius = r * 0.85f, center = center, alpha = fade * 0.75f)
        drawCircle(Color(0xFF3A3F47), radius = r, center = center, alpha = fade * 0.35f)
        repeat(8) { i ->
            rotate(degrees = i * 45f, pivot = center) {
                drawRect(
                    color = Color(0xFFFFC46B),
                    topLeft = Offset(center.x + r * 0.6f, center.y - maxRadius * 0.04f),
                    size = Size(r * 0.5f * p, maxRadius * 0.08f),
                    alpha = fade * 0.8f,
                )
            }
        }
    }

    /** Un chapoteo: anillos que se abren. */
    fun DrawScope.drawSplash(center: Offset, maxRadius: Float, progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        val fade = 1f - p
        drawCircle(
            color = Color(0xFFD7E6F2),
            radius = maxRadius * (0.2f + 0.8f * p),
            center = center,
            alpha = fade * 0.7f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = maxRadius * 0.14f),
        )
        drawCircle(
            color = Color(0xFF7FC4E8),
            radius = maxRadius * (0.1f + 0.45f * p),
            center = center,
            alpha = fade * 0.5f,
        )
    }
}

/** Oscurece un color mezclándolo con negro. [amount] 0 = igual, 1 = negro. */
fun Color.darken(amount: Float): Color = Color(
    red = red * (1 - amount),
    green = green * (1 - amount),
    blue = blue * (1 - amount),
    alpha = alpha,
)

/** Aclara un color mezclándolo con blanco. */
fun Color.lighten(amount: Float): Color = Color(
    red = red + (1 - red) * amount,
    green = green + (1 - green) * amount,
    blue = blue + (1 - blue) * amount,
    alpha = alpha,
)
