package com.ayatu.barquitos.core

/** Lo que un atacante llega a saber del tablero rival. Es lo único que recibe la IA: nunca ve
 *  dónde están los barcos que no ha hundido. */
data class BoardView(
    val size: Int,
    /** Disparos al agua. */
    val misses: Set<Coord>,
    /** Disparos que dieron en un barco (hundido o no). */
    val hits: Set<Coord>,
    /** Casillas de los barcos ya hundidos (se revelan al hundirlos). */
    val sunkCells: Set<Coord>,
    /** Barcos del rival que siguen a flote, por tipo y forma. */
    val remaining: List<ShipDef>,
    val mustSeparate: Boolean,
) {
    val shots: Set<Coord> get() = misses + hits

    /** Impactos que pertenecen a barcos todavía a flote: las pistas calientes. */
    val activeHits: Set<Coord> get() = hits - sunkCells

    fun shot(c: Coord) = c in misses || c in hits
    fun inBoard(c: Coord) = c.x in 0 until size && c.y in 0 until size
}

/** Una línea del parte de batalla. */
data class ShotRecord(
    val round: Int,
    val player: Int,
    val coord: Coord,
    val hit: Boolean,
    val sunkKind: ShipKind?,
)

/** Resultado de una ronda de SALVA: lo que ha disparado cada jugador. */
data class SalvoRound(val outcomes: Map<Int, List<ShotOutcome>>)

/**
 * Motor de la partida. Mantiene los dos tableros, de quién es el turno y cuántos disparos le
 * quedan, y aplica las cinco reglas de disparo.
 *
 * Hay dos formas de jugar un turno, según la regla:
 *
 *  - **Secuencial** (UN_DISPARO, RACHA, FUEGO_EN_CADENA, MULTIDISPARO): el jugador de turno llama
 *    a [fire] una vez por disparo y el motor decide cuándo pasa el turno.
 *  - **Simultánea** (SALVA): cada jugador marca sus objetivos con [toggleAim] y confirma con
 *    [confirmAim]; cuando los dos han confirmado, [resolveSalvo] revela los disparos de ambos a
 *    la vez.
 */
