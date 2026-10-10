package com.ayatu.barquitos.log

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Registro de eventos de Barquitos AyaTu en fichero de texto plano, con rotación automática cada
 * ~25 KB — mismo esquema que `RutasLogger` (Rutas Ayatu) y `AguedaLogger` (Agueda), para que el
 * visor de registro se lea igual en todas las apps.
 *
 * El fichero activo es siempre "Barquitos.log"; al llenarse se renombra a "BarquitosNNNN.log"
 * (NNNN creciente = rotación más reciente) y se abre un "Barquitos.log" nuevo cuya primera línea
 * dice en qué fichero sigue la historia anterior.
 *
 * El color con el que el visor pinta cada línea se decide por palabra clave en el propio mensaje:
 * INICIO/FIN -> verde, ERROR -> rojo, AVISO -> naranja.
 */
class BarquitosLogger private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val logDir: File = File(appContext.filesDir, "logs").apply { mkdirs() }
    private val activeFile: File get() = File(logDir, ACTIVE_NAME)
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    fun logInicio(msg: String) = log("INICIO - $msg")
    fun logFin(msg: String) = log("FIN - $msg")
    fun logError(msg: String) = log("ERROR - $msg")
    fun logAviso(msg: String) = log("AVISO - $msg")
    fun logInfo(msg: String) = log(msg)

    fun log(message: String) {
        scope.launch { mutex.withLock { writeLineLocked(message) } }
    }

    /** Versión suspendida, para cuando el llamador ya está en una corrutina y quiere esperar. */
    suspend fun logAndAwait(message: String) {
        mutex.withLock { writeLineLocked(message) }
    }

    private fun writeLineLocked(message: String) {
        val line = "${timeFormat.format(System.currentTimeMillis())}  $message\n"
        if (activeFile.exists() &&
            activeFile.length() + line.toByteArray(Charsets.UTF_8).size > MAX_SIZE_BYTES
        ) {
            rotateLocked()
        }
        runCatching { activeFile.appendText(line, Charsets.UTF_8) }
    }

    private fun rotateLocked() {
        val maxN = rotatedFiles().maxOfOrNull { it.second } ?: 0
        val rotatedName = String.format(Locale.ROOT, "Barquitos%04d.log", maxN + 1)
        val rotatedFile = File(logDir, rotatedName)
        runCatching {
            activeFile.copyTo(rotatedFile, overwrite = true)
            activeFile.delete()
            val header = "${timeFormat.format(System.currentTimeMillis())}  " +
                "Versión $versionName HISTORIA ANTERIOR VIENE EN FICHERO: $rotatedName\n"
            activeFile.writeText(header, Charsets.UTF_8)
        }
    }

    /** [(fichero, número)] de los ya rotados, con el número sacado del nombre BarquitosNNNN.log. */
    private fun rotatedFiles(): List<Pair<File, Int>> =
        logDir.listFiles()?.mapNotNull { f ->
            ROTATED_REGEX.matchEntire(f.name)?.groupValues?.get(1)?.toIntOrNull()?.let { f to it }
        } ?: emptyList()

    /** Todos los ficheros de registro: el activo primero, luego los rotados de más a menos reciente. */
    fun listLogFiles(): List<File> {
        val rotated = rotatedFiles().sortedByDescending { it.second }.map { it.first }
        return if (activeFile.exists()) listOf(activeFile) + rotated else rotated
    }

    private val versionName: String
        get() = runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "?"
        }.getOrDefault("?")

    companion object {
        private const val ACTIVE_NAME = "Barquitos.log"
        private const val MAX_SIZE_BYTES = 25 * 1024
        private val ROTATED_REGEX = Regex("""Barquitos(\d{4})\.log""")

        @Volatile
        private var instance: BarquitosLogger? = null

        fun get(context: Context): BarquitosLogger = instance ?: synchronized(this) {
            instance ?: BarquitosLogger(context).also { instance = it }
        }
    }
}
