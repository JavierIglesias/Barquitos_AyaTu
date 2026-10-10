package com.ayatu.barquitos.ui.common

import android.widget.TextView
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ayatu.barquitos.R
import com.ayatu.barquitos.core.Coord
import com.ayatu.barquitos.core.Orientation
import com.ayatu.barquitos.core.PlacedShip
import com.ayatu.barquitos.core.ShipDef
import com.ayatu.barquitos.data.FlagOverlay
import com.ayatu.barquitos.data.Nation
import com.ayatu.barquitos.data.Skin
import com.ayatu.barquitos.ui.common.ShipPainter.drawShipPreview
import com.ayatu.barquitos.ui.theme.Flota
import com.ayatu.barquitos.ui.theme.Mar
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Fondo de mar que comparten todas las pantallas. */
@Composable
fun SeaBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            ),
    ) { content() }
}

/** Andamio común: barra con título, botón de volver y el fondo de mar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarquitosScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back),
                            )
                        }
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        SeaBackdrop { content(Modifier.padding(padding)) }
    }
}

/** Botón grande del menú principal. */
@Composable
fun MenuButton(
    title: String,
    subtitle: String? = null,
    icon: @Composable (() -> Unit)? = null,
    highlight: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (highlight) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentAlpha(0.75f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalContentAlpha(alpha: Float): Color =
    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)

/** Tarjeta de opción con título, explicación y marca de selección. */
@Composable
fun OptionCard(
    title: String,
    description: String? = null,
    selected: Boolean,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
                if (description != null) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(10.dp))
                trailing()
            }
        }
    }
}

/** Dato suelto, para los paneles de estadísticas y de resultado. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Markdown (manual, novedades, historial) con Markwon dentro de Compose. Lleva la extensión de
 * tablas porque el manual las usa para la composición de las flotas y las reglas de disparo; sin
 * ella se verían como una ristra de barras verticales.
 */
@Composable
fun MarkdownView(text: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val markwon = remember {
        Markwon.builder(context).usePlugin(TablePlugin.create(context)).build()
    }
    val textColor = MaterialTheme.colorScheme.onBackground
    AndroidView(
        modifier = modifier,
        factory = { ctx -> TextView(ctx).apply { textSize = 15f } },
        update = { tv ->
            tv.setTextColor(textColor.toArgbCompat())
            markwon.setMarkdown(tv, text)
        },
    )
}

private fun Color.toArgbCompat(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(),
)

/** La bandera de una armada, dibujada con Canvas a partir de su descripción geométrica. */
@Composable
fun FlagView(nation: Nation, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stripes = nation.flag.stripes
        if (nation.flag.vertical) {
            val sw = w / stripes.size
            stripes.forEachIndexed { i, c ->
                drawRect(c, topLeft = Offset(i * sw, 0f), size = Size(sw, h))
            }
        } else {
            val sh = h / stripes.size
            stripes.forEachIndexed { i, c ->
                drawRect(c, topLeft = Offset(0f, i * sh), size = Size(w, sh))
            }
        }
        when (nation.flag.overlay) {
            FlagOverlay.NINGUNO, FlagOverlay.ASPA -> Unit

            FlagOverlay.CIRCULO -> drawCircle(
                color = nation.flag.overlayColor,
                radius = min(w, h) * 0.22f,
                center = Offset(w / 2, h / 2),
            )

            FlagOverlay.MEDIA_LUNA -> {
                val r = min(w, h) * 0.22f
                drawCircle(nation.flag.overlayColor, radius = r, center = Offset(w * 0.42f, h / 2))
                drawCircle(
                    color = nation.flag.stripes.first(),
                    radius = r * 0.82f,
                    center = Offset(w * 0.47f, h / 2),
                )
                // La estrella de cinco puntas, al lado de la luna.
                drawPath(starPath(Offset(w * 0.62f, h / 2), r * 0.55f), nation.flag.overlayColor)
            }

            FlagOverlay.UNION -> {
                val stroke = min(w, h) * 0.16f
                // Aspa diagonal blanca y roja, más la cruz recta encima.
                drawLine(nation.flag.overlayColor, Offset(0f, 0f), Offset(w, h), stroke)
                drawLine(nation.flag.overlayColor, Offset(w, 0f), Offset(0f, h), stroke)
                drawLine(nation.flag.overlayColor2, Offset(0f, 0f), Offset(w, h), stroke * 0.45f)
                drawLine(nation.flag.overlayColor2, Offset(w, 0f), Offset(0f, h), stroke * 0.45f)
                drawLine(nation.flag.overlayColor, Offset(w / 2, 0f), Offset(w / 2, h), stroke * 1.5f)
                drawLine(nation.flag.overlayColor, Offset(0f, h / 2), Offset(w, h / 2), stroke * 1.5f)
                drawLine(nation.flag.overlayColor2, Offset(w / 2, 0f), Offset(w / 2, h), stroke * 0.9f)
                drawLine(nation.flag.overlayColor2, Offset(0f, h / 2), Offset(w, h / 2), stroke * 0.9f)
            }

            FlagOverlay.ROMBO -> {
                val path = Path().apply {
                    moveTo(w / 2, h * 0.1f)
                    lineTo(w * 0.9f, h / 2)
                    lineTo(w / 2, h * 0.9f)
                    lineTo(w * 0.1f, h / 2)
                    close()
                }
                drawPath(path, nation.flag.overlayColor)
                drawCircle(nation.flag.overlayColor2, radius = min(w, h) * 0.17f, center = Offset(w / 2, h / 2))
            }

            FlagOverlay.CANTON -> {
                val cw = w * 0.42f
                val ch = h * 0.54f
                drawRect(nation.flag.overlayColor, size = Size(cw, ch))
                clipRect(0f, 0f, cw, ch) {
                    var y = ch * 0.16f
                    var row = 0
                    while (y < ch) {
                        var x = if (row % 2 == 0) cw * 0.12f else cw * 0.22f
                        while (x < cw) {
                            drawCircle(
                                color = nation.flag.overlayColor2,
                                radius = min(cw, ch) * 0.045f,
                                center = Offset(x, y),
                            )
                            x += cw * 0.2f
                        }
                        y += ch * 0.2f
                        row++
                    }
                }
            }
        }
        drawRect(
            color = Color.Black.copy(alpha = 0.25f),
            size = size,
            style = Stroke(width = min(w, h) * 0.04f),
        )
    }
}

