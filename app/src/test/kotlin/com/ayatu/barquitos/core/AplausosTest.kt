package com.ayatu.barquitos.core

import com.ayatu.barquitos.sound.Sfx
import com.ayatu.barquitos.sound.SfxSynth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Los aplausos son el único sonido que **no** se sintetiza: son una grabación real (CC0, de
 * Wikimedia Commons) que va en `res/raw/aplausos.wav`. Un público sintetizado sonaba a lluvia, así
 * que se cambió por esto.
 *
 * Aquí se mide el fichero tal cual está en el repositorio, para que no se cuele un recorte mudo,
 * en estéreo, a otra frecuencia o sin palmadas distinguibles. Lo que hace que algo suene a aplauso
 * y no a ruido son los **transitorios**: muchas subidas bruscas y cortas de nivel.
 */
class AplausosTest {

    private val fichero: File by lazy {
        // Según quién lance las pruebas, el directorio de trabajo es el módulo o la raíz.
        listOf(
            File("src/main/res/raw/aplausos.wav"),
            File("app/src/main/res/raw/aplausos.wav"),
        ).firstOrNull { it.exists() } ?: error("no se encuentra res/raw/aplausos.wav")
    }

    @Test
    fun `los aplausos no se sintetizan`() {
        assertTrue("APLAUSOS no debería estar marcado como sintetizado", !Sfx.APLAUSOS.sintetizado)
        val error = runCatching { SfxSynth.render(Sfx.APLAUSOS) }.exceptionOrNull()
        assertTrue("render(APLAUSOS) debería fallar y no devolver silencio", error != null)
    }

    @Test
    fun `el fichero es el que el SoundPool espera`() {
        val wav = leer()
        assertEquals("tiene que ser mono", 1, wav.canales)
        assertEquals("tiene que ser PCM de 16 bits", 16, wav.bits)
        // La misma frecuencia que los efectos sintetizados: así no hay que remuestrear nada.
        assertEquals(SfxSynth.SAMPLE_RATE, wav.frecuencia)
        val segundos = wav.muestras.size.toDouble() / wav.frecuencia
        assertTrue("dura $segundos s: se queda corto o se hace largo", segundos in 2.0..4.0)
    }

    @Test
    fun `suena al nivel de los demas efectos`() {
        val m = leer().muestras
        val pico = (m.maxOfOrNull { abs(it.toInt()) } ?: 0) / 32768.0
        assertTrue("se sale de escala (pico $pico)", pico <= 1.0)
        assertTrue("sale flojo: pico ${db(pico)} dBFS", db(pico) > -3.0)
        assertTrue("el conjunto sale flojo: rms ${db(rms(m))} dBFS", db(rms(m)) > -24.0)
    }

    @Test
    fun `se distinguen las palmadas, no es un siseo`() {
        val m = leer().muestras
        assertTrue("solo ${transitorios(m)} palmadas: no suena a público", transitorios(m) > 60)
    }

    @Test
    fun `no empieza ni acaba de golpe`() {
        val wav = leer()
        val m = wav.muestras
        val diezMs = wav.frecuencia / 100
        assertTrue("arranca con un chasquido", abs(m.first().toInt()) < 2000)
        assertTrue("se corta en seco", abs(m.last().toInt()) < 2000)
        // Y el final tiene que estar de verdad apagándose, no cortado con un fundido de 10 ms.
        val cola = rms(m.copyOfRange(m.size - diezMs * 20, m.size))
        val medio = rms(m.copyOfRange(m.size / 3, m.size / 3 + diezMs * 20))
        assertTrue("la cola no se apaga", cola < medio)
    }

    // ------------------------------------------------------------------ medida

    private class Wav(
        val canales: Int,
        val bits: Int,
        val frecuencia: Int,
        val muestras: ShortArray,
    )

    private fun leer(): Wav {
        val b = fichero.readBytes()
        fun le16(i: Int) = (b[i].toInt() and 0xFF) or ((b[i + 1].toInt() and 0xFF) shl 8)
        fun le32(i: Int) = le16(i) or (le16(i + 2) shl 16)
        assertEquals("no es un RIFF", "RIFF", String(b, 0, 4))
        assertEquals("no es un WAVE", "WAVE", String(b, 8, 4))
        var pos = 12
        var canales = 0
        var bits = 0
        var frecuencia = 0
        var muestras = ShortArray(0)
        while (pos + 8 <= b.size) {
            val id = String(b, pos, 4)
            val tam = le32(pos + 4)
            val datos = pos + 8
            when (id) {
                "fmt " -> {
                    canales = le16(datos + 2)
                    frecuencia = le32(datos + 4)
                    bits = le16(datos + 14)
                }
                "data" -> {
                    val n = tam / 2
                    muestras = ShortArray(n) { le16(datos + it * 2).toShort() }
                }
            }
            pos = datos + tam + (tam and 1)
        }
        assertTrue("el WAV no trae muestras", muestras.isNotEmpty())
        return Wav(canales, bits, frecuencia, muestras)
    }

    private fun rms(m: ShortArray): Double {
        var suma = 0.0
        m.forEach { suma += (it / 32768.0) * (it / 32768.0) }
        return sqrt(suma / m.size)
    }

    private fun db(v: Double): Double = if (v > 1e-12) 20 * log10(v) else -999.0

    /** Palmadas: picos de la envolvente en ventanas de 5 ms que destacan sobre sus vecinas. */
    private fun transitorios(m: ShortArray): Int {
        val ventana = SfxSynth.SAMPLE_RATE / 200
        val env = ArrayList<Double>(m.size / ventana + 1)
        var i = 0
        while (i + ventana <= m.size) {
            var max = 0.0
            for (j in i until i + ventana) {
                val v = abs(m[j].toInt()) / 32768.0
                if (v > max) max = v
            }
            env.add(max)
            i += ventana
        }
        var cuenta = 0
        for (k in 1 until env.size - 1) {
            if (env[k] > env[k - 1] && env[k] >= env[k + 1] && env[k] > 0.12) cuenta++
        }
        return cuenta
    }
}
