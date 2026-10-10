package com.ayatu.barquitos.core

import com.ayatu.barquitos.core.ShipShape.Companion.line

/**
 * La flota, que es una sola: la del juego de lápiz y papel de toda la vida. Diez barcos rectos
 * sobre un tablero de 10×10 —uno de cuatro casillas, dos de tres, tres de dos y cuatro de una,
 * veinte casillas en total— y la regla de que **dos barcos nunca pueden tocarse**, tampoco en
 * diagonal.
 *
 * Esa separación obligatoria no es un adorno: con cuatro submarinos de una sola casilla, si los
 * barcos pudieran quedar pegados no habría manera de saber si dos impactos seguidos son un
 * destructor o dos submarinos, y hundir dejaría de estar definido.
 */
object Fleet {

    const val BOARD_SIZE = 10

    /** Los barcos han de dejar siempre una casilla de agua alrededor, diagonales incluidas. */
    const val MUST_SEPARATE = true

    val ships: List<ShipDef> = listOf(
        ShipDef(1, ShipKind.PORTAAVIONES, line(4)),
        ShipDef(2, ShipKind.ACORAZADO, line(3)),
        ShipDef(3, ShipKind.ACORAZADO, line(3)),
        ShipDef(4, ShipKind.DESTRUCTOR, line(2)),
        ShipDef(5, ShipKind.DESTRUCTOR, line(2)),
        ShipDef(6, ShipKind.DESTRUCTOR, line(2)),
        ShipDef(7, ShipKind.SUBMARINO, line(1)),
        ShipDef(8, ShipKind.SUBMARINO, line(1)),
        ShipDef(9, ShipKind.SUBMARINO, line(1)),
        ShipDef(10, ShipKind.SUBMARINO, line(1)),
    )

    /** 20 casillas de barco sobre las 100 del tablero. */
    val totalCells: Int = ships.sumOf { it.size }

    val shipCount: Int get() = ships.size

    fun shipById(id: Int): ShipDef? = ships.firstOrNull { it.id == id }

    /** Cuántos barcos hay de cada clase, para los resúmenes de flota. */
    val byKind: List<Pair<ShipKind, Int>>
        get() = ships.groupingBy { it.kind }.eachCount().toList()
}
