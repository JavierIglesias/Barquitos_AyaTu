package com.ayatu.barquitos.core

import com.ayatu.barquitos.ui.common.ContadorDeDedos
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El gesto oculto que abre el registro desde «Acerca de». La regla es **exactamente tres dedos a
 * la vez**: si fuese «al menos tres», un roce con la palma al sujetar el móvil abriría el registro,
 * y si contase los dedos solo al final del gesto no lo abriría nunca, porque al levantarlos no
 * suelen irse los tres a la vez.
 */
class GestosTest {

    @Test
    fun `tres dedos a la vez abren el registro`() {
        val c = ContadorDeDedos(3)
        listOf(1, 2, 3, 3, 2, 1, 0).forEach(c::registra)
        assertTrue(c.cumple)
    }

    @Test
    fun `un toque normal no abre nada`() {
        val c = ContadorDeDedos(3)
        listOf(1, 1, 0).forEach(c::registra)
        assertFalse(c.cumple)
    }

    @Test
    fun `dos dedos tampoco`() {
        val c = ContadorDeDedos(3)
        listOf(1, 2, 2, 0).forEach(c::registra)
        assertFalse(c.cumple)
    }

    @Test
    fun `cuatro dedos no cuentan como tres`() {
        val c = ContadorDeDedos(3)
        listOf(1, 2, 3, 4, 2, 0).forEach(c::registra)
        assertFalse(c.cumple)
    }

    @Test
    fun `un gesto sin dedos no dispara`() {
        assertFalse(ContadorDeDedos(3).cumple)
    }
}
