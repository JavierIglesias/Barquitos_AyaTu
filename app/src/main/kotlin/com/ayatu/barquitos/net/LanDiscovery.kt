package com.ayatu.barquitos.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.ayatu.barquitos.core.GameConfig
import com.ayatu.barquitos.log.BarquitosLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

/**
 * Buscar y publicar partidas en la red local, **por dos caminos a la vez**:
 *
 * 1. **NSD**, el descubrimiento de servicios que trae Android (el mismo mecanismo que usan las
 *    impresoras o Chromecast): el anfitrión publica un servicio `_barquitosayatu._tcp`.
 * 2. **La baliza UDP** de [LanBeacon]: un paquete de difusión cada segundo diciendo en qué puerto
 *    está la partida.
 *
 * Con cualquiera de los dos basta para que el otro móvil vea la partida; lo que se conecta después
 * es siempre un socket TCP directo. Son dos porque NSD no se puede dar por bueno: depende de la
 * implementación de mDNS de cada móvil y de si el router deja pasar el multicast.
 *
 * No hay servidor en internet ni cuenta de usuario: los dos móviles hablan directamente, y si no
 * están en la misma red WiFi simplemente no se ven.
 */
private const val SERVICE_TYPE = "_barquitosayatu._tcp."
private const val SERVICE_PREFIX = "BarquitosAyaTu"

/** `BarquitosAyaTu#a3f1 Javier` → identificador `a3f1` y etiqueta `Javier`. */
private val SERVICE_NAME_REGEX = Regex("^$SERVICE_PREFIX#([0-9a-z]{4})\\s*(.*)$")

/** Nombre con el que se publica la partida en NSD. */
private fun serviceNameFor(gameId: String, playerName: String): String =
    "$SERVICE_PREFIX#$gameId ${playerName.trim()}".trim().take(40)

/** Caracteres del identificador de partida: solo los que el regex del nombre admite. */
private const val ID_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz"

/** Un identificador corto y distinto por partida, para no repetir nombre ni confundir dos partidas. */
private fun randomGameId(): String = (1..4)
    .map { ID_CHARS[Random.nextInt(ID_CHARS.length)] }
    .joinToString("")

/** Qué dicen los códigos de error de NSD, que si no son números sueltos en el registro. */
private fun nsdError(code: Int): String = when (code) {
    // Valores de NsdManager; los dos últimos solo existen desde API 34, así que van a mano.
    0 -> "error interno"
    2 -> "parámetros no válidos"
    3 -> "ya estaba activo"
    4 -> "demasiados servicios"
    5 -> "no estaba en marcha"
    else -> "código $code"
}

/** Una partida vista en la red, por la baliza o por NSD. */
data class LanGameFound(
    val gameId: String,
    val label: String,
    /** Por dónde se vio, solo para el registro. */
    val via: String,
    /** Dirección y puerto, si se vio por la baliza: entonces no hay que resolver nada. */
    internal val address: InetAddress? = null,
    internal val port: Int = 0,
    /** El servicio de NSD, si se vio por ahí: hay que resolverlo antes de conectar. */
    internal val service: NsdServiceInfo? = null,
) {
    /** Si ya tenemos dirección, conectar es inmediato; si no, hay que pasar por NSD. */
    internal val resuelta: Boolean get() = address != null
}

