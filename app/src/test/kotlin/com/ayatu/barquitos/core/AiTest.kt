package com.ayatu.barquitos.core

import com.ayatu.barquitos.core.ai.ShipAi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Pruebas de la IA. Lo importante que se comprueba aquí:
 *  - nunca repite casilla (sería gastar turnos a cambio de nada),
 *  - solo mira el [BoardView], así que no puede hacer trampas,
 *  - y los tres niveles están realmente escalonados: Difícil barre el tablero en bastantes menos
 *    disparos que Fácil.
 *
 * Los listones están puestos para esta flota: cuatro submarinos de una casilla sueltos por el
 * tablero hacen que ni la mejor IA pueda bajar mucho, porque un barco de uno no deja pistas.
 */
class AiTest {

    /** Juega «a solitario»: la IA dispara hasta hundir la flota y devuelve cuántos disparos gastó. */
    private fun shotsToClear(difficulty: Difficulty, seed: Int): Int {
        val rng = Random(seed)
        val board = PlayerBoard(Fleet.BOARD_SIZE, Placement.randomLayout(rng))
        val ai = ShipAi(difficulty, rng)
        val config = GameConfig(shotRule = ShotRule.UN_DISPARO)
        // El jugador 0 es la IA y el 1 el tablero a batir; el 1 nunca dispara.
        val game = Game(
            config,
            listOf(PlayerBoard(Fleet.BOARD_SIZE, Placement.randomLayout(rng)), board),
            0,
        )
        var shots = 0
        val used = HashSet<Coord>()
        while (!board.fleetDestroyed) {
            val target = ai.chooseShot(game.viewFor(0))
            assertTrue("La IA ha repetido la casilla ${target.label()}", used.add(target))
            board.receive(target)
            shots++
            if (shots > Fleet.BOARD_SIZE * Fleet.BOARD_SIZE) break
        }
        assertTrue("La IA no llegó a hundir la flota", board.fleetDestroyed)
        return shots
    }

    @Test
    fun `la IA dificil barre la flota en bastante menos de 100 disparos`() {
        val runs = (1..15).map { shotsToClear(Difficulty.DIFICIL, it) }
        val average = runs.average()
        assertTrue("Media de $average disparos, se esperaba menos de 80", average < 80.0)
        assertTrue("El peor caso fue ${runs.max()} disparos", runs.max() <= 98)
    }

    @Test
    fun `la IA dificil es claramente mejor que la facil`() {
        val hard = (1..12).map { shotsToClear(Difficulty.DIFICIL, it) }.average()
        val easy = (1..12).map { shotsToClear(Difficulty.FACIL, it) }.average()
        assertTrue("Difícil $hard vs Fácil $easy", hard < easy - 8)
    }

    @Test
    fun `la IA normal queda entre la facil y la dificil`() {
        val hard = (1..12).map { shotsToClear(Difficulty.DIFICIL, it) }.average()
        val normal = (1..12).map { shotsToClear(Difficulty.NORMAL, it) }.average()
        val easy = (1..12).map { shotsToClear(Difficulty.FACIL, it) }.average()
        assertTrue("Normal $normal debería ser mejor que Fácil $easy", normal < easy)
        assertTrue("Normal $normal no debería ganar a Difícil $hard", normal > hard - 5)
    }

    @Test
    fun `al tocar un barco la IA dificil dispara pegado al impacto`() {
        // Rival con un único portaaviones de 4 en D5-G5, ya tocado en E5.
        val def = Fleet.ships.first { it.size == 4 }
        val view = BoardView(
            size = 10,
            misses = emptySet(),
            hits = setOf(Coord(4, 4)),
            sunkCells = emptySet(),
            remaining = listOf(def),
        )
        val shot = ShipAi(Difficulty.DIFICIL, Random(1)).chooseShot(view)
        val adjacent = Coord.NEIGHBOURS_4.map { Coord(4, 4) + it }
        assertTrue("Disparó a ${shot.label()} en vez de pegado al impacto", shot in adjacent)
    }

    @Test
    fun `la IA no dispara pegada a un barco ya hundido`() {
        // Un submarino de una casilla hundido en E5: por la regla de separación, las ocho
        // casillas de alrededor son agua seguro y no merecen un disparo.
        val submarino = Fleet.ships.first { it.size == 1 }
        val resto = Fleet.ships.filter { it.size > 1 }
        val view = BoardView(
            size = 10,
            misses = emptySet(),
            hits = setOf(Coord(4, 4)),
            sunkCells = setOf(Coord(4, 4)),
            remaining = resto,
        )
        val density = ShipAi(Difficulty.DIFICIL, Random(1)).densityMap(view)
        Coord.NEIGHBOURS_8.map { Coord(4, 4) + it }.forEach { vecina ->
            assertEquals(
                "La IA cree que puede haber algo en ${vecina.label()}, pegado a un hundido",
                0.0,
                density[vecina] ?: 0.0,
                0.0001,
            )
        }
        assertTrue("El submarino hundido debería seguir en la flota de prueba", submarino.size == 1)
    }

    @Test
    fun `el reparto de la flota de la IA es legal en los tres niveles`() {
        for (difficulty in Difficulty.entries) {
            val layout = ShipAi.placeFleet(difficulty, Random(3))
            assertEquals(Fleet.shipCount, layout.size)
            val cells = layout.flatMap { it.cells }
            assertEquals("La IA ha solapado barcos", cells.size, cells.toSet().size)
            assertTrue(Placement.insideBoard(cells))
            // Y respeta la separación obligatoria, que es lo que más fácil se escapa.
            for (ship in layout) {
                val vecinas = ship.cells.flatMap { c -> Coord.NEIGHBOURS_8.map { c + it } }.toSet()
                for (otro in layout.filter { it.id != ship.id }) {
                    assertTrue(
                        "En $difficulty los barcos ${ship.id} y ${otro.id} se tocan",
                        otro.cells.none { it in vecinas },
                    )
                }
            }
        }
    }
}
