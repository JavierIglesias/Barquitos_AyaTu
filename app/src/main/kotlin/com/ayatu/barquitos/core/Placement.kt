package com.ayatu.barquitos.core

import kotlin.random.Random

/**
 * Colocación de la flota: qué posiciones son legales y cómo repartir los barcos al azar.
 *
 * La única diferencia entre flotas es [Fleet.mustSeparate]: en la Estándar y la Clásica los
 * barcos pueden quedar pegados (solo no pueden solaparse); en la Rusa han de dejar una casilla
 * de agua alrededor, diagonales incluidas.
 */
object Placement {

    /** Las casillas que ocuparía [def] anclado en [anchor] con la orientación [orientation]. */
    fun cellsOf(def: ShipDef, anchor: Coord, orientation: Orientation): List<Coord> =
        def.shape.rotated(orientation).cells.map { it + anchor }

    /** True si [cells] cabe en un tablero de [size]×[size]. */
    fun insideBoard(cells: List<Coord>, size: Int): Boolean =
        cells.all { it.x in 0 until size && it.y in 0 until size }

    /**
     * True si se puede colocar [def] en [anchor]/[orientation] sin chocar con [others].
     * [ignoreId] permite recolocar un barco que ya estaba puesto sin que choque consigo mismo.
     */
    fun canPlace(
        fleet: Fleet,
        def: ShipDef,
        anchor: Coord,
        orientation: Orientation,
        others: List<PlacedShip>,
        ignoreId: Int? = null,
    ): Boolean {
        val cells = cellsOf(def, anchor, orientation)
        if (!insideBoard(cells, fleet.boardSize)) return false
        val rivals = others.filter { it.id != (ignoreId ?: def.id) }
        val occupied = rivals.flatMapTo(HashSet()) { it.cells }
        if (cells.any { it in occupied }) return false
        if (fleet.mustSeparate) {
            val forbidden = occupied.flatMapTo(HashSet()) { c -> Coord.NEIGHBOURS_8.map { c + it } }
            if (cells.any { it in forbidden }) return false
        }
        return true
    }

    /** Todas las posiciones legales de [def] con la flota [placed] ya puesta. */
    fun legalPlacements(
        fleet: Fleet,
        def: ShipDef,
        placed: List<PlacedShip>,
    ): List<PlacedShip> {
        val result = mutableListOf<PlacedShip>()
        // Formas rectas: NORTE/SUR y ESTE/OESTE dan el mismo dibujo, así que con dos
        // orientaciones basta y no se ofrecen giros que no cambian nada.
        val orientations = if (def.shape.isStraight) {
            listOf(Orientation.NORTE, Orientation.ESTE)
        } else {
            Orientation.entries
        }
        for (orientation in orientations) {
            for (y in 0 until fleet.boardSize) {
                for (x in 0 until fleet.boardSize) {
                    val anchor = Coord(x, y)
                    if (canPlace(fleet, def, anchor, orientation, placed)) {
                        result += PlacedShip(def, anchor, orientation)
                    }
                }
            }
        }
        return result
    }

    /**
     * Reparte al azar los barcos que falten. [fixed] son los que el jugador ya ha colocado a
     * mano y no se tocan. Devuelve la flota completa o null si no hubo forma de encajarla (no
     * pasa con los tableros del juego, pero el llamador no debería dar por hecho que siempre sale).
     */
    fun randomFill(fleet: Fleet, fixed: List<PlacedShip> = emptyList(), rng: Random = Random): List<PlacedShip>? {
        val pending = fleet.ships.filter { def -> fixed.none { it.id == def.id } }
        // De mayor a menor: los barcos grandes son los que menos sitios tienen, así que si se
        // colocan primero se reduce muchísimo el número de reintentos.
        val order = pending.sortedByDescending { it.size }
        repeat(MAX_ATTEMPTS) {
            val placed = fixed.toMutableList()
            var ok = true
            for (def in order) {
                val options = legalPlacements(fleet, def, placed)
                if (options.isEmpty()) { ok = false; break }
                placed += options[rng.nextInt(options.size)]
            }
            if (ok) return placed.sortedBy { it.id }
        }
        return null
    }

    /** Flota completa al azar (atajo del botón «Al azar»). */
    fun randomLayout(fleet: Fleet, rng: Random = Random): List<PlacedShip> =
        randomFill(fleet, emptyList(), rng)
            ?: error("No se pudo repartir la flota ${fleet.type} al azar")

    private const val MAX_ATTEMPTS = 400
}
