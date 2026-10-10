package com.ayatu.barquitos.core.ai

import com.ayatu.barquitos.core.BoardView
import com.ayatu.barquitos.core.Coord
import com.ayatu.barquitos.core.Difficulty
import com.ayatu.barquitos.core.Fleet
import com.ayatu.barquitos.core.Orientation
import com.ayatu.barquitos.core.Placement
import com.ayatu.barquitos.core.PlacedShip
import kotlin.random.Random

/**
 * La IA del juego. Solo recibe un [BoardView], es decir lo mismo que ve un jugador humano:
 * dónde ha disparado, qué disparos dieron en algo y qué barcos ha hundido. **No puede mirar la
 * flota del rival**, así que no hace trampas en ningún nivel.
 *
 * Los tres niveles:
 *
 *  - **Fácil**: dispara casi a ciegas. Solo de vez en cuando (40 %) remata alrededor de un
 *    impacto suyo, para que no resulte absurda.
 *  - **Normal**: «buscar y rematar» clásico. Al acertar deduce si el barco va en horizontal o
 *    vertical y sigue la línea, y descarta el agua que se sabe segura: alrededor de un barco
 *    hundido no puede haber otro, así que no gasta disparos ahí. Mientras le quedan barcos de
 *    dos o más busca al tresbolillo; en cuanto el rival solo conserva submarinos de una casilla
 *    el tresbolillo deja de valer y busca por todo el tablero, que es lo único que se puede
 *    hacer contra un barco que no deja pistas.
 *  - **Difícil**: cuenta, para cada casilla sin disparar, de cuántas maneras podría estar ahí
 *    alguno de los barcos que al rival le quedan a flote (mapa de probabilidad), dando mucho
 *    más peso a las colocaciones que explican los impactos sin hundir. Dispara al máximo. Esto
 *    aprovecha también las formas raras de la flota Estándar y la casilla de separación
 *    obligatoria de la flota Rusa.
 */