class Game(
    val config: GameConfig,
    val boards: List<PlayerBoard>,
    startingPlayer: Int = 0,
) {
    init {
        require(boards.size == 2) { "Barquitos se juega entre dos tableros" }
    }

    var current: Int = startingPlayer
        private set

    /** Disparos que le quedan al jugador de turno (solo reglas secuenciales). */
    var shotsLeft: Int = 0
        private set

    /** Ronda actual, empezando en 1. Sube cada vez que vuelve a disparar el jugador inicial. */
    var round: Int = 1
        private set

    /** Ganador: null mientras se juega, 0 o 1 el vencedor, [DRAW] si se hunden las dos flotas. */
    var winner: Int? = null
        private set

    val finished: Boolean get() = winner != null

    private val _history = mutableListOf<ShotRecord>()
    val history: List<ShotRecord> get() = _history

    /** Objetivos marcados por cada jugador en la ronda de SALVA. */
    private val aim = listOf(mutableListOf<Coord>(), mutableListOf<Coord>())
    private val aimConfirmed = booleanArrayOf(false, false)

    /** Disparos totales de cada jugador, para las estadísticas. */
    private val shotsFired = intArrayOf(0, 0)
    private val hitsScored = intArrayOf(0, 0)

    private val firstPlayer = startingPlayer

    init {
        shotsLeft = if (config.shotRule.isSimultaneous) 0 else shotsForTurn(current)
    }

    fun enemyOf(player: Int) = 1 - player

    fun shotsFiredBy(player: Int) = shotsFired[player]
    fun hitsBy(player: Int) = hitsScored[player]
    fun accuracyOf(player: Int): Float =
        if (shotsFired[player] == 0) 0f else hitsScored[player].toFloat() / shotsFired[player]

    /** Cuántos disparos le tocan a [player] si empezara turno ahora mismo. */
    fun shotsForTurn(player: Int): Int = when (config.shotRule) {
        ShotRule.UN_DISPARO, ShotRule.RACHA -> 1
        ShotRule.MULTIDISPARO -> config.multiShotCount
        // «Tantos disparos como barcos te queden a flote»: según te hunden, disparas menos.
        ShotRule.FUEGO_EN_CADENA, ShotRule.SALVA -> boards[player].aliveShips.size.coerceAtLeast(1)
    }

    /** Lo que [player] sabe del tablero de su rival. */
    fun viewFor(player: Int): BoardView {
        val enemyBoard = boards[enemyOf(player)]
        // Los barcos que le quedan al rival se deducen de los hundidos: al hundir uno, el juego
        // dice qué barco era, y la composición de la flota se pacta al empezar, así que esto no
        // es información oculta. Se saca del propio tablero (y no de la flota de la
        // configuración) para que también valga con tableros hechos a mano en las pruebas.
        return BoardView(
            size = enemyBoard.size,
            misses = enemyBoard.misses,
            hits = enemyBoard.hits,
            sunkCells = enemyBoard.sunkCells,
            remaining = enemyBoard.aliveShips.map { it.def },
            mustSeparate = config.fleetDef.mustSeparate,
        )
    }

    // ----------------------------------------------------------------- reglas secuenciales

    /**
     * Dispara a [target] en el tablero del rival del jugador de turno. Devuelve null si la
     * partida ya acabó, si la regla es simultánea o si en esa casilla ya se había disparado.
     */
    fun fire(target: Coord): ShotOutcome? {
        if (finished || config.shotRule.isSimultaneous) return null
        val shooter = current
        val outcome = boards[enemyOf(shooter)].receive(target) ?: return null
        register(shooter, outcome)
        if (outcome.fleetDestroyed) {
            winner = shooter
            return outcome
        }
        when (config.shotRule) {
            // En RACHA no se gasta disparo al acertar: se sigue disparando hasta fallar.
            ShotRule.RACHA -> if (!outcome.hit) endTurn()
            else -> {
                shotsLeft -= 1
                if (shotsLeft <= 0) endTurn()
            }
        }
        return outcome
    }

    private fun register(shooter: Int, outcome: ShotOutcome) {
        shotsFired[shooter] += 1
        if (outcome.hit) hitsScored[shooter] += 1
        _history += ShotRecord(round, shooter, outcome.coord, outcome.hit, outcome.sunk?.kind)
    }

    private fun endTurn() {
        current = enemyOf(current)
        if (current == firstPlayer) round += 1
        shotsLeft = shotsForTurn(current)
    }

    // ------------------------------------------------------------------- regla SALVA

    /** Cuántos objetivos tiene que marcar [player] esta ronda. */
    fun salvoSize(player: Int) = shotsForTurn(player)

    /** Objetivos ya marcados por [player]. */
    fun aimOf(player: Int): List<Coord> = aim[player]

    fun hasConfirmed(player: Int) = aimConfirmed[player]

    /**
     * Marca o desmarca [target] para el jugador de turno. Devuelve true si la lista cambió
     * (false si la casilla ya estaba disparada o si la salva está completa).
     */
    fun toggleAim(target: Coord): Boolean {
        if (finished || !config.shotRule.isSimultaneous) return false
        val player = current
        if (aimConfirmed[player]) return false
        val list = aim[player]
        if (list.remove(target)) return true
        val enemyBoard = boards[enemyOf(player)]
        if (!enemyBoard.contains(target) || enemyBoard.alreadyShot(target)) return false
        if (list.size >= salvoSize(player)) return false
        list += target
        return true
    }

    /** Fija directamente la salva de [player] (la que elige la IA o la que llega por WiFi). */
    fun setAim(player: Int, targets: List<Coord>) {
        aim[player].clear()
        aim[player] += targets.take(salvoSize(player))
        aimConfirmed[player] = true
    }

    /**
     * Da por buena la salva del jugador de turno. Devuelve true si ya han confirmado los dos y
     * toca llamar a [resolveSalvo]; false si falta el otro jugador (y entonces [current] pasa a
     * ser él).
     */
    fun confirmAim(): Boolean {
        if (finished || !config.shotRule.isSimultaneous) return false
        if (aim[current].size != salvoSize(current)) return false
        aimConfirmed[current] = true
        val other = enemyOf(current)
        if (aimConfirmed[other]) return true
        current = other
        return false
    }

    /**
     * Resuelve la ronda de SALVA: los dos jugadores disparan a la vez, así que los impactos de
     * uno no cambian la salva del otro aunque le hundan barcos.
     */
    fun resolveSalvo(): SalvoRound {
        require(config.shotRule.isSimultaneous) { "resolveSalvo solo vale con la regla SALVA" }
        val results = mutableMapOf<Int, List<ShotOutcome>>()
        for (player in 0..1) {
            val outcomes = aim[player].mapNotNull { boards[enemyOf(player)].receive(it) }
            outcomes.forEach { register(player, it) }
            results[player] = outcomes
        }
        val destroyed0 = boards[0].fleetDestroyed
        val destroyed1 = boards[1].fleetDestroyed
        winner = when {
            destroyed0 && destroyed1 -> DRAW
            destroyed1 -> 0
            destroyed0 -> 1
            else -> null
        }
        aim[0].clear(); aim[1].clear()
        aimConfirmed[0] = false; aimConfirmed[1] = false
        if (winner == null) {
            round += 1
            current = firstPlayer
        }
        return SalvoRound(results)
    }

    // ------------------------------------------------------------------- guardar / restaurar

    /** Restaura el estado interno de una partida guardada. */
    fun restore(
        current: Int,
        shotsLeft: Int,
        round: Int,
        winner: Int?,
        history: List<ShotRecord>,
        shotsFired: IntArray,
        hitsScored: IntArray,
    ) {
        this.current = current
        this.shotsLeft = shotsLeft
        this.round = round
        this.winner = winner
        this._history.clear()
        this._history += history
        this.shotsFired[0] = shotsFired[0]
        this.shotsFired[1] = shotsFired[1]
        this.hitsScored[0] = hitsScored[0]
        this.hitsScored[1] = hitsScored[1]
    }

    fun snapshotCounters(): Pair<IntArray, IntArray> = shotsFired.copyOf() to hitsScored.copyOf()

    companion object {
        /** Valor de [winner] cuando las dos flotas se hunden en la misma salva. */
        const val DRAW = -1
    }
}

private fun PlayerBoard.contains(c: Coord) = c.x in 0 until size && c.y in 0 until size
