package com.ayatu.barquitos.sound

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Los efectos del juego. */
enum class Sfx { DISPARO, IMPACTO, AGUA, HUNDIDO, VICTORIA, DERROTA, TOQUE }

/**
 * La síntesis de los efectos. No lleva nada de Android a propósito, para poder **medirla en las
 * pruebas**: lo que falló una vez fue justo esto, unos efectos correctos pero inaudibles.
 *
 * Los efectos están pensados para **el altavoz de un móvil**, que es pequeño y no da graves:
 * apenas reproduce nada por debajo de unos 300 Hz. Por eso el cuerpo de los cañonazos y de las
 * explosiones vive entre 150 y 300 Hz —y no en 50 ó 70, donde un móvil no suena— y el grueso de la
 * energía cae entre 300 Hz y 8 kHz. Además cada efecto se **normaliza** a un pico fijo, para que
 * ninguno acabe veinte decibelios por debajo de otro solo por cómo se mezclaron sus componentes.
 */
object SfxSynth {

    const val SAMPLE_RATE = 22050

    /** Se sube cuando cambia la síntesis, para regenerar los WAV de la caché. */
    const val GENERATION = 2

    /** Nombre del fichero en la caché. Lleva la generación para no reusar los de antes. */
    fun fileName(sfx: Sfx): String = "${sfx.name.lowercase()}_v$GENERATION.wav"

    /** Las muestras del efecto, en 16 bits mono a [SAMPLE_RATE] Hz, normalizadas a su pico. */
    fun render(sfx: Sfx): ShortArray {
        val rng = Random(20261014)
        fun noise() = rng.nextDouble(-1.0, 1.0)
        return when (sfx) {
            // Cañonazo: un chasquido de ruido en la zona media con un golpe corto de 420 a 240 Hz
            // debajo. Nada de 50 ni de 150 Hz: en un móvil eso no se oye, solo gasta amplitud, y
            // como luego se normaliza al pico, lo que no suena se lleva por delante lo que sí.
            Sfx.DISPARO -> {
                val crack = BandPass(1600.0, 0.9)
                build(0.22, peak = 0.90) { t, d ->
                    val env = exp(-16.0 * t / d)
                    val golpe = sin(2 * PI * (420 - 180 * t / d) * t) * exp(-9.0 * t / d)
                    env * crack.process(noise()) * 1.6 + 0.45 * golpe
                }
            }
            // Impacto: el mismo chasquido más abierto y con más cuerpo, y una cola más larga.
            Sfx.IMPACTO -> {
                val crack = BandPass(1100.0, 0.8)
                build(0.45, peak = 0.95) { t, d ->
                    val env = exp(-8.0 * t / d)
                    val cuerpo = sin(2 * PI * (520 - 260 * t / d) * t) * exp(-5.0 * t / d)
                    env * crack.process(noise()) * 1.8 + 0.5 * cuerpo
                }
            }
            // Agua: chapoteo. Ruido centrado hacia los 2 kHz, que es donde se oye un salpicón, y
            // un «plop» por debajo que cae deprisa.
            Sfx.AGUA -> {
                val splash = BandPass(2200.0, 0.7)
                build(0.33, peak = 0.85) { t, d ->
                    val env = exp(-8.0 * t / d) * (1 - t / d)
                    val plop = sin(2 * PI * (620 - 340 * t / d) * t) * exp(-14.0 * t / d)
                    env * splash.process(noise()) * 1.4 + 0.55 * plop
                }
            }
            // Hundimiento: explosión larga, con el lamento descendente bien dentro de lo que se oye.
            Sfx.HUNDIDO -> {
                val boom = BandPass(700.0, 0.5)
                build(1.05, peak = 0.97) { t, d ->
                    val env = exp(-4.0 * t / d)
                    val lamento = sin(2 * PI * (760 - 430 * t / d) * t) * exp(-3.0 * t / d)
                    val cuerpo = sin(2 * PI * (400 - 170 * t / d) * t) * exp(-2.5 * t / d)
                    env * boom.process(noise()) * 1.3 + 0.6 * lamento + 0.35 * cuerpo
                }
            }
            // Victoria: tres notas ascendentes (arpegio mayor).
            Sfx.VICTORIA -> build(0.90, peak = 0.88) { t, d ->
                val paso = (t / (d / 3)).toInt().coerceIn(0, 2)
                val freq = listOf(440.0, 554.37, 659.25)[paso]
                val local = t - paso * (d / 3)
                val env = exp(-3.0 * local / (d / 3))
                env * (sin(2 * PI * freq * t) + 0.35 * sin(4 * PI * freq * t))
            }
            // Derrota: dos notas descendentes. La grave no baja de 230 Hz, o no se oiría.
            Sfx.DERROTA -> build(0.90, peak = 0.88) { t, d ->
                val paso = (t / (d / 2)).toInt().coerceIn(0, 1)
                val freq = listOf(349.23, 233.08)[paso]
                val local = t - paso * (d / 2)
                val env = exp(-2.5 * local / (d / 2))
                env * (sin(2 * PI * freq * t) + 0.3 * sin(4 * PI * freq * t))
            }
            // Toque de interfaz: clic corto, pero que se note. Es el efecto más flojo de todos por
            // definición (dura 60 ms), así que no se le puede escatimar amplitud o no se oye.
            Sfx.TOQUE -> build(0.06, peak = 0.70) { t, d ->
                val env = exp(-14.0 * t / d)
                env * (sin(2 * PI * 1800 * t) + 0.45 * sin(2 * PI * 2700 * t))
            }
        }
    }

