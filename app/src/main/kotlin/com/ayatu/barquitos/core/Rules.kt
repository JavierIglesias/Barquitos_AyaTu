package com.ayatu.barquitos.core

/**
 * Cómo se reparten los disparos.
 *
 * - **RACHA**: sigues disparando mientras aciertes; el primer fallo acaba tu turno. Es como se
 *   juega normalmente a lápiz y papel, y es la regla por defecto.
 * - **UN_DISPARO**: un disparo por turno, turnos estrictamente alternos, aciertes o no.
 */
enum class ShotRule(val resKey: String) {
    RACHA("rule_streak"),
    UN_DISPARO("rule_one_shot"),
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

/** Configuración completa de una partida. La flota no se elige: es siempre la misma. */
data class GameConfig(
    val mode: GameMode = GameMode.UN_JUGADOR,
    val shotRule: ShotRule = ShotRule.RACHA,
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
