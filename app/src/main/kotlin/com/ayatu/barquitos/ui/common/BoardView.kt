package com.ayatu.barquitos.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayatu.barquitos.core.Coord
import com.ayatu.barquitos.core.PlacedShip
import com.ayatu.barquitos.ui.common.ShipPainter.drawExplosion
import com.ayatu.barquitos.ui.common.ShipPainter.drawShip
import com.ayatu.barquitos.ui.common.ShipPainter.drawSplash
import com.ayatu.barquitos.ui.theme.Flota
import com.ayatu.barquitos.ui.theme.Mar
import kotlin.math.min

/** Todo lo que hay que pintar en un tablero. */
data class BoardRender(
    val size: Int,
    /** Barcos visibles: toda tu flota en tu tablero, solo los hundidos en el del rival. */
    val ships: List<PlacedShip> = emptyList(),
    val sunkShipIds: Set<Int> = emptySet(),
    val hits: Set<Coord> = emptySet(),
    val misses: Set<Coord> = emptySet(),
    /** Objetivos marcados en la regla SALVA, en orden. */
    val aim: List<Coord> = emptyList(),
    /** Agua deducida: en la flota Rusa, lo que rodea a un barco hundido. */
    val water: Set<Coord> = emptySet(),
    /** Casillas del barco que se está colocando ahora mismo. */
    val preview: List<Coord> = emptyList(),
    val previewValid: Boolean = true,
    /** Barco colocado que está seleccionado: se pinta en rojo y es el que gira el botón. */
    val selectedShipId: Int? = null,
    /** Casilla a la espera de confirmación de disparo. */
    val pendingShot: Coord? = null,
    /** Último disparo, para la animación. */
    val lastShot: Coord? = null,
    /** 0..1 de la animación del último disparo. */
    val shotProgress: Float = 1f,
    val lastShotHit: Boolean = false,
    val showCoords: Boolean = true,
    val enabled: Boolean = true,
    /** Atenúa el tablero cuando no es el que toca mirar. */
    val dimmed: Boolean = false,
)

/**
 * Dónde ha quedado el tablero en la pantalla y cómo convertir un punto de la pantalla en una
 * casilla. Hace falta porque los barcos se arrastran **desde la bandeja**, que es otro componente:
 * el dedo empieza fuera del tablero y hay que saber sobre qué casilla pasa.
 */
class BoardHitTest(
    private val topLeft: Offset,
    private val width: Float,
    private val height: Float,
    private val n: Int,
    private val gutter: Float,
) {
    /** La casilla que hay bajo [root] (coordenadas de la ventana), o null si está fuera. */
    fun coordAtRoot(root: Offset): Coord? =
        coordAt(root - topLeft, width, height, n, gutter)
}

/**
 * El tablero. Se dibuja entero con Canvas (agua, retícula, barcos, impactos y animaciones), así
 * que el mismo componente sirve para la pantalla de colocación, para la de partida y para la de
 * resultado, y no hay ni una imagen que escalar.
 */
