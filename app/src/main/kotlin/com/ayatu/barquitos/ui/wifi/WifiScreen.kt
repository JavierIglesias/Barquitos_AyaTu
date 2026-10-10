package com.ayatu.barquitos.ui.wifi

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayatu.barquitos.BarquitosApplication
import com.ayatu.barquitos.R
import com.ayatu.barquitos.core.GameConfig
import com.ayatu.barquitos.core.GameMode
import com.ayatu.barquitos.net.LanBrowser
import com.ayatu.barquitos.net.LanGameFound
import com.ayatu.barquitos.net.LanHost
import com.ayatu.barquitos.net.LanLink
import com.ayatu.barquitos.ui.common.BarquitosScreen
import com.ayatu.barquitos.ui.common.MenuButton
import com.ayatu.barquitos.ui.common.OptionCard
import com.ayatu.barquitos.ui.setup.SetupScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * La conexión que se acaba de establecer, para que la pantalla de partida la coja. Se guarda aquí
 * y no se pasa por la navegación porque un socket abierto no cabe en los argumentos de una ruta.
 */
object LanSession {
    var link: LanLink? = null
    var config: GameConfig? = null

    fun take(): Pair<LanLink, GameConfig>? {
        val l = link ?: return null
        val c = config ?: return null
        return l to c
    }

    fun clear() {
        link = null
        config = null
    }
}

/** En qué paso del WiFi local estamos. */
private enum class WifiStep { ELEGIR, REGLAS, ESPERANDO, BUSCANDO, CONECTANDO }

class WifiViewModel(application: Application) : AndroidViewModel(application) {

    private val logger = (application as BarquitosApplication).logger
    private val browser = LanBrowser(application)
    private var host: LanHost? = null

    val found: StateFlow<List<LanGameFound>> = browser.found

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun startHosting(playerName: String, config: GameConfig) {
        cancel()
        val newHost = LanHost(getApplication(), playerName, config)
        host = newHost
        logger.logInfo("WiFi local: publicando partida (${config.summary()})")
        viewModelScope.launch {
            runCatching { newHost.await() }
                .onSuccess { link ->
                    LanSession.link = link
                    LanSession.config = config
                    logger.logInfo("WiFi local: se ha unido un rival")
                    _connected.value = true
                }
                .onFailure {
                    logger.logError("WiFi local: fallo al publicar la partida: ${it.message}")
                    _error.value = it.message
                }
        }
    }

    fun startBrowsing() {
        cancel()
        logger.logInfo("WiFi local: buscando partidas en la red")
        browser.start()
    }

    fun join(game: LanGameFound) {
        viewModelScope.launch {
            runCatching { browser.connect(game) }
                .onSuccess { (link, config) ->
                    LanSession.link = link
                    LanSession.config = config
                    logger.logInfo("WiFi local: conectado a ${game.label} (${config.summary()})")
                    _connected.value = true
                }
                .onFailure {
                    logger.logError("WiFi local: no se pudo conectar: ${it.message}")
                    _error.value = it.message
                }
        }
    }

    fun cancel() {
        host?.cancel()
        host = null
        browser.stop()
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        cancel()
    }
}

@Composable
fun WifiScreen(
    playerName: String,
    lastConfig: GameConfig,
    onBack: () -> Unit,
    onConnected: () -> Unit,
    viewModel: WifiViewModel = viewModel(),
) {
    var step by remember { mutableStateOf(WifiStep.ELEGIR) }
    val found by viewModel.found.collectAsState()
    val connected by viewModel.connected.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(connected) { if (connected) onConnected() }
    DisposableEffect(Unit) { onDispose { viewModel.cancel() } }

    if (step == WifiStep.REGLAS) {
        SetupScreen(
            mode = GameMode.WIFI_LOCAL,
            initial = lastConfig.copy(mode = GameMode.WIFI_LOCAL),
            player1 = playerName,
            player2 = "",
            onNames = { _, _ -> },
            onBack = { step = WifiStep.ELEGIR },
            onStart = { config ->
                viewModel.startHosting(playerName, config)
                step = WifiStep.ESPERANDO
            },
        )
        return
    }

    BarquitosScreen(
        title = stringResource(R.string.wifi_title),
        onBack = {
            viewModel.cancel()
            if (step == WifiStep.ELEGIR) onBack() else step = WifiStep.ELEGIR
        },
    ) { modifier ->
        Column(
            modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.wifi_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (step) {
                WifiStep.ELEGIR -> {
                    MenuButton(
                        title = stringResource(R.string.wifi_host),
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = { step = WifiStep.REGLAS },
                    )
                    MenuButton(
                        title = stringResource(R.string.wifi_join),
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        onClick = {
                            viewModel.startBrowsing()
                            step = WifiStep.BUSCANDO
                        },
                    )
                }

                WifiStep.ESPERANDO -> Waiting(stringResource(R.string.wifi_waiting))

                WifiStep.BUSCANDO -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(Modifier.height(28.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.wifi_searching),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (found.isEmpty()) {
                        Text(
                            stringResource(R.string.wifi_none),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    found.forEach { game ->
                        OptionCard(
                            title = game.label,
                            selected = false,
                            trailing = { Icon(Icons.Default.Wifi, contentDescription = null) },
                            onClick = {
                                viewModel.join(game)
                                step = WifiStep.CONECTANDO
                            },
                        )
                    }
                }

                WifiStep.CONECTANDO -> Waiting(stringResource(R.string.wifi_connecting))

                WifiStep.REGLAS -> Unit
            }

            if (error != null) {
                Text(
                    error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = {
                    viewModel.cancel()
                    step = WifiStep.ELEGIR
                }) { Text(stringResource(R.string.common_close)) }
            }
        }
    }
}

@Composable
private fun Waiting(message: String) {
    Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}
