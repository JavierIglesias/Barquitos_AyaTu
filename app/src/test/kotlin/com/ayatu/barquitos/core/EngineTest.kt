package com.ayatu.barquitos.core

import com.ayatu.barquitos.core.ShipShape.Companion.line
import com.ayatu.barquitos.core.ShipShape.Companion.shapeOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas del modelo y de la colocación de la flota. */
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
        val s = line(3).rotated(Orientation.ESTE)
        assertEquals(1, s.width)
        assertEquals(3, s.height)
        assertEquals(listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2)), s.cells)
    }

    @Test
    fun `girar cuatro veces devuelve la forma original`() {
        val original = Fleet.PORTAAVIONES_SHAPE
        var s = original
        repeat(4) { s = s.rotated(Orientation.ESTE) }
        assertEquals(original.cells, s.cells)
    }

    @Test
    fun `el portaaviones tiene seis casillas y no es recto`() {
        assertEquals(6, Fleet.PORTAAVIONES_SHAPE.size)
        assertFalse(Fleet.PORTAAVIONES_SHAPE.isStraight)
    }

    @Test
    fun `el submarino tiene cuatro casillas con la torreta en el centro`() {
        assertEquals(4, Fleet.SUBMARINO_SHAPE.size)
        assertFalse(Fleet.SUBMARINO_SHAPE.isStraight)
        assertTrue(Coord(1, 0) in Fleet.SUBMARINO_SHAPE.cells)
    }

    @Test
    fun `la forma en texto se normaliza a la esquina superior izquierda`() {
        val s = shapeOf(
            "....",
            "..##",
            "..#.",
        )
        assertEquals(3, s.size)
        assertEquals(Coord(0, 0), s.cells.first())
    }

    @Test
    fun `las flotas tienen el numero de barcos y casillas esperado`() {
        assertEquals(6, Fleet.ESTANDAR.shipCount)
        assertEquals(21, Fleet.ESTANDAR.totalCells)
        assertEquals(5, Fleet.CLASICA.shipCount)
        assertEquals(17, Fleet.CLASICA.totalCells)
        assertEquals(10, Fleet.RUSA.shipCount)
        assertEquals(20, Fleet.RUSA.totalCells)
        assertTrue(Fleet.RUSA.mustSeparate)
        assertFalse(Fleet.CLASICA.mustSeparate)
    }

    @Test
    fun `las etiquetas de casilla se leen como en el parte de batalla`() {
        assertEquals("A1", Coord(0, 0).label())
        assertEquals("C7", Coord(2, 6).label())
        assertEquals("J10", Coord(9, 9).label())
    }
}

class PlacementTest {

    private val classic = Fleet.CLASICA
    private val russian = Fleet.RUSA

    @Test
    fun `un barco no puede salirse del tablero`() {
        val def = classic.ships.first() // 5 casillas
        assertFalse(Placement.canPlace(classic, def, Coord(6, 0), Orientation.NORTE, emptyList()))
        assertTrue(Placement.canPlace(classic, def, Coord(5, 0), Orientation.NORTE, emptyList()))
    }

    @Test
    fun `dos barcos no pueden solaparse`() {
        val a = PlacedShip(classic.ships[0], Coord(0, 0), Orientation.NORTE)
        val b = classic.ships[1]
        assertFalse(Placement.canPlace(classic, b, Coord(3, 0), Orientation.NORTE, listOf(a)))
        assertTrue(Placement.canPlace(classic, b, Coord(5, 0), Orientation.NORTE, listOf(a)))
    }

    @Test
    fun `en la flota clasica los barcos pueden tocarse`() {
        val a = PlacedShip(classic.ships[0], Coord(0, 0), Orientation.NORTE)
        // Justo debajo, pegado: legal en la Clásica.
        assertTrue(Placement.canPlace(classic, classic.ships[1], Coord(0, 1), Orientation.NORTE, listOf(a)))
    }

