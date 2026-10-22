package com.ayatu.barquitos.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * El tamaño de la ventana en la que va la app. Importa desde que Android tiene **modo escritorio**:
 * al conectar el móvil a un monitor por USB-C o HDMI, la app se abre en una ventana que se mueve y
 * se redimensiona, con ratón y teclado, y lo mismo pasa en una tablet, en un Chromebook o con la
 * pantalla partida.
 *
 * Las reglas son **puras** (dp de ancho y de alto, nada de Android) para poder probarlas en la JVM:
 * el modo escritorio es justo lo que no se puede comprobar a mano todos los días.
 */
object WindowClass {

    /** A partir de este ancho caben dos columnas: el tablero a un lado y el resto al otro. */
    const val ANCHA_DP = 840

    /** Alto mínimo para que sea una ventana de verdad y no un móvil tumbado. */
    const val ALTA_DP = 600

    /** Ancho máximo del contenido de texto: en un monitor, una línea de 2.000 px no se lee. */
    val CONTENIDO_MAX: Dp = 900.dp

    /** Ancho máximo de las pantallas de juego, que sí aprovechan el monitor. */
    val JUEGO_MAX: Dp = 1500.dp

    /** Más grande que esto el tablero no gana nada y obliga a mover la cabeza. */
    const val TABLERO_MAX_DP = 620

    /** Dos columnas: en el móvil tumbado también, que ahí sobra ancho y falta alto. */
    fun esAncha(anchoDp: Int, altoDp: Int): Boolean = anchoDp >= ANCHA_DP && altoDp >= 400

    /** Ventana de escritorio (monitor, tablet grande): ancha **y** alta. */
    fun esEscritorio(anchoDp: Int, altoDp: Int): Boolean =
        anchoDp >= ANCHA_DP && altoDp >= ALTA_DP

    /**
     * El lado del tablero, en dp.
     *
     * En una columna el límite es el **alto**: si el tablero se come la pantalla, el tablero propio
     * y los botones se quedan fuera de la vista ([fraccionAlto] es cuánto puede ocupar). En dos
     * columnas el límite es la **mitad del ancho**, y ahí el alto casi nunca estorba.
     */
    fun ladoTablero(anchoDp: Int, altoDp: Int, fraccionAlto: Float = 0.5f): Int {
        val lado = if (esAncha(anchoDp, altoDp)) {
            minOf((anchoDp - 40) / 2, (altoDp * 0.85f).toInt())
        } else {
            minOf(anchoDp, (altoDp * fraccionAlto).toInt())
        }
        return lado.coerceAtMost(TABLERO_MAX_DP)
    }
}

@Composable
fun esVentanaAncha(): Boolean = LocalConfiguration.current.let {
    WindowClass.esAncha(it.screenWidthDp, it.screenHeightDp)
}

@Composable
fun esVentanaEscritorio(): Boolean = LocalConfiguration.current.let {
    WindowClass.esEscritorio(it.screenWidthDp, it.screenHeightDp)
}

/** El lado del tablero para esta ventana. */
@Composable
fun ladoTablero(fraccionAlto: Float = 0.5f): Dp = LocalConfiguration.current.let {
    WindowClass.ladoTablero(it.screenWidthDp, it.screenHeightDp, fraccionAlto).dp
}

/**
 * Dos bloques de contenido que, según el sitio, van **uno debajo del otro** (móvil de pie) o
 * **lado a lado** (ventana ancha: móvil tumbado, tablet o monitor). Cada columna se desplaza por su
 * cuenta cuando van al lado, que es lo que se espera en una ventana de ordenador.
 */
@Composable
fun DosPaneles(
    ancha: Boolean,
    modifier: Modifier = Modifier,
    separacion: Dp = 8.dp,
    principal: @Composable ColumnScope.() -> Unit,
    secundario: @Composable ColumnScope.() -> Unit,
) {
    if (ancha) {
        Row(
            modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(separacion),
                content = principal,
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(separacion),
                content = secundario,
            )
        }
    } else {
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(separacion),
        ) {
            principal()
            secundario()
        }
    }
}
