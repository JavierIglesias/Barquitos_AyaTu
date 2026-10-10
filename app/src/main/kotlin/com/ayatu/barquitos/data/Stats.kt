package com.ayatu.barquitos.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.ayatu.barquitos.core.Difficulty
import com.ayatu.barquitos.core.GameConfig
import com.ayatu.barquitos.core.GameMode
import com.ayatu.barquitos.core.ShotRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Partidas jugadas y ganadas de un corte concreto (una regla, un nivel). */
data class Record(val played: Int = 0, val won: Int = 0) {
    val winRate: Float get() = if (played == 0) 0f else won.toFloat() / played
}

/** Estadísticas del jugador: cuentan las partidas contra la máquina y las de WiFi local. */
data class Stats(
    val played: Int = 0,
    val won: Int = 0,
    val lost: Int = 0,
    val shots: Int = 0,
    val hits: Int = 0,
    val sunkByMe: Int = 0,
    val sunkOnMe: Int = 0,
    /** Disparos de la victoria más limpia (0 si todavía no se ha ganado ninguna). */
    val bestWinShots: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    /** Mejor porcentaje de acierto en una partida ganada, de 0 a 100. */
    val bestAccuracy: Int = 0,
    val byRule: Map<ShotRule, Record> = emptyMap(),
    val byDifficulty: Map<Difficulty, Record> = emptyMap(),
) {
    val accuracy: Float get() = if (shots == 0) 0f else hits.toFloat() / shots
    val winRate: Float get() = if (played == 0) 0f else won.toFloat() / played
    val isEmpty: Boolean get() = played == 0

    /** Con cuántas reglas distintas se ha ganado alguna vez (para las medallas). */
    val rulesWon: Int get() = byRule.count { it.value.won > 0 }

    val hardWins: Int get() = byDifficulty[Difficulty.DIFICIL]?.won ?: 0
}

/** Cómo acabó una partida, desde el punto de vista del jugador local. */
enum class MatchResult { GANADA, PERDIDA }

/** Resumen de una partida acabada, lo que hace falta para actualizar las estadísticas. */
data class MatchSummary(
    val config: GameConfig,
    val result: MatchResult,
    val myShots: Int,
    val myHits: Int,
    val shipsSunkByMe: Int,
    val shipsSunkOnMe: Int,
)

class StatsStore(private val context: Context) {

    val stats: Flow<Stats> = context.barquitosStore.data.map { p ->
        Stats(
            played = p[K.PLAYED] ?: 0,
            won = p[K.WON] ?: 0,
            lost = p[K.LOST] ?: 0,
            shots = p[K.SHOTS] ?: 0,
            hits = p[K.HITS] ?: 0,
            sunkByMe = p[K.SUNK_BY_ME] ?: 0,
            sunkOnMe = p[K.SUNK_ON_ME] ?: 0,
            bestWinShots = p[K.BEST_WIN_SHOTS] ?: 0,
            currentStreak = p[K.STREAK] ?: 0,
            bestStreak = p[K.BEST_STREAK] ?: 0,
            bestAccuracy = p[K.BEST_ACCURACY] ?: 0,
            byRule = ShotRule.entries.associateWith { readRecord(p, "rule", it.name) },
            byDifficulty = Difficulty.entries.associateWith { readRecord(p, "diff", it.name) },
        )
    }

    /** Apunta una partida acabada. */
    suspend fun record(summary: MatchSummary) {
        val config = summary.config
        context.barquitosStore.edit { p ->
            p[K.PLAYED] = (p[K.PLAYED] ?: 0) + 1
            p[K.SHOTS] = (p[K.SHOTS] ?: 0) + summary.myShots
            p[K.HITS] = (p[K.HITS] ?: 0) + summary.myHits
            p[K.SUNK_BY_ME] = (p[K.SUNK_BY_ME] ?: 0) + summary.shipsSunkByMe
            p[K.SUNK_ON_ME] = (p[K.SUNK_ON_ME] ?: 0) + summary.shipsSunkOnMe
            when (summary.result) {
                MatchResult.GANADA -> {
                    p[K.WON] = (p[K.WON] ?: 0) + 1
                    val streak = (p[K.STREAK] ?: 0) + 1
                    p[K.STREAK] = streak
                    p[K.BEST_STREAK] = maxOf(p[K.BEST_STREAK] ?: 0, streak)
                    val best = p[K.BEST_WIN_SHOTS] ?: 0
                    if (summary.myShots > 0 && (best == 0 || summary.myShots < best)) {
                        p[K.BEST_WIN_SHOTS] = summary.myShots
                    }
                    val accuracy = if (summary.myShots == 0) 0
                    else (summary.myHits * 100) / summary.myShots
                    p[K.BEST_ACCURACY] = maxOf(p[K.BEST_ACCURACY] ?: 0, accuracy)
                }
                MatchResult.PERDIDA -> {
                    p[K.LOST] = (p[K.LOST] ?: 0) + 1
                    p[K.STREAK] = 0
                }
            }
            bumpRecord(p, "rule", config.shotRule.name, summary.result == MatchResult.GANADA)
            if (config.mode == GameMode.UN_JUGADOR) {
                bumpRecord(p, "diff", config.difficulty.name, summary.result == MatchResult.GANADA)
            }
        }
    }

    suspend fun reset() {
        context.barquitosStore.edit { p ->
            // Se borran solo las claves de estadísticas: los ajustes y los desbloqueos se quedan.
            val prefixes = listOf("stat_", "rule_", "diff_")
            p.asMap().keys.filter { key -> prefixes.any { key.name.startsWith(it) } }
                .forEach { p.remove(it) }
        }
    }

    private fun readRecord(p: Preferences, group: String, name: String) = Record(
        played = p[intPreferencesKey("${group}_${name}_played")] ?: 0,
        won = p[intPreferencesKey("${group}_${name}_won")] ?: 0,
    )

    private fun bumpRecord(p: androidx.datastore.preferences.core.MutablePreferences, group: String, name: String, won: Boolean) {
        val playedKey = intPreferencesKey("${group}_${name}_played")
        p[playedKey] = (p[playedKey] ?: 0) + 1
        if (won) {
            val wonKey = intPreferencesKey("${group}_${name}_won")
            p[wonKey] = (p[wonKey] ?: 0) + 1
        }
    }

    private object K {
        val PLAYED = intPreferencesKey("stat_played")
        val WON = intPreferencesKey("stat_won")
        val LOST = intPreferencesKey("stat_lost")
        val SHOTS = intPreferencesKey("stat_shots")
        val HITS = intPreferencesKey("stat_hits")
        val SUNK_BY_ME = intPreferencesKey("stat_sunk_by_me")
        val SUNK_ON_ME = intPreferencesKey("stat_sunk_on_me")
        val BEST_WIN_SHOTS = intPreferencesKey("stat_best_win_shots")
        val STREAK = intPreferencesKey("stat_streak")
        val BEST_STREAK = intPreferencesKey("stat_best_streak")
        val BEST_ACCURACY = intPreferencesKey("stat_best_accuracy")
    }
}
