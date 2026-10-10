package com.ayatu.barquitos.ui.match

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ayatu.barquitos.R
import com.ayatu.barquitos.core.Coord
import com.ayatu.barquitos.core.ShotRule
import com.ayatu.barquitos.data.Settings
import com.ayatu.barquitos.ui.common.BarquitosScreen
import com.ayatu.barquitos.ui.common.BoardView
import com.ayatu.barquitos.ui.common.FleetStatus
import com.ayatu.barquitos.ui.common.labelRes
import com.ayatu.barquitos.ui.theme.Flota

/**
 * La batalla. Arriba el tablero del rival (donde se dispara) y abajo el propio, más pequeño, con
 * el estado de las dos flotas y el parte de batalla.
 */
@Composable
fun GameScreen(
    state: MatchUiState,
    settings: Settings,
    onTapEnemy: (Coord) -> Unit,
    onCancelPending: () -> Unit,
    onSurrender: () -> Unit,
    onBack: () -> Unit,
) {
    var askSurrender by remember { mutableStateOf(false) }

    // Igual que al colocar: en una pantalla casi cuadrada el tablero enemigo no puede ocuparla
    // entera, o el propio y el parte de batalla se quedan fuera de la vista.
    val pantalla = LocalConfiguration.current
    val tableroMax = minOf(
        pantalla.screenWidthDp,
        (pantalla.screenHeightDp * 0.5f).toInt(),
    ).dp

    // Animación del último disparo: explosión si tocó, chapoteo si cayó al agua.
    var shotProgress by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(state.lastShot, state.flash?.stamp) {
        if (state.lastShot != null && settings.animations) {
            shotProgress = 0f
            animate(0f, 1f, animationSpec = tween(520)) { value, _ -> shotProgress = value }
        } else {
            shotProgress = 1f
        }
    }

    // El aviso grande («¡Tocado!», «¡Hundido!») se enseña un momento y se va.
    var flashVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.flash?.stamp) {
        if (state.flash != null) {
            flashVisible = true
            kotlinx.coroutines.delay(1100)
            flashVisible = false
        }
    }

    val turnText = when {
        state.busy -> stringResource(R.string.game_rival_turn)
        state.isMyTurn -> stringResource(R.string.game_your_turn)
        else -> stringResource(R.string.game_rival_turn)
    }

    BarquitosScreen(
        title = turnText,
        onBack = onBack,
        actions = {
            IconButton(onClick = { askSurrender = true }) {
                Icon(Icons.Default.Flag, contentDescription = stringResource(R.string.game_surrender))
            }
        },
    ) { modifier ->
        Box(modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Cabecera: ronda y disparos que quedan.
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.game_round, state.round),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(state.config.shotRule.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Text(
                    stringResource(R.string.game_enemy_waters),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.enemyRender?.let { render ->
                    BoardView(
                        // `enabled` se saca aquí del estado recién llegado y no de lo que venía en
                        // el render, para que no pueda quedarse desfasado tras un turno de la IA.
                        render = render.copy(
                            shotProgress = shotProgress,
                            showCoords = settings.showCoords,
                            enabled = state.isMyTurn && !state.busy && !state.finished,
                        ),
                        maxSize = tableroMax,
                        onTap = onTapEnemy,
                    )
                }

                if (state.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(2.dp))

                // Tablero propio y estado de las flotas, lado a lado.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.game_my_fleet),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        state.myRender?.let { render ->
                            BoardView(
                                render = render.copy(
                                    shotProgress = shotProgress,
                                    showCoords = false,
                                ),
                                    )
                        }
                    }
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FleetStatus(stringResource(R.string.game_fleet_status), state.enemyFleet)
                        FleetStatus(stringResource(R.string.game_my_fleet_status), state.myFleet)
                    }
                }

                // Parte de batalla.
                if (state.history.isNotEmpty()) {
                    Text(
                        stringResource(R.string.game_report),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                            .padding(8.dp),
                    ) {
                        state.history.take(8).forEach { record ->
                            val who = playerName(state, settings, record.player)
                            val sunk = record.sunkKind?.let { " · ${stringResource(it.labelRes)}" } ?: ""
                            Text(
                                "${record.coord.label()}  $who  " +
                                    (if (record.hit) stringResource(R.string.game_hit) else stringResource(R.string.game_miss)) +
                                    sunk,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = when {
                                    record.sunkKind != null -> Flota.Hundido
                                    record.hit -> Flota.Fuego
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Aviso grande encima del tablero.
            AnimatedVisibility(
                visible = flashVisible && state.flash != null,
                enter = fadeIn() + scaleIn(initialScale = 0.7f),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                state.flash?.let { FlashBanner(it) }
            }
        }
    }

    if (state.pendingShot != null) {
        AlertDialog(
            onDismissRequest = onCancelPending,
            title = { Text(state.pendingShot.label()) },
            text = { Text(stringResource(R.string.settings_confirm_shot_desc)) },
            confirmButton = {
                TextButton(onClick = { onTapEnemy(state.pendingShot) }) {
                    Text(stringResource(R.string.game_fire))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelPending) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (askSurrender) {
        AlertDialog(
            onDismissRequest = { askSurrender = false },
            title = { Text(stringResource(R.string.game_surrender)) },
            text = { Text(stringResource(R.string.game_surrender_ask)) },
            confirmButton = {
                TextButton(onClick = {
                    askSurrender = false
                    onSurrender()
                }) { Text(stringResource(R.string.common_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { askSurrender = false }) {
                    Text(stringResource(R.string.common_no))
                }
            },
        )
    }
}

@Composable
private fun FlashBanner(flash: Flash) {
    val (text, color) = when (flash.kind) {
        FlashKind.TOCADO -> stringResource(R.string.game_hit) to Flota.Fuego
        FlashKind.AGUA -> stringResource(R.string.game_miss) to MaterialTheme.colorScheme.secondary
        FlashKind.OTRA_VEZ -> stringResource(R.string.game_streak_again) to Flota.Oro
        FlashKind.HUNDIDO -> stringResource(
            R.string.game_sunk,
            flash.shipKind?.let { stringResource(it.labelRes) } ?: "",
        ) to Flota.Victoria
        FlashKind.HUNDIDO_PROPIO -> stringResource(
            R.string.game_sunk,
            flash.shipKind?.let { stringResource(it.labelRes) } ?: "",
        ) to Flota.Derrota
    }
    Text(
        text,
        style = MaterialTheme.typography.displaySmall,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 22.dp, vertical = 12.dp),
    )
}
