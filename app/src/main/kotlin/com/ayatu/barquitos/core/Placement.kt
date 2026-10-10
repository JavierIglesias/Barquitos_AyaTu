package com.ayatu.barquitos.core

import kotlin.random.Random

/**
 * Colocación de la flota: qué posiciones son legales y cómo repartir los barcos al azar.
 *
 * La regla que manda es [Fleet.MUST_SEPARATE]: además de no solaparse, dos barcos han de dejar
 * siempre una casilla de agua entre ellos, diagonales incluidas.
 */
object Placement {

    /** Las casillas que ocuparía [def] anclado en [anchor] con la orientación [orientation]. */
    fun cellsOf(def: ShipDef, anchor: Coord, orientation: Orientation): List<Coord> =
        def.shape.rotated(orientation).cells.map { it + anchor }

    /** True si [cells] cabe en el tablero. */
    fun insideBoard(cells: List<Coord>, size: Int = Fleet.BOARD_SIZE): Boolean =
        cells.all { it.x in 0 until size && it.y in 0 until size }

    /**
     * True si se puede colocar [def] en [anchor]/[orientation] sin chocar con [others] ni quedar
     * pegado a ellos. [ignoreId] permite recolocar un barco que ya estaba puesto sin que choque
     * consigo mismo.
     */
    fun canPlace(
        def: ShipDef,
        anchor: Coord,
        orientation: Orientation,
        others: List<PlacedShip>,
        ignoreId: Int? = null,
    ): Boolean {
        val cells = cellsOf(def, anchor, orientation)
        if (!insideBoard(cells)) return false
        val rivals = others.filter { it.id != (ignoreId ?: def.id) }
        val occupied = rivals.flatMapTo(HashSet()) { it.cells }
        if (cells.any { it in occupied }) return false
        if (Fleet.MUST_SEPARATE) {
            val forbidden = occupied.flatMapTo(HashSet()) { c -> Coord.NEIGHBOURS_8.map { c + it } }
            if (cells.any { it in forbidden }) return false
        }
        return true
    }

    /** Todas las posiciones legales de [def] con la flota [placed] ya puesta. */
    fun legalPlacements(def: ShipDef, placed: List<PlacedShip>): List<PlacedShip> {
        val result = mutableListOf<PlacedShip>()
        // Un barco de una casilla da el mismo dibujo en las dos orientaciones: ofrecer las dos
        // duplicaría cada posición y sesgaría el reparto al azar hacia los submarinos.
        val orientations =
            if (def.size == 1) listOf(Orientation.HORIZONTAL) else Orientation.entries
        for (orientation in orientations) {
            for (y in 0 until Fleet.BOARD_SIZE) {
                for (x in 0 until Fleet.BOARD_SIZE) {
                    val anchor = Coord(x, y)
                    if (canPlace(def, anchor, orientation, placed)) {
                        result += PlacedShip(def, anchor, orientation)
                    }
                }
            }
        }
        return result
    }

    /**
     * Reparte al azar los barcos que falten. [fixed] son los que el jugador ya ha colocado a
     * mano y no se tocan. Devuelve la flota completa o null si no hubo forma de encajarla (con
     * esta flota y este tablero no pasa, pero el llamador no debería darlo por hecho).
     */
    fun randomFill(fixed: List<PlacedShip> = emptyList(), rng: Random = Random): List<PlacedShip>? {
        val pending = Fleet.ships.filter { def -> fixed.none { it.id == def.id } }
        // De mayor a menor: los barcos grandes son los que menos sitios tienen, así que si se
        // colocan primero se reduce muchísimo el número de reintentos.
        val order = pending.sortedByDescending { it.size }
        repeat(MAX_ATTEMPTS) {
            val placed = fixed.toMutableList()
            var ok = true
            for (def in order) {
                val options = legalPlacements(def, placed)
                if (options.isEmpty()) { ok = false; break }
                placed += options[rng.nextInt(options.size)]
            }
            if (ok) return placed.sortedBy { it.id }
        }
        return null
    }

    /** Flota completa al azar (atajo del botón «Al azar»). */
    fun randomLayout(rng: Random = Random): List<PlacedShip> =
        randomFill(emptyList(), rng) ?: error("No se pudo repartir la flota al azar")

    private const val MAX_ATTEMPTS = 400
}
