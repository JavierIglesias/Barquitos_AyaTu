package com.ayatu.barquitos.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Lleva la cuenta de cuántos dedos ha habido a la vez en un gesto. Está aparte del `Modifier` para
 * poder **probarlo en la JVM**, sin emulador: la regla es «exactamente [dedos] a la vez», no «al
 * menos», para que un toque normal o un arrastre con un dedo no abran nada por accidente.
 */
class ContadorDeDedos(private val dedos: Int) {

    private var maximo = 0

    /** Apunta cuántos punteros hay en este evento. */
    fun registra(punteros: Int) {
        if (punteros > maximo) maximo = punteros
    }

    /** `true` si el gesto fue justo de [dedos] dedos. */
    val cumple: Boolean get() = maximo == dedos
}

/**
 * Dispara [onTap] cuando en un mismo gesto han llegado a estar [dedos] dedos apoyados a la vez.
 *
 * Es el truco de «toca siete veces el número de compilación» de Android, pero con multitáctil:
 * sirve para llegar al registro técnico sin ponerle un botón a la vista. **No consume los
 * eventos**, así que convive con el desplazamiento y con los toques normales de la pantalla. Es el
 * mismo gesto que usan Rutas AyaTu y Audio AyaTu, para no tener que acordarse de uno distinto en
 * cada app.
 */
fun Modifier.onMultiFingerTap(dedos: Int, onTap: () -> Unit): Modifier = pointerInput(dedos) {
    awaitEachGesture {
        val contador = ContadorDeDedos(dedos)
        do {
            val evento = awaitPointerEvent()
            contador.registra(evento.changes.size)
        } while (evento.changes.any { it.pressed })
        if (contador.cumple) onTap()
    }
}
