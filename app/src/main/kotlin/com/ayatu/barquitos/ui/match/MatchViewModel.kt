package com.ayatu.barquitos.ui.match

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ayatu.barquitos.BarquitosApplication
import com.ayatu.barquitos.core.Coord
import com.ayatu.barquitos.core.Fleet
import com.ayatu.barquitos.core.Game
import com.ayatu.barquitos.core.GameConfig
import com.ayatu.barquitos.core.GameMode
import com.ayatu.barquitos.core.Orientation
import com.ayatu.barquitos.core.PlacedShip
import com.ayatu.barquitos.core.Placement
import com.ayatu.barquitos.core.PlayerBoard
import com.ayatu.barquitos.core.ShipDef
import com.ayatu.barquitos.core.ShipKind
import com.ayatu.barquitos.core.ShotRecord
import com.ayatu.barquitos.core.ShotRule
import com.ayatu.barquitos.core.ai.ShipAi
import com.ayatu.barquitos.data.MatchResult
import com.ayatu.barquitos.data.MatchStore
import com.ayatu.barquitos.data.MatchSummary
import com.ayatu.barquitos.data.SettingsStore
import com.ayatu.barquitos.data.StatsStore
import com.ayatu.barquitos.net.LanLink
import com.ayatu.barquitos.net.NetMessage
import com.ayatu.barquitos.sound.Sfx
import com.ayatu.barquitos.sound.SoundEngine
import com.ayatu.barquitos.ui.common.BoardRender
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

/** En qué momento de la partida estamos. */
enum class MatchPhase { COLOCANDO, ESPERANDO_RIVAL, JUGANDO, RESULTADO }

/** El aviso grande que aparece un momento sobre el tablero. */
enum class FlashKind { TOCADO, AGUA, HUNDIDO, OTRA_VEZ, HUNDIDO_PROPIO }

data class Flash(val kind: FlashKind, val shipKind: ShipKind? = null, val stamp: Long = 0L)

data class MatchUiState(
    val phase: MatchPhase = MatchPhase.COLOCANDO,
    val config: GameConfig = GameConfig(),
    val boardSize: Int = 10,
    /** Qué tablero es «el mío»: el 0 contra la máquina, el que toque por WiFi. */
    val localPlayer: Int = 0,
    // --- colocación
    val placed: List<PlacedShip> = emptyList(),
    val pendingShips: List<ShipDef> = emptyList(),
    val selectedShipId: Int? = null,
    val orientation: Orientation = Orientation.HORIZONTAL,
    val preview: List<Coord> = emptyList(),
    val previewValid: Boolean = true,
    // --- partida
    val current: Int = 0,
    val round: Int = 1,
    val enemyRender: BoardRender? = null,
    val myRender: BoardRender? = null,
    val myFleet: List<FleetEntry> = emptyList(),
    val enemyFleet: List<FleetEntry> = emptyList(),
    val history: List<ShotRecord> = emptyList(),
    val flash: Flash? = null,
    val lastShot: Coord? = null,
    val lastShotHit: Boolean = false,
    /** True si el último disparo fue mío (va en el tablero enemigo). */
    val lastShotOnEnemy: Boolean = true,
    val busy: Boolean = false,
    val pendingShot: Coord? = null,
    // --- red
    val netStatus: String? = null,
    // --- resultado
    val winner: Int? = null,
    val myShots: Int = 0,
    val myHits: Int = 0,
    val revealAll: Boolean = false,
) {
    val isMyTurn: Boolean get() = current == localPlayer
    val finished: Boolean get() = winner != null
}

/** Una línea del panel de flotas: qué barco es, de qué tamaño y si está hundido. */
data class FleetEntry(val kind: ShipKind, val size: Int, val sunk: Boolean, val hits: Int)

/**
 * El cerebro de una partida. Lleva la colocación de las flotas, los turnos, la IA, los avisos y
 * el guardado automático, y expone un único [MatchUiState] para que las pantallas solo tengan que
 * pintar.
 *
 */