/** El lado que crea la partida: publica el servicio, manda la baliza y espera a que alguien entre. */
class LanHost(
    context: Context,
    private val playerName: String,
    private val config: GameConfig,
    private val logger: BarquitosLogger,
) {
    private val nsd = context.getSystemService(NsdManager::class.java)
    private val gameId = randomGameId()
    private var serverSocket: ServerSocket? = null
    private var registration: NsdManager.RegistrationListener? = null

    /**
     * Si se dejó de publicar a propósito. Cerrar el socket hace que `accept()` lance «Socket
     * closed», y eso no es un fallo: es que el usuario se ha vuelto atrás.
     */
    @Volatile
    var cancelled: Boolean = false
        private set

    /**
     * Publica la partida y se queda esperando. Devuelve la conexión cuando el otro móvil entra.
     * Es una función suspendida y cancelable: al salir de la pantalla se deja de publicar.
     */
    suspend fun await(): LanLink = withContext(Dispatchers.IO) {
        val server = ServerSocket(0).also { serverSocket = it }
        logger.logInfo("WiFi: partida $gameId esperando en el puerto ${server.localPort}")
        register(server.localPort)
        val baliza = launch {
            LanBeacon(logger).announce(gameId, playerName.ifBlank { "partida" }, server.localPort)
        }
        try {
            val socket = server.accept()
            socket.tcpNoDelay = true
            logger.logInfo("WiFi: ha entrado ${socket.inetAddress?.hostAddress} en la partida $gameId")
            // El anfitrión manda primero las reglas, para que el invitado juegue con las mismas.
            val writer = socket.getOutputStream().bufferedWriter()
            writer.write(NetCodec.encodeHello(Hello(playerName.ifBlank { "Anfitrión" }, config)))
            writer.newLine()
            writer.flush()
            LanLink(socket, localPlayer = 0, rivalName = "")
        } finally {
            baliza.cancel()
        }
    }

    private fun register(port: Int) {
        val manager = nsd
        if (manager == null) {
            logger.logAviso("WiFi: este móvil no tiene NSD; la partida se anuncia solo con la baliza")
            return
        }
        val nombre = serviceNameFor(gameId, playerName)
        val info = NsdServiceInfo().apply {
            serviceName = nombre
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                logger.logInfo("WiFi: partida publicada en NSD como «${info.serviceName}»")
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                logger.logError(
                    "WiFi: NSD no ha podido publicar «$nombre» (${nsdError(errorCode)}). " +
                        "La partida sigue anunciándose con la baliza.",
                )
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                logger.logInfo("WiFi: partida retirada de NSD")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                logger.logAviso("WiFi: NSD no ha podido retirar la partida (${nsdError(errorCode)})")
            }
        }
        registration = listener
        runCatching { manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure {
                registration = null
                logger.logError("WiFi: fallo al pedir la publicación en NSD: ${it.message}")
            }
    }

    fun cancel() {
        cancelled = true
        registration?.let { listener -> runCatching { nsd?.unregisterService(listener) } }
        registration = null
        runCatching { serverSocket?.close() }
        serverSocket = null
    }
}

/** El lado que se une: busca partidas publicadas y se conecta a la que elija el usuario. */
class LanBrowser(context: Context, private val logger: BarquitosLogger) {

    private val nsd = context.getSystemService(NsdManager::class.java)
    private var discovery: NsdManager.DiscoveryListener? = null
    private var scope: CoroutineScope? = null

    private val _found = MutableStateFlow<List<LanGameFound>>(emptyList())
    val found: StateFlow<List<LanGameFound>> = _found.asStateFlow()

