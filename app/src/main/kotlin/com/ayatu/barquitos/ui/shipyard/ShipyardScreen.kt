package com.ayatu.barquitos.ui.shipyard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayatu.barquitos.R
import com.ayatu.barquitos.core.Coord
import com.ayatu.barquitos.core.Fleet
import com.ayatu.barquitos.core.Orientation
import com.ayatu.barquitos.core.PlacedShip
import com.ayatu.barquitos.data.Blueprints
import com.ayatu.barquitos.data.Emblems
import com.ayatu.barquitos.data.Nations
import com.ayatu.barquitos.data.Skins
import com.ayatu.barquitos.ui.AppViewModel
import com.ayatu.barquitos.ui.common.BarquitosScreen
import com.ayatu.barquitos.ui.common.EmblemView
import com.ayatu.barquitos.ui.common.FlagView
import com.ayatu.barquitos.ui.common.ShipPainter.drawShipPreview
import com.ayatu.barquitos.ui.theme.Flota
import com.ayatu.barquitos.ui.theme.Mar

/**
 * El Astillero: armada, pintura del casco, emblema, nombre y los planos de buques históricos que
 * se van desbloqueando al hundir barcos.
 */
@Composable
fun ShipyardScreen(appViewModel: AppViewModel, onBack: () -> Unit) {
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val stats by appViewModel.stats.collectAsStateWithLifecycle()
    var name by remember(settings.playerName) { mutableStateOf(settings.playerName) }

    val nation = Nations.byId(settings.nationId)
    val skin = Skins.byId(settings.skinId)
    val unlockedSkins = Skins.unlocked(stats.sunkByMe).map { it.id }.toSet()
    val unlockedEmblems = Emblems.unlocked(stats.won).map { it.id }.toSet()

    BarquitosScreen(title = stringResource(R.string.shipyard_title), onBack = onBack) { modifier ->
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Vista previa: el portaaviones con la pintura y el emblema elegidos.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Mar.Medio),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val ship = PlacedShip(Fleet.ESTANDAR.ships[0], Coord(0, 0), Orientation.NORTE)
                    drawShipPreview(
                        ship,
                        Rect(size.width * 0.1f, size.height * 0.1f, size.width * 0.9f, size.height * 0.9f),
                        skin,
                    )
                }
                Row(
                    Modifier.align(Alignment.TopEnd).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlagView(nation, Modifier.size(width = 34.dp, height = 22.dp).clip(RoundedCornerShape(3.dp)))
                    Spacer(Modifier.width(6.dp))
                    EmblemView(settings.emblemId, Flota.Oro, Modifier.size(24.dp))
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(14)
                    appViewModel.setPlayerName(name)
                },
                label = { Text(stringResource(R.string.shipyard_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(stringResource(R.string.shipyard_nation))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Nations.all, key = { it.id }) { option ->
                    Column(
                        Modifier
                            .width(76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                2.dp,
                                if (option.id == settings.nationId) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { appViewModel.setNation(option.id) }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        FlagView(option, Modifier.size(width = 56.dp, height = 36.dp).clip(RoundedCornerShape(3.dp)))
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }

            SectionTitle(
                stringResource(R.string.shipyard_skin) +
                    "  (${unlockedSkins.size}/${Skins.all.size})",
            )
            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(170.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Skins.all, key = { it.id }) { option ->
                    val unlocked = option.id in unlockedSkins
                    Column(
                        Modifier
                            .width(104.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                2.dp,
                                if (option.id == settings.skinId) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable(enabled = unlocked) { appViewModel.setSkin(option.id) }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize()) {
                                val ship = PlacedShip(Fleet.CLASICA.ships[1], Coord(0, 0), Orientation.NORTE)
                                drawShipPreview(
                                    ship,
                                    Rect(0f, 0f, size.width, size.height),
                                    option,
                                )
                            }
                            if (!unlocked) {
                                androidx.compose.material3.Icon(
                                    Icons.Default.Lock,
                                    contentDescription = stringResource(R.string.shipyard_locked),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            color = if (unlocked) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        if (!unlocked) {
                            Text(
                                stringResource(R.string.shipyard_unlock_at, option.unlockAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.shipyard_emblem))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Emblems.all, key = { it.id }) { option ->
                    val unlocked = option.id in unlockedEmblems
                    Column(
                        Modifier
                            .width(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                2.dp,
                                if (option.id == settings.emblemId) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable(enabled = unlocked) { appViewModel.setEmblem(option.id) }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        EmblemView(
                            option.id,
                            if (unlocked) Flota.Oro else MaterialTheme.colorScheme.outline,
                            Modifier.size(40.dp),
                        )
                        Text(option.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        if (!unlocked) {
                            Text(
                                "${option.unlockAtWins} ${stringResource(R.string.stats_won).lowercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.shipyard_blueprints))
            Text(
                stringResource(R.string.shipyard_blueprints_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.shipyard_sunk_total, stats.sunkByMe),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Blueprints.all.forEach { blueprint ->
                val unlocked = blueprint.unlockAt <= stats.sunkByMe
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = if (unlocked) 1f else 0.35f),
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlagView(
                        Nations.byId(blueprint.nationId),
                        Modifier.size(width = 32.dp, height = 21.dp).clip(RoundedCornerShape(2.dp)),
                    )
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(
                            if (unlocked) "${blueprint.name} · ${blueprint.year}" else "? ? ?",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            if (unlocked) {
                                blueprint.note
                            } else {
                                stringResource(R.string.shipyard_unlock_at, blueprint.unlockAt)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!unlocked) {
                        androidx.compose.material3.Icon(
                            Icons.Default.Lock,
                            contentDescription = stringResource(R.string.shipyard_locked),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
    )
}
