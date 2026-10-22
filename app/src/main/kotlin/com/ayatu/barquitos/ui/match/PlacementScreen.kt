package com.ayatu.barquitos.ui.match

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ayatu.barquitos.R
import com.ayatu.barquitos.core.Coord
import com.ayatu.barquitos.core.Fleet
import com.ayatu.barquitos.core.Orientation
import com.ayatu.barquitos.core.PlacedShip
import com.ayatu.barquitos.core.ShipDef
import com.ayatu.barquitos.core.ShipKind
import com.ayatu.barquitos.data.Settings
import com.ayatu.barquitos.ui.common.BarquitosScreen
import com.ayatu.barquitos.ui.common.BoardHitTest
import com.ayatu.barquitos.ui.common.BoardRender
import com.ayatu.barquitos.ui.common.BoardView
import com.ayatu.barquitos.ui.common.DosPaneles
import com.ayatu.barquitos.ui.common.ShipChip
import com.ayatu.barquitos.ui.common.WindowClass
import com.ayatu.barquitos.ui.common.esVentanaAncha
import com.ayatu.barquitos.ui.common.ladoTablero
import com.ayatu.barquitos.ui.common.ShipPainter.drawShipPreview
import com.ayatu.barquitos.ui.common.labelRes

/**
 * Colocación de la flota.
 *
 * El gesto principal es **sacar el barco de la bandeja**: se mantiene pulsado el botón de su clase
 * (uno por clase, con cuántos quedan debajo), se arrastra hasta el tablero —el barco va siguiendo
 * al dedo y la sombra enseña dónde caería, verde si cabe y roja si no— y se suelta. El barco queda
 * puesto y **elegido, en rojo**, de modo que «Girar» lo gira en su sitio.
 *
 * Sobre el tablero: un toque en un barco puesto lo elige (para girarlo) y arrastrarlo lo mueve;
 * sacarlo fuera del tablero lo devuelve a la bandeja.
 */
