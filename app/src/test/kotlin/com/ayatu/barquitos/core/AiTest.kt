package com.ayatu.barquitos.core

import com.ayatu.barquitos.core.ai.ShipAi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Pruebas de la IA. Lo importante que se comprueba aquí:
 *  - nunca repite casilla (sería gastar turnos a cambio de nada),
 *  - solo mira el [BoardView], así que no puede hacer trampas,
 *  - y los tres niveles están realmente escalonados: Difícil barre el tablero en muchos menos
 *    disparos que Fácil.
 */
class AiTest {

    /** Juega una partida «a solitario»: la IA dispara hasta hundir la flota y devuelve los disparos. */
    private fun shotsToClear(difficulty: Difficulty, fleet: Fleet, seed: Int): Int {
        val rng = Random(seed)
        val board = PlayerBoard(fleet.boardSize, Placement.randomLayout(fleet, rng))
        val ai = ShipAi(difficulty, rng)
        val config = GameConfig(fleet = fleet.type, shotRule = ShotRule.UN_DISPARO)
        // El jugador 0 es la IA y el 1 el tablero a batir; el 1 nunca dispara.
        val game = Game(config, listOf(PlayerBoard(fleet.boardSize, Placement.randomLayout(fleet, rng)), board), 0)
        var shots = 0
        val used = HashSet<Coord>()
        while (!board.fleetDestroyed) {
            val target = ai.chooseShot(game.viewFor(0))
            assertTrue("La IA ha repetido la casilla ${target.label()}", used.add(target))
            board.receive(target)
            shots++
            if (shots > fleet.boardSize * fleet.boardSize) break
        }
        assertTrue("La IA no llegó a hundir la flota", board.fleetDestroyed)
        return shots
    }

    @Test
    fun `la IA dificil barre la flota clasica en bastante menos de 100 disparos`() {
        val runs = (1..15).map { shotsToClear(Difficulty.DIFICIL, Fleet.CLASICA, it) }
        val average = runs.average()
        assertTrue("Media de $average disparos, se esperaba menos de 60", average < 60.0)
        assertTrue("El peor caso fue ${runs.max()} disparos", runs.max() < 95)
    }

    @Test
    fun `la IA dificil es claramente mejor que la facil`() {
        val hard = (1..10).map { shotsToClear(Difficulty.DIFICIL, Fleet.CLASICA, it) }.average()
        val easy = (1..10).map { shotsToClear(Difficulty.FACIL, Fleet.CLASICA, it) }.average()
        assertTrue("Difícil $hard vs Fácil $easy", hard < easy - 10)
    }

    @Test
    fun `la IA normal queda entre la facil y la dificil`() {
        val hard = (1..10).map { shotsToClear(Difficulty.DIFICIL, Fleet.CLASICA, it) }.average()
        val normal = (1..10).map { shotsToClear(Difficulty.NORMAL, Fleet.CLASICA, it) }.average()
        val easy = (1..10).map { shotsToClear(Difficulty.FACIL, Fleet.CLASICA, it) }.average()
        assertTrue("Normal $normal debería ser mejor que Fácil $easy", normal < easy)
        assertTrue("Normal $normal debería ser peor que Difícil $hard", normal > hard - 5)
    }

    @Test
    fun `la IA se las arregla con las formas raras de la flota estandar`() {
        val runs = (1..8).map { shotsToClear(Difficulty.DIFICIL, Fleet.ESTANDAR, it) }
        assertTrue("Media ${runs.average()}", runs.average() < 70.0)
    }

    @Test
    fun `la IA aprovecha la separacion obligatoria de la flota rusa`() {
        val runs = (1..8).map { shotsToClear(Difficulty.DIFICIL, Fleet.RUSA, it) }
        assertTrue("Media ${runs.average()}", runs.average() < 80.0)
    }

    @Test
    fun `la salva de la IA son casillas distintas y sin disparar`() {
        val fleet = Fleet.CLASICA
        val rng = Random(7)
        val board = PlayerBoard(10, Placement.randomLayout(fleet, rng))
        repeat(20) { board.receive(Coord(rng.nextInt(10), rng.nextInt(10))) }
        val config = GameConfig(fleet = fleet.type, shotRule = ShotRule.SALVA)
        val game = Game(config, listOf(PlayerBoard(10, Placement.randomLayout(fleet, rng)), board), 0)
        for (difficulty in Difficulty.entries) {
            val shots = ShipAi(difficulty, rng).chooseShots(game.viewFor(0), 5)
            assertEquals("$difficulty no eligió 5 objetivos", 5, shots.size)
            assertEquals("$difficulty repitió objetivo", 5, shots.toSet().size)
            shots.forEach { assertFalse("$difficulty disparó donde ya había disparado", board.alreadyShot(it)) }
        }
    }

    @Test
    fun `al tocar un barco la IA dificil dispara pegado al impacto`() {
        val fleet = Fleet.CLASICA
        // Rival con un único crucero de 3 en D5-F5, ya tocado en el centro.
        val def = ShipDef(3, ShipKind.CRUCERO, ShipShape.line(3))
        val board = PlayerBoard(10, listOf(PlacedShip(def, Coord(3, 4), Orientation.NORTE)))
        board.receive(Coord(4, 4))
        val config = GameConfig(fleet = fleet.type, shotRule = ShotRule.UN_DISPARO)
        val game = Game(config, listOf(PlayerBoard(10, Placement.randomLayout(fleet)), board), 0)
        val view = BoardView(
            size = 10,
            misses = emptySet(),
            hits = setOf(Coord(4, 4)),
            sunkCells = emptySet(),
            remaining = listOf(def),
            mustSeparate = false,
        )
        val shot = ShipAi(Difficulty.DIFICIL, Random(1)).chooseShot(view)
        val adjacent = Coord.NEIGHBOURS_4.map { Coord(4, 4) + it }
        assertTrue("Disparó a ${shot.label()} en vez de pegado al impacto", shot in adjacent)
        assertTrue(game.viewFor(0).hits.contains(Coord(4, 4)))
    }

    @Test
    fun `el reparto de la flota de la IA es legal en las tres flotas y los tres niveles`() {
        for (fleet in Fleet.all) {
            for (difficulty in Difficulty.entries) {
                val layout = ShipAi.placeFleet(fleet, difficulty, Random(3))
                assertEquals(fleet.shipCount, layout.size)
                val cells = layout.flatMap { it.cells }
                assertEquals(cells.size, cells.toSet().size)
                assertTrue(Placement.insideBoard(cells, fleet.boardSize))
            }
        }
    }
}
