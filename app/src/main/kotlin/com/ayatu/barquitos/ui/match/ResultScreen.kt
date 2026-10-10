package com.ayatu.barquitos.ui.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ayatu.barquitos.R
import com.ayatu.barquitos.core.Game
import com.ayatu.barquitos.core.GameMode
import com.ayatu.barquitos.data.Settings
import com.ayatu.barquitos.ui.common.BarquitosScreen
import com.ayatu.barquitos.ui.common.BoardView
import com.ayatu.barquitos.ui.common.StatTile
import com.ayatu.barquitos.ui.theme.Flota

/** Resultado de la partida, con los dos tableros al descubierto y lo que se ha desbloqueado. */
@Composable
fun ResultScreen(
    state: MatchUiState,
    settings: Settings,
    onReveal: () -> Unit,
    onHome: () -> Unit,
) {
    val won = state.winner == state.localPlayer
    val title = if (won) {
        stringResource(R.string.result_victory)
    } else {
        stringResource(R.string.result_defeat)
    }
    val subtitle = if (won) {
        stringResource(R.string.result_victory_desc)
    } else {
        stringResource(R.string.result_defeat_desc)
    }
    val accent = if (won) Flota.Victoria else Flota.Derrota

    BarquitosScreen(title = stringResource(R.string.app_name)) { modifier ->
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.displaySmall,
                color = accent,
                textAlign = TextAlign.Center,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatTile(stringResource(R.string.result_shots), "${state.myShots}", Modifier.weight(1f))
                StatTile(stringResource(R.string.result_hits), "${state.myHits}", Modifier.weight(1f))
                StatTile(
                    stringResource(R.string.result_accuracy),
                    if (state.myShots == 0) "—" else "${state.myHits * 100 / state.myShots} %",
                    Modifier.weight(1f),
                )
                StatTile(stringResource(R.string.result_rounds), "${state.round}", Modifier.weight(1f))
            }


            Text(
                stringResource(R.string.game_enemy_waters),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.enemyRender?.let {
                BoardView(render = it.copy(enabled = false, showCoords = settings.showCoords))
            }

            Text(
                stringResource(R.string.game_my_fleet),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.myRender?.let {
                BoardView(render = it.copy(enabled = false, dimmed = false, showCoords = settings.showCoords))
            }

            OutlinedButton(onClick = onReveal, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.result_show_boards))
            }
            Button(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.result_home), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
