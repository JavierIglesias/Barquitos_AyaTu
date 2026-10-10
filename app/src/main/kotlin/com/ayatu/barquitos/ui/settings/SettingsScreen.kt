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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.ayatu.barquitos.ui.theme.ThemeChoice

/** Ajustes: sonido, vibración, ayudas del tablero y aspecto. */
@Composable
fun SettingsScreen(appViewModel: AppViewModel, onBack: () -> Unit) {
    val settings by appViewModel.settings.collectAsStateWithLifecycle()

    BarquitosScreen(title = stringResource(R.string.settings_title), onBack = onBack) { modifier ->
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
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
            SwitchRow(
                title = stringResource(R.string.settings_animations),
                description = stringResource(R.string.settings_animations_desc),
                checked = settings.animations,
                onChange = appViewModel::setAnimations,
            )
            SwitchRow(
                title = stringResource(R.string.settings_show_coords),
                description = stringResource(R.string.settings_show_coords_desc),
                checked = settings.showCoords,
                onChange = appViewModel::setShowCoords,
            )
            SwitchRow(
                title = stringResource(R.string.settings_mark_water),
                description = stringResource(R.string.settings_mark_water_desc),
                checked = settings.markWater,
                onChange = appViewModel::setMarkWater,
            )
            SwitchRow(
                title = stringResource(R.string.settings_confirm_shot),
                description = stringResource(R.string.settings_confirm_shot_desc),
                checked = settings.confirmShot,
                onChange = appViewModel::setConfirmShot,
            )

            Text(
                stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeChip(ThemeChoice.SISTEMA, R.string.settings_theme_system, settings.theme, appViewModel)
                ThemeChip(ThemeChoice.CLARO, R.string.settings_theme_light, settings.theme, appViewModel)
                ThemeChip(ThemeChoice.OSCURO, R.string.settings_theme_dark, settings.theme, appViewModel)
            }

            Text(
                stringResource(R.string.settings_about),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
            )
            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
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
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
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
