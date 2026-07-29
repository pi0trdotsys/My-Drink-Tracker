package com.devqube.drinktracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Paleta odwzorowana z projektu TypeScript (oklch -> sRGB).
val Background = Color(0xFF171922)
val Surface = Color(0xFF23252F)
val Surface2 = Color(0xFF2E313D)
val Foreground = Color(0xFFF1F2F7)
val MutedForeground = Color(0xFFA8ABB8)
val Primary = Color(0xFFE3A54B)
val PrimaryForeground = Color(0xFF2A2010)
val OkColor = Color(0xFF45C08A)
val WarnColor = Color(0xFFE6B646)
val OverColor = Color(0xFFDE5140)
val BorderColor = Color(0x33FFFFFF)

/** Kolory semantyczne spoza schematu Material. */
data class DrinkColors(
    val surface2: Color = Surface2,
    val muted: Color = MutedForeground,
    val ok: Color = OkColor,
    val warn: Color = WarnColor,
    val over: Color = OverColor,
    val border: Color = BorderColor,
)

val LocalDrinkColors = staticCompositionLocalOf { DrinkColors() }

private val DarkScheme = darkColorScheme(
    primary = Primary,
    onPrimary = PrimaryForeground,
    background = Background,
    onBackground = Foreground,
    surface = Surface,
    onSurface = Foreground,
    surfaceVariant = Surface2,
    onSurfaceVariant = MutedForeground,
    error = OverColor,
    outline = BorderColor,
)

@Composable
fun DrinkTrackerTheme(
    // Aplikacja jest zaprojektowana jako ciemna, niezależnie od ustawień systemu.
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDrinkColors provides DrinkColors()) {
        MaterialTheme(
            colorScheme = DarkScheme,
            typography = DrinkTypography,
            content = content,
        )
    }
}
