package com.ayatu.barquitos.data

import android.content.Context
import com.ayatu.barquitos.core.Game
import com.ayatu.barquitos.core.GameMode
import com.ayatu.barquitos.core.GameSave
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Guarda la partida a medias en un fichero de texto, para poder seguirla al volver a abrir la
 * app. Solo se guardan las partidas de un jugador y las de dos en el mismo móvil: una de WiFi
 * local no tiene sentido guardarla, porque al salir se corta la conexión con el otro móvil.
 */
class MatchStore(private val context: Context) {

    private val file: File get() = File(context.filesDir, FILE_NAME)

    suspend fun save(game: Game) = withContext(Dispatchers.IO) {
        if (game.config.mode == GameMode.WIFI_LOCAL || game.finished) {
            clear()
            return@withContext
        }
        runCatching { file.writeText(GameSave.serialize(game), Charsets.UTF_8) }
        Unit
    }

    suspend fun load(): Game? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return@withContext null
        val game = GameSave.deserialize(text)
        if (game == null || game.finished) {
            clear()
            null
        } else {
            game
        }
    }

    suspend fun hasSaved(): Boolean = withContext(Dispatchers.IO) { file.exists() }

    fun clear() {
        runCatching { if (file.exists()) file.delete() }
    }

    private companion object {
        const val FILE_NAME = "partida.sav"
    }
}
