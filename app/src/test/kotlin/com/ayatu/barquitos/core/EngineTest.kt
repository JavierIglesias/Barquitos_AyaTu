package com.ayatu.barquitos.core

import com.ayatu.barquitos.core.ShipShape.Companion.line
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Pruebas de las formas y de la flota. */
class ShapeTest {

    @Test
    fun `una linea de 4 ocupa 4 casillas en fila`() {
        val s = line(4)
        assertEquals(4, s.size)
        assertEquals(4, s.width)
        assertEquals(1, s.height)
    }

    @Test
    fun `girar una linea la pone en vertical`() {
        val s = line(3).rotated(Orientation.VERTICAL)
        assertEquals(1, s.width)
        assertEquals(3, s.height)
        assertEquals(listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2)), s.cells)
    }

    @Test
    fun `pedir la orientacion horizontal devuelve el dibujo original`() {
        // `rotated` da la forma EN esa orientación a partir del dibujo original, que siempre es
        // horizontal; no gira lo que se le pase, así que encadenarlo no tendría sentido.
        val original = line(3)
        assertEquals(original.cells, original.rotated(Orientation.HORIZONTAL).cells)
    }

    @Test
    fun `un barco de una casilla es igual en las dos orientaciones`() {
        assertEquals(
            line(1).rotated(Orientation.HORIZONTAL).cells,
            line(1).rotated(Orientation.VERTICAL).cells,
        )
    }
}

/** La flota es la del juego de lápiz y papel: 1 de 4, 2 de 3, 3 de 2 y 4 de 1. */
class FleetTest {

    @Test
    fun `son diez barcos y veinte casillas`() {
        assertEquals(10, Fleet.shipCount)
        assertEquals(20, Fleet.totalCells)
    }

    @Test
    fun `la composicion es 1-2-3-4 de mayor a menor`() {
        val bySize = Fleet.ships.groupingBy { it.size }.eachCount()
        assertEquals(mapOf(4 to 1, 3 to 2, 2 to 3, 1 to 4), bySize)
    }

    @Test
    fun `el barco de cuatro es el portaaviones`() {
        val grande = Fleet.ships.single { it.size == 4 }
        assertEquals(ShipKind.PORTAAVIONES, grande.kind)
    }

    @Test
    fun `los identificadores no se repiten`() {
        assertEquals(Fleet.shipCount, Fleet.ships.map { it.id }.toSet().size)
    }

    @Test
    fun `todos los barcos son lineas rectas`() {
        Fleet.ships.forEach { def ->
            assertTrue(
                "El barco ${def.id} no es recto",
                def.shape.cells.all { it.y == 0 } || def.shape.cells.all { it.x == 0 },
            )
        }
    }
}

/** Colocación: dentro del tablero, sin solapar y sin tocarse ni en diagonal. */
class PlacementTest {

    private val patrullera = Fleet.ships.first { it.size == 2 }
    private val otra = Fleet.ships.last { it.size == 2 }

    @Test
    fun `un barco que se sale del tablero no vale`() {
        assertFalse(Placement.canPlace(patrullera, Coord(9, 0), Orientation.HORIZONTAL, emptyList()))
        assertTrue(Placement.canPlace(patrullera, Coord(8, 0), Orientation.HORIZONTAL, emptyList()))
    }

    @Test
    fun `dos barcos no se pueden solapar`() {
        val puesto = PlacedShip(patrullera, Coord(3, 3), Orientation.HORIZONTAL)
        assertFalse(Placement.canPlace(otra, Coord(3, 3), Orientation.HORIZONTAL, listOf(puesto)))
    }

    @Test
    fun `dos barcos no se pueden tocar de lado`() {
        val puesto = PlacedShip(patrullera, Coord(3, 3), Orientation.HORIZONTAL)
        // (5,3) está pegado a la popa del otro, que acaba en (4,3).
        assertFalse(Placement.canPlace(otra, Coord(5, 3), Orientation.HORIZONTAL, listOf(puesto)))
    }

