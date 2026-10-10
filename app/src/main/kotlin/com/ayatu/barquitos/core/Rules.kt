package com.ayatu.barquitos.core

/**
 * Reglas de disparo. Deciden cuántas veces dispara cada jugador en su turno y cuándo pasa el
 * turno al rival.
 *
 * - **UN_DISPARO**: un disparo por turno, turnos estrictamente alternos.
 * - **RACHA**: sigues disparando mientras aciertes; el primer fallo acaba tu turno.
 * - **SALVA**: los dos jugadores disparan a la vez, un disparo por cada barco que les quede a
 *   flote. Según se hunde tu flota, disparas menos.
 * - **FUEGO_EN_CADENA**: disparas tantas veces como barcos te queden, pero uno a uno (ves el
 *   resultado de cada disparo antes de elegir el siguiente).
 * - **MULTIDISPARO**: un número fijo de disparos por turno (de 2 a 6, se elige al crear la
 *   partida).
 */
enum class ShotRule(val resKey: String) {
    UN_DISPARO("rule_one_shot"),
    RACHA("rule_streak"),
    SALVA("rule_salvo"),
    FUEGO_EN_CADENA("rule_chainfire"),
    MULTIDISPARO("rule_multishot");

    /** True si los dos jugadores disparan a la vez en la misma ronda. */
    val isSimultaneous: Boolean get() = this == SALVA

    /** True si el número de disparos del turno depende de los barcos que queden a flote. */
    val scalesWithFleet: Boolean get() = this == SALVA || this == FUEGO_EN_CADENA
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
    DOS_JUGADORES("mode_hotseat"),
    WIFI_LOCAL("mode_wifi"),
}

/** Configuración completa de una partida. */
data class GameConfig(
    val mode: GameMode = GameMode.UN_JUGADOR,
    val fleet: FleetType = FleetType.CLASICA,
    val shotRule: ShotRule = ShotRule.UN_DISPARO,
    /** Disparos por turno en MULTIDISPARO (2..6). Se ignora con las demás reglas. */
    val multiShotCount: Int = 3,
    val difficulty: Difficulty = Difficulty.NORMAL,
    /** 0 = empieza el jugador 1, 1 = empieza el rival, null = se sortea. */
    val startingPlayer: Int? = null,
) {
    val fleetDef: Fleet get() = Fleet.of(fleet)
    val boardSize: Int get() = fleetDef.boardSize

    /** Texto corto que resume las reglas, para la cabecera de la partida y el registro. */
    fun summary(): String = buildString {
        append(fleet.name)
        append(" / ")
        append(shotRule.name)
        if (shotRule == ShotRule.MULTIDISPARO) append("(x" + multiShotCount + ")")
        if (mode == GameMode.UN_JUGADOR) {
            append(" / ")
            append(difficulty.name)
        }
    }
}
