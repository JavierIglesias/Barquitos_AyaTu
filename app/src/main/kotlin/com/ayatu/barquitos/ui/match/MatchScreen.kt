package com.ayatu.barquitos.ui.match

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayatu.barquitos.R
import com.ayatu.barquitos.core.GameMode
import com.ayatu.barquitos.data.Settings
import com.ayatu.barquitos.ui.common.SeaBackdrop
import com.ayatu.barquitos.ui.theme.Flota

/**
 * La partida entera vive en esta pantalla: colocar la flota, la batalla y el resultado. Se
 * cambia de una cosa a otra según el [MatchUiState.phase], en vez de navegar entre rutas,
 * porque todas comparten el mismo [MatchViewModel] y el mismo estado de la partida.
 */
@Composable
fun MatchScreen(
    viewModel: MatchViewModel,
    settings: Settings,
    onExit: () -> Unit,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    var askQuit by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (state.phase == MatchPhase.RESULTADO) {
            onExit()
        } else {
            askQuit = true
        }
    }

    when (state.phase) {
        MatchPhase.COLOCANDO -> PlacementScreen(
            state = state,
            settings = settings,
            onSelect = viewModel::selectShip,
            onRotate = viewModel::rotate,
            onAim = viewModel::aimAt,
            onDrop = viewModel::dropAt,
            onRandom = viewModel::randomPlacement,
            onClear = viewModel::clearPlacement,
            onReady = viewModel::confirmPlacement,
            onBack = { askQuit = true },
        )

        MatchPhase.ESPERANDO_RIVAL -> WaitingScreen(
            message = stringResource(R.string.wifi_rival_placing),
            onCancel = { askQuit = true },
        )

        MatchPhase.JUGANDO -> GameScreen(
            state = state,
            settings = settings,
            onTapEnemy = viewModel::tapEnemy,
            onCancelPending = viewModel::cancelPendingShot,
            onSurrender = viewModel::surrender,
            onBack = { askQuit = true },
        )

        MatchPhase.RESULTADO -> ResultScreen(
            state = state,
            settings = settings,
            onReveal = viewModel::toggleReveal,
            onHome = onExit,
        )
    }

    if (askQuit) {
        AlertDialog(
            onDismissRequest = { askQuit = false },
            title = { Text(stringResource(R.string.game_surrender)) },
            text = { Text(stringResource(R.string.game_quit_ask)) },
            confirmButton = {
                TextButton(onClick = {
                    askQuit = false
                    viewModel.leave()
                    onExit()
                }) { Text(stringResource(R.string.common_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { askQuit = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

/** El nombre que se le pone a un jugador en pantalla. */
@Composable
fun playerName(state: MatchUiState, settings: Settings, player: Int): String = when {
    player == state.localPlayer -> settings.playerName.ifBlank { stringResource(R.string.common_you) }
    state.config.mode == GameMode.UN_JUGADOR -> stringResource(R.string.common_machine)
    else -> stringResource(R.string.common_rival)
}

@Composable
private fun WaitingScreen(message: String, onCancel: () -> Unit) {
    SeaBackdrop {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(18.dp))
                Text(message, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(18.dp))
                TextButton(onClick = onCancel) { Text(stringResource(R.string.wifi_cancel)) }
            }
        }
    }
}
