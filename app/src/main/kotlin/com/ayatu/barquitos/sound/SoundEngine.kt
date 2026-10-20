package com.ayatu.barquitos.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.ayatu.barquitos.R
import com.ayatu.barquitos.log.BarquitosLogger
import java.io.File

/**
 * Sonido del juego casi todo **sintetizado**: la primera vez que se arranca, [SfxSynth] genera unos
 * WAV cortos en la caché y aquí se cargan en un [SoundPool]. La única excepción son los aplausos,
 * que son una grabación real incluida en la app (`res/raw/aplausos.wav`).
 *
 * Si algo falla al generar o al cargar, el juego sigue: simplemente se queda en silencio, y el
 * registro de la app lo cuenta.
 */
class SoundEngine(context: Context, private val logger: BarquitosLogger? = null) {

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

    /**
     * Se escribe desde el hilo de disco y se lee desde el hilo principal. Es un mapa **inmutable**
     * detrás de una referencia `@Volatile` a propósito: así el hilo que dibuja ve siempre un mapa
     * entero y ya publicado, sin depender de que dos hilos se pongan de acuerdo sobre las tripas
     * de un HashMap.
     */
    @Volatile
    private var ids: Map<Sfx, Int> = emptyMap()

    /** Los efectos que no se sintetizan, con el fichero de la app del que salen. */
    private val recursos: Map<Sfx, Int> = mapOf(Sfx.APLAUSOS to R.raw.aplausos)

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
        // Los efectos de versiones anteriores ya no valen y ocupan sitio: fuera.
        val vigentes = Sfx.entries.filter { it.sintetizado }
            .map { SfxSynth.fileName(it) }
            .toSet()
        runCatching {
            dir.listFiles()?.forEach { if (it.name !in vigentes) it.delete() }
        }
        val cargados = HashMap<Sfx, Int>(Sfx.entries.size)
        for (sfx in Sfx.entries) {
            runCatching {
                val recurso = recursos[sfx]
                val id = if (recurso != null) {
                    pool.load(appContext, recurso, 1)
                } else {
                    val file = File(dir, SfxSynth.fileName(sfx))
                    if (!file.exists()) SfxSynth.writeWav(file, SfxSynth.render(sfx))
                    pool.load(file.absolutePath, 1)
                }
                if (id != 0) cargados[sfx] = id
            }.onFailure { logger?.logError("No se pudo preparar el sonido $sfx: ${it.message}") }
        }
        ids = cargados
        if (cargados.size == Sfx.entries.size) {
            logger?.logInfo("Sonido listo: ${cargados.size} efectos cargados")
        } else {
            logger?.logAviso("Sonido incompleto: ${cargados.size} de ${Sfx.entries.size} efectos")
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
}
