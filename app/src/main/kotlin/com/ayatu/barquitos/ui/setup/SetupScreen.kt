package com.ayatu.barquitos.ui.setup

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ayatu.barquitos.R
import com.ayatu.barquitos.core.Difficulty
import com.ayatu.barquitos.core.FleetType
import com.ayatu.barquitos.core.GameConfig
import com.ayatu.barquitos.core.GameMode
import com.ayatu.barquitos.core.ShotRule
import com.ayatu.barquitos.ui.common.BarquitosScreen
import com.ayatu.barquitos.ui.common.OptionCard
import com.ayatu.barquitos.ui.common.descriptionRes
import com.ayatu.barquitos.ui.common.labelRes

/**
 * «Nueva partida»: se elige la flota, la regla de disparo y, contra la máquina, el nivel. Es la
 * misma pantalla para un jugador, para dos en el mismo móvil y para crear una partida por WiFi
 * (en ese caso las reglas que se elijan aquí son las que jugarán los dos).
 */
@Composable
fun SetupScreen(
    mode: GameMode,
    initial: GameConfig,
    player1: String,
    player2: String,
    onNames: (String, String) -> Unit,
    onBack: () -> Unit,
    onStart: (GameConfig) -> Unit,
) {
    var fleet by remember { mutableStateOf(initial.fleet) }
    var rule by remember { mutableStateOf(initial.shotRule) }
    var multi by remember { mutableStateOf(initial.multiShotCount) }
    var difficulty by remember { mutableStateOf(initial.difficulty) }
    var starting by remember { mutableStateOf(initial.startingPlayer) }
    var name1 by remember { mutableStateOf(player1) }
    var name2 by remember { mutableStateOf(player2) }

    BarquitosScreen(title = stringResource(R.string.setup_title), onBack = onBack) { modifier ->
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle(stringResource(R.string.setup_fleet))
            FleetType.entries.forEach { option ->
                OptionCard(
                    title = stringResource(option.labelRes),
                    description = stringResource(option.descriptionRes),
                    selected = fleet == option,
                    onClick = { fleet = option },
                )
            }

            SectionTitle(stringResource(R.string.setup_rule))
            ShotRule.entries.forEach { option ->
                OptionCard(
                    title = stringResource(option.labelRes),
                    description = stringResource(option.descriptionRes),
                    selected = rule == option,
                    onClick = { rule = option },
                )
            }

            if (rule == ShotRule.MULTIDISPARO) {
                SectionTitle(stringResource(R.string.setup_shots_per_turn))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (2..6).forEach { n ->
                        FilterChip(
                            selected = multi == n,
                            onClick = { multi = n },
                            label = { Text("$n") },
                        )
                    }
                }
            }

            if (mode == GameMode.UN_JUGADOR) {
                SectionTitle(stringResource(R.string.setup_difficulty))
                Difficulty.entries.forEach { option ->
                    OptionCard(
                        title = stringResource(option.labelRes),
                        description = stringResource(option.descriptionRes),
                        selected = difficulty == option,
                        onClick = { difficulty = option },
                    )
                }
            }

            SectionTitle(stringResource(R.string.setup_who_starts))
            // En dos jugadores los botones llevan los nombres; en los demás modos, «Yo» / «El rival».
            val defaultName1 = stringResource(R.string.setup_player1)
            val defaultName2 = stringResource(R.string.setup_player2)
            val firstLabel = if (mode == GameMode.DOS_JUGADORES) {
                name1.ifBlank { defaultName1 }
            } else {
                stringResource(R.string.setup_start_me)
            }
            val secondLabel = if (mode == GameMode.DOS_JUGADORES) {
                name2.ifBlank { defaultName2 }
            } else {
                stringResource(R.string.setup_start_rival)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = starting == 0,
                    onClick = { starting = 0 },
                    label = { Text(firstLabel) },
                )
                FilterChip(
                    selected = starting == 1,
                    onClick = { starting = 1 },
                    label = { Text(secondLabel) },
                )
                FilterChip(
                    selected = starting == null,
                    onClick = { starting = null },
                    label = { Text(stringResource(R.string.setup_start_random)) },
                )
            }

            if (mode == GameMode.DOS_JUGADORES) {
                SectionTitle(stringResource(R.string.setup_player_names))
                OutlinedTextField(
                    value = name1,
                    onValueChange = { name1 = it.take(14) },
                    label = { Text(stringResource(R.string.setup_player1)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = name2,
                    onValueChange = { name2 = it.take(14) },
                    label = { Text(stringResource(R.string.setup_player2)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    onNames(name1, name2)
                    onStart(
                        GameConfig(
                            mode = mode,
                            fleet = fleet,
                            shotRule = rule,
                            multiShotCount = multi,
                            difficulty = difficulty,
                            startingPlayer = starting,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text(
                    stringResource(R.string.setup_play),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}
