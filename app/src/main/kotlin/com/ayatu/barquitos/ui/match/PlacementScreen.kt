package com.ayatu.barquitos.ui.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ayatu.barquitos.R
import com.ayatu.barquitos.core.Coord
import com.ayatu.barquitos.core.Fleet
import com.ayatu.barquitos.data.Settings
import com.ayatu.barquitos.ui.common.BarquitosScreen
import com.ayatu.barquitos.ui.common.BoardRender
import com.ayatu.barquitos.ui.common.BoardView
import com.ayatu.barquitos.ui.common.ShipChip
import com.ayatu.barquitos.ui.common.labelRes

/**
 * Colocación de la flota. Se elige un barco de la bandeja de abajo y se lleva al tablero con el
 * dedo: vale tocar, mantener pulsado o arrastrar, porque mientras el dedo está encima se ve la
 * sombra del barco (verde si cabe, roja si no) y al levantarlo se coloca. Si el dedo baja sobre un
 * barco ya puesto, lo levanta y pasa a arrastrarlo; si se suelta fuera del tablero, vuelve a la
 * bandeja. «Girar» lo rota (las formas raras de la flota Estándar tienen cuatro posiciones; las
 * rectas, dos).
 */
@Composable
fun PlacementScreen(
    state: MatchUiState,
    settings: Settings,
    onSelect: (Int) -> Unit,
    onRotate: () -> Unit,
    onAim: (Coord) -> Unit,
    onDrop: (Coord?) -> Unit,
    onRandom: () -> Unit,
    onClear: () -> Unit,
    onReady: () -> Unit,
    onBack: () -> Unit,
) {
    BarquitosScreen(title = stringResource(R.string.placement_title), onBack = onBack) { modifier ->
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
                    showCoords = settings.showCoords,
                    enabled = true,
                ),
                onAim = onAim,
                onDrop = onDrop,
            )

            // Bandeja de barcos pendientes y colocados.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Fleet.ships, key = { it.id }) { def ->
                    val placed = state.placed.any { it.id == def.id }
                    ShipChip(
                        def = def,
                                selected = state.selectedShipId == def.id && !placed,
                        placed = placed,
                        label = stringResource(def.kind.labelRes),
                        onClick = { onSelect(def.id) },
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onRotate, modifier = Modifier.weight(1f)) {
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
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.placement_clear))
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
        }
    }
}