private fun starPath(center: Offset, radius: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val angle = (-90 + i * 36) * Math.PI / 180
        val p = Offset(
            center.x + (r * cos(angle)).toFloat(),
            center.y + (r * sin(angle)).toFloat(),
        )
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

/** El emblema del jugador, también dibujado a mano. */
@Composable
fun EmblemView(emblemId: String, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = min(size.width, size.height)
        val c = Offset(size.width / 2, size.height / 2)
        val stroke = Stroke(width = s * 0.09f)
        when (emblemId) {
            "ancla" -> {
                drawCircle(color, radius = s * 0.1f, center = Offset(c.x, c.y - s * 0.3f), style = stroke)
                drawLine(color, Offset(c.x, c.y - s * 0.2f), Offset(c.x, c.y + s * 0.34f), s * 0.09f)
                drawLine(color, Offset(c.x - s * 0.2f, c.y - s * 0.06f), Offset(c.x + s * 0.2f, c.y - s * 0.06f), s * 0.08f)
                drawArc(
                    color = color,
                    startAngle = 20f, sweepAngle = 140f, useCenter = false,
                    topLeft = Offset(c.x - s * 0.3f, c.y - s * 0.05f),
                    size = Size(s * 0.6f, s * 0.5f),
                    style = stroke,
                )
            }
            "timon" -> {
                drawCircle(color, radius = s * 0.3f, center = c, style = stroke)
                drawCircle(color, radius = s * 0.1f, center = c)
                repeat(6) { i ->
                    val a = (i * 60) * Math.PI / 180
                    drawLine(
                        color,
                        Offset(c.x + (s * 0.14f * cos(a)).toFloat(), c.y + (s * 0.14f * sin(a)).toFloat()),
                        Offset(c.x + (s * 0.42f * cos(a)).toFloat(), c.y + (s * 0.42f * sin(a)).toFloat()),
                        s * 0.07f,
                    )
                }
            }
            "estrella" -> drawPath(starPath(c, s * 0.4f), color)
            "brujula" -> {
                drawCircle(color, radius = s * 0.36f, center = c, style = stroke)
                drawPath(
                    Path().apply {
                        moveTo(c.x, c.y - s * 0.3f)
                        lineTo(c.x + s * 0.12f, c.y)
                        lineTo(c.x, c.y + s * 0.3f)
                        lineTo(c.x - s * 0.12f, c.y)
                        close()
                    },
                    color,
                )
            }
            "ola" -> repeat(3) { i ->
                val y = c.y - s * 0.18f + i * s * 0.18f
                val path = Path().apply {
                    moveTo(c.x - s * 0.38f, y)
                    cubicTo(c.x - s * 0.18f, y - s * 0.14f, c.x - s * 0.02f, y + s * 0.14f, c.x + s * 0.14f, y)
                    cubicTo(c.x + s * 0.24f, y - s * 0.1f, c.x + s * 0.32f, y + s * 0.06f, c.x + s * 0.38f, y)
                }
                drawPath(path, color, style = stroke)
            }
            "tridente" -> {
                drawLine(color, Offset(c.x, c.y - s * 0.1f), Offset(c.x, c.y + s * 0.4f), s * 0.09f)
                listOf(-0.22f, 0f, 0.22f).forEach { dx ->
                    drawLine(
                        color,
                        Offset(c.x + s * dx, c.y - s * 0.38f),
                        Offset(c.x + s * dx, c.y - s * 0.05f),
                        s * 0.08f,
                    )
                }
                drawLine(color, Offset(c.x - s * 0.24f, c.y - s * 0.06f), Offset(c.x + s * 0.24f, c.y - s * 0.06f), s * 0.08f)
            }
            "rayo" -> drawPath(
                Path().apply {
                    moveTo(c.x + s * 0.12f, c.y - s * 0.4f)
                    lineTo(c.x - s * 0.22f, c.y + s * 0.06f)
                    lineTo(c.x + s * 0.02f, c.y + s * 0.06f)
                    lineTo(c.x - s * 0.1f, c.y + s * 0.4f)
                    lineTo(c.x + s * 0.26f, c.y - s * 0.08f)
                    lineTo(c.x + s * 0.02f, c.y - s * 0.08f)
                    close()
                },
                color,
            )
            "canon" -> {
                drawRect(color, topLeft = Offset(c.x - s * 0.36f, c.y - s * 0.08f), size = Size(s * 0.6f, s * 0.16f))
                drawCircle(color, radius = s * 0.16f, center = Offset(c.x - s * 0.3f, c.y + s * 0.14f))
                drawCircle(color, radius = s * 0.09f, center = Offset(c.x + s * 0.3f, c.y))
            }
            "calavera" -> {
                drawCircle(color, radius = s * 0.26f, center = Offset(c.x, c.y - s * 0.08f))
                drawRect(color, topLeft = Offset(c.x - s * 0.12f, c.y + s * 0.14f), size = Size(s * 0.24f, s * 0.16f))
                drawCircle(Color.Black.copy(alpha = 0.6f), radius = s * 0.07f, center = Offset(c.x - s * 0.1f, c.y - s * 0.1f))
                drawCircle(Color.Black.copy(alpha = 0.6f), radius = s * 0.07f, center = Offset(c.x + s * 0.1f, c.y - s * 0.1f))
            }
            "corona" -> drawPath(
                Path().apply {
                    moveTo(c.x - s * 0.36f, c.y + s * 0.22f)
                    lineTo(c.x - s * 0.28f, c.y - s * 0.26f)
                    lineTo(c.x - s * 0.1f, c.y + s * 0.02f)
                    lineTo(c.x, c.y - s * 0.32f)
                    lineTo(c.x + s * 0.1f, c.y + s * 0.02f)
                    lineTo(c.x + s * 0.28f, c.y - s * 0.26f)
                    lineTo(c.x + s * 0.36f, c.y + s * 0.22f)
                    close()
                },
                color,
            )
            "nudo" -> {
                drawCircle(color, radius = s * 0.2f, center = Offset(c.x - s * 0.12f, c.y), style = stroke)
                drawCircle(color, radius = s * 0.2f, center = Offset(c.x + s * 0.12f, c.y), style = stroke)
            }
            "kraken" -> {
                drawCircle(color, radius = s * 0.17f, center = Offset(c.x, c.y - s * 0.14f))
                repeat(5) { i ->
                    val a = (200 + i * 35) * Math.PI / 180
                    drawLine(
                        color,
                        Offset(c.x, c.y),
                        Offset(c.x + (s * 0.4f * cos(a)).toFloat(), c.y + (s * 0.4f * sin(a)).toFloat() + s * 0.2f),
                        s * 0.07f,
                    )
                }
            }
            else -> drawCircle(color, radius = s * 0.3f, center = c, style = stroke)
        }
    }
}

