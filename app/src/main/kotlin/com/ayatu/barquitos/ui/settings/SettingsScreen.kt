package com.ayatu.barquitos.ui.settings

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
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayatu.barquitos.BuildConfig
import com.ayatu.barquitos.R
import com.ayatu.barquitos.ui.AppViewModel
import com.ayatu.barquitos.ui.common.BarquitosScreen
import com.ayatu.barquitos.ui.common.OptionGroup
import com.ayatu.barquitos.ui.theme.ThemeChoice

/**
 * Ajustes. Las opciones van en grupos plegables con icono y título, el mismo patrón que usan
 * Radio AyaTu y Rutas AyaTu: así las tres apps de la casa se manejan igual y la pantalla no es
 * una lista interminable de interruptores.
 */
@Composable
fun SettingsScreen(appViewModel: AppViewModel, onBack: () -> Unit) {
    val settings by appViewModel.settings.collectAsStateWithLifecycle()

    BarquitosScreen(title = stringResource(R.string.settings_title), onBack = onBack) { modifier ->
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OptionGroup(
                icon = Icons.Default.Sailing,
                title = stringResource(R.string.settings_group_game),
                summary = stringResource(R.string.settings_group_game_summary),
                initiallyExpanded = true,
            ) {
                SwitchRow(
                    title = stringResource(R.string.settings_confirm_shot),
                    description = stringResource(R.string.settings_confirm_shot_desc),
                    checked = settings.confirmShot,
                    onChange = appViewModel::setConfirmShot,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_mark_water),
                    description = stringResource(R.string.settings_mark_water_desc),
                    checked = settings.markWater,
                    onChange = appViewModel::setMarkWater,
                )
            }

            OptionGroup(
                icon = Icons.Default.GridOn,
                title = stringResource(R.string.settings_group_board),
                summary = stringResource(R.string.settings_group_board_summary),
            ) {
                SwitchRow(
                    title = stringResource(R.string.settings_show_coords),
                    description = stringResource(R.string.settings_show_coords_desc),
                    checked = settings.showCoords,
                    onChange = appViewModel::setShowCoords,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_animations),
                    description = stringResource(R.string.settings_animations_desc),
                    checked = settings.animations,
                    onChange = appViewModel::setAnimations,
                )
            }

            OptionGroup(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = stringResource(R.string.settings_group_sound),
                summary = stringResource(R.string.settings_group_sound_summary),
            ) {
                SwitchRow(
                    title = stringResource(R.string.settings_sound),
                    description = stringResource(R.string.settings_sound_desc),
                    checked = settings.sound,
                    onChange = appViewModel::setSound,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_haptics),
                    description = stringResource(R.string.settings_haptics_desc),
                    checked = settings.haptics,
                    onChange = appViewModel::setHaptics,
                )
                OutlinedButton(
                    onClick = appViewModel::testSound,
                    enabled = settings.sound,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.padding(2.dp))
                    Text(stringResource(R.string.settings_sound_test))
                }
                Text(
                    stringResource(R.string.settings_sound_test_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OptionGroup(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.settings_theme),
                summary = stringResource(
                    when (settings.theme) {
                        ThemeChoice.SISTEMA -> R.string.settings_theme_system
                        ThemeChoice.CLARO -> R.string.settings_theme_light
                        ThemeChoice.OSCURO -> R.string.settings_theme_dark
                    },
                ),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChip(ThemeChoice.SISTEMA, R.string.settings_theme_system, settings.theme, appViewModel)
                    ThemeChip(ThemeChoice.CLARO, R.string.settings_theme_light, settings.theme, appViewModel)
                    ThemeChip(ThemeChoice.OSCURO, R.string.settings_theme_dark, settings.theme, appViewModel)
                }
            }

            OptionGroup(
                icon = Icons.Default.Person,
                title = stringResource(R.string.shipyard_name),
                summary = settings.playerName.ifBlank { stringResource(R.string.common_you) },
            ) {
                OutlinedTextField(
                    value = settings.playerName,
                    onValueChange = appViewModel::setPlayerName,
                    label = { Text(stringResource(R.string.shipyard_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OptionGroup(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_about),
                summary = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            ) {
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.settings_about_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeChip(
    choice: ThemeChoice,
    labelRes: Int,
    current: ThemeChoice,
    appViewModel: AppViewModel,
) {
    FilterChip(
        selected = current == choice,
        onClick = { appViewModel.setTheme(choice) },
        label = { Text(stringResource(labelRes)) },
    )
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