@Composable
fun BoardView(
    render: BoardRender,
    modifier: Modifier = Modifier,
    /**
     * Lado máximo del tablero. Sin límite, en una pantalla ancha y casi cuadrada (un plegable
     * abierto) el tablero se come la pantalla entera y hay que bajar a buscar la bandeja y los
     * botones. Quien usa el tablero le pone un tope en función de la altura de la pantalla.
     */
    maxSize: Dp = Dp.Unspecified,
    onTap: ((Coord) -> Unit)? = null,
    onLongPress: ((Coord) -> Unit)? = null,
    /** Colocación: el dedo baja sobre esa casilla. */
    onPress: ((Coord) -> Unit)? = null,
    /** Colocación: el dedo, sin levantarse, pasa a otra casilla. */
    onMove: ((Coord) -> Unit)? = null,
    /** Colocación: el dedo se levanta sobre esa casilla, o fuera del tablero (`null`). */
    onRelease: ((Coord?) -> Unit)? = null,
    /**
     * Avisa de dónde ha quedado el tablero en la pantalla, para que quien lo contiene pueda
     * saber a qué casilla apunta un dedo que viene de fuera (arrastrar un barco de la bandeja).
     */
    onGeometry: ((BoardHitTest) -> Unit)? = null,
) {
    val measurer = rememberTextMeasurer()
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = maxSize).fillMaxWidth().aspectRatio(1f)) {
            val gutter = if (render.showCoords) GUTTER_CELLS else 0f
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .let { base ->
                        if (onGeometry == null) base else base.onGloballyPositioned { c ->
                            onGeometry(
                                BoardHitTest(
                                    topLeft = c.positionInRoot(),
                                    width = c.size.width.toFloat(),
                                    height = c.size.height.toFloat(),
                                    n = render.size,
                                    gutter = gutter,
                                ),
                            )
                        }
                    }
                    .let { base ->
                        when {
                            // Colocación. No se distingue entre tocar, mantener pulsado y
                            // arrastrar: en los tres casos el dedo «lleva» el barco y se suelta
                            // donde uno quiere. Con `detectTapGestures` una pulsación sostenida no
                            // llegaba a ser un toque y no pasaba nada, que es justo lo que uno
                            // intenta hacer la primera vez.
                            onPress != null && onRelease != null ->
                                base.pointerInput(render.size, render.enabled, onPress, onMove, onRelease) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        if (!render.enabled) return@awaitEachGesture
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()
                                        var last = coordAt(down.position, w, h, render.size, gutter)
                                        var outside = last == null
                                        last?.let(onPress)
                                        down.consume()
                                        while (true) {
                                            val change = awaitPointerEvent().changes
                                                .firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) {
                                                change.consume()
                                                break
                                            }
                                            val c = coordAt(change.position, w, h, render.size, gutter)
                                            outside = c == null
                                            if (c != null && c != last) {
                                                last = c
                                                onMove?.invoke(c)
                                            }
                                            change.consume()
                                        }
                                        // Soltar fuera del tablero devuelve el barco a la bandeja
                                        // en vez de colocarlo donde estuvo por última vez.
                                        onRelease(if (outside) null else last)
                                    }
                                }

                            onTap != null || onLongPress != null ->
                                base.pointerInput(render.size, render.enabled, onTap, onLongPress) {
                                    detectTapGestures(
                                        onTap = { p ->
                                            if (render.enabled) {
                                                coordAt(p, size.width.toFloat(), size.height.toFloat(), render.size, gutter)
                                                    ?.let { onTap?.invoke(it) }
                                            }
                                        },
                                        onLongPress = { p ->
                                            if (render.enabled) {
                                                coordAt(p, size.width.toFloat(), size.height.toFloat(), render.size, gutter)
                                                    ?.let { onLongPress?.invoke(it) }
                                            }
                                        },
                                    )
                                }

                            else -> base
                        }
                    },
            ) {
                val n = render.size
                val cell = min(size.width / (n + gutter), size.height / (n + gutter))
                val origin = Offset(gutter * cell, gutter * cell)
                val alpha = if (render.dimmed) 0.45f else 1f

                drawSea(origin, cell, n, alpha)
                drawGrid(origin, cell, n, alpha)
                if (render.showCoords) drawLabels(measurer, origin, cell, n, alpha)

                render.ships.forEach { ship ->
                    drawShip(
                        ship = ship,
                        origin = origin,
                        cell = cell,
                        sunk = ship.id in render.sunkShipIds,
                        selected = ship.id == render.selectedShipId,
                        alpha = alpha,
                    )
                }

                render.water.forEach { c ->
                    drawCircle(
                        color = Mar.Espuma,
                        radius = cell * 0.07f,
                        center = centerOf(c, origin, cell),
                        alpha = alpha * 0.45f,
                    )
                }

                render.misses.forEach { c ->
                    val center = centerOf(c, origin, cell)
                    drawCircle(Mar.Niebla, radius = cell * 0.16f, center = center, alpha = alpha * 0.85f)
                    drawCircle(
                        color = Mar.Espuma,
                        radius = cell * 0.3f,
                        center = center,
                        alpha = alpha * 0.5f,
                        style = Stroke(width = cell * 0.05f),
                    )
                }

                render.hits.forEach { c ->
                    val center = centerOf(c, origin, cell)
                    drawCircle(Flota.Fuego, radius = cell * 0.3f, center = center, alpha = alpha * 0.9f)
                    drawCircle(Color(0xFFFFE9A8), radius = cell * 0.13f, center = center, alpha = alpha)
                }

                // Los barcos hundidos del rival se tachan, para distinguirlos de los tocados.
                render.ships.filter { it.id in render.sunkShipIds }.forEach { ship ->
                    ship.cells.forEach { c ->
                        val p = Offset(origin.x + c.x * cell, origin.y + c.y * cell)
                        drawLine(
                            color = Flota.Hundido,
                            start = Offset(p.x + cell * 0.2f, p.y + cell * 0.2f),
                            end = Offset(p.x + cell * 0.8f, p.y + cell * 0.8f),
                            strokeWidth = cell * 0.08f,
                            alpha = alpha * 0.8f,
                        )
                        drawLine(
                            color = Flota.Hundido,
                            start = Offset(p.x + cell * 0.8f, p.y + cell * 0.2f),
                            end = Offset(p.x + cell * 0.2f, p.y + cell * 0.8f),
                            strokeWidth = cell * 0.08f,
                            alpha = alpha * 0.8f,
                        )
                    }
                }

                render.preview.forEach { c ->
                    val color = if (render.previewValid) Flota.Victoria else Flota.Derrota
                    drawRect(
                        color = color,
                        topLeft = Offset(origin.x + c.x * cell, origin.y + c.y * cell),
                        size = Size(cell, cell),
                        alpha = 0.35f,
                    )
                    drawRect(
                        color = color,
                        topLeft = Offset(origin.x + c.x * cell, origin.y + c.y * cell),
                        size = Size(cell, cell),
                        style = Stroke(width = cell * 0.06f),
                    )
                }

                render.aim.forEachIndexed { index, c ->
                    val center = centerOf(c, origin, cell)
                    drawCircle(
                        color = Flota.Oro,
                        radius = cell * 0.34f,
                        center = center,
                        style = Stroke(width = cell * 0.09f),
                    )
                    drawText(
                        textMeasurer = measurer,
                        text = "${index + 1}",
                        topLeft = Offset(center.x - cell * 0.14f, center.y - cell * 0.26f),
                        style = TextStyle(
                            color = Flota.Oro,
                            fontSize = spOf(cell * 0.36f),
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                render.pendingShot?.let { c ->
                    val center = centerOf(c, origin, cell)
                    drawCircle(
                        color = Flota.Oro,
                        radius = cell * 0.42f,
                        center = center,
                        style = Stroke(width = cell * 0.08f),
                    )
                    drawLine(
                        color = Flota.Oro,
                        start = Offset(center.x - cell * 0.5f, center.y),
                        end = Offset(center.x + cell * 0.5f, center.y),
                        strokeWidth = cell * 0.04f,
                    )
                    drawLine(
                        color = Flota.Oro,
                        start = Offset(center.x, center.y - cell * 0.5f),
                        end = Offset(center.x, center.y + cell * 0.5f),
                        strokeWidth = cell * 0.04f,
                    )
                }

                if (render.lastShot != null && render.shotProgress < 1f) {
                    val center = centerOf(render.lastShot, origin, cell)
                    if (render.lastShotHit) {
                        drawExplosion(center, cell * 0.9f, render.shotProgress)
                    } else {
                        drawSplash(center, cell * 0.8f, render.shotProgress)
                    }
                }
            }
        }
    }
}

