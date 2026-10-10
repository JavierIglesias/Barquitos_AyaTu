package com.ayatu.barquitos.core

import com.ayatu.barquitos.core.ShipShape.Companion.line
import com.ayatu.barquitos.core.ShipShape.Companion.shapeOf

/** Las tres flotas del juego. */
enum class FleetType(val resKey: String) {
    ESTANDAR("fleet_standard"),
    CLASICA("fleet_classic"),
    RUSA("fleet_russian"),
}

/**
 * Una flota: qué barcos la componen, en qué tablero juega y si sus barcos pueden tocarse.
 *
 * - **Estándar**: seis barcos, dos de ellos con forma especial (el portaaviones tiene isla de
 *   mando y el submarino torreta), así que no son simples líneas rectas. Pueden tocarse.
 * - **Clásica**: la de toda la vida, cinco barcos rectos de 5-4-3-3-2. Pueden tocarse.
 * - **Rusa**: diez barcos (4, 3×2, 2×3, 1×4) que han de dejar **siempre una casilla de
 *   separación** entre ellos, también en diagonal.
 */
data class Fleet(
    val type: FleetType,
    val ships: List<ShipDef>,
    val boardSize: Int = 10,
    /** True si los barcos han de quedar separados por al menos una casilla (flota Rusa). */
    val mustSeparate: Boolean = false,
) {
    val totalCells: Int get() = ships.sumOf { it.size }
    val shipCount: Int get() = ships.size

    fun shipById(id: Int): ShipDef? = ships.firstOrNull { it.id == id }

    companion object {

        /** Portaaviones: casco de cinco más la isla de mando. Seis casillas. */
        val PORTAAVIONES_SHAPE = shapeOf(
            "..#..",
            "#####",
        )

        /** Submarino: casco de tres con la torreta en el centro. Cuatro casillas. */
        val SUBMARINO_SHAPE = shapeOf(
            ".#.",
            "###",
        )

        val ESTANDAR = Fleet(
            type = FleetType.ESTANDAR,
            ships = listOf(
                ShipDef(1, ShipKind.PORTAAVIONES, PORTAAVIONES_SHAPE),
                ShipDef(2, ShipKind.ACORAZADO, line(4)),
                ShipDef(3, ShipKind.SUBMARINO, SUBMARINO_SHAPE),
                ShipDef(4, ShipKind.CRUCERO, line(3)),
                ShipDef(5, ShipKind.DESTRUCTOR, line(2)),
                ShipDef(6, ShipKind.PATRULLERA, line(2)),
            ),
        )

        val CLASICA = Fleet(
            type = FleetType.CLASICA,
            ships = listOf(
                ShipDef(1, ShipKind.PORTAAVIONES, line(5)),
                ShipDef(2, ShipKind.ACORAZADO, line(4)),
                ShipDef(3, ShipKind.CRUCERO, line(3)),
                ShipDef(4, ShipKind.SUBMARINO, line(3)),
                ShipDef(5, ShipKind.DESTRUCTOR, line(2)),
            ),
        )

        val RUSA = Fleet(
            type = FleetType.RUSA,
            mustSeparate = true,
            ships = listOf(
                ShipDef(1, ShipKind.ACORAZADO, line(4)),
                ShipDef(2, ShipKind.CRUCERO, line(3)),
                ShipDef(3, ShipKind.CRUCERO, line(3)),
                ShipDef(4, ShipKind.DESTRUCTOR, line(2)),
                ShipDef(5, ShipKind.DESTRUCTOR, line(2)),
                ShipDef(6, ShipKind.DESTRUCTOR, line(2)),
                ShipDef(7, ShipKind.LANCHA, line(1)),
                ShipDef(8, ShipKind.LANCHA, line(1)),
                ShipDef(9, ShipKind.LANCHA, line(1)),
                ShipDef(10, ShipKind.LANCHA, line(1)),
            ),
        )

        fun of(type: FleetType): Fleet = when (type) {
            FleetType.ESTANDAR -> ESTANDAR
            FleetType.CLASICA -> CLASICA
            FleetType.RUSA -> RUSA
        }

        val all: List<Fleet> get() = listOf(ESTANDAR, CLASICA, RUSA)
    }
}