/** Dibuja un barco suelto, para la bandeja de colocación y el astillero. */
@Composable
fun ShipChip(
    def: ShipDef,
    skin: Skin,
    selected: Boolean,
    placed: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (placed) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surface,
            )
            .border(2.dp, border, RoundedCornerShape(12.dp))
            .clickable(enabled = !placed, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .graphicsLayer(alpha = if (placed) 0.3f else 1f),
        ) {
            val ship = PlacedShip(def, Coord(0, 0), Orientation.NORTE)
            drawShipPreview(ship, Rect(0f, 0f, size.width, size.height), skin)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "${def.size}",
            style = MaterialTheme.typography.labelSmall,
            color = if (placed) Flota.Victoria else MaterialTheme.colorScheme.primary,
        )
    }
}

/** Panel con el estado de una flota: un barco por línea, tachado si está hundido. */
@Composable
fun FleetStatus(
    title: String,
    entries: List<com.ayatu.barquitos.ui.match.FleetEntry>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(10.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        entries.forEach { entry ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(entry.kind.labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entry.sunk) Flota.Hundido else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (entry.sunk) {
                        androidx.compose.ui.text.style.TextDecoration.LineThrough
                    } else {
                        null
                    },
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(entry.size) { i ->
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    when {
                                        entry.sunk -> Flota.Hundido
                                        i < entry.hits -> Flota.Fuego
                                        else -> Mar.Espuma.copy(alpha = 0.35f)
                                    },
                                ),
                        )
                    }
                }
            }
        }
    }
}