    @Test
    fun `dos barcos no se pueden tocar en diagonal`() {
        val puesto = PlacedShip(patrullera, Coord(3, 3), Orientation.HORIZONTAL)
        assertFalse(Placement.canPlace(otra, Coord(5, 4), Orientation.HORIZONTAL, listOf(puesto)))
    }

    @Test
    fun `dejando una casilla de agua si valen`() {
        val puesto = PlacedShip(patrullera, Coord(3, 3), Orientation.HORIZONTAL)
        assertTrue(Placement.canPlace(otra, Coord(6, 3), Orientation.HORIZONTAL, listOf(puesto)))
        assertTrue(Placement.canPlace(otra, Coord(3, 5), Orientation.HORIZONTAL, listOf(puesto)))
    }

    @Test
    fun `un barco de una casilla tiene cien sitios en un tablero vacio`() {
        val submarino = Fleet.ships.first { it.size == 1 }
        // Una sola orientación: si se ofrecieran las dos, cada casilla saldría repetida.
        assertEquals(100, Placement.legalPlacements(submarino, emptyList()).size)
    }

    @Test
    fun `el reparto al azar coloca la flota entera y respeta la separacion`() {
        repeat(50) { seed ->
            val layout = Placement.randomFill(rng = Random(seed))
            assertNotNull("No se pudo repartir con la semilla $seed", layout)
            val ships = layout!!
            assertEquals(Fleet.shipCount, ships.size)
            assertEquals(Fleet.totalCells, ships.flatMap { it.cells }.toSet().size)
            // Ninguna casilla de un barco puede tocar a otro barco, tampoco en diagonal.
            for (ship in ships) {
                val vecinas = ship.cells.flatMap { c -> Coord.NEIGHBOURS_8.map { c + it } }.toSet()
                for (otro in ships.filter { it.id != ship.id }) {
                    assertTrue(
                        "Los barcos ${ship.id} y ${otro.id} se tocan (semilla $seed)",
                        otro.cells.none { it in vecinas },
                    )
                }
            }
        }
    }
}

/** Turnos y reglas de disparo. */
class GameTest {

    /** Dos tableros a mano, con un solo barco cada uno, para que las pruebas sean legibles. */
    private fun partida(rule: ShotRule): Game {
        val def = Fleet.ships.first { it.size == 2 }
        val boards = listOf(
            PlayerBoard(10, listOf(PlacedShip(def, Coord(0, 0), Orientation.HORIZONTAL))),
            PlayerBoard(10, listOf(PlacedShip(def, Coord(5, 5), Orientation.HORIZONTAL))),
        )
        return Game(GameConfig(shotRule = rule), boards, startingPlayer = 0)
    }

    @Test
    fun `con un disparo por turno el turno cambia siempre`() {
        val g = partida(ShotRule.UN_DISPARO)
        assertEquals(0, g.current)
        g.fire(Coord(5, 5)) // acierta
        assertEquals("Aun acertando, el turno pasa", 1, g.current)
        g.fire(Coord(9, 9)) // falla
        assertEquals(0, g.current)
    }

    @Test
    fun `con racha se sigue disparando mientras se acierte`() {
        val g = partida(ShotRule.RACHA)
        g.fire(Coord(5, 5)) // acierta
        assertEquals("Al acertar se repite", 0, g.current)
        g.fire(Coord(9, 9)) // falla
        assertEquals("Al fallar se pasa el turno", 1, g.current)
    }

    @Test
    fun `disparar dos veces a la misma casilla no cuenta`() {
        val g = partida(ShotRule.RACHA)
        assertNotNull(g.fire(Coord(9, 9)))
        g.fire(Coord(0, 9)) // turno del rival, falla
        assertEquals(0, g.current)
        assertNull("Repetir casilla no debe gastar turno", g.fire(Coord(9, 9)))
    }

