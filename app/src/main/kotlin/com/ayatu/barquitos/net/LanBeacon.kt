package com.ayatu.barquitos.net

import com.ayatu.barquitos.log.BarquitosLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException

/**
 * La baliza: el segundo camino para que los dos móviles se encuentren, además de NSD.
 *
 * El anfitrión manda un paquete UDP de **difusión** (broadcast) cada segundo diciendo «aquí hay una
 * partida, en este puerto»; el que busca escucha en ese mismo puerto y se queda con la dirección
 * desde la que llegó el paquete, así que no hace falta resolver nada.
 *
 * Existe porque NSD no se puede dar por bueno: depende de la implementación de mDNS del móvil y de
 * si el router deja pasar el multicast, y falla de maneras distintas en cada uno (el 17-10-2026, una
 * partida publicada desde un Pixel no la vio nunca el otro móvil). La difusión UDP es mucho más
 * tonta y por eso más difícil de romper: no necesita permisos nuevos ni que nadie resuelva nombres.
 *
 * El contenido va en texto, como el resto del protocolo, para poderlo leer con un `nc -ul 48101`.
 */
object BeaconCodec {

    /** Puerto fijo de la baliza. Alto y poco habitual, para no chocar con nada conocido. */
    const val PORT = 48101

    /** Cada cuánto se repite el anuncio mientras el anfitrión espera. */
    const val INTERVAL_MS = 1_000L

    private const val MAGIC = "BQ1"

    /** Lo que dice un anuncio. La dirección no viaja dentro: es la del paquete. */
    data class Beacon(val gameId: String, val name: String, val port: Int)

    fun encode(beacon: Beacon): String = listOf(
        MAGIC,
        beacon.gameId,
        beacon.name.replace('|', ' ').take(40),
        beacon.port.toString(),
    ).joinToString("|")

    fun decode(text: String): Beacon? {
        val parts = text.trim().split("|")
        if (parts.size < 4 || parts[0] != MAGIC) return null
        if (parts[1].isBlank()) return null
        val port = parts[3].toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
        return Beacon(gameId = parts[1], name = parts[2], port = port)
    }
}

/** Manda y escucha balizas. Una instancia por partida o por búsqueda. */
class LanBeacon(private val logger: BarquitosLogger) {

    /**
     * Anuncia la partida hasta que se cancele la corrutina. No devuelve nunca por su cuenta: el que
     * la lanza la cancela cuando el rival ya ha entrado.
     */
    suspend fun announce(gameId: String, name: String, tcpPort: Int) = withContext(Dispatchers.IO) {
        val texto = BeaconCodec.encode(BeaconCodec.Beacon(gameId, name, tcpPort))
        val datos = texto.toByteArray(Charsets.UTF_8)
        runCatching {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                var avisado = false
                while (currentCoroutineContext().isActive) {
                    val destinos = broadcastTargets()
                    var enviados = 0
                    for (destino in destinos) {
                        val ok = runCatching {
                            socket.send(DatagramPacket(datos, datos.size, destino, BeaconCodec.PORT))
                        }.isSuccess
                        if (ok) enviados++
                    }
                    if (!avisado) {
                        avisado = true
                        if (enviados == 0) {
                            logger.logError("WiFi: no se pudo mandar la baliza a ninguna red")
                        } else {
                            logger.logInfo(
                                "WiFi: baliza de la partida $gameId cada segundo en el puerto " +
                                    "${BeaconCodec.PORT} ($enviados destino(s) de difusión)",
                            )
                        }
                    }
                    delay(BeaconCodec.INTERVAL_MS)
                }
            }
        }.onFailure {
            if (currentCoroutineContext().isActive) {
                logger.logError("WiFi: la baliza se ha caído: ${it.message}")
            }
        }
    }

    /**
     * Escucha anuncios hasta que se cancele la corrutina. Llama a [onFound] por cada paquete
     * válido, con la dirección de la que vino; el que la llama se encarga de no repetir partidas.
     */
    suspend fun listen(onFound: (BeaconCodec.Beacon, InetAddress) -> Unit) =
        withContext(Dispatchers.IO) {
            runCatching {
                DatagramSocket(null).use { socket ->
                    // reuseAddress para que dos apps (o la versión normal y la de pruebas) puedan
                    // escuchar a la vez; soTimeout para que el bucle se entere de la cancelación,
                    // porque `receive` bloquea el hilo y no responde a la corrutina.
                    socket.reuseAddress = true
                    socket.broadcast = true
                    socket.soTimeout = 1_000
                    socket.bind(InetSocketAddress(BeaconCodec.PORT))
                    logger.logInfo("WiFi: escuchando balizas en el puerto ${BeaconCodec.PORT}")
                    val buffer = ByteArray(512)
                    while (currentCoroutineContext().isActive) {
                        val paquete = DatagramPacket(buffer, buffer.size)
                        try {
                            socket.receive(paquete)
                        } catch (_: SocketTimeoutException) {
                            continue
                        }
                        val texto = String(paquete.data, paquete.offset, paquete.length, Charsets.UTF_8)
                        BeaconCodec.decode(texto)?.let { onFound(it, paquete.address) }
                    }
                }
            }.onFailure {
                if (currentCoroutineContext().isActive) {
                    logger.logError(
                        "WiFi: no se pudo escuchar la baliza en el puerto ${BeaconCodec.PORT}: " +
                            "${it.message}. Queda NSD.",
                    )
                }
            }
        }

    /**
     * A dónde mandar la difusión: la dirección de difusión de cada red con la que está conectado el
     * móvil, más la general. Se mandan todas porque algunos móviles y routers descartan
     * `255.255.255.255` y solo dejan pasar la de la propia subred, y otros al contrario.
     */
    private fun broadcastTargets(): List<InetAddress> {
        val destinos = mutableListOf<InetAddress>()
        runCatching {
            for (nic in NetworkInterface.getNetworkInterfaces()) {
                if (!nic.isUp || nic.isLoopback) continue
                for (dir in nic.interfaceAddresses) {
                    dir.broadcast?.let { destinos += it }
                }
            }
        }
        runCatching { destinos += InetAddress.getByName("255.255.255.255") }
        return destinos.distinct()
    }
}
