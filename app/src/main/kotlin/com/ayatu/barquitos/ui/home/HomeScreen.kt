package com.ayatu.barquitos.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayatu.barquitos.R
import com.ayatu.barquitos.core.Coord
import com.ayatu.barquitos.core.Fleet
import com.ayatu.barquitos.core.Orientation
import com.ayatu.barquitos.core.PlacedShip
import com.ayatu.barquitos.ui.AppViewModel
import com.ayatu.barquitos.ui.common.MenuButton
import com.ayatu.barquitos.ui.common.SeaBackdrop
import com.ayatu.barquitos.ui.common.ShipPainter.drawShipPreview
import com.ayatu.barquitos.ui.theme.Flota
import com.ayatu.barquitos.ui.theme.Mar

/** Pantalla principal: el puerto desde el que se sale a jugar. */
@Composable
fun HomeScreen(
    appViewModel: AppViewModel,
    onSinglePlayer: () -> Unit,
    onWifi: () -> Unit,
    onResume: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onManual: () -> Unit,
    onNews: () -> Unit,
    onLog: () -> Unit,
    onAbout: () -> Unit,
) {
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val stats by appViewModel.stats.collectAsStateWithLifecycle()
    val hasSaved by appViewModel.hasSavedMatch.collectAsStateWithLifecycle()

    SeaBackdrop {
        Column(
            Modifier
                .fillMaxSize()
                // La app va a pantalla completa, así que hay que dejar sitio a la barra de estado
                // y a la de navegación a mano.
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Un barco grande navegando, para que el puerto no esté vacío.
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Mar.Medio),
            ) {
                val ship = PlacedShip(Fleet.ships[0], Coord(0, 0), Orientation.HORIZONTAL)
                drawShipPreview(
                    ship,
                    Rect(size.width * 0.08f, size.height * 0.12f, size.width * 0.92f, size.height * 0.8f),
                )
                // Línea de flotación.
                val wave = Path().apply {
                    moveTo(0f, size.height * 0.82f)
                    var x = 0f
                    var up = true
                    while (x < size.width) {
                        val next = x + size.width / 14f
                        quadraticBezierTo(
                            (x + next) / 2f,
                            size.height * (if (up) 0.74f else 0.9f),
                            next,
                            size.height * 0.82f,
                        )
                        x = next
                        up = !up
                    }
                }
                drawPath(
                    wave,
                    Mar.Espuma.copy(alpha = 0.5f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.height * 0.05f),
                )
                drawRect(
                    Mar.Profundo.copy(alpha = 0.75f),
                    topLeft = Offset(0f, size.height * 0.82f),
                    size = Size(size.width, size.height * 0.18f),
                )
            }

            Spacer(Modifier.height(4.dp))

            if (hasSaved) {
                MenuButton(
                    title = stringResource(R.string.home_resume),
                    subtitle = stringResource(R.string.home_resume_desc),
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    highlight = true,
                    onClick = onResume,
                )
            }
            MenuButton(
                title = stringResource(R.string.home_single),
                subtitle = stringResource(R.string.home_single_desc),
                icon = { Icon(Icons.Default.Anchor, contentDescription = null) },
                onClick = onSinglePlayer,
            )
            MenuButton(
                title = stringResource(R.string.home_wifi),
                subtitle = stringResource(R.string.home_wifi_desc),
                icon = { Icon(Icons.Default.Wifi, contentDescription = null) },
                onClick = onWifi,
            )

            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallTile(
                    label = stringResource(R.string.home_stats),
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    onClick = onStats,
                )
                SmallTile(
                    label = stringResource(R.string.home_settings),
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    onClick = onSettings,
                )
            }

            if (!stats.isEmpty) {
                Text(
                    "${stringResource(R.string.stats_played)}: ${stats.played}  ·  " +
                        "${stringResource(R.string.stats_won)}: ${stats.won}  ·  " +
                        "${stringResource(R.string.stats_sunk)}: ${stats.sunkByMe}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(2.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onManual) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.home_manual))
                }
                TextButton(onClick = onNews) {
                    Icon(Icons.Default.NewReleases, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.home_news))
                }
                TextButton(onClick = onLog) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.home_log))
                }
            }

            TextButton(onClick = onAbout) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.home_about))
            }
        }
    }
}

@Composable
private fun SmallTile(
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon()
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
