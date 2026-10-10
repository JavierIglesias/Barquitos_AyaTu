package com.ayatu.barquitos.net

import com.ayatu.barquitos.core.Coord
import com.ayatu.barquitos.core.Difficulty
import com.ayatu.barquitos.core.Fleet
import com.ayatu.barquitos.core.GameConfig
import com.ayatu.barquitos.core.GameMode
import com.ayatu.barquitos.core.Orientation
import com.ayatu.barquitos.core.PlacedShip
import com.ayatu.barquitos.core.ShotRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket

/**
 * Lo que se mandan los dos móviles durante una partida por WiFi local.
 *
 * El protocolo es texto, una línea por mensaje, para que se pueda leer con un `nc` si hay que
 * depurar. Las posiciones de los barcos **solo** se mandan al empezar y van directamente de un
 * móvil al otro: aquí no hay servidor ninguno.
 */
sealed interface NetMessage {
    /** La flota colocada del que lo manda. */
    data class Layout(val ships: List<PlacedShip>) : NetMessage

    /** Un disparo (reglas secuenciales). */
    data class Shot(val target: Coord) : NetMessage

    /** Los objetivos de una salva (regla SALVA). */

    /** El que lo manda se rinde. */
    data object Surrender : NetMessage

    /** El que lo manda deja la partida. */
    data object Bye : NetMessage
}

/** El saludo del anfitrión: su nombre y las reglas de la partida que ha creado. */
data class Hello(val name: String, val config: GameConfig)

/** Codificación y descodificación del protocolo. Separado para poderlo probar con pruebas. */
object NetCodec {

    const val HELLO_PREFIX = "HELLO"

    fun encodeHello(hello: Hello): String = listOf(
        HELLO_PREFIX,
        hello.name.replace('|', ' '),
        hello.config.shotRule.name,
        (hello.config.startingPlayer ?: -1).toString(),
    ).joinToString("|")

    fun decodeHello(line: String): Hello? {
        val parts = line.trim().split("|")
        if (parts.size < 4 || parts[0] != HELLO_PREFIX) return null
        return runCatching {
            Hello(
                name = parts[1],
                config = GameConfig(
                    mode = GameMode.WIFI_LOCAL,
                    shotRule = ShotRule.valueOf(parts[2]),
                    difficulty = Difficulty.NORMAL,
                    startingPlayer = parts[3].toInt().takeIf { it >= 0 },
                ),
            )
        }.getOrNull()
    }

    fun encode(message: NetMessage): String = when (message) {
        is NetMessage.Layout -> "LAYOUT|" + message.ships.joinToString(";") {
            "${it.id}:${it.anchor.x},${it.anchor.y}:${it.orientation.name}"
        }
        is NetMessage.Shot -> "SHOT|${message.target.x},${message.target.y}"
        NetMessage.Surrender -> "SURRENDER"
        NetMessage.Bye -> "BYE"
    }

    fun decode(line: String): NetMessage? {
        val trimmed = line.trim()
        val head = trimmed.substringBefore('|')
        val body = trimmed.substringAfter('|', "")
        return runCatching {
            when (head) {
                "LAYOUT" -> NetMessage.Layout(
                    body.split(";").filter { it.isNotBlank() }.map { spec ->
                        val parts = spec.split(":")
                        val def = Fleet.shipById(parts[0].toInt()) ?: error("barco ${parts[0]}")
                        val anchor = parts[1].split(",").map(String::toInt)
                        PlacedShip(def, Coord(anchor[0], anchor[1]), Orientation.valueOf(parts[2]))
                    },
                )
                "SHOT" -> {
                    val xy = body.split(",").map(String::toInt)
                    NetMessage.Shot(Coord(xy[0], xy[1]))
                }
                "SURRENDER" -> NetMessage.Surrender
                "BYE" -> NetMessage.Bye
                else -> null
            }
        }.getOrNull()
    }
}

/**
 * La conexión con el otro móvil, ya establecida. Lee líneas en un hilo de entrada/salida y las
 * publica en [incoming]; [send] escribe. Si se corta la conexión, emite [NetMessage.Bye] y se
 * cierra sola, que es lo que la partida necesita saber.
 */
class LanLink(
    private val socket: Socket,
    /** 0 si este móvil es el anfitrión, 1 si es el invitado. */
    val localPlayer: Int,
    val rivalName: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reader: BufferedReader = socket.getInputStream().bufferedReader()
    private val writer: BufferedWriter = socket.getOutputStream().bufferedWriter()

    private val _incoming = MutableSharedFlow<NetMessage>(
        replay = 8,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.SUSPEND,
    )
    val incoming: SharedFlow<NetMessage> = _incoming.asSharedFlow()

    @Volatile private var closed = false

    init {
        scope.launch {
            runCatching {
                while (!closed) {
                    val line = reader.readLine() ?: break
                    NetCodec.decode(line)?.let { _incoming.emit(it) }
                }
            }
            if (!closed) _incoming.emit(NetMessage.Bye)
        }
    }

    suspend fun send(message: NetMessage) {
        if (closed) return
        withContext(Dispatchers.IO) {
            runCatching {
                writer.write(NetCodec.encode(message))
                writer.newLine()
                writer.flush()
            }
        }
    }

    fun close() {
        if (closed) return
        closed = true
        scope.launch {
            runCatching { send(NetMessage.Bye) }
            runCatching { socket.close() }
        }
    }
}
