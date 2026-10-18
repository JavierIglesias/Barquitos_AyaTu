package com.ayatu.barquitos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ayatu.barquitos.core.GameMode
import com.ayatu.barquitos.ui.AppViewModel
import com.ayatu.barquitos.ui.about.AboutScreen
import com.ayatu.barquitos.ui.docs.DocKind
import com.ayatu.barquitos.ui.docs.DocsScreen
import com.ayatu.barquitos.ui.docs.LogScreen
import com.ayatu.barquitos.ui.home.HomeScreen
import com.ayatu.barquitos.ui.match.MatchScreen
import com.ayatu.barquitos.ui.match.MatchViewModel
import com.ayatu.barquitos.ui.settings.SettingsScreen
import com.ayatu.barquitos.ui.setup.SetupScreen
import com.ayatu.barquitos.ui.stats.StatsScreen
import com.ayatu.barquitos.ui.theme.BarquitosTheme
import com.ayatu.barquitos.ui.wifi.LanSession
import com.ayatu.barquitos.ui.wifi.WifiScreen
import androidx.compose.ui.res.stringResource

/** Única actividad de la app: dentro va toda la navegación en Compose. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BarquitosApp() }
    }
}

private object Routes {
    const val HOME = "home"
    const val SETUP = "setup/{mode}"
    const val MATCH = "match"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val WIFI = "wifi"
    const val LOG = "log"
    const val ABOUT = "about"
    const val DOCS = "docs/{kind}"

    fun setup(mode: GameMode) = "setup/${mode.name}"
    fun docs(kind: DocKind) = "docs/${kind.name}"
}

@Composable
private fun BarquitosApp() {
    // Los dos ViewModel se crean aquí, al nivel de la actividad, para que la partida y los
    // ajustes sobrevivan a los cambios de pantalla.
    val appViewModel: AppViewModel = viewModel()
    val matchViewModel: MatchViewModel = viewModel()
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    BarquitosTheme(choice = settings.theme) {
        NavHost(navController = navController, startDestination = Routes.HOME) {

            composable(Routes.HOME) {
                HomeScreen(
                    appViewModel = appViewModel,
                    onSinglePlayer = { navController.navigate(Routes.setup(GameMode.UN_JUGADOR)) },
                    onWifi = { navController.navigate(Routes.WIFI) },
                    onResume = {
                        matchViewModel.resumeSaved(onNothing = { appViewModel.refreshSaved() })
                        navController.navigate(Routes.MATCH)
                    },
                    onStats = { navController.navigate(Routes.STATS) },
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                    onManual = { navController.navigate(Routes.docs(DocKind.MANUAL)) },
                    onNews = { navController.navigate(Routes.docs(DocKind.NOVEDADES)) },
                    onLog = { navController.navigate(Routes.LOG) },
                    onAbout = { navController.navigate(Routes.ABOUT) },
                )
            }

            composable(
                Routes.SETUP,
                arguments = listOf(navArgument("mode") { type = NavType.StringType }),
            ) { entry ->
                val mode = runCatching {
                    GameMode.valueOf(entry.arguments?.getString("mode") ?: "")
                }.getOrDefault(GameMode.UN_JUGADOR)
                SetupScreen(
                    mode = mode,
                    initial = settings.lastConfig.copy(mode = mode),
                    playerName = settings.playerName,
                    onName = appViewModel::setPlayerName,
                    onBack = { navController.popBackStack() },
                    onStart = { config ->
                        matchViewModel.startNew(config)
                        navController.navigate(Routes.MATCH) {
                            popUpTo(Routes.HOME)
                        }
                    },
                )
            }

            composable(Routes.MATCH) {
                MatchScreen(
                    viewModel = matchViewModel,
                    settings = settings,
                    onExit = {
                        appViewModel.refreshSaved()
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    },
                )
            }

            composable(Routes.WIFI) {
                WifiScreen(
                    playerName = settings.playerName,
                    lastConfig = settings.lastConfig,
                    onBack = { navController.popBackStack() },
                    onConnected = {
                        LanSession.take()?.let { (link, config) ->
                            matchViewModel.startNew(config, link)
                            LanSession.clear()
                            navController.navigate(Routes.MATCH) { popUpTo(Routes.HOME) }
                        }
                    },
                )
            }

            composable(Routes.STATS) {
                StatsScreen(appViewModel = appViewModel, onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(appViewModel = appViewModel, onBack = { navController.popBackStack() })
            }

            composable(Routes.LOG) {
                LogScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }

            composable(
                Routes.DOCS,
                arguments = listOf(navArgument("kind") { type = NavType.StringType }),
            ) { entry ->
                val kind = runCatching {
                    DocKind.valueOf(entry.arguments?.getString("kind") ?: "")
                }.getOrDefault(DocKind.MANUAL)
                val title = when (kind) {
                    DocKind.MANUAL -> stringResource(R.string.docs_manual)
                    DocKind.NOVEDADES -> stringResource(R.string.docs_news)
                    DocKind.HISTORIAL -> stringResource(R.string.home_log)
                }
                DocsScreen(kind = kind, title = title, onBack = { navController.popBackStack() })
            }
        }
    }
}
