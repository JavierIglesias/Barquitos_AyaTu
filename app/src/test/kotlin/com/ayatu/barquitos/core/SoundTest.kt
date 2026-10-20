package com.ayatu.barquitos.core

import com.ayatu.barquitos.sound.Sfx
import com.ayatu.barquitos.sound.SfxSynth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pruebas de que los efectos **se oyen en un móvil**.
 *
 * El fallo que hubo no fue que el sonido no sonara, sino que sonaba a nada: los efectos salían
 * veinte o treinta decibelios por debajo de lo normal y, encima, con casi toda su energía por
 * debajo de los 300 Hz, que es donde el altavoz de un móvil no reproduce. Aquí se mide el espectro
 * de verdad (con una FFT) para que no vuelva a colarse.
 */
class SoundTest {

    /** Los que se sintetizan: los aplausos son un fichero y se miden en [AplausosTest]. */
    private val sinteticos = Sfx.entries.filter { it.sintetizado }

    /** Lo que un altavoz de móvil sí reproduce. Por debajo de esto, un móvil no da nada. */
    private val bandaBaja = 300.0
    private val bandaAlta = 8000.0

    @Test
    fun `todos los efectos llegan casi al tope sin pasarse`() {
        for (sfx in sinteticos) {
            val p = pico(SfxSynth.render(sfx))
            assertTrue("$sfx se sale de escala (pico $p)", p <= 1.0)
            assertTrue("$sfx sale flojo: pico ${dbTexto(p)}", db(p) > -8.0)
        }
    }

    @Test
    fun `lo que el altavoz de un movil reproduce se oye`() {
        // Referencia: un efecto de juego decente anda por -20 dBFS de RMS útil. Por debajo de -26
        // ya no se oye con ruido alrededor, y ahí es donde estaban los efectos antiguos: el agua
        // salía a -31 dBFS y encima se reproducía al 30 % de volumen.
        for (sfx in sinteticos) {
            val util = rmsUtil(SfxSynth.render(sfx))
            assertTrue(
                "$sfx no se oiría en un móvil: ${dbTexto(util)} entre 300 Hz y 8 kHz",
                db(util) > -26.0,
            )
        }
    }

    @Test
    fun `el grueso de la energia no se va a los graves que un movil no da`() {
        for (sfx in sinteticos) {
            val muestras = SfxSynth.render(sfx)
            val fraccion = fraccionUtil(muestras)
            assertTrue(
                "$sfx gasta la amplitud fuera de lo que suena (solo ${
                    "%.0f".format(fraccion * 100)
                } % en banda)",
                fraccion > 0.40,
            )
        }
    }

    @Test
    fun `cada efecto dura lo suyo y no se corta de golpe`() {
        for (sfx in sinteticos) {
            val m = SfxSynth.render(sfx)
            assertTrue("$sfx está vacío", m.size > SfxSynth.SAMPLE_RATE / 50)
            assertEquals("$sfx no termina en silencio", 0, m.last().toInt())
        }
    }

    @Test
    fun `el nombre del fichero lleva la generacion para no reusar los viejos`() {
        assertEquals("agua_v${SfxSynth.GENERATION}.wav", SfxSynth.fileName(Sfx.AGUA))
    }

    @Test
    fun `la sintesis es repetible`() {
        assertTrue(SfxSynth.render(Sfx.HUNDIDO).contentEquals(SfxSynth.render(Sfx.HUNDIDO)))
    }

    // ------------------------------------------------------------------ medida

    private fun pico(m: ShortArray): Double = (m.maxOfOrNull { abs(it.toInt()) } ?: 0) / 32768.0

    private fun rms(m: ShortArray): Double {
        var suma = 0.0
        m.forEach { suma += (it / 32768.0) * (it / 32768.0) }
        return sqrt(suma / m.size)
    }

    /** RMS de la parte del efecto que cae dentro de lo que reproduce un altavoz de móvil. */
    private fun rmsUtil(m: ShortArray): Double = rms(m) * sqrt(fraccionUtil(m))

    /** Qué fracción de la energía está entre [bandaBaja] y [bandaAlta]. */
    private fun fraccionUtil(m: ShortArray): Double {
        val n = 2048
        var total = 0.0
        var banda = 0.0
        var off = 0
        while (off + n <= m.size) {
            val re = DoubleArray(n)
            val im = DoubleArray(n)
            for (i in 0 until n) {
                // Ventana de Hann, para que el trozo no invente frecuencias en los bordes.
                re[i] = m[off + i] / 32768.0 * (0.5 - 0.5 * cos(2 * PI * i / n))
            }
            fft(re, im)
            for (k in 0 until n / 2) {
                val f = k.toDouble() * SfxSynth.SAMPLE_RATE / n
                val p = re[k] * re[k] + im[k] * im[k]
                total += p
                if (f in bandaBaja..bandaAlta) banda += p
            }
            off += n
        }
        if (off == 0) {
            // Efecto más corto que un bloque (el clic): se mide entero, rellenando con silencio.
            val re = DoubleArray(n)
            val im = DoubleArray(n)
            for (i in m.indices) re[i] = m[i] / 32768.0
            fft(re, im)
            for (k in 0 until n / 2) {
                val f = k.toDouble() * SfxSynth.SAMPLE_RATE / n
                val p = re[k] * re[k] + im[k] * im[k]
                total += p
                if (f in bandaBaja..bandaAlta) banda += p
            }
        }
        return if (total > 0) banda / total else 0.0
    }

    /** FFT iterativa (Cooley-Tukey) en el sitio. [re] y [im] han de tener tamaño potencia de dos. */
    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                re[i] = re[j].also { re[j] = re[i] }
                im[i] = im[j].also { im[j] = im[i] }
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2 * PI / len
            val wr = cos(ang)
            val wi = sin(ang)
            var i = 0
            while (i < n) {
                var cr = 1.0
                var ci = 0.0
                for (k in 0 until len / 2) {
                    val ur = re[i + k]
                    val ui = im[i + k]
                    val vr = re[i + k + len / 2] * cr - im[i + k + len / 2] * ci
                    val vi = re[i + k + len / 2] * ci + im[i + k + len / 2] * cr
                    re[i + k] = ur + vr
                    im[i + k] = ui + vi
                    re[i + k + len / 2] = ur - vr
                    im[i + k + len / 2] = ui - vi
                    val nr = cr * wr - ci * wi
                    ci = cr * wi + ci * wr
                    cr = nr
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun db(v: Double): Double = if (v <= 0.0) -99.0 else 20 * log10(v)

    private fun dbTexto(v: Double): String = "%.1f dBFS".format(db(v))
}
