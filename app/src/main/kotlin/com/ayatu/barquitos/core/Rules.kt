package com.ayatu.barquitos.core

import kotlin.random.Random

/**
 * Cómo se reparten los disparos.
 *
 * - **UN_DISPARO**: un disparo por turno, turnos estrictamente alternos, aciertes o no. Es la regla
 *   clásica y la que viene puesta por defecto.
 * - **CONTINUO**: sigues disparando mientras aciertes; el primer fallo acaba tu turno.
 *
 * El orden de las constantes es el orden en que se ofrecen al elegir las reglas.
 */
enum class ShotRule(val resKey: String) {
    UN_DISPARO("rule_one_shot"),
    CONTINUO("rule_continuous"),
}

/** Nivel de la IA. */
enum class Difficulty(val resKey: String) {
    FACIL("difficulty_easy"),
    NORMAL("difficulty_normal"),
    DIFICIL("difficulty_hard"),
}

/** Con quién se juega. */
enum class GameMode(val resKey: String) {
    UN_JUGADOR("mode_single"),
    WIFI_LOCAL("mode_wifi"),
}

/**
 * Deja la configuración lista para publicarla por WiFi: «al azar» se sortea **aquí**, en el
 * anfitrión, antes de mandar las reglas al otro móvil.
 *
 * Es importante que esto no se haga en cada móvil por separado: los dos montan su propia partida
 * a partir de la misma configuración, así que si «quién empieza» viajara sin decidir, cada uno
 * echaría su dado y la mitad de las veces acabarían los dos esperando al otro.
 */
fun GameConfig.resolvedForHosting(rng: Random = Random): GameConfig =
    copy(startingPlayer = startingPlayer ?: rng.nextInt(2))

/** Configuración completa de una partida. La flota no se elige: es siempre la misma. */
data class GameConfig(
    val mode: GameMode = GameMode.UN_JUGADOR,
    val shotRule: ShotRule = ShotRule.UN_DISPARO,
    val difficulty: Difficulty = Difficulty.NORMAL,
    /** 0 = empieza el jugador 1, 1 = empieza el rival, null = se sortea. */
    val startingPlayer: Int? = null,
) {
    val boardSize: Int get() = Fleet.BOARD_SIZE

    /** Texto corto que resume las reglas, para la cabecera de la partida y el registro. */
    fun summary(): String = buildString {
        append(shotRule.name)
        if (mode == GameMode.UN_JUGADOR) {
            append(" / ")
            append(difficulty.name)
        }
    }
}
