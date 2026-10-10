package com.ayatu.barquitos.data

import androidx.compose.ui.graphics.Color

/**
 * Los pocos datos de catálogo que le quedan al juego: los colores con los que se dibujan los
 * barcos y la lista de medallas.
 *
 * Ni los barcos ni las medallas son recursos binarios: se **dibujan** con Canvas a partir de
 * estos datos, así que la app no lleva ni una imagen. Ver `ui/common/ShipPainter.kt`.
 */

// --------------------------------------------------------------------------- colores

/** Los tres colores de un barco: casco, cubierta y detalle. */
data class ShipColors(val hull: Color, val deck: Color, val accent: Color)

object Palette {
    /** Gris de guerra, que es lo que pide un barco de guerra. */
    val BARCOS = ShipColors(
        hull = Color(0xFF8D99A6),
        deck = Color(0xFFBCC7D1),
        accent = Color(0xFFF2B33D),
    )
}

// --------------------------------------------------------------------------- medallas

/** Una medalla y la condición que la gana. */
data class Medal(
    val id: String,
    val label: String,
    val description: String,
    val earned: (Stats) -> Boolean,
)

/**
 * Las medallas. Los listones están puestos para **esta** flota: veinte casillas de barco sobre
 * cien de tablero, así que un 45 % de acierto en una partida ganada ya es una barbaridad (son
 * veinte impactos en cuarenta y cuatro disparos) y una victoria en cincuenta disparos es una
 * partida casi perfecta.
 */
object Medals {
    val all: List<Medal> = listOf(
        Medal("primer_hundimiento", "Primera sangre", "Hunde tu primer barco") { it.sunkByMe >= 1 },
        Medal("primera_victoria", "Primera victoria", "Gana una partida") { it.won >= 1 },
        Medal("artillero", "Artillero", "100 impactos en total") { it.hits >= 100 },
        Medal("maestro_artillero", "Maestro artillero", "500 impactos en total") { it.hits >= 500 },
        Medal("cazaflotas", "Cazaflotas", "Hunde 100 barcos") { it.sunkByMe >= 100 },
        Medal("francotirador", "Francotirador", "Gana con un 35 % de acierto o más") { it.bestAccuracy >= 35 },
        Medal("certero", "Puntería de almirante", "Gana con un 45 % de acierto o más") { it.bestAccuracy >= 45 },
        Medal("racha3", "Tres seguidas", "Gana tres partidas seguidas") { it.bestStreak >= 3 },
        Medal("racha10", "Invicto", "Gana diez partidas seguidas") { it.bestStreak >= 10 },
        Medal("almirante", "Almirante", "Gana 50 partidas") { it.won >= 50 },
        Medal("dos_reglas", "Las dos reglas", "Gana jugando a racha y a un disparo por turno") { it.rulesWon >= 2 },
        Medal("contra_la_maquina", "A pulso", "Gana a la máquina en Difícil") { it.hardWins >= 1 },
        Medal("veterano", "Veterano", "Juega 100 partidas") { it.played >= 100 },
        Medal("limpia", "Victoria limpia", "Gana en 50 disparos o menos") { it.bestWinShots in 1..50 },
    )

    fun earned(stats: Stats): List<Medal> = all.filter { it.earned(stats) }
}
