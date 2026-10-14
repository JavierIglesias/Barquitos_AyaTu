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
    /** Barcos del rival que siguen a flote. */
    val remaining: List<ShipDef>,
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

/**
 * Motor de la partida. Mantiene los dos tableros, de quién es el turno y quién ha ganado.
 *
 * El jugador de turno llama a [fire] una vez por disparo y el motor decide cuándo pasa el turno:
 * con [ShotRule.UN_DISPARO] siempre después de disparar, con [ShotRule.CONTINUO] solo al fallar.
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

    /** Ronda actual, empezando en 1. Sube cada vez que vuelve a disparar el jugador inicial. */
    var round: Int = 1
        private set

    /** Ganador: null mientras se juega, 0 o 1 el vencedor. */
    var winner: Int? = null
        private set

    val finished: Boolean get() = winner != null

    private val _history = mutableListOf<ShotRecord>()
    val history: List<ShotRecord> get() = _history

    /** Disparos totales de cada jugador, para las estadísticas. */
    private val shotsFired = intArrayOf(0, 0)
    private val hitsScored = intArrayOf(0, 0)

    private val firstPlayer = startingPlayer

    fun enemyOf(player: Int) = 1 - player

    fun shotsFiredBy(player: Int) = shotsFired[player]
    fun hitsBy(player: Int) = hitsScored[player]
    fun accuracyOf(player: Int): Float =
        if (shotsFired[player] == 0) 0f else hitsScored[player].toFloat() / shotsFired[player]

    /** Lo que [player] sabe del tablero de su rival. */
    fun viewFor(player: Int): BoardView {
        val enemyBoard = boards[enemyOf(player)]
        // Los barcos que le quedan al rival se deducen de los hundidos: al hundir uno, el juego
        // dice qué barco era, y la composición de la flota la saben los dos de antemano, así que
        // esto no es información oculta. Se saca del propio tablero (y no de la flota fija) para
        // que también valga con tableros hechos a mano en las pruebas.
        return BoardView(
            size = enemyBoard.size,
            misses = enemyBoard.misses,
            hits = enemyBoard.hits,
            sunkCells = enemyBoard.sunkCells,
            remaining = enemyBoard.aliveShips.map { it.def },
        )
    }

    /**
     * Dispara a [target] en el tablero del rival del jugador de turno. Devuelve null si la
     * partida ya acabó o si en esa casilla ya se había disparado.
     */
    fun fire(target: Coord): ShotOutcome? {
        if (finished) return null
        val shooter = current
        val outcome = boards[enemyOf(shooter)].receive(target) ?: return null
        register(shooter, outcome)
        if (outcome.fleetDestroyed) {
            winner = shooter
            return outcome
        }
        // En CONTINUO no se pierde el turno al acertar: se sigue disparando hasta fallar.
        if (config.shotRule == ShotRule.UN_DISPARO || !outcome.hit) endTurn()
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
    }

    // ------------------------------------------------------------------- guardar / restaurar

    /** Restaura el estado interno de una partida guardada. */
    fun restore(
        current: Int,
        round: Int,
        winner: Int?,
        history: List<ShotRecord>,
        shotsFired: IntArray,
        hitsScored: IntArray,
    ) {
        this.current = current
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
}
