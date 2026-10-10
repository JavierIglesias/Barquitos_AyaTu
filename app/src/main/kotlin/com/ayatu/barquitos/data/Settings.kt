package com.ayatu.barquitos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ayatu.barquitos.core.Difficulty
import com.ayatu.barquitos.core.FleetType
import com.ayatu.barquitos.core.GameConfig
import com.ayatu.barquitos.core.GameMode
import com.ayatu.barquitos.core.ShotRule
import com.ayatu.barquitos.ui.theme.ThemeChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Un único DataStore para toda la app: ajustes, estadísticas y desbloqueos. */
val Context.barquitosStore: DataStore<Preferences> by preferencesDataStore(name = "barquitos")

/** Todo lo que el usuario puede tocar en Ajustes, más lo que eligió la última vez que jugó. */
data class Settings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val animations: Boolean = true,
    val showCoords: Boolean = true,
    /** En la flota Rusa, pintar sola el agua alrededor de un barco hundido (ahí no cabe otro). */
    val markWater: Boolean = true,
    /** Pedir una segunda pulsación antes de disparar, para no disparar por error. */
    val confirmShot: Boolean = false,
    val theme: ThemeChoice = ThemeChoice.SISTEMA,
    val playerName: String = "",
    val player2Name: String = "",
    val nationId: String = "neutral",
    val skinId: String = "neutral-liso",
    val emblemId: String = "ancla",
    /** Lo último que se eligió en «Nueva partida», para no repetir la configuración cada vez. */
    val lastConfig: GameConfig = GameConfig(),
)

class SettingsStore(private val context: Context) {

    val settings: Flow<Settings> = context.barquitosStore.data.map { p ->
        Settings(
            sound = p[K.SOUND] ?: true,
            haptics = p[K.HAPTICS] ?: true,
            animations = p[K.ANIMATIONS] ?: true,
            showCoords = p[K.SHOW_COORDS] ?: true,
            markWater = p[K.MARK_WATER] ?: true,
            confirmShot = p[K.CONFIRM_SHOT] ?: false,
            theme = p[K.THEME]?.let { runCatching { ThemeChoice.valueOf(it) }.getOrNull() }
                ?: ThemeChoice.SISTEMA,
            playerName = p[K.PLAYER_NAME] ?: "",
            player2Name = p[K.PLAYER2_NAME] ?: "",
            nationId = p[K.NATION] ?: "neutral",
            skinId = p[K.SKIN] ?: "neutral-liso",
            emblemId = p[K.EMBLEM] ?: "ancla",
            lastConfig = GameConfig(
                mode = p[K.LAST_MODE]?.let { enumOrNull<GameMode>(it) } ?: GameMode.UN_JUGADOR,
                fleet = p[K.LAST_FLEET]?.let { enumOrNull<FleetType>(it) } ?: FleetType.CLASICA,
                shotRule = p[K.LAST_RULE]?.let { enumOrNull<ShotRule>(it) } ?: ShotRule.UN_DISPARO,
                multiShotCount = p[K.LAST_MULTI] ?: 3,
                difficulty = p[K.LAST_DIFFICULTY]?.let { enumOrNull<Difficulty>(it) } ?: Difficulty.NORMAL,
                startingPlayer = (p[K.LAST_START] ?: -1).takeIf { it >= 0 },
            ),
        )
    }

    suspend fun setSound(value: Boolean) = put(K.SOUND, value)
    suspend fun setHaptics(value: Boolean) = put(K.HAPTICS, value)
    suspend fun setAnimations(value: Boolean) = put(K.ANIMATIONS, value)
    suspend fun setShowCoords(value: Boolean) = put(K.SHOW_COORDS, value)
    suspend fun setMarkWater(value: Boolean) = put(K.MARK_WATER, value)
    suspend fun setConfirmShot(value: Boolean) = put(K.CONFIRM_SHOT, value)
    suspend fun setTheme(value: ThemeChoice) = put(K.THEME, value.name)
    suspend fun setPlayerName(value: String) = put(K.PLAYER_NAME, value)
    suspend fun setPlayer2Name(value: String) = put(K.PLAYER2_NAME, value)
    suspend fun setNation(value: String) = put(K.NATION, value)
    suspend fun setSkin(value: String) = put(K.SKIN, value)
    suspend fun setEmblem(value: String) = put(K.EMBLEM, value)

    suspend fun saveLastConfig(config: GameConfig) {
        context.barquitosStore.edit { p ->
            p[K.LAST_MODE] = config.mode.name
            p[K.LAST_FLEET] = config.fleet.name
            p[K.LAST_RULE] = config.shotRule.name
            p[K.LAST_MULTI] = config.multiShotCount
            p[K.LAST_DIFFICULTY] = config.difficulty.name
            p[K.LAST_START] = config.startingPlayer ?: -1
        }
    }

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.barquitosStore.edit { it[key] = value }
    }

    private object K {
        val SOUND = booleanPreferencesKey("sound")
        val HAPTICS = booleanPreferencesKey("haptics")
        val ANIMATIONS = booleanPreferencesKey("animations")
        val SHOW_COORDS = booleanPreferencesKey("show_coords")
        val MARK_WATER = booleanPreferencesKey("mark_water")
        val CONFIRM_SHOT = booleanPreferencesKey("confirm_shot")
        val THEME = stringPreferencesKey("theme")
        val PLAYER_NAME = stringPreferencesKey("player_name")
        val PLAYER2_NAME = stringPreferencesKey("player2_name")
        val NATION = stringPreferencesKey("nation")
        val SKIN = stringPreferencesKey("skin")
        val EMBLEM = stringPreferencesKey("emblem")
        val LAST_MODE = stringPreferencesKey("last_mode")
        val LAST_FLEET = stringPreferencesKey("last_fleet")
        val LAST_RULE = stringPreferencesKey("last_rule")
        val LAST_MULTI = intPreferencesKey("last_multi")
        val LAST_DIFFICULTY = stringPreferencesKey("last_difficulty")
        val LAST_START = intPreferencesKey("last_start")
    }
}

private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
    runCatching { enumValueOf<T>(name) }.getOrNull()