    /** Escribe las muestras como un WAV PCM de 16 bits mono. */
    fun writeWav(file: File, samples: ShortArray) {
        val dataBytes = samples.size * 2
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.writeBytes("RIFF")
            raf.writeIntLE(36 + dataBytes)
            raf.writeBytes("WAVE")
            raf.writeBytes("fmt ")
            raf.writeIntLE(16)
            raf.writeShortLE(1)             // PCM
            raf.writeShortLE(1)             // mono
            raf.writeIntLE(SAMPLE_RATE)
            raf.writeIntLE(SAMPLE_RATE * 2) // bytes por segundo
            raf.writeShortLE(2)             // bytes por bloque
            raf.writeShortLE(16)            // bits por muestra
            raf.writeBytes("data")
            raf.writeIntLE(dataBytes)
            val buffer = ByteArray(dataBytes)
            for (i in samples.indices) {
                buffer[i * 2] = (samples[i].toInt() and 0xFF).toByte()
                buffer[i * 2 + 1] = ((samples[i].toInt() shr 8) and 0xFF).toByte()
            }
            raf.write(buffer)
        }
    }

    /**
     * Filtro de banda de dos polos (variable de estado de Chamberlin). Sirve para teñir el ruido
     * blanco y dejarlo donde el altavoz del móvil lo reproduce, en vez de repartirlo por todo el
     * espectro, que es parte de por qué los efectos sonaban a nada.
     */
    private class BandPass(freq: Double, q: Double) {
        private val f = 2 * sin(PI * freq / SAMPLE_RATE)
        private val damp = 1.0 / q
        private var low = 0.0
        private var band = 0.0
        fun process(input: Double): Double {
            low += f * band
            val high = input - low - damp * band
            band += f * high
            return band
        }
    }

    /** Genera el efecto y lo normaliza al pico pedido, con un desvanecido al final. */
    private inline fun build(
        seconds: Double,
        peak: Double,
        sample: (t: Double, duration: Double) -> Double,
    ): ShortArray {
        val count = (seconds * SAMPLE_RATE).toInt()
        val crudo = DoubleArray(count)
        var maximo = 0.0
        for (i in 0 until count) {
            val v = sample(i.toDouble() / SAMPLE_RATE, seconds)
            crudo[i] = v
            if (abs(v) > maximo) maximo = abs(v)
        }
        val ganancia = if (maximo > 1e-9) peak / maximo else 0.0
        val out = ShortArray(count)
        for (i in 0 until count) {
            out[i] = ((crudo[i] * ganancia).coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        // Pequeño desvanecido final para que no haya chasquido al cortar.
        val fade = (0.004 * SAMPLE_RATE).toInt().coerceAtMost(count)
        for (i in 0 until fade) {
            val k = i.toDouble() / fade
            out[count - 1 - i] = (out[count - 1 - i] * k).toInt().toShort()
        }
        return out
    }

    private fun RandomAccessFile.writeIntLE(value: Int) {
        write(
            byteArrayOf(
                (value and 0xFF).toByte(),
                ((value shr 8) and 0xFF).toByte(),
                ((value shr 16) and 0xFF).toByte(),
                ((value shr 24) and 0xFF).toByte(),
            ),
        )
    }

    private fun RandomAccessFile.writeShortLE(value: Int) {
        write(byteArrayOf((value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte()))
    }
}