    fun start() {
        stop()
        _found.value = emptyList()
        val nuevo = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scope = it }
        nuevo.launch {
            LanBeacon(logger).listen { beacon, desde ->
                add(
                    LanGameFound(
                        gameId = beacon.gameId,
                        label = beacon.name.ifBlank { "partida" },
                        via = "la baliza",
                        address = desde,
                        port = beacon.port,
                    ),
                )
            }
        }
        startNsd()
    }

    private fun startNsd() {
        val manager = nsd
        if (manager == null) {
            logger.logAviso("WiFi: este móvil no tiene NSD; se buscan partidas solo con la baliza")
            return
        }
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                logger.logInfo("WiFi: buscando partidas en NSD")
            }

            override fun onServiceFound(info: NsdServiceInfo) {
                val match = SERVICE_NAME_REGEX.find(info.serviceName)
                if (match == null) {
                    if (info.serviceName.startsWith(SERVICE_PREFIX)) {
                        logger.logAviso("WiFi: partida con nombre raro, se ignora: «${info.serviceName}»")
                    }
                    return
                }
                val (id, etiqueta) = match.destructured
                add(
                    LanGameFound(
                        gameId = id,
                        label = etiqueta.ifBlank { "partida" },
                        via = "NSD",
                        service = info,
                    ),
                )
            }

            override fun onServiceLost(info: NsdServiceInfo) {
                val id = SERVICE_NAME_REGEX.find(info.serviceName)?.groupValues?.get(1) ?: return
                synchronized(this@LanBrowser) {
                    // Solo se quita si la vimos por NSD: si hay baliza, la baliza manda.
                    val actual = _found.value.firstOrNull { it.gameId == id } ?: return
                    if (actual.resuelta) return
                    logger.logInfo("WiFi: la partida «${actual.label}» ha dejado de anunciarse")
                    _found.value = _found.value.filterNot { it.gameId == id }
                }
            }

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                logger.logError(
                    "WiFi: NSD no ha podido buscar partidas (${nsdError(errorCode)}). " +
                        "Queda la baliza.",
                )
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                logger.logAviso("WiFi: NSD no ha podido dejar de buscar (${nsdError(errorCode)})")
            }
        }
        discovery = listener
        runCatching { manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure {
                discovery = null
                logger.logError("WiFi: fallo al pedir la búsqueda en NSD: ${it.message}")
            }
    }

    /**
     * Mete la partida en la lista. La baliza llega cada segundo y NSD puede ver la misma partida,
     * así que se agrupan por identificador y gana la que ya trae dirección, que es la que se puede
     * conectar sin resolver. Solo se escribe en el registro la primera vez que se ve cada una.
     */
    private fun add(game: LanGameFound) {
        synchronized(this) {
            val anterior = _found.value.firstOrNull { it.gameId == game.gameId }
            if (anterior != null && anterior.resuelta && !game.resuelta) return
            if (anterior == null) {
                logger.logInfo("WiFi: encontrada la partida «${game.label}» por ${game.via}")
            }
            _found.value = (_found.value.filterNot { it.gameId == game.gameId } + game)
                .sortedBy { it.label.lowercase() }
        }
    }

    fun stop() {
        discovery?.let { listener -> runCatching { nsd?.stopServiceDiscovery(listener) } }
        discovery = null
        scope?.cancel()
        scope = null
    }

    /**
     * Se conecta a la partida elegida y devuelve la conexión junto con las reglas que ha mandado
     * el anfitrión (se juega siempre con las de quien crea la partida).
     */
    suspend fun connect(game: LanGameFound): Pair<LanLink, GameConfig> {
        val destino = destinoDe(game)
        return withContext(Dispatchers.IO) {
            val socket = Socket()
            socket.connect(destino, CONNECT_TIMEOUT_MS)
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

    /** Dónde conectarse: directo si vino por la baliza, resolviendo primero si vino por NSD. */
    @Suppress("DEPRECATION") // NsdServiceInfo.host: la alternativa (hostAddresses) es API 34+.
    private suspend fun destinoDe(game: LanGameFound): InetSocketAddress {
        if (game.address != null) {
            logger.logInfo(
                "WiFi: conectando con «${game.label}» en " +
                    "${game.address.hostAddress}:${game.port} (visto por la baliza)",
            )
            return InetSocketAddress(game.address, game.port)
        }
        val info = game.service ?: error("No sé dónde está esa partida")
        val resolved = resolve(info)
        logger.logInfo(
            "WiFi: conectando con «${game.label}» en " +
                "${resolved.host?.hostAddress}:${resolved.port} (resuelto por NSD)",
        )
        return InetSocketAddress(resolved.host, resolved.port)
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
                        logger.logError("WiFi: NSD no ha podido resolver la partida (${nsdError(errorCode)})")
                        if (cont.isActive) {
                            cont.resumeWithException(
                                IllegalStateException("No se pudo resolver la partida (${nsdError(errorCode)})"),
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
