package com.ayatu.barquitos.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Los efectos del juego. */
enum class Sfx { DISPARO, IMPACTO, AGUA, HUNDIDO, VICTORIA, DERROTA, TOQUE }

/**
 * Sonido del juego **sintetizado**: la app no lleva ni un fichero de audio. La primera vez que se
 * arranca se generan unos WAV cortos en la caché (ruido filtrado con envolvente para los
 * cañonazos y las explosiones, senoidales para los avisos) y se cargan en un [SoundPool].
 *
 * Así el APK no engorda, no hay que licenciar muestras de nadie y el sonido es completamente
 * propio. Si algo falla al generar o al cargar, el juego sigue: simplemente se queda en silencio.
 */
class SoundEngine(context: Context) {

    private val appContext = context.applicationContext
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids = mutableMapOf<Sfx, Int>()

    @Volatile var soundEnabled: Boolean = true
    @Volatile var hapticsEnabled: Boolean = true

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Vibrator::class.java)
        }
    }.getOrNull()

    /** Genera (si hace falta) y carga los efectos. Conviene llamarlo fuera del hilo principal. */
    fun prepare() {
        val dir = File(appContext.cacheDir, "sfx").apply { mkdirs() }
        for (sfx in Sfx.entries) {
            runCatching {
                val file = File(dir, "${sfx.name.lowercase()}_v$GENERATION.wav")
                if (!file.exists()) writeWav(file, render(sfx))
                ids[sfx] = pool.load(file.absolutePath, 1)
            }
        }
    }

    fun play(sfx: Sfx, volume: Float = 1f) {
        if (!soundEnabled) return
        val id = ids[sfx] ?: return
        runCatching { pool.play(id, volume, volume, 1, 0, 1f) }
    }

    /** Un toque corto (impacto) o largo (hundimiento). */
    fun vibrate(millis: Long) {
        if (!hapticsEnabled) return
        val v = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(millis)
            }
        }
    }

    fun release() {
        runCatching { pool.release() }
    }

    // ------------------------------------------------------------------ síntesis

    /** Devuelve las muestras del efecto, en 16 bits mono a [SAMPLE_RATE] Hz. */
    private fun render(sfx: Sfx): ShortArray = when (sfx) {
        // Cañonazo: golpe de ruido con un «pum» grave debajo que cae rápido.
        Sfx.DISPARO -> build(0.30) { t, d ->
            val env = exp(-14.0 * t / d)
            val boom = sin(2 * PI * (90 - 45 * t / d) * t)
            0.55 * env * (0.6 * noise() + 0.8 * boom)
        }
        // Impacto: más ruido, más cuerpo y cola más larga.
        Sfx.IMPACTO -> build(0.55) { t, d ->
            val env = exp(-7.0 * t / d)
            val rumble = sin(2 * PI * (70 - 30 * t / d) * t)
            0.75 * env * (0.75 * noise() + 0.6 * rumble)
        }
        // Agua: chapoteo, ruido más agudo y corto, sin graves.
        Sfx.AGUA -> build(0.28) { t, d ->
            val env = exp(-10.0 * t / d) * (1 - t / d)
            0.45 * env * (highNoise() * 0.9 + 0.2 * sin(2 * PI * 600 * t))
        }
        // Hundimiento: explosión larga con un lamento descendente encima.
        Sfx.HUNDIDO -> build(1.10) { t, d ->
            val env = exp(-4.0 * t / d)
            val wail = sin(2 * PI * (420 - 320 * t / d) * t)
            0.8 * env * (0.6 * noise() + 0.55 * wail + 0.3 * sin(2 * PI * 55 * t))
        }
        // Victoria: tres notas ascendentes (arpegio mayor), con poco ataque.
        Sfx.VICTORIA -> build(0.95) { t, d ->
            val step = (t / (d / 3)).toInt().coerceIn(0, 2)
            val freq = listOf(440.0, 554.37, 659.25)[step]
            val local = t - step * (d / 3)
            val env = exp(-3.0 * local / (d / 3))
            0.45 * env * (sin(2 * PI * freq * t) + 0.3 * sin(4 * PI * freq * t))
        }
        // Derrota: dos notas descendentes y apagadas.
        Sfx.DERROTA -> build(1.00) { t, d ->
            val step = (t / (d / 2)).toInt().coerceIn(0, 1)
            val freq = listOf(330.0, 196.0)[step]
            val local = t - step * (d / 2)
            val env = exp(-2.5 * local / (d / 2))
            0.45 * env * (sin(2 * PI * freq * t) + 0.25 * sin(2 * PI * freq * 0.5 * t))
        }
        // Toque de interfaz: clic muy corto.
        Sfx.TOQUE -> build(0.05) { t, d ->
            val env = exp(-45.0 * t / d)
            0.3 * env * sin(2 * PI * 1200 * t)
        }
    }

    private val rng = Random(20261010)

    private fun noise(): Double = rng.nextDouble(-1.0, 1.0)

    /** Ruido con un filtro de paso alto de un polo, para el chapoteo. */
    private var lastNoise = 0.0
    private fun highNoise(): Double {
        val n = rng.nextDouble(-1.0, 1.0)
        val out = n - lastNoise
        lastNoise = n
        return out * 0.5
    }

    private inline fun build(seconds: Double, sample: (t: Double, duration: Double) -> Double): ShortArray {
        val count = (seconds * SAMPLE_RATE).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val v = sample(t, seconds).coerceIn(-1.0, 1.0)
            out[i] = (v * Short.MAX_VALUE).toInt().toShort()
        }
        // Pequeño desvanecido final para que no haya chasquido al cortar.
        val fade = (0.004 * SAMPLE_RATE).toInt().coerceAtMost(count)
        for (i in 0 until fade) {
            val k = i.toDouble() / fade
            out[count - 1 - i] = (out[count - 1 - i] * k).toInt().toShort()
        }
        return out
    }

    private fun writeWav(file: File, samples: ShortArray) {
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

    private fun RandomAccessFile.writeIntLE(value: Int) {
        write(byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte(),
        ))
    }

    private fun RandomAccessFile.writeShortLE(value: Int) {
        write(byteArrayOf((value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte()))
    }

    private companion object {
        const val SAMPLE_RATE = 22050
        /** Se sube cuando cambia la síntesis, para regenerar los WAV de la caché. */
        const val GENERATION = 1
    }
}
