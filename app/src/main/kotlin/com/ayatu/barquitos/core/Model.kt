package com.ayatu.barquitos.core

/**
 * Modelo del juego, Kotlin puro y sin dependencias de Android para que se pueda probar con
 * pruebas unitarias normales (ver `app/src/test/kotlin/.../core`).
 *
 * Convenio de coordenadas: (0,0) es la esquina superior izquierda; x crece hacia la derecha
 * (columnas A, B, C...) e y hacia abajo (filas 1, 2, 3...).
 */
data class Coord(val x: Int, val y: Int) {
    /** Nombre humano de la casilla, estilo «C7», como se escribe en el parte de batalla. */
    fun label(): String = "${('A' + x)}${y + 1}"

    operator fun plus(other: Coord) = Coord(x + other.x, y + other.y)

    companion object {
        /** Las ocho casillas que rodean a una (incluidas las diagonales). */
        val NEIGHBOURS_8 = listOf(
            Coord(-1, -1), Coord(0, -1), Coord(1, -1),
            Coord(-1, 0), Coord(1, 0),
            Coord(-1, 1), Coord(0, 1), Coord(1, 1),
        )

        /** Las cuatro casillas en cruz. */
        val NEIGHBOURS_4 = listOf(Coord(0, -1), Coord(0, 1), Coord(-1, 0), Coord(1, 0))
    }
}

/** Cómo se apoya un barco en el tablero. Todos los barcos son líneas rectas, así que con dos
 *  posiciones basta. */
enum class Orientation {
    HORIZONTAL, VERTICAL;

    fun next(): Orientation = if (this == HORIZONTAL) VERTICAL else HORIZONTAL
}

/**
 * Forma de un barco como conjunto de casillas relativas, siempre normalizada: la casilla más
 * arriba y más a la izquierda del dibujo queda en x=0 / y=0. Así el «ancla» de un barco colocado
 * es siempre la esquina superior izquierda de su caja.
 */
data class ShipShape(val cells: List<Coord>) {

    val size: Int get() = cells.size
    val width: Int get() = (cells.maxOf { it.x } + 1)
    val height: Int get() = (cells.maxOf { it.y } + 1)

    /** Esta misma forma puesta en vertical, o tal cual si se pide horizontal. */
    fun rotated(orientation: Orientation): ShipShape =
        if (orientation == Orientation.HORIZONTAL) this
        else ShipShape(normalize(cells.map { Coord(-it.y, it.x) }))

    companion object {
        /** Línea recta horizontal de [n] casillas. */
        fun line(n: Int) = ShipShape((0 until n).map { Coord(it, 0) })

        private fun normalize(cells: List<Coord>): List<Coord> {
            val minX = cells.minOf { it.x }
            val minY = cells.minOf { it.y }
            return cells.map { Coord(it.x - minX, it.y - minY) }
                .sortedWith(compareBy({ it.y }, { it.x }))
        }
    }
}

/** Clase de barco. El nombre que ve el usuario sale de los recursos (`R.string.ship_*`). */
enum class ShipKind(val resKey: String) {
    PORTAAVIONES("ship_carrier"),
    ACORAZADO("ship_battleship"),
    DESTRUCTOR("ship_destroyer"),
    SUBMARINO("ship_submarine"),
}

/** Un barco de la flota antes de colocarlo: qué es y de qué largo. [id] lo distingue de sus
 *  gemelos (la flota lleva cuatro submarinos iguales). */
data class ShipDef(val id: Int, val kind: ShipKind, val shape: ShipShape) {
    val size: Int get() = shape.size
}

/** Un barco ya colocado en un tablero. */
data class PlacedShip(val def: ShipDef, val anchor: Coord, val orientation: Orientation) {

    /** Las casillas que ocupa en el tablero. */
    val cells: List<Coord> = def.shape.rotated(orientation).cells.map { it + anchor }

    val id: Int get() = def.id
    val kind: ShipKind get() = def.kind
    val size: Int get() = def.size
}

/** Resultado de un disparo. */
data class ShotOutcome(
    val coord: Coord,
    val hit: Boolean,
    /** El barco que se acaba de hundir con este disparo, o null si no se hundió ninguno. */
    val sunk: PlacedShip?,
    /** True si con este disparo se ha hundido la flota entera. */
    val fleetDestroyed: Boolean,
)

/**
 * Tablero de un jugador: la flota colocada más los disparos recibidos. Es el estado «de verdad»:
 * el rival nunca lo ve, solo recibe [ShotOutcome] y, cuando hunde un barco, sus casillas.
 */
class PlayerBoard(val size: Int, val ships: List<PlacedShip>) {

    private val _shots = LinkedHashSet<Coord>()
    private val index: Map<Coord, PlacedShip> =
        buildMap { ships.forEach { s -> s.cells.forEach { put(it, s) } } }

    /** Todas las casillas donde ha caído un disparo, en orden. */
    val shots: Set<Coord> get() = _shots

    /** Disparos que dieron en un barco. */
    val hits: Set<Coord> get() = _shots.filterTo(LinkedHashSet()) { it in index }

    /** Disparos al agua. */
    val misses: Set<Coord> get() = _shots.filterTo(LinkedHashSet()) { it !in index }

    fun shipAt(c: Coord): PlacedShip? = index[c]
    fun alreadyShot(c: Coord): Boolean = c in _shots
    fun isSunk(ship: PlacedShip): Boolean = ship.cells.all { it in _shots }
    fun hitsOn(ship: PlacedShip): Int = ship.cells.count { it in _shots }

    val sunkShips: List<PlacedShip> get() = ships.filter { isSunk(it) }
    val aliveShips: List<PlacedShip> get() = ships.filter { !isSunk(it) }
    val fleetDestroyed: Boolean get() = ships.all { isSunk(it) }

    /** Casillas ocupadas por barcos ya hundidos (lo único de la flota que el rival llega a ver). */
    val sunkCells: Set<Coord> get() = sunkShips.flatMapTo(LinkedHashSet()) { it.cells }

    /**
     * Encaja un disparo en [c]. Devuelve null si ahí ya se había disparado (el disparo no cuenta),
     * para que ni la interfaz ni la IA puedan gastar turnos repitiendo casilla.
     */
    fun receive(c: Coord): ShotOutcome? {
        if (c.x !in 0 until size || c.y !in 0 until size) return null
        if (!_shots.add(c)) return null
        val ship = index[c]
        val sunk = ship?.takeIf { isSunk(it) }
        return ShotOutcome(c, hit = ship != null, sunk = sunk, fleetDestroyed = fleetDestroyed)
    }

    /** Copia del tablero con los mismos barcos y los mismos disparos (para guardar/restaurar). */
    fun copy(): PlayerBoard = PlayerBoard(size, ships).also { it._shots += _shots }

    /** Reinyecta disparos al restaurar una partida guardada. */
    fun restoreShots(coords: Collection<Coord>) {
        _shots.clear()
        _shots += coords
    }
}
