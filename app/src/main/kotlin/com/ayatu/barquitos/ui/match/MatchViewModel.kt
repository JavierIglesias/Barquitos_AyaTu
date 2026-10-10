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
import com.ayatu.barquitos.data.Blueprint
import com.ayatu.barquitos.data.Blueprints
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
enum class MatchPhase { COLOCANDO, TRASPASO, ESPERANDO_RIVAL, JUGANDO, RESULTADO }

/** El aviso grande que aparece un momento sobre el tablero. */
enum class FlashKind { TOCADO, AGUA, HUNDIDO, OTRA_VEZ, HUNDIDO_PROPIO }

data class Flash(val kind: FlashKind, val shipKind: ShipKind? = null, val stamp: Long = 0L)

/** Resumen de una ronda de SALVA, que se muestra con los disparos de los dos a la vez. */
data class SalvoSummary(
    val myShots: List<Pair<Coord, Boolean>>,
    val rivalShots: List<Pair<Coord, Boolean>>,
    val mySunk: List<ShipKind>,
    val rivalSunk: List<ShipKind>,
)

data class MatchUiState(
    val phase: MatchPhase = MatchPhase.COLOCANDO,
    val config: GameConfig = GameConfig(),
    val boardSize: Int = 10,
    /** Qué tablero es «el mío» en pantalla ahora mismo (en dos jugadores va alternando). */
    val localPlayer: Int = 0,
    // --- colocación
    val placingPlayer: Int = 0,
    val placed: List<PlacedShip> = emptyList(),
    val pendingShips: List<ShipDef> = emptyList(),
    val selectedShipId: Int? = null,
    val orientation: Orientation = Orientation.NORTE,
    val preview: List<Coord> = emptyList(),
    val previewValid: Boolean = true,
    // --- partida
    val current: Int = 0,
    val shotsLeft: Int = 0,
    val round: Int = 1,
    val salvoSize: Int = 0,
    val aimCount: Int = 0,
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
    val salvoSummary: SalvoSummary? = null,
    // --- traspaso y red
    val handoffPlayer: Int = 0,
    val netStatus: String? = null,
    // --- resultado
    val winner: Int? = null,
    val myShots: Int = 0,
    val myHits: Int = 0,
    val unlocked: List<Blueprint> = emptyList(),
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
 * Se encarga también de que en «dos jugadores en el mismo móvil» nadie vea la flota del otro:
 * entre turno y turno se mete una pantalla de traspaso ([MatchPhase.TRASPASO]).
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
            placingPlayer = local,
            pendingShips = config.fleetDef.ships,
            selectedShipId = config.fleetDef.ships.firstOrNull()?.id,
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
            val local = if (saved.config.mode == GameMode.DOS_JUGADORES) saved.current else 0
            _ui.value = MatchUiState(
                phase = MatchPhase.JUGANDO,
                config = saved.config,
                boardSize = saved.config.boardSize,
                localPlayer = local,
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

    /** Dibuja la sombra del barco seleccionado en esa casilla (para ver si cabe). */
    fun previewAt(anchor: Coord) {
        val state = _ui.value
        val def = state.pendingShips.firstOrNull { it.id == state.selectedShipId } ?: return
        val fleet = state.config.fleetDef
        // El dedo señala el centro del barco, no su esquina: así se coloca donde uno mira.
        val shape = def.shape.rotated(state.orientation)
        val offset = Coord(anchor.x - (shape.width - 1) / 2, anchor.y - (shape.height - 1) / 2)
        val cells = Placement.cellsOf(def, offset, state.orientation)
        val valid = Placement.canPlace(fleet, def, offset, state.orientation, state.placed)
        _ui.update { it.copy(preview = cells, previewValid = valid) }
    }

    /** Coloca el barco seleccionado. Si en esa casilla ya hay uno, lo levanta para recolocarlo. */
    fun placeAt(anchor: Coord) {
        val state = _ui.value
        val fleet = state.config.fleetDef
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
        if (!Placement.canPlace(fleet, def, offset, state.orientation, state.placed)) {
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

    fun removeAt(anchor: Coord) {
        val existing = _ui.value.placed.firstOrNull { anchor in it.cells } ?: return
        _ui.update {
            it.copy(
                placed = it.placed - existing,
                pendingShips = (it.pendingShips + existing.def).sortedBy { s -> s.id },
                selectedShipId = existing.id,
                preview = emptyList(),
            )
        }
        sound.play(Sfx.TOQUE, 0.5f)
    }

    fun randomPlacement() {
        val state = _ui.value
        val layout = Placement.randomFill(state.config.fleetDef, emptyList()) ?: return
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
                pendingShips = state.config.fleetDef.ships,
                selectedShipId = state.config.fleetDef.ships.firstOrNull()?.id,
                preview = emptyList(),
            )
        }
    }

    /** La flota está lista: según el modo, le toca al otro jugador, a la red o empezar ya. */
    fun confirmPlacement() {
        val state = _ui.value
        if (state.pendingShips.isNotEmpty()) return
        val player = state.placingPlayer
        layouts[player] = state.placed
        logger.logInfo("Flota colocada por el jugador ${player + 1} (${state.config.fleet.name})")
        when (state.config.mode) {
            GameMode.UN_JUGADOR -> {
                layouts[1] = ShipAi.placeFleet(state.config.fleetDef, state.config.difficulty)
                beginGame()
            }
            GameMode.DOS_JUGADORES -> {
                if (player == 0) {
                    _ui.update {
                        it.copy(
                            phase = MatchPhase.TRASPASO,
                            handoffPlayer = 1,
                            placingPlayer = 1,
                            placed = emptyList(),
                            pendingShips = it.config.fleetDef.ships,
                            selectedShipId = it.config.fleetDef.ships.firstOrNull()?.id,
                            preview = emptyList(),
                        )
                    }
                } else {
                    beginGame()
                }
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

    /** El jugador al que se le pasa el móvil dice que ya lo tiene. */
    fun handoffDone() {
        val state = _ui.value
        when {
            // Traspaso antes de colocar la segunda flota.
            layouts[1] == null && state.config.mode == GameMode.DOS_JUGADORES ->
                _ui.update { it.copy(phase = MatchPhase.COLOCANDO) }
            // Traspaso entre turnos.
            else -> {
                _ui.update { it.copy(phase = MatchPhase.JUGANDO, localPlayer = state.handoffPlayer) }
                refresh()
            }
        }
    }

    private fun beginGame() {
        val state = _ui.value
        val fleet = state.config.fleetDef
        val boards = listOf(
            PlayerBoard(fleet.boardSize, layouts[0] ?: Placement.randomLayout(fleet)),
            PlayerBoard(fleet.boardSize, layouts[1] ?: Placement.randomLayout(fleet)),
        )
        var starting = state.config.startingPlayer ?: Random.nextInt(2)
        // En SALVA los dos disparan a la vez, así que «quién empieza» solo decide quién apunta
        // primero, y eso únicamente tiene sentido cuando se comparte el móvil. Contra la máquina
        // o por WiFi, el que apunta primero tiene que ser siempre el jugador de este móvil: si no,
        // se quedaría esperando un turno que no llega nunca.
        if (state.config.shotRule.isSimultaneous && state.config.mode != GameMode.DOS_JUGADORES) {
            starting = if (state.config.mode == GameMode.WIFI_LOCAL) state.localPlayer else 0
        }
        val created = Game(state.config, boards, starting)
        game = created
        val local = when (state.config.mode) {
            GameMode.DOS_JUGADORES -> starting
            GameMode.WIFI_LOCAL -> state.localPlayer
            GameMode.UN_JUGADOR -> 0
        }
        _ui.update { it.copy(phase = MatchPhase.JUGANDO, localPlayer = local) }
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
        if (g.config.shotRule == ShotRule.SALVA) {
            if (g.toggleAim(target)) {
                sound.play(Sfx.TOQUE, 0.5f)
                refresh()
            }
            return
        }
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
        if (shooter != g.current) handleTurnChange(previous = shooter)
        maybeRunAi()
    }

    /** Al cambiar de turno en dos jugadores hay que tapar la pantalla antes de pasar el móvil. */
    private fun handleTurnChange(previous: Int) {
        val g = game ?: return
        if (g.config.mode != GameMode.DOS_JUGADORES) return
        viewModelScope.launch {
            delay(HANDOFF_DELAY_MS)
            if (game?.finished == true) return@launch
            _ui.update { it.copy(phase = MatchPhase.TRASPASO, handoffPlayer = g.current, flash = null) }
        }
    }

    /** Si le toca a la máquina, que juegue su turno entero con su pausa para que se vea. */
    private fun maybeRunAi() {
        val g = game ?: return
        val machine = ai ?: return
        if (g.finished || g.current != 1) return
        if (g.config.shotRule == ShotRule.SALVA) return
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

    // ------------------------------------------------------------------ salva

    /** El jugador da por buena su salva. */
    fun confirmSalvo() {
        val g = game ?: return
        if (g.config.shotRule != ShotRule.SALVA) return
        val state = _ui.value
        if (g.aimOf(state.localPlayer).size != g.salvoSize(state.localPlayer)) return
        val myShots = g.aimOf(state.localPlayer).toList()
        val bothReady = g.confirmAim()
        when {
            bothReady -> resolveSalvo()
            ai != null -> {
                // La máquina apunta sin ver lo que has marcado tú.
                val machine = ai!!
                val targets = machine.chooseShots(g.viewFor(1), g.salvoSize(1))
                g.setAim(1, targets)
                resolveSalvo()
            }
            g.config.mode == GameMode.WIFI_LOCAL -> {
                viewModelScope.launch { link?.send(NetMessage.Salvo(myShots)) }
                _ui.update { it.copy(busy = true, netStatus = WAITING_RIVAL) }
                refresh()
            }
            else -> {
                // Dos jugadores en el mismo móvil: le toca apuntar al otro.
                _ui.update { it.copy(phase = MatchPhase.TRASPASO, handoffPlayer = g.current) }
            }
        }
    }

    private fun resolveSalvo() {
        val g = game ?: return
        val local = _ui.value.localPlayer
        val before = listOf(g.boards[0].sunkShips.size, g.boards[1].sunkShips.size)
        val round = g.resolveSalvo()
        val mine = round.outcomes[local].orEmpty()
        val theirs = round.outcomes[g.enemyOf(local)].orEmpty()
        val mySunk = mine.mapNotNull { it.sunk?.kind }
        val rivalSunk = theirs.mapNotNull { it.sunk?.kind }
        if (mine.any { it.hit }) {
            sound.play(if (mySunk.isNotEmpty()) Sfx.HUNDIDO else Sfx.IMPACTO)
            sound.vibrate(if (mySunk.isNotEmpty()) 150 else 40)
        } else {
            sound.play(Sfx.AGUA)
        }
        logger.logInfo(
            "Salva resuelta: tú ${mine.count { it.hit }}/${mine.size} impactos, " +
                "rival ${theirs.count { it.hit }}/${theirs.size}",
        )
        if (before.sum() != g.boards[0].sunkShips.size + g.boards[1].sunkShips.size) {
            logger.logInfo("Hundidos en la salva: tú ${mySunk.size}, rival ${rivalSunk.size}")
        }
        _ui.update {
            it.copy(
                busy = false,
                netStatus = null,
                salvoSummary = SalvoSummary(
                    myShots = mine.map { o -> o.coord to o.hit },
                    rivalShots = theirs.map { o -> o.coord to o.hit },
                    mySunk = mySunk,
                    rivalSunk = rivalSunk,
                ),
            )
        }
        refresh()
        if (g.finished) finish() else save()
    }

    /** Cierra el resumen de la salva y arranca la ronda siguiente. */
    fun dismissSalvoSummary() {
        val g = game ?: return
        _ui.update { it.copy(salvoSummary = null) }
        if (g.finished) return
        if (g.config.mode == GameMode.DOS_JUGADORES) {
            _ui.update { it.copy(phase = MatchPhase.TRASPASO, handoffPlayer = g.current) }
        }
        refresh()
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
        val result = when {
            winner == Game.DRAW -> MatchResult.EMPATADA
            winner == local -> MatchResult.GANADA
            else -> MatchResult.PERDIDA
        }
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
            val before = statsStore.stats.first().sunkByMe
            statsStore.record(summary)
            val after = before + summary.shipsSunkByMe
            val unlocked = Blueprints.all.filter { it.unlockAt in (before + 1)..after }
            if (unlocked.isNotEmpty()) {
                logger.logInfo("Planos desbloqueados: ${unlocked.joinToString { it.name }}")
            }
            _ui.update {
                it.copy(
                    phase = MatchPhase.RESULTADO,
                    winner = winner,
                    myShots = summary.myShots,
                    myHits = summary.myHits,
                    unlocked = unlocked,
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
                    is NetMessage.Salvo -> {
                        val g = game ?: return@collect
                        g.setAim(1 - transport.localPlayer, message.targets)
                        if (g.hasConfirmed(transport.localPlayer)) resolveSalvo()
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
            aim = if (g.config.shotRule == ShotRule.SALVA) g.aimOf(local) else emptyList(),
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
                shotsLeft = g.shotsLeft,
                round = g.round,
                salvoSize = if (g.config.shotRule == ShotRule.SALVA) g.salvoSize(local) else 0,
                aimCount = g.aimOf(local).size,
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
     * Agua deducida: en la flota Rusa, los barcos tienen que dejar una casilla de separación, así
     * que alrededor de uno hundido no puede haber nada. Se marca para no gastar disparos ahí.
     */
    private fun deducedWater(board: PlayerBoard): Set<Coord> {
        if (game?.config?.fleetDef?.mustSeparate != true) return emptySet()
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
        private const val HANDOFF_DELAY_MS = 1100L
        private const val HISTORY_LINES = 30
        const val WAITING_RIVAL = "waiting_rival"
        const val RIVAL_LEFT = "rival_left"
    }
}

/** Atajo: la flota de la configuración, para las pantallas. */
val GameConfig.fleetOrDefault: Fleet get() = fleetDef
