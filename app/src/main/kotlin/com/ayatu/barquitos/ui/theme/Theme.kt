package com.ayatu.barquitos.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Paleta de Barquitos AyaTu. Todo el juego se mira desde la mesa de mapas de un buque: azules de
 * mar, gris de casco, naranja de fuego y oro de galones.
 */
object Mar {
    val Profundo = Color(0xFF071A2B)
    val Medio = Color(0xFF0E2E46)
    val Claro = Color(0xFF14456A)
    val Espuma = Color(0xFF7FC4E8)
    val Niebla = Color(0xFFD7E6F2)
}

object Flota {
    val Casco = Color(0xFF8D99A6)
    val CascoOscuro = Color(0xFF5A6673)
    val Cubierta = Color(0xFFBCC7D1)
    val Oro = Color(0xFFF2B33D)
    val Fuego = Color(0xFFE8562A)
    val Humo = Color(0xFF3A3F47)
    val Hundido = Color(0xFF8E1C0F)
    val Agua = Color(0xFF4E7FA3)
    val Victoria = Color(0xFF4CAF7D)
    val Derrota = Color(0xFFD9534F)
}

private val DarkScheme = darkColorScheme(
    primary = Flota.Oro,
    onPrimary = Mar.Profundo,
    primaryContainer = Mar.Claro,
    onPrimaryContainer = Mar.Niebla,
    secondary = Mar.Espuma,
    onSecondary = Mar.Profundo,
    secondaryContainer = Mar.Medio,
    onSecondaryContainer = Mar.Niebla,
    tertiary = Flota.Fuego,
    onTertiary = Color.White,
    background = Mar.Profundo,
    onBackground = Mar.Niebla,
    surface = Mar.Medio,
    onSurface = Mar.Niebla,
    surfaceVariant = Mar.Claro,
    onSurfaceVariant = Mar.Niebla,
    outline = Flota.CascoOscuro,
    error = Flota.Derrota,
)

private val LightScheme = lightColorScheme(
    primary = Mar.Claro,
    onPrimary = Color.White,
    primaryContainer = Mar.Niebla,
    onPrimaryContainer = Mar.Profundo,
    secondary = Mar.Medio,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4EFF7),
    onSecondaryContainer = Mar.Profundo,
    tertiary = Flota.Fuego,
    onTertiary = Color.White,
    background = Color(0xFFF4F8FB),
    onBackground = Mar.Profundo,
    surface = Color.White,
    onSurface = Mar.Profundo,
    surfaceVariant = Color(0xFFDFEAF3),
    onSurfaceVariant = Mar.Medio,
    outline = Flota.Casco,
    error = Flota.Derrota,
)

private val BarquitosTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        letterSpacing = 1.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

/** Cómo quiere el usuario ver la app. */
enum class ThemeChoice { SISTEMA, CLARO, OSCURO }

@Composable
fun BarquitosTheme(
    choice: ThemeChoice = ThemeChoice.SISTEMA,
    content: @Composable () -> Unit,
) {
    val dark = when (choice) {
        ThemeChoice.SISTEMA -> isSystemInDarkTheme()
        ThemeChoice.CLARO -> false
        ThemeChoice.OSCURO -> true
    }
    val scheme = if (dark) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        // Solo se ajusta el color de los iconos de la barra de estado: el fondo lo pintan las
        // propias pantallas (la app va a pantalla completa con `enableEdgeToEdge`), porque desde
        // Android 15 los colores de las barras ya no se pueden fijar a mano.
        SideEffect {
            (context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = scheme, typography = BarquitosTypography, content = content)
}