private const val GUTTER_CELLS = 0.75f

private fun centerOf(c: Coord, origin: Offset, cell: Float) =
    Offset(origin.x + (c.x + 0.5f) * cell, origin.y + (c.y + 0.5f) * cell)

/** Convierte una pulsación en casilla, o null si cayó en el margen de las letras. */
private fun coordAt(p: Offset, width: Float, height: Float, n: Int, gutter: Float): Coord? {
    val cell = min(width / (n + gutter), height / (n + gutter))
    val x = ((p.x - gutter * cell) / cell).toInt()
    val y = ((p.y - gutter * cell) / cell).toInt()
    if (p.x < gutter * cell || p.y < gutter * cell) return null
    if (x !in 0 until n || y !in 0 until n) return null
    return Coord(x, y)
}

private fun DrawScope.drawSea(origin: Offset, cell: Float, n: Int, alpha: Float) {
    val boardSize = cell * n
    drawRect(
        brush = Brush.verticalGradient(listOf(Mar.Claro, Mar.Profundo)),
        topLeft = origin,
        size = Size(boardSize, boardSize),
        alpha = alpha,
    )
    // Unas olas muy suaves para que el agua no sea un rectángulo plano.
    var y = origin.y + cell * 0.6f
    var row = 0
    while (y < origin.y + boardSize) {
        val shift = if (row % 2 == 0) cell * 0.3f else cell * 0.75f
        var x = origin.x + shift
        while (x < origin.x + boardSize - cell * 0.3f) {
            drawLine(
                color = Mar.Espuma,
                start = Offset(x, y),
                end = Offset(x + cell * 0.35f, y),
                strokeWidth = cell * 0.035f,
                alpha = alpha * 0.12f,
            )
            x += cell * 1.6f
        }
        y += cell * 1.1f
        row++
    }
}

