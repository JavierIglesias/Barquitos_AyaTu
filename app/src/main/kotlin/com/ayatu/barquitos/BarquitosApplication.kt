package com.ayatu.barquitos

import android.app.Application
import com.ayatu.barquitos.log.BarquitosLogger

/**
 * Punto de entrada de la app. Crea el registro de eventos y lo deja disponible para todo el
 * mundo, y engancha un manejador de excepciones no capturadas para que un cierre inesperado deje
 * rastro en el registro antes de que la app se vaya.
 */
class BarquitosApplication : Application() {

    lateinit var logger: BarquitosLogger
        private set

    override fun onCreate() {
        super.onCreate()
        logger = BarquitosLogger.get(this)
        val version = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: "?"
        logger.logInicio("Barquitos AyaTu $version arrancando")

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            logger.logError("Cierre inesperado en ${thread.name}: ${error.javaClass.simpleName}: ${error.message}")
            previous?.uncaughtException(thread, error)
        }
    }
}
