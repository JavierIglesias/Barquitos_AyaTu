package com.ayatu.barquitos.core

import com.ayatu.barquitos.ui.common.WindowClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cuándo la app se comporta como un móvil y cuándo como una ventana de ordenador (**modo
 * escritorio**: el móvil conectado a un monitor, una tablet con teclado o un Chromebook). Es justo
 * lo que no se puede comprobar a mano todos los días, así que las reglas son puras y se prueban
 * aquí con los tamaños reales de los aparatos.
 */
class VentanaTest {

    @Test
    fun movilDePie() {
        // Pixel 9 Pro XL vertical: una sola columna, y el tablero tan ancho como la pantalla
        // (412), porque la mitad del alto (457) da de sobra.
        assertFalse(WindowClass.esAncha(412, 915))
        assertFalse(WindowClass.esEscritorio(412, 915))
        assertEquals(412, WindowClass.ladoTablero(412, 915))
    }

    @Test
    fun movilTumbado() {
        // Tumbado sobra ancho y falta alto: dos columnas, pero no es una ventana de escritorio.
        assertTrue(WindowClass.esAncha(915, 412))
        assertFalse(WindowClass.esEscritorio(915, 412))
        // Media ventana (437) contra el 85 % del alto (350): manda el alto.
        assertEquals(350, WindowClass.ladoTablero(915, 412))
    }

    @Test
    fun monitorEnModoEscritorio() {
        assertTrue(WindowClass.esAncha(1280, 800))
        assertTrue(WindowClass.esEscritorio(1280, 800))
        // Media ventana serían 620: justo el tope, que es donde el tablero deja de crecer.
        assertEquals(WindowClass.TABLERO_MAX_DP, WindowClass.ladoTablero(1280, 800))
    }

    @Test
    fun monitorGrandeNoAgrandaElTableroSinFin() {
        // En un monitor de 2.560 dp el tablero no se hace de medio metro: se queda en el tope.
        assertEquals(WindowClass.TABLERO_MAX_DP, WindowClass.ladoTablero(2560, 1440))
    }

    @Test
    fun ventanaEncogidaDentroDelMonitor() {
        // La ventana se puede estrechar a mano: entonces vuelve a comportarse como un móvil.
        assertFalse(WindowClass.esAncha(500, 700))
        assertFalse(WindowClass.esEscritorio(500, 700))
        assertEquals(350, WindowClass.ladoTablero(500, 700))
    }

    @Test
    fun pantallaPartidaMuyBaja() {
        // Media pantalla en horizontal: ancha de sobra pero bajísima; no son dos columnas.
        assertFalse(WindowClass.esAncha(915, 380))
    }

    @Test
    fun laFraccionDeAltoSoloCuentaEnUnaColumna() {
        // Al colocar la flota el tablero deja sitio a la bandeja (0,48 en vez de 0,5): en un
        // móvil alto no se nota, pero en uno cuadrado sí (720 x 900: 432 en vez de 450).
        assertEquals(412, WindowClass.ladoTablero(412, 915, fraccionAlto = 0.48f))
        assertEquals(432, WindowClass.ladoTablero(720, 900, fraccionAlto = 0.48f))
        assertEquals(450, WindowClass.ladoTablero(720, 900))
        // ...pero con dos columnas la bandeja va al lado y la fracción ya no pinta nada.
        assertEquals(
            WindowClass.ladoTablero(1280, 800),
            WindowClass.ladoTablero(1280, 800, fraccionAlto = 0.48f),
        )
    }
}