class ShipAi(
    private val difficulty: Difficulty,
    private val rng: Random = Random,
) {

    /** Elige la casilla a la que disparar. Nunca repite una ya disparada. */
    fun chooseShot(view: BoardView): Coord = when (difficulty) {
        Difficulty.FACIL -> easyShot(view)
        Difficulty.NORMAL -> normalShot(view)
        Difficulty.DIFICIL -> bestByDensity(view, 1).first()
    }

    // ------------------------------------------------------------------ niveles sencillos

    private fun easyShot(view: BoardView): Coord {
        // El nivel fácil no deduce nada: ni sigue las líneas con ganas ni descarta el agua
        // segura de alrededor de un hundido.
        val around = targetsAroundHits(view)
        if (around.isNotEmpty() && rng.nextFloat() < 0.4f) return around.random(rng)
        return freeCells(view).random(rng)
    }

    private fun normalShot(view: BoardView): Coord {
        val around = targetsAroundHits(view)
        if (around.isNotEmpty()) return around.first()
        val parity = parityCells(view)
        if (parity.isNotEmpty()) return parity.random(rng)
        val sensatas = freeCells(view) - knownWater(view)
        return if (sensatas.isNotEmpty()) sensatas.random(rng) else freeCells(view).random(rng)
    }

    /**
     * Agua que se sabe segura sin haber disparado: como dos barcos no pueden tocarse, todo lo que
     * rodea a uno hundido es mar. Es la deducción que hace cualquiera jugando a mano y la que
     * separa de verdad el nivel Normal del Fácil, porque con diez barcos son muchas casillas.
     */
    private fun knownWater(view: BoardView): Set<Coord> {
        if (!Fleet.MUST_SEPARATE || view.sunkCells.isEmpty()) return emptySet()
        val result = HashSet<Coord>()
        view.sunkCells.forEach { c -> Coord.NEIGHBOURS_8.forEach { result += c + it } }
        return result
    }

    /**
     * Casillas a las que merece la pena disparar para rematar un barco tocado: si ya hay dos
     * impactos seguidos se prolonga la línea por sus dos puntas; si hay uno solo, las cuatro
     * casillas en cruz.
     */
    private fun targetsAroundHits(view: BoardView): List<Coord> {
        val active = view.activeHits
        if (active.isEmpty()) return emptyList()
        val result = LinkedHashSet<Coord>()
        // Líneas: pares de impactos contiguos marcan dirección, y las puntas son lo más probable.
        for (hit in active) {
            for (dir in listOf(Coord(1, 0), Coord(0, 1))) {
                if (hit + dir in active) {
                    var head = hit
                    while (head + dir in active) head += dir
                    val forward = head + dir
                    if (view.inBoard(forward) && !view.shot(forward)) result += forward
                    val back = Coord(hit.x - dir.x, hit.y - dir.y)
                    if (view.inBoard(back) && !view.shot(back)) result += back
                }
            }
        }
        if (result.isNotEmpty()) return result.toList()
        for (hit in active) {
            for (dir in Coord.NEIGHBOURS_4) {
                val c = hit + dir
                if (view.inBoard(c) && !view.shot(c)) result += c
            }
        }
        return result.toList()
    }

    /**
     * Casillas libres «al tresbolillo»: con una de cada dos basta para no saltarse un barco de dos
     * o más. Si al rival solo le quedan submarinos de una casilla no sirve de nada (habría que
     * mirar todas), así que devuelve la lista vacía y el que llama busca por otro lado.
     */
    private fun parityCells(view: BoardView): List<Coord> {
        val minSize = view.remaining.minOfOrNull { it.size } ?: 1
        if (minSize < 2) return emptyList()
        val descartadas = knownWater(view)
        return freeCells(view).filter { (it.x + it.y) % 2 == 0 && it !in descartadas }
    }

    private fun freeCells(view: BoardView): List<Coord> =
        (0 until view.size).flatMap { y -> (0 until view.size).map { x -> Coord(x, y) } }
            .filter { !view.shot(it) }

    // ------------------------------------------------------------------ nivel difícil

    /** Las [count] casillas con más probabilidad de esconder barco, de mayor a menor. */
    private fun bestByDensity(view: BoardView, count: Int): List<Coord> {
        val density = densityMap(view)
        val free = freeCells(view)
        if (free.isEmpty()) return emptyList()
        if (density.isEmpty()) return free.shuffled(rng).take(count)
        // Desempate al azar para que dos partidas iguales no se jueguen exactamente igual: se
        // barajan antes y se ordena con un orden estable, en vez de meter el azar dentro del
        // comparador (un comparador que no es consistente consigo mismo hace que TimSort lance
        // «Comparison method violates its general contract!»).
        return free.shuffled(rng).sortedByDescending { density[it] ?: 0.0 }.take(count)
    }

    /**
     * Para cada casilla sin disparar, suma el peso de todas las colocaciones posibles de los
     * barcos que le quedan al rival que pasarían por ahí. Una colocación es posible si cabe en el
     * tablero, no pisa agua conocida ni barcos ya hundidos y respeta la casilla de separación
     * con los hundidos. Las colocaciones que explican impactos sin hundir
     * valen [HIT_WEIGHT] veces más por cada impacto que cubren, que es lo que hace que la IA
     * remate los barcos tocados en vez de seguir buscando.
     */
    fun densityMap(view: BoardView): Map<Coord, Double> {
        val density = HashMap<Coord, Double>()
        val active = view.activeHits
        val blocked = HashSet<Coord>(view.misses)
        blocked += view.sunkCells
        if (Fleet.MUST_SEPARATE) {
            // Pegado a un barco hundido no puede haber otro barco.
            view.sunkCells.forEach { c -> Coord.NEIGHBOURS_8.forEach { blocked += c + it } }
        }
        for (def in view.remaining) {
            val orientations =
                if (def.size == 1) listOf(Orientation.HORIZONTAL) else Orientation.entries
            for (orientation in orientations) {
                val shape = def.shape.rotated(orientation)
                for (y in 0..(view.size - shape.height)) {
                    for (x in 0..(view.size - shape.width)) {
                        val anchor = Coord(x, y)
                        val cells = shape.cells.map { it + anchor }
                        if (cells.any { it in blocked }) continue
                        val covered = cells.count { it in active }
                        var weight = 1.0
                        repeat(covered) { weight *= HIT_WEIGHT }
                        for (c in cells) {
                            if (!view.shot(c)) density[c] = (density[c] ?: 0.0) + weight
                        }
                    }
                }
            }
        }
        return density
    }

    companion object {
        private const val HIT_WEIGHT = 60.0

        /**
         * Reparte la flota de la IA. En Difícil se prueban varias repartidas y se queda con la
         * menos «agrupada» (barcos poco pegados entre sí y no todos contra el borde), que es la
         * que más cuesta de encontrar a un jugador humano.
         */
        fun placeFleet(difficulty: Difficulty, rng: Random = Random): List<PlacedShip> {
            val candidates = when (difficulty) {
                Difficulty.FACIL -> 1
                Difficulty.NORMAL -> 3
                Difficulty.DIFICIL -> 24
            }
            var best: List<PlacedShip>? = null
            var bestScore = Double.NEGATIVE_INFINITY
            repeat(candidates) {
                val layout = Placement.randomFill(emptyList(), rng) ?: return@repeat
                val score = spreadScore(layout, Fleet.BOARD_SIZE)
                if (score > bestScore) { bestScore = score; best = layout }
            }
            return best ?: Placement.randomLayout(rng)
        }

        /** Nota de reparto: penaliza barcos pegados entre sí y barcos arrimados al borde. */
        private fun spreadScore(layout: List<PlacedShip>, size: Int): Double {
            val occupied = layout.flatMapTo(HashSet()) { it.cells }
            var touching = 0
            var edge = 0
            for (ship in layout) {
                for (c in ship.cells) {
                    for (d in Coord.NEIGHBOURS_8) {
                        val n = c + d
                        if (n in occupied && layout.first { it.cells.contains(n) }.id != ship.id) touching++
                    }
                    if (c.x == 0 || c.y == 0 || c.x == size - 1 || c.y == size - 1) edge++
                }
            }
            return -(touching * 1.0) - (edge * 0.35)
        }

    }
}
