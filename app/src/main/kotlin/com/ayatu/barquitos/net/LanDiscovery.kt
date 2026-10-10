package com.ayatu.barquitos.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.ayatu.barquitos.core.GameConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Buscar y publicar partidas en la red local con **NSD** (el descubrimiento de servicios que trae
 * Android, el mismo mecanismo que usan las impresoras o Chromecast): el móvil que crea la partida
 * publica un servicio `_barquitosayatu._tcp` y el otro lo encuentra y se conecta por TCP.
 *
 * No hay servidor en internet ni cuenta de usuario: los dos móviles hablan directamente, y si no
 * están en la misma red WiFi simplemente no se ven.
 */
private const val SERVICE_TYPE = "_barquitosayatu._tcp."
private const val SERVICE_PREFIX = "BarquitosAyaTu"

/** Una partida vista en la red. */
data class LanGameFound(val serviceName: String, val label: String, val info: NsdServiceInfo)

/** El lado que crea la partida: publica el servicio y espera a que alguien se conecte. */
class LanHost(
    context: Context,
    private val playerName: String,
    private val config: GameConfig,
) {
    private val nsd = context.getSystemService(NsdManager::class.java)
    private var serverSocket: ServerSocket? = null
    private var registration: NsdManager.RegistrationListener? = null

    /**
     * Publica la partida y se queda esperando. Devuelve la conexión cuando el otro móvil entra.
     * Es una función suspendida y cancelable: al salir de la pantalla se deja de publicar.
     */
    suspend fun await(): LanLink = withContext(Dispatchers.IO) {
        val server = ServerSocket(0).also { serverSocket = it }
        register(server.localPort)
        val socket = server.accept()
        socket.tcpNoDelay = true
        // El anfitrión manda primero las reglas, para que el invitado juegue con las mismas.
        val writer = socket.getOutputStream().bufferedWriter()
        writer.write(NetCodec.encodeHello(Hello(playerName.ifBlank { "Anfitrión" }, config)))
        writer.newLine()
        writer.flush()
        LanLink(socket, localPlayer = 0, rivalName = "")
    }

    private fun register(port: Int) {
        val manager = nsd ?: return
        val info = NsdServiceInfo().apply {
            serviceName = "$SERVICE_PREFIX ${playerName.ifBlank { "partida" }}".take(40)
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
        }
        registration = listener
        runCatching { manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
    }

    fun cancel() {
        registration?.let { listener -> runCatching { nsd?.unregisterService(listener) } }
        registration = null
        runCatching { serverSocket?.close() }
        serverSocket = null
    }
}

/** El lado que se une: busca partidas publicadas y se conecta a la que elija el usuario. */
class LanBrowser(context: Context) {

    private val nsd = context.getSystemService(NsdManager::class.java)
    private var discovery: NsdManager.DiscoveryListener? = null

    private val _found = MutableStateFlow<List<LanGameFound>>(emptyList())
    val found: StateFlow<List<LanGameFound>> = _found.asStateFlow()

    fun start() {
        val manager = nsd ?: return
        stop()
        _found.value = emptyList()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onServiceFound(info: NsdServiceInfo) {
                if (!info.serviceName.startsWith(SERVICE_PREFIX)) return
                val label = info.serviceName.removePrefix(SERVICE_PREFIX).trim()
                    .ifBlank { info.serviceName }
                _found.value = (_found.value.filterNot { it.serviceName == info.serviceName } +
                    LanGameFound(info.serviceName, label, info))
            }

            override fun onServiceLost(info: NsdServiceInfo) {
                _found.value = _found.value.filterNot { it.serviceName == info.serviceName }
            }

            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }
        discovery = listener
        runCatching { manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
    }

    fun stop() {
        discovery?.let { listener -> runCatching { nsd?.stopServiceDiscovery(listener) } }
        discovery = null
    }

    /**
     * Se conecta a la partida elegida y devuelve la conexión junto con las reglas que ha mandado
     * el anfitrión (se juega siempre con las de quien crea la partida).
     */
    @Suppress("DEPRECATION") // NsdServiceInfo.host: la alternativa (hostAddresses) es API 34+.
    suspend fun connect(game: LanGameFound): Pair<LanLink, GameConfig> {
        val resolved = resolve(game.info)
        return withContext(Dispatchers.IO) {
            val socket = Socket()
            socket.connect(
                InetSocketAddress(resolved.host, resolved.port),
                CONNECT_TIMEOUT_MS,
            )
            socket.tcpNoDelay = true
            val line = socket.getInputStream().bufferedReader().readLine()
                ?: error("El anfitrión no ha mandado las reglas")
            val hello = NetCodec.decodeHello(line) ?: error("Saludo no reconocido: $line")
            LanLink(
                socket = socket,
                localPlayer = 1,
                rivalName = hello.name,
            ) to hello.config
        }
    }

    private suspend fun resolve(info: NsdServiceInfo): NsdServiceInfo =
        suspendCancellableCoroutine { cont ->
            val manager = nsd
            if (manager == null) {
                cont.resumeWithException(IllegalStateException("Este móvil no tiene NSD"))
                return@suspendCancellableCoroutine
            }
            @Suppress("DEPRECATION")
            manager.resolveService(
                info,
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                        if (cont.isActive) {
                            cont.resumeWithException(
                                IllegalStateException("No se pudo resolver la partida (código $errorCode)"),
                            )
                        }
                    }

                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        if (cont.isActive) cont.resume(resolved)
                    }
                },
            )
        }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000
    }
}