class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BarquitosApplication
    private val logger = app.logger
    private val settingsStore = SettingsStore(application)
    private val statsStore = StatsStore(application)
    private val matchStore = MatchStore(application)
    val sound = SoundEngine(application)

    private val _ui = MutableStateFlow(MatchUiState())
    val ui: StateFlow<MatchUiState> = _ui.asStateFlow()

    private var game: Game? = null
    private var ai: ShipAi? = null
    private val layouts = arrayOfNulls<List<PlacedShip>>(2)
    private var markWater = true
    private var confirmShot = false
    private var link: LanLink? = null
    private var remoteReady = false
    private var recorded = false

    init {
        viewModelScope.launch {
            val s = settingsStore.settings.first()
            sound.soundEnabled = s.sound
            sound.hapticsEnabled = s.haptics
            markWater = s.markWater
            confirmShot = s.confirmShot
            // Genera los WAV de los efectos la primera vez: es trabajo de disco, fuera del hilo
            // principal.
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { sound.prepare() }
        }
    }

    // ------------------------------------------------------------------ arrancar

    /** Empieza una partida nueva: toca colocar la flota. */
    fun startNew(config: GameConfig, transport: LanLink? = null) {
        link = transport
        remoteReady = false
        recorded = false
        game = null
        layouts[0] = null
        layouts[1] = null
        ai = if (config.mode == GameMode.UN_JUGADOR) ShipAi(config.difficulty) else null
        val local = if (config.mode == GameMode.WIFI_LOCAL) (transport?.localPlayer ?: 0) else 0
        _ui.value = MatchUiState(
            phase = MatchPhase.COLOCANDO,
            config = config,
            boardSize = config.boardSize,
            localPlayer = local,
            pendingShips = Fleet.ships,
            selectedShipId = Fleet.ships.firstOrNull()?.id,
        )
        logger.logInicio("Partida nueva: ${config.summary()}")
        viewModelScope.launch { settingsStore.saveLastConfig(config) }
        matchStore.clear()
        if (transport != null) listenToLink(transport)
    }

    /** Recupera la partida que se había quedado a medias. */
    fun resumeSaved(onNothing: () -> Unit = {}) {
        viewModelScope.launch {
            val saved = matchStore.load()
            if (saved == null) {
                onNothing()
                return@launch
            }
            game = saved
            ai = if (saved.config.mode == GameMode.UN_JUGADOR) ShipAi(saved.config.difficulty) else null
            layouts[0] = saved.boards[0].ships
            layouts[1] = saved.boards[1].ships
            _ui.value = MatchUiState(
                phase = MatchPhase.JUGANDO,
                config = saved.config,
                boardSize = saved.config.boardSize,
                localPlayer = 0,
            )
            logger.logInfo("Partida recuperada: ${saved.config.summary()}, ronda ${saved.round}")
            refresh()
            // Si al guardar le tocaba a la máquina, que siga ella.
            maybeRunAi()
        }
    }

    // ------------------------------------------------------------------ colocación

    fun selectShip(id: Int) {
        _ui.update { it.copy(selectedShipId = id, preview = emptyList()) }
        sound.play(Sfx.TOQUE, 0.4f)
    }

    fun rotate() {
        _ui.update { it.copy(orientation = it.orientation.next(), preview = emptyList()) }
        sound.play(Sfx.TOQUE, 0.4f)
    }

    /**
     * El dedo apunta a una casilla durante la colocación: tocándola, manteniéndola pulsada o
     * arrastrando por encima. Si baja sobre un barco ya puesto, lo levanta para que se pueda mover
     * de sitio sin tener que quitarlo primero.
     */
    fun aimAt(anchor: Coord) {
        val state = _ui.value
        val existing = state.placed.firstOrNull { anchor in it.cells }
        if (existing != null) {
            _ui.update {
                it.copy(
                    placed = it.placed - existing,
                    pendingShips = (it.pendingShips + existing.def).sortedBy { s -> s.id },
                    selectedShipId = existing.id,
                    orientation = existing.orientation,
                )
            }
            sound.play(Sfx.TOQUE, 0.5f)
        }
        previewAt(anchor)
    }

    /** Dibuja la sombra del barco seleccionado en esa casilla (para ver si cabe). */
    fun previewAt(anchor: Coord) {
        val state = _ui.value
        val def = state.pendingShips.firstOrNull { it.id == state.selectedShipId }
            ?: run {
                if (state.preview.isNotEmpty()) _ui.update { it.copy(preview = emptyList()) }
                return
            }
        // El dedo señala el centro del barco, no su esquina: así se coloca donde uno mira.
        val shape = def.shape.rotated(state.orientation)
        val offset = Coord(anchor.x - (shape.width - 1) / 2, anchor.y - (shape.height - 1) / 2)
        val cells = Placement.cellsOf(def, offset, state.orientation)
        val valid = Placement.canPlace(def, offset, state.orientation, state.placed)
        _ui.update { it.copy(preview = cells, previewValid = valid) }
    }

    /**
     * El dedo se levanta durante la colocación. Fuera del tablero no se coloca nada: el barco que
     * se venía arrastrando se queda en la bandeja y solo hay que borrar su sombra.
     */
    fun dropAt(anchor: Coord?) {
        if (anchor == null) {
            _ui.update { it.copy(preview = emptyList()) }
        } else {
            placeAt(anchor)
        }
    }

    /** Coloca el barco seleccionado. Si en esa casilla ya hay uno, lo levanta para recolocarlo. */
    fun placeAt(anchor: Coord) {
        val state = _ui.value
        val existing = state.placed.firstOrNull { anchor in it.cells }
        if (existing != null) {
            _ui.update {
                it.copy(
                    placed = it.placed - existing,
                    pendingShips = (it.pendingShips + existing.def).sortedBy { s -> s.id },
                    selectedShipId = existing.id,
                    orientation = existing.orientation,
                    preview = emptyList(),
                )
            }
            sound.play(Sfx.TOQUE, 0.5f)
            return
        }
        val def = state.pendingShips.firstOrNull { it.id == state.selectedShipId }
            ?: state.pendingShips.firstOrNull() ?: return
        val shape = def.shape.rotated(state.orientation)
        val offset = Coord(anchor.x - (shape.width - 1) / 2, anchor.y - (shape.height - 1) / 2)
        if (!Placement.canPlace(def, offset, state.orientation, state.placed)) {
            sound.play(Sfx.AGUA, 0.3f)
            return
        }
        val ship = PlacedShip(def, offset, state.orientation)
        val pending = state.pendingShips - def
        _ui.update {
            it.copy(
                placed = it.placed + ship,
                pendingShips = pending,
                selectedShipId = pending.firstOrNull()?.id,
                preview = emptyList(),
            )
        }
        sound.play(Sfx.TOQUE, 0.6f)
    }

    fun randomPlacement() {
        val state = _ui.value
        val layout = Placement.randomFill() ?: return
        _ui.update {
            it.copy(placed = layout, pendingShips = emptyList(), selectedShipId = null, preview = emptyList())
        }
        sound.play(Sfx.TOQUE, 0.7f)
    }

    fun clearPlacement() {
        val state = _ui.value
        _ui.update {
            it.copy(
                placed = emptyList(),
                pendingShips = Fleet.ships,
                selectedShipId = Fleet.ships.firstOrNull()?.id,
                preview = emptyList(),
            )
        }
    }

    /** La flota está lista: según el modo, le toca al otro jugador, a la red o empezar ya. */
    fun confirmPlacement() {
        val state = _ui.value
        if (state.pendingShips.isNotEmpty()) return
        val player = state.localPlayer
        layouts[player] = state.placed
        logger.logInfo("Flota colocada por el jugador ${player + 1}")
        when (state.config.mode) {
            GameMode.UN_JUGADOR -> {
                layouts[1] = ShipAi.placeFleet(state.config.difficulty)
                beginGame()
            }
            GameMode.WIFI_LOCAL -> {
                viewModelScope.launch {
                    link?.send(NetMessage.Layout(state.placed))
                    if (remoteReady) beginGame() else {
                        _ui.update { it.copy(phase = MatchPhase.ESPERANDO_RIVAL) }
                    }
                }
            }
        }
    }

    private fun beginGame() {
        val state = _ui.value
        val boards = listOf(
            PlayerBoard(Fleet.BOARD_SIZE, layouts[0] ?: Placement.randomLayout()),
            PlayerBoard(Fleet.BOARD_SIZE, layouts[1] ?: Placement.randomLayout()),
        )
        val starting = state.config.startingPlayer ?: Random.nextInt(2)
        game = Game(state.config, boards, starting)
        _ui.update { it.copy(phase = MatchPhase.JUGANDO) }
        logger.logInfo(
            "Empieza la batalla (${state.config.summary()}); abre fuego el jugador ${starting + 1}",
        )
        refresh()
        save()
        maybeRunAi()
    }

    // ------------------------------------------------------------------ disparar

    /** Pulsación en el tablero enemigo. */
    fun tapEnemy(target: Coord) {
        val state = _ui.value
        val g = game ?: return
        if (state.phase != MatchPhase.JUGANDO || state.busy || g.finished) return
        if (!state.isMyTurn) return
        if (g.boards[g.enemyOf(state.localPlayer)].alreadyShot(target)) return
        if (confirmShot && state.pendingShot != target) {
            _ui.update { it.copy(pendingShot = target) }
            return
        }
        _ui.update { it.copy(pendingShot = null) }
        fireLocal(target)
    }

    private fun fireLocal(target: Coord) {
        val g = game ?: return
        val shooter = g.current
        val outcome = g.fire(target) ?: return
        if (g.config.mode == GameMode.WIFI_LOCAL) {
            viewModelScope.launch { link?.send(NetMessage.Shot(target)) }
        }
        afterShot(shooter, outcome.coord, outcome.hit, outcome.sunk?.kind, byMe = true)
    }

    /** Pinta el resultado de un disparo y encadena lo que toque (IA, traspaso, fin). */
    private fun afterShot(
        shooter: Int,
        coord: Coord,
        hit: Boolean,
        sunkKind: ShipKind?,
        byMe: Boolean,
    ) {
        val g = game ?: return
        when {
            sunkKind != null -> {
                sound.play(Sfx.HUNDIDO)
                sound.vibrate(if (byMe) 120 else 200)
                logger.logInfo(
                    "Hundido ${sunkKind.name} del jugador ${g.enemyOf(shooter) + 1} en ${coord.label()}",
                )
            }
            hit -> {
                sound.play(Sfx.IMPACTO)
                sound.vibrate(40)
            }
            else -> sound.play(Sfx.AGUA)
        }
        val flash = when {
            sunkKind != null -> Flash(if (byMe) FlashKind.HUNDIDO else FlashKind.HUNDIDO_PROPIO, sunkKind, now())
            hit && byMe && g.config.shotRule == ShotRule.RACHA && !g.finished -> Flash(FlashKind.OTRA_VEZ, stamp = now())
            hit -> Flash(FlashKind.TOCADO, stamp = now())
            else -> Flash(FlashKind.AGUA, stamp = now())
        }
        _ui.update {
            it.copy(
                flash = flash,
                lastShot = coord,
                lastShotHit = hit,
                lastShotOnEnemy = shooter == it.localPlayer,
            )
        }
        refresh()
        if (g.finished) {
            finish()
            return
        }
        save()
        maybeRunAi()
    }

    /** Si le toca a la máquina, que juegue su turno entero con su pausa para que se vea. */
    private fun maybeRunAi() {
        val g = game ?: return
        val machine = ai ?: return
        if (g.finished || g.current != 1) return
        _ui.update { it.copy(busy = true) }
        refresh()
        viewModelScope.launch {
            while (true) {
                val current = game ?: break
                if (current.finished || current.current != 1) break
                delay(AI_THINK_MS)
                val view = current.viewFor(1)
                val target = machine.chooseShot(view)
                val shooter = current.current
                val outcome = current.fire(target) ?: break
                afterShotFromRival(shooter, outcome.coord, outcome.hit, outcome.sunk?.kind)
                delay(AI_SHOT_GAP_MS)
            }
            // Importante: después de soltar `busy` hay que volver a calcular los tableros, porque
            // de ahí sale si se puede disparar o no; si no, el tablero se quedaría muerto.
            _ui.update { it.copy(busy = false) }
            refresh()
        }
    }

    private fun afterShotFromRival(shooter: Int, coord: Coord, hit: Boolean, sunkKind: ShipKind?) {
        afterShot(shooter, coord, hit, sunkKind, byMe = false)
    }

    fun cancelPendingShot() {
        _ui.update { it.copy(pendingShot = null) }
    }

    // ------------------------------------------------------------------ fin

    fun surrender() {
        val g = game ?: return
        if (g.finished) return
        val local = _ui.value.localPlayer
        logger.logAviso("El jugador ${local + 1} se rinde en la ronda ${g.round}")
        if (g.config.mode == GameMode.WIFI_LOCAL) {
            viewModelScope.launch { link?.send(NetMessage.Surrender) }
        }
        // Rendirse equivale a que el rival hunda la flota: se da por perdida.
        _ui.update { it.copy(winner = g.enemyOf(local)) }
        finish(forcedWinner = g.enemyOf(local))
    }

    private fun finish(forcedWinner: Int? = null) {
        val g = game ?: return
        if (recorded) return
        recorded = true
        matchStore.clear()
        val local = _ui.value.localPlayer
        val winner = forcedWinner ?: g.winner
        val result = if (winner == local) MatchResult.GANADA else MatchResult.PERDIDA
        sound.play(if (result == MatchResult.GANADA) Sfx.VICTORIA else Sfx.DERROTA)
        val enemy = g.enemyOf(local)
        val summary = MatchSummary(
            config = g.config,
            result = result,
            myShots = g.shotsFiredBy(local),
            myHits = g.hitsBy(local),
            shipsSunkByMe = g.boards[enemy].sunkShips.size,
            shipsSunkOnMe = g.boards[local].sunkShips.size,
        )
        logger.logFin(
            "Partida acabada (${g.config.summary()}): ${result.name}, " +
                "${summary.myHits}/${summary.myShots} impactos en ${g.round} rondas",
        )
        viewModelScope.launch {
            statsStore.record(summary)
            _ui.update {
                it.copy(
                    phase = MatchPhase.RESULTADO,
                    winner = winner,
                    myShots = summary.myShots,
                    myHits = summary.myHits,
                    busy = false,
                    revealAll = true,
                )
            }
            refresh()
        }
    }

    fun toggleReveal() {
        _ui.update { it.copy(revealAll = !it.revealAll) }
        refresh()
    }

    /** Se abandona la partida (botón atrás): se guarda para poder seguirla. */
    fun leave() {
        val g = game ?: return
        if (!g.finished) {
            save()
            logger.logInfo("Partida dejada a medias en la ronda ${g.round}; queda guardada")
        }
        link?.close()
        link = null
    }

    override fun onCleared() {
        super.onCleared()
        link?.close()
        sound.release()
    }

    // ------------------------------------------------------------------ red

    private fun listenToLink(transport: LanLink) {
        viewModelScope.launch {
            transport.incoming.collect { message ->
                when (message) {
                    is NetMessage.Layout -> {
                        layouts[1 - transport.localPlayer] = message.ships
                        remoteReady = true
                        if (_ui.value.phase == MatchPhase.ESPERANDO_RIVAL) beginGame()
                    }
                    is NetMessage.Shot -> {
                        val g = game ?: return@collect
                        val shooter = g.current
                        val outcome = g.fire(message.target)
                        if (outcome != null) {
                            afterShotFromRival(shooter, outcome.coord, outcome.hit, outcome.sunk?.kind)
                        }
                    }
                    NetMessage.Surrender -> {
                        logger.logInfo("El rival se ha rendido")
                        finish(forcedWinner = transport.localPlayer)
                    }
                    NetMessage.Bye -> {
                        _ui.update { it.copy(netStatus = RIVAL_LEFT) }
                        logger.logAviso("El rival ha dejado la partida")
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ estado

    private fun save() {
        val g = game ?: return
        viewModelScope.launch { matchStore.save(g) }
    }

    /** Recalcula todo lo que se pinta a partir del estado del motor. */
    private fun refresh() {
        val g = game ?: return
        val state = _ui.value
        val local = state.localPlayer
        val enemy = g.enemyOf(local)
        val myBoard = g.boards[local]
        val enemyBoard = g.boards[enemy]
        val reveal = state.revealAll || g.finished

        val enemyRender = BoardRender(
            size = enemyBoard.size,
            ships = if (reveal) enemyBoard.ships else enemyBoard.sunkShips,
            sunkShipIds = enemyBoard.sunkShips.mapTo(HashSet()) { it.id },
            hits = enemyBoard.hits,
            misses = enemyBoard.misses,
            water = if (markWater) deducedWater(enemyBoard) else emptySet(),
            pendingShot = state.pendingShot,
            lastShot = state.lastShot?.takeIf { state.lastShotOnEnemy },
            lastShotHit = state.lastShotHit,
            showCoords = true,
            enabled = !g.finished && g.current == local && !state.busy,
        )
        val myRender = BoardRender(
            size = myBoard.size,
            ships = myBoard.ships,
            sunkShipIds = myBoard.sunkShips.mapTo(HashSet()) { it.id },
            hits = myBoard.hits,
            misses = myBoard.misses,
            showCoords = true,
            enabled = false,
            lastShot = state.lastShot?.takeIf { !state.lastShotOnEnemy },
            lastShotHit = state.lastShotHit,
            dimmed = g.current != local && !g.finished,
        )
        _ui.update {
            it.copy(
                current = g.current,
                round = g.round,
                enemyRender = enemyRender,
                myRender = myRender,
                myFleet = fleetEntries(myBoard),
                enemyFleet = fleetEntries(enemyBoard),
                history = g.history.takeLast(HISTORY_LINES).reversed(),
                winner = g.winner ?: it.winner,
            )
        }
    }

    private fun fleetEntries(board: PlayerBoard) = board.ships.map { ship ->
        FleetEntry(
            kind = ship.kind,
            size = ship.size,
            sunk = board.isSunk(ship),
            hits = board.hitsOn(ship),
        )
    }

    /**
     * Agua deducida: los barcos tienen que dejar una casilla de separación, así que alrededor de
     * uno hundido no puede haber nada. Se marca para no gastar disparos ahí.
     */
    private fun deducedWater(board: PlayerBoard): Set<Coord> {
        if (!Fleet.MUST_SEPARATE) return emptySet()
        val result = HashSet<Coord>()
        board.sunkCells.forEach { c ->
            Coord.NEIGHBOURS_8.forEach { d ->
                val n = c + d
                if (n.x in 0 until board.size && n.y in 0 until board.size &&
                    !board.alreadyShot(n) && board.shipAt(n) == null
                ) {
                    result += n
                }
            }
        }
        return result
    }

    private fun now() = System.currentTimeMillis()

    private inline fun MutableStateFlow<MatchUiState>.update(block: (MatchUiState) -> MatchUiState) {
        value = block(value)
    }

    companion object {
        private const val AI_THINK_MS = 700L
        private const val AI_SHOT_GAP_MS = 450L
        private const val HISTORY_LINES = 30
        const val WAITING_RIVAL = "waiting_rival"
        const val RIVAL_LEFT = "rival_left"
    }
}