    @Test
    fun `en la flota rusa los barcos han de dejar una casilla de separacion`() {
        val a = PlacedShip(russian.ships[0], Coord(0, 0), Orientation.NORTE) // 4 en fila 0
        // Pegado debajo: ilegal.
        assertFalse(Placement.canPlace(russian, russian.ships[1], Coord(0, 1), Orientation.NORTE, listOf(a)))
        // En diagonal también es pegado: ilegal.
        assertFalse(Placement.canPlace(russian, russian.ships[6], Coord(4, 1), Orientation.NORTE, listOf(a)))
        // Con una fila de agua en medio: legal.
        assertTrue(Placement.canPlace(russian, russian.ships[1], Coord(0, 2), Orientation.NORTE, listOf(a)))
    }

    @Test
    fun `el reparto al azar es siempre legal en las tres flotas`() {
        for (fleet in Fleet.all) {
            repeat(60) {
                val layout = Placement.randomFill(fleet)
                assertNotNull("No se pudo repartir ${fleet.type}", layout)
                layout!!
                assertEquals(fleet.shipCount, layout.size)
                val cells = layout.flatMap { it.cells }
                assertEquals("Barcos solapados en ${fleet.type}", cells.size, cells.toSet().size)
                assertEquals(fleet.totalCells, cells.size)
                assertTrue(Placement.insideBoard(cells, fleet.boardSize))
                if (fleet.mustSeparate) {
                    for (ship in layout) {
                        val others = layout.filter { it.id != ship.id }.flatMap { it.cells }.toSet()
                        for (c in ship.cells) {
                            for (d in Coord.NEIGHBOURS_8) {
                                assertFalse("Barcos pegados en ${fleet.type}", (c + d) in others)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `el reparto respeta los barcos que el jugador ya puso a mano`() {
        val fixed = listOf(PlacedShip(classic.ships[0], Coord(0, 0), Orientation.NORTE))
        val layout = Placement.randomFill(classic, fixed)!!
        assertTrue(layout.any { it.id == 1 && it.anchor == Coord(0, 0) })
    }
}

class GameRulesTest {

    /** Tablero con un único destructor de 2 en A1-B1, para probar las reglas sin ruido. */
    private fun tinyBoard(): PlayerBoard {
        val def = ShipDef(5, ShipKind.DESTRUCTOR, line(2))
        return PlayerBoard(10, listOf(PlacedShip(def, Coord(0, 0), Orientation.NORTE)))
    }

    private fun classicGame(rule: ShotRule, multi: Int = 3): Game {
        val config = GameConfig(fleet = FleetType.CLASICA, shotRule = rule, multiShotCount = multi)
        val fleet = config.fleetDef
        val boards = listOf(
            PlayerBoard(10, Placement.randomLayout(fleet)),
            PlayerBoard(10, Placement.randomLayout(fleet)),
        )
        return Game(config, boards, startingPlayer = 0)
    }

    @Test
    fun `un disparo por turno alterna siempre`() {
        val game = classicGame(ShotRule.UN_DISPARO)
        assertEquals(0, game.current)
        game.fire(Coord(0, 0))
        assertEquals(1, game.current)
        game.fire(Coord(0, 0))
        assertEquals(0, game.current)
        assertEquals(2, game.round)
    }

    @Test
    fun `no se puede disparar dos veces a la misma casilla`() {
        val game = classicGame(ShotRule.MULTIDISPARO, multi = 4)
        assertNotNull(game.fire(Coord(3, 3)))
        assertNull("La casilla repetida no debe gastar disparo", game.fire(Coord(3, 3)))
        assertEquals(3, game.shotsLeft)
    }

    @Test
    fun `en racha se sigue disparando al acertar y se pierde el turno al fallar`() {
        val config = GameConfig(fleet = FleetType.CLASICA, shotRule = ShotRule.RACHA)
        // Tablero del rival: un barco en A1-B1; el resto agua.
        val boards = listOf(tinyBoard(), tinyBoard())
        val game = Game(config, boards, startingPlayer = 0)

        val first = game.fire(Coord(0, 0))!!
        assertTrue(first.hit)
        assertEquals("Al acertar no pasa el turno", 0, game.current)

        val second = game.fire(Coord(5, 5))!!
        assertFalse(second.hit)
        assertEquals("Al fallar pasa el turno", 1, game.current)
    }

    @Test
    fun `multidisparo da el numero de disparos elegido`() {
        val game = classicGame(ShotRule.MULTIDISPARO, multi = 3)
        assertEquals(3, game.shotsLeft)
        game.fire(Coord(0, 9)); assertEquals(2, game.shotsLeft); assertEquals(0, game.current)
        game.fire(Coord(1, 9)); assertEquals(1, game.shotsLeft); assertEquals(0, game.current)
        game.fire(Coord(2, 9)); assertEquals(1, game.current)
        assertEquals(3, game.shotsLeft)
    }

    @Test
    fun `fuego en cadena da un disparo por barco a flote y baja al hundirse`() {
        val config = GameConfig(fleet = FleetType.CLASICA, shotRule = ShotRule.FUEGO_EN_CADENA)
        val fleet = config.fleetDef
        // Flota del jugador 0 colocada a mano para poder hundirle un barco entero.
        val mine = listOf(
            PlacedShip(fleet.ships[0], Coord(0, 0), Orientation.NORTE), // 5 en fila 0
            PlacedShip(fleet.ships[1], Coord(0, 2), Orientation.NORTE),
            PlacedShip(fleet.ships[2], Coord(0, 4), Orientation.NORTE),
            PlacedShip(fleet.ships[3], Coord(0, 6), Orientation.NORTE),
            PlacedShip(fleet.ships[4], Coord(0, 8), Orientation.NORTE),
        )
        val boards = listOf(PlayerBoard(10, mine), PlayerBoard(10, Placement.randomLayout(fleet)))
        val game = Game(config, boards, startingPlayer = 0)
        assertEquals("Cinco barcos a flote, cinco disparos", 5, game.shotsLeft)

        // El jugador 0 gasta su turno al agua (columna 9, que su rival casi seguro no ocupa toda).
        repeat(5) { game.fire(Coord(9, it)) }
        assertEquals(1, game.current)

        // El jugador 1 hunde el destructor de 2 casillas del jugador 0 (fila 8).
        game.fire(Coord(0, 8))
        game.fire(Coord(1, 8))
        assertTrue(boards[0].isSunk(mine[4]))
        // Cuando le vuelva a tocar, el jugador 0 tendrá un disparo menos.
        while (game.current != 0 && !game.finished) game.fire(firstFreeCell(boards[0]))
        assertEquals(4, game.shotsLeft)
    }

    private fun firstFreeCell(board: PlayerBoard): Coord =
        (0 until board.size).flatMap { y -> (0 until board.size).map { x -> Coord(x, y) } }
            .first { !board.alreadyShot(it) }

    @Test
    fun `hundir la flota entera gana la partida`() {
        val config = GameConfig(fleet = FleetType.CLASICA, shotRule = ShotRule.MULTIDISPARO, multiShotCount = 6)
        val boards = listOf(tinyBoard(), tinyBoard())
        val game = Game(config, boards, startingPlayer = 0)
        game.fire(Coord(0, 0))
        val last = game.fire(Coord(1, 0))!!
        assertTrue(last.fleetDestroyed)
        assertEquals(0, game.winner)
        assertTrue(game.finished)
        assertNull("Con la partida acabada no se dispara más", game.fire(Coord(4, 4)))
    }

    @Test
    fun `en salva los dos disparan a la vez y pueden empatar`() {
        val config = GameConfig(fleet = FleetType.CLASICA, shotRule = ShotRule.SALVA)
        val boards = listOf(tinyBoard(), tinyBoard())
        val game = Game(config, boards, startingPlayer = 0)
        assertEquals("Un barco a flote, un disparo de salva", 1, game.salvoSize(0))

        // Primera ronda: cada uno toca el barco del otro sin hundirlo.
        assertTrue(game.toggleAim(Coord(0, 0)))
        assertFalse("Con la salva llena no se marca más", game.toggleAim(Coord(5, 5)))
        assertFalse("Aún falta el rival", game.confirmAim())
        assertEquals(1, game.current)
        game.toggleAim(Coord(0, 0))
        assertTrue("Ya han confirmado los dos", game.confirmAim())
        val round = game.resolveSalvo()
        assertTrue(round.outcomes.getValue(0).single().hit)
        assertTrue(round.outcomes.getValue(1).single().hit)
        assertNull(game.winner)

        // Segunda ronda: los dos remataron a la vez -> empate.
        game.toggleAim(Coord(1, 0)); game.confirmAim()
        game.toggleAim(Coord(1, 0)); game.confirmAim()
        game.resolveSalvo()
        assertEquals(Game.DRAW, game.winner)
    }

    @Test
    fun `en salva no se puede marcar una casilla ya disparada`() {
        val config = GameConfig(fleet = FleetType.CLASICA, shotRule = ShotRule.SALVA)
        val fleet = config.fleetDef
        val boards = listOf(
            PlayerBoard(10, Placement.randomLayout(fleet)),
            PlayerBoard(10, Placement.randomLayout(fleet)),
        )
        val game = Game(config, boards, startingPlayer = 0)
        boards[1].receive(Coord(4, 4))
        assertFalse(game.toggleAim(Coord(4, 4)))
    }

    @Test
    fun `la vista del rival no filtra los barcos a flote`() {
        val config = GameConfig(fleet = FleetType.CLASICA, shotRule = ShotRule.UN_DISPARO)
        val boards = listOf(tinyBoard(), tinyBoard())
        val game = Game(config, boards, startingPlayer = 0)
        val view = game.viewFor(0)
        assertTrue(view.sunkCells.isEmpty())
        assertTrue(view.hits.isEmpty())
        assertEquals(1, view.remaining.size)
        game.fire(Coord(0, 0))
        val after = game.viewFor(0)
        assertEquals(setOf(Coord(0, 0)), after.hits)
        assertEquals("Mientras no se hunda, sigue contando como a flote", 1, after.remaining.size)
    }

    @Test
    fun `la precision se calcula sobre los disparos hechos`() {
        val config = GameConfig(fleet = FleetType.CLASICA, shotRule = ShotRule.MULTIDISPARO, multiShotCount = 4)
        val boards = listOf(tinyBoard(), tinyBoard())
        val game = Game(config, boards, startingPlayer = 0)
        game.fire(Coord(0, 0))   // acierto
        game.fire(Coord(5, 5))   // agua
        assertEquals(2, game.shotsFiredBy(0))
        assertEquals(1, game.hitsBy(0))
        assertEquals(0.5f, game.accuracyOf(0), 0.001f)
    }
}

class GameSaveTest {

    @Test
    fun `una partida a medias se guarda y se recupera igual`() {
        val config = GameConfig(
            mode = GameMode.UN_JUGADOR,
            fleet = FleetType.ESTANDAR,
            shotRule = ShotRule.MULTIDISPARO,
            multiShotCount = 4,
            difficulty = Difficulty.DIFICIL,
            startingPlayer = 1,
        )
        val fleet = config.fleetDef
        val boards = listOf(
            PlayerBoard(10, Placement.randomLayout(fleet)),
            PlayerBoard(10, Placement.randomLayout(fleet)),
        )
        val game = Game(config, boards, startingPlayer = 1)
        repeat(7) { game.fire(Coord(it % 10, it / 10)) }

        val text = GameSave.serialize(game)
        val restored = GameSave.deserialize(text)
        assertNotNull(restored)
        restored!!
        assertEquals(game.config, restored.config)
        assertEquals(game.current, restored.current)
        assertEquals(game.shotsLeft, restored.shotsLeft)
        assertEquals(game.round, restored.round)
        assertEquals(game.history.size, restored.history.size)
        for (p in 0..1) {
            assertEquals(game.boards[p].shots, restored.boards[p].shots)
            assertEquals(
                game.boards[p].ships.map { it.anchor to it.orientation },
                restored.boards[p].ships.map { it.anchor to it.orientation },
            )
            assertEquals(game.shotsFiredBy(p), restored.shotsFiredBy(p))
            assertEquals(game.hitsBy(p), restored.hitsBy(p))
        }
    }

    @Test
    fun `un guardado roto o de otra version no revienta`() {
        assertNull(GameSave.deserialize(""))
        assertNull(GameSave.deserialize("v=999\nmode=UN_JUGADOR"))
        assertNull(GameSave.deserialize("v=1\nmode=LO_QUE_SEA"))
    }
}
