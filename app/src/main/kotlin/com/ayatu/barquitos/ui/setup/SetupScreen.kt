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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ayatu.barquitos.R
import com.ayatu.barquitos.core.Difficulty
import com.ayatu.barquitos.core.Fleet
import com.ayatu.barquitos.core.GameConfig
import com.ayatu.barquitos.core.GameMode
import com.ayatu.barquitos.core.ShotRule
import com.ayatu.barquitos.ui.common.BarquitosScreen
import com.ayatu.barquitos.ui.common.OptionCard
import com.ayatu.barquitos.ui.common.OptionGroup
import com.ayatu.barquitos.ui.common.descriptionRes
import com.ayatu.barquitos.ui.common.labelRes

/**
 * «Nueva partida». La flota no se elige —es siempre la misma—, así que lo único que hay que
 * decidir es cómo se reparten los disparos, el nivel de la máquina y quién abre fuego.
 *
 * Las opciones van en grupos plegables, igual que en Radio AyaTu y en Rutas AyaTu. Al crear una
 * partida por WiFi, lo que se elija aquí es lo que jugarán los dos móviles: el anfitrión manda
 * sus reglas al invitado al conectarse.
 */
@Composable
fun SetupScreen(
    mode: GameMode,
    initial: GameConfig,
    playerName: String,
    onName: (String) -> Unit,
    onBack: () -> Unit,
    onStart: (GameConfig) -> Unit,
) {
    var rule by remember { mutableStateOf(initial.shotRule) }
    var difficulty by remember { mutableStateOf(initial.difficulty) }
    var starting by remember { mutableStateOf(initial.startingPlayer) }
    var name by remember { mutableStateOf(playerName) }

    val startingLabel = when (starting) {
        0 -> stringResource(R.string.setup_start_me)
        1 -> stringResource(R.string.setup_start_rival)
        else -> stringResource(R.string.setup_start_random)
    }

    BarquitosScreen(title = stringResource(R.string.setup_title), onBack = onBack) { modifier ->
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // La flota no se elige, pero conviene recordar con qué se juega.
            Text(
                stringResource(
                    R.string.setup_fleet_fixed,
                    Fleet.shipCount,
                    Fleet.totalCells,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OptionGroup(
                icon = Icons.Default.GpsFixed,
                title = stringResource(R.string.setup_rule),
                summary = stringResource(rule.labelRes),
            ) {
                ShotRule.entries.forEach { option ->
                    OptionCard(
                        title = stringResource(option.labelRes),
                        description = stringResource(option.descriptionRes),
                        selected = rule == option,
                        onClick = { rule = option },
                    )
                }
            }

            if (mode == GameMode.UN_JUGADOR) {
                OptionGroup(
                    icon = Icons.Default.SmartToy,
                    title = stringResource(R.string.setup_difficulty),
                    summary = stringResource(difficulty.labelRes),
                ) {
                    Difficulty.entries.forEach { option ->
                        OptionCard(
                            title = stringResource(option.labelRes),
                            description = stringResource(option.descriptionRes),
                            selected = difficulty == option,
                            onClick = { difficulty = option },
                        )
                    }
                }
            }

            OptionGroup(
                icon = Icons.Default.Casino,
                title = stringResource(R.string.setup_who_starts),
                summary = startingLabel,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = starting == 0,
                        onClick = { starting = 0 },
                        label = { Text(stringResource(R.string.setup_start_me)) },
                    )
                    FilterChip(
                        selected = starting == 1,
                        onClick = { starting = 1 },
                        label = { Text(stringResource(R.string.setup_start_rival)) },
                    )
                    FilterChip(
                        selected = starting == null,
                        onClick = { starting = null },
                        label = { Text(stringResource(R.string.setup_start_random)) },
                    )
                }
            }

            OptionGroup(
                icon = Icons.Default.Person,
                title = stringResource(R.string.setup_player_names),
                summary = name.ifBlank { stringResource(R.string.common_you) },
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onName(it)
                    },
                    label = { Text(stringResource(R.string.shipyard_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = {
                    onStart(
                        GameConfig(
                            mode = mode,
                            shotRule = rule,
                            difficulty = difficulty,
                            startingPlayer = starting,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.padding(2.dp))
                Text(stringResource(R.string.setup_play), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
