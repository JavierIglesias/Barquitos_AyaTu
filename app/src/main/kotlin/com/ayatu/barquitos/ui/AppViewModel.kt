package com.ayatu.barquitos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ayatu.barquitos.data.MatchStore
import com.ayatu.barquitos.data.Settings
import com.ayatu.barquitos.data.SettingsStore
import com.ayatu.barquitos.data.Stats
import com.ayatu.barquitos.data.StatsStore
import com.ayatu.barquitos.ui.theme.ThemeChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lo que comparten todas las pantallas: los ajustes, las estadísticas y si hay una partida a
 * medias. Vive mientras vive la actividad, así que el tema y los contadores no se recargan al
 * moverse por el menú.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore(application)
    private val statsStore = StatsStore(application)
    private val matchStore = MatchStore(application)

    val settings: StateFlow<Settings> =
        settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    val stats: StateFlow<Stats> =
        statsStore.stats.stateIn(viewModelScope, SharingStarted.Eagerly, Stats())

    private val _hasSavedMatch = MutableStateFlow(false)
    val hasSavedMatch: StateFlow<Boolean> = _hasSavedMatch.asStateFlow()

    init {
        refreshSaved()
    }

    fun refreshSaved() {
        viewModelScope.launch { _hasSavedMatch.value = matchStore.hasSaved() }
    }

    fun setSound(value: Boolean) = edit { settingsStore.setSound(value) }
    fun setHaptics(value: Boolean) = edit { settingsStore.setHaptics(value) }
    fun setAnimations(value: Boolean) = edit { settingsStore.setAnimations(value) }
    fun setShowCoords(value: Boolean) = edit { settingsStore.setShowCoords(value) }
    fun setMarkWater(value: Boolean) = edit { settingsStore.setMarkWater(value) }
    fun setConfirmShot(value: Boolean) = edit { settingsStore.setConfirmShot(value) }
    fun setTheme(value: ThemeChoice) = edit { settingsStore.setTheme(value) }
    fun setPlayerName(value: String) = edit { settingsStore.setPlayerName(value) }
    fun resetStats() = edit { statsStore.reset() }

    private fun edit(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