    @Test
    fun `hundir la flota entera gana la partida`() {
        val g = partida(ShotRule.RACHA)
        g.fire(Coord(5, 5))
        val last = g.fire(Coord(6, 5))
        assertTrue(last!!.fleetDestroyed)
        assertEquals(0, g.winner)
        assertTrue(g.finished)
        assertNull("Con la partida acabada no se dispara más", g.fire(Coord(1, 1)))
    }

    @Test
    fun `la ronda sube cuando vuelve a disparar el que empezo`() {
        val g = partida(ShotRule.UN_DISPARO)
        assertEquals(1, g.round)
        g.fire(Coord(9, 9))
        g.fire(Coord(9, 9))
        assertEquals(2, g.round)
    }

    @Test
    fun `la vista del rival no ensena los barcos que siguen a flote`() {
        val g = partida(ShotRule.UN_DISPARO)
        val view = g.viewFor(0)
        assertTrue(view.sunkCells.isEmpty())
        assertEquals(1, view.remaining.size)
        g.fire(Coord(5, 5))
        assertTrue("Un barco tocado pero no hundido no se revela", g.viewFor(0).sunkCells.isEmpty())
    }

    @Test
    fun `al hundir un barco deja de contar como a flote`() {
        val g = partida(ShotRule.RACHA)
        g.fire(Coord(5, 5))
        g.fire(Coord(6, 5))
        assertTrue(g.viewFor(0).remaining.isEmpty())
        assertEquals(2, g.viewFor(0).sunkCells.size)
    }

    @Test
    fun `las estadisticas cuentan disparos e impactos de cada uno`() {
        val g = partida(ShotRule.UN_DISPARO)
        g.fire(Coord(5, 5)) // jugador 0 acierta
        g.fire(Coord(9, 9)) // jugador 1 falla
        assertEquals(1, g.shotsFiredBy(0))
        assertEquals(1, g.hitsBy(0))
        assertEquals(1, g.shotsFiredBy(1))
        assertEquals(0, g.hitsBy(1))
        assertEquals(1f, g.accuracyOf(0), 0.001f)
        assertEquals(0f, g.accuracyOf(1), 0.001f)
    }
}

/** Guardado y recuperación de la partida. */
class SaveTest {

    private fun partidaReal(): Game {
        val boards = listOf(
            PlayerBoard(Fleet.BOARD_SIZE, Placement.randomLayout(Random(1))),
            PlayerBoard(Fleet.BOARD_SIZE, Placement.randomLayout(Random(2))),
        )
        return Game(GameConfig(shotRule = ShotRule.RACHA), boards, startingPlayer = 1)
    }

    @Test
    fun `una partida guardada se recupera igual`() {
        val g = partidaReal()
        g.fire(Coord(0, 0))
        g.fire(Coord(3, 4))
        g.fire(Coord(7, 7))

        val texto = GameSave.serialize(g)
        val vuelta = GameSave.deserialize(texto)
        assertNotNull(vuelta)
        val r = vuelta!!
        assertEquals(g.current, r.current)
        assertEquals(g.round, r.round)
        assertEquals(g.winner, r.winner)
        assertEquals(g.config, r.config)
        assertEquals(g.history.size, r.history.size)
        for (p in 0..1) {
            assertEquals(g.boards[p].shots, r.boards[p].shots)
            assertEquals(
                g.boards[p].ships.map { it.anchor to it.orientation },
                r.boards[p].ships.map { it.anchor to it.orientation },
            )
            assertEquals(g.shotsFiredBy(p), r.shotsFiredBy(p))
            assertEquals(g.hitsBy(p), r.hitsBy(p))
        }
    }

    @Test
    fun `un guardado de otra version se descarta`() {
        val texto = GameSave.serialize(partidaReal()).replace("v=${GameSave.VERSION}", "v=0")
        assertNull(GameSave.deserialize(texto))
    }

    @Test
    fun `un guardado roto se descarta sin reventar`() {
        assertNull(GameSave.deserialize("esto no es una partida"))
        assertNull(GameSave.deserialize(""))
    }
}
