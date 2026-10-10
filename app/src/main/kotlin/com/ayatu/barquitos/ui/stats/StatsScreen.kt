package com.ayatu.barquitos.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayatu.barquitos.R
import com.ayatu.barquitos.core.Difficulty
import com.ayatu.barquitos.core.ShotRule
import com.ayatu.barquitos.data.Medals
import com.ayatu.barquitos.data.Record
import com.ayatu.barquitos.ui.AppViewModel
import com.ayatu.barquitos.ui.common.BarquitosScreen
import com.ayatu.barquitos.ui.common.StatTile
import com.ayatu.barquitos.ui.common.labelRes
import com.ayatu.barquitos.ui.theme.Flota

/** Estadísticas y medallas. */
@Composable
fun StatsScreen(appViewModel: AppViewModel, onBack: () -> Unit) {
    val stats by appViewModel.stats.collectAsStateWithLifecycle()
    var askReset by remember { mutableStateOf(false) }
    val earned = Medals.earned(stats).map { it.id }.toSet()

    BarquitosScreen(
        title = stringResource(R.string.stats_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = { askReset = true }) {
                Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.stats_reset))
            }
        },
    ) { modifier ->
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (stats.isEmpty) {
                Text(
                    stringResource(R.string.stats_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(stringResource(R.string.stats_played), "${stats.played}", Modifier.weight(1f))
                StatTile(stringResource(R.string.stats_won), "${stats.won}", Modifier.weight(1f))
                StatTile(stringResource(R.string.stats_lost), "${stats.lost}", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    stringResource(R.string.stats_winrate),
                    "${(stats.winRate * 100).toInt()} %",
                    Modifier.weight(1f),
                )
                StatTile(
                    stringResource(R.string.stats_accuracy),
                    "${(stats.accuracy * 100).toInt()} %",
                    Modifier.weight(1f),
                )
                StatTile(stringResource(R.string.stats_sunk), "${stats.sunkByMe}", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(stringResource(R.string.stats_shots), "${stats.shots}", Modifier.weight(1f))
                StatTile(stringResource(R.string.stats_hits), "${stats.hits}", Modifier.weight(1f))
                StatTile(
                    stringResource(R.string.stats_best),
                    if (stats.bestWinShots == 0) "—" else "${stats.bestWinShots}",
                    Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(stringResource(R.string.stats_streak), "${stats.currentStreak}", Modifier.weight(1f))
                StatTile(stringResource(R.string.stats_best_streak), "${stats.bestStreak}", Modifier.weight(1f))
            }

            SectionTitle(stringResource(R.string.stats_by_rule))
            ShotRule.entries.forEach { rule ->
                RecordRow(stringResource(rule.labelRes), stats.byRule[rule] ?: Record())
            }

            SectionTitle(stringResource(R.string.stats_by_difficulty))
            Difficulty.entries.forEach { difficulty ->
                RecordRow(stringResource(difficulty.labelRes), stats.byDifficulty[difficulty] ?: Record())
            }

            SectionTitle(stringResource(R.string.stats_medals))
            Medals.all.forEach { medal ->
                val won = medal.id in earned
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = if (won) 1f else 0.35f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = if (won) Flota.Oro else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.padding(4.dp))
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(medal.label, style = MaterialTheme.typography.titleSmall)
                        Text(
                            medal.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (askReset) {
        AlertDialog(
            onDismissRequest = { askReset = false },
            title = { Text(stringResource(R.string.stats_reset)) },
            text = { Text(stringResource(R.string.stats_reset_ask)) },
            confirmButton = {
                TextButton(onClick = {
                    askReset = false
                    appViewModel.resetStats()
                }) { Text(stringResource(R.string.common_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { askReset = false }) { Text(stringResource(R.string.common_no)) }
            },
        )
    }
}

@Composable
private fun RecordRow(label: String, record: Record) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${record.won}/${record.played}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { record.winRate },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
        )
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