@Composable
fun PlacementScreen(
    state: MatchUiState,
    settings: Settings,
    onTakeFromTray: (ShipKind) -> Unit,
    onDragOverBoard: (Coord?) -> Unit,
    onDropFromTray: (Coord?) -> Unit,
    onBoardPress: (Coord) -> Unit,
    onBoardMove: (Coord) -> Unit,
    onBoardRelease: (Coord?) -> Unit,
    onRotate: () -> Unit,
    onRandom: () -> Unit,
    onClear: () -> Unit,
    onReady: () -> Unit,
    onBack: () -> Unit,
) {
    // Dónde está el tablero en la pantalla: hace falta porque el dedo empieza en la bandeja, que
    // es otro componente, y hay que saber sobre qué casilla pasa.
    var hitTest by remember { mutableStateOf<BoardHitTest?>(null) }
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    // El barco que va pegado al dedo mientras se trae de la bandeja.
    var carried by remember { mutableStateOf<ShipDef?>(null) }
    var carriedAt by remember { mutableStateOf<Offset?>(null) }

    val pendientes = state.pendingShips.groupingBy { it.kind }.eachCount()
    // El tablero no puede comerse la pantalla: en un plegable abierto, que es casi cuadrado, un
    // tablero tan ancho como la pantalla dejaría la bandeja y los botones fuera de la vista. Con
    // dos columnas manda el ancho de la columna (ver WindowClass.ladoTablero).
    val ancha = esVentanaAncha()
    val tableroMax = ladoTablero(fraccionAlto = 0.48f)

    BarquitosScreen(
        title = stringResource(R.string.placement_title),
        onBack = onBack,
        anchoMaximo = WindowClass.JUEGO_MAX,
    ) { modifier ->
        Box(
            modifier
                .fillMaxSize()
                .onGloballyPositioned { overlayOrigin = it.positionInRoot() },
        ) {
            // En el móvil de pie, el tablero arriba y la bandeja debajo. En una ventana ancha
            // (móvil tumbado, tablet o monitor en modo escritorio), el tablero a la izquierda y la
            // bandeja con los botones a la derecha: así se ve entero sin tener que desplazar.
            DosPaneles(
                ancha = ancha,
                modifier = Modifier.padding(horizontal = 12.dp),
                separacion = 8.dp,
                principal = {
                    Text(
                        stringResource(R.string.placement_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.placement_separation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    BoardView(
                        render = BoardRender(
                            size = state.boardSize,
                            ships = state.placed,
                            preview = state.preview,
                            previewValid = state.previewValid,
                            selectedShipId = state.selectedPlacedId,
                            showCoords = settings.showCoords,
                            enabled = true,
                        ),
                        maxSize = tableroMax,
                        onPress = onBoardPress,
                        onMove = onBoardMove,
                        onRelease = onBoardRelease,
                        onGeometry = { hitTest = it },
                    )

                },
                secundario = {
                    // Bandeja: un botón por clase de barco, con cuántos quedan por colocar.
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TIPOS.forEach { kind ->
                            val def = Fleet.ships.first { it.kind == kind }
                            val quedan = pendientes[kind] ?: 0
                            ShipChip(
                                def = def,
                                label = stringResource(kind.labelRes),
                                remaining = quedan,
                                selected = state.pendingShips
                                    .firstOrNull { it.id == state.selectedShipId }?.kind == kind,
                                modifier = Modifier.weight(1f),
                                onDragStart = {
                                    onTakeFromTray(kind)
                                    carried = def
                                },
                                onDrag = { pos ->
                                    carriedAt = pos
                                    onDragOverBoard(hitTest?.coordAtRoot(pos))
                                },
                                onDragEnd = {
                                    onDropFromTray(carriedAt?.let { p -> hitTest?.coordAtRoot(p) })
                                    carried = null
                                    carriedAt = null
                                },
                            )
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = onRotate,
                            enabled = state.selectedPlacedId != null || state.selectedShipId != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Rotate90DegreesCw, contentDescription = null)
                            Spacer(Modifier.padding(2.dp))
                            Text(stringResource(R.string.placement_rotate))
                        }
                        OutlinedButton(onClick = onRandom, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Casino, contentDescription = null)
                            Spacer(Modifier.padding(2.dp))
                            Text(stringResource(R.string.placement_random))
                        }
                        OutlinedButton(onClick = onClear) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.placement_clear),
                            )
                        }
                    }

                    val pending = state.pendingShips.size
                    Button(
                        onClick = onReady,
                        enabled = pending == 0,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(
                            if (pending == 0) {
                                stringResource(R.string.placement_ready)
                            } else {
                                stringResource(R.string.placement_pending, pending)
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                },
            )

            // El barco que se lleva en el dedo, por encima de todo lo demás.
            val llevado = carried
            val donde = carriedAt
            if (llevado != null && donde != null) {
                val density = LocalDensity.current
                val ancho = 34.dp * llevado.size
                val alto = 34.dp
                val local = donde - overlayOrigin
                Canvas(
                    Modifier
                        .size(ancho, alto)
                        .graphicsLayer {
                            translationX = local.x - with(density) { ancho.toPx() } / 2f
                            translationY = local.y - with(density) { alto.toPx() } * 1.4f
                            alpha = 0.85f
                        },
                ) {
                    val ship = PlacedShip(llevado, Coord(0, 0), Orientation.HORIZONTAL)
                    drawShipPreview(ship, Rect(0f, 0f, size.width, size.height), selected = true)
                }
            }
        }
    }
}

/** Las clases de barco, de mayor a menor: un botón por clase en la bandeja. */
private val TIPOS = listOf(
    ShipKind.PORTAAVIONES,
    ShipKind.ACORAZADO,
    ShipKind.DESTRUCTOR,
    ShipKind.SUBMARINO,
)
