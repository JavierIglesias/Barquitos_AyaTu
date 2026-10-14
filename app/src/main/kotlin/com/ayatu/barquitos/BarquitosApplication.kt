package com.ayatu.barquitos

import android.app.Application
import com.ayatu.barquitos.data.SettingsStore
import com.ayatu.barquitos.log.BarquitosLogger
import com.ayatu.barquitos.sound.SoundEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Punto de entrada de la app. Crea el registro de eventos y lo deja disponible para todo el
 * mundo, y engancha un manejador de excepciones no capturadas para que un cierre inesperado deje
 * rastro en el registro antes de que la app se vaya.
 */
class BarquitosApplication : Application() {

    lateinit var logger: BarquitosLogger
        private set

    /**
     * El sonido es de la app entera, no de una pantalla: así se prepara una sola vez al arrancar,
     * los ajustes de «Sonido» y «Vibración» se aplican **en cuanto se tocan** (antes había que
     * reiniciar la app) y Ajustes puede probarlo sin empezar una partida.
     */
    lateinit var sound: SoundEngine
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        logger = BarquitosLogger.get(this)
        val version = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: "?"
        logger.logInicio("Barquitos AyaTu $version arrancando")

        sound = SoundEngine(this, logger)
        scope.launch { sound.prepare() }
        scope.launch {
            SettingsStore(this@BarquitosApplication).settings.collect {
                sound.soundEnabled = it.sound
                sound.hapticsEnabled = it.haptics
            }
        }

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            logger.logError("Cierre inesperado en ${thread.name}: ${error.javaClass.simpleName}: ${error.message}")
            previous?.uncaughtException(thread, error)
        }
    }
}