private fun DrawScope.drawGrid(origin: Offset, cell: Float, n: Int, alpha: Float) {
    val boardSize = cell * n
    for (i in 0..n) {
        val p = i * cell
        drawLine(
            color = Mar.Espuma,
            start = Offset(origin.x + p, origin.y),
            end = Offset(origin.x + p, origin.y + boardSize),
            strokeWidth = if (i == 0 || i == n) cell * 0.05f else cell * 0.02f,
            alpha = alpha * 0.35f,
        )
        drawLine(
            color = Mar.Espuma,
            start = Offset(origin.x, origin.y + p),
            end = Offset(origin.x + boardSize, origin.y + p),
            strokeWidth = if (i == 0 || i == n) cell * 0.05f else cell * 0.02f,
            alpha = alpha * 0.35f,
        )
    }
}

private fun DrawScope.drawLabels(
    measurer: TextMeasurer,
    origin: Offset,
    cell: Float,
    n: Int,
    alpha: Float,
) {
    val style = TextStyle(
        color = Mar.Niebla.copy(alpha = alpha * 0.8f),
        fontSize = spOf(cell * 0.42f),
        fontWeight = FontWeight.SemiBold,
    )
    for (i in 0 until n) {
        val letter = ('A' + i).toString()
        val letterLayout = measurer.measure(letter, style)
        drawText(
            textLayoutResult = letterLayout,
            topLeft = Offset(
                origin.x + (i + 0.5f) * cell - letterLayout.size.width / 2f,
                origin.y - cell * 0.72f,
            ),
        )
        val number = (i + 1).toString()
        val numberLayout = measurer.measure(number, style)
        drawText(
            textLayoutResult = numberLayout,
            topLeft = Offset(
                origin.x - cell * 0.68f + (cell * 0.6f - numberLayout.size.width) / 2f,
                origin.y + (i + 0.5f) * cell - numberLayout.size.height / 2f,
            ),
        )
    }
}

/** Píxeles a sp dentro de un [DrawScope]: el tamaño de letra del tablero depende de la casilla. */
private fun DrawScope.spOf(px: Float) = (px / density).sp
