package com.devqube.drinktracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.devqube.drinktracker.data.AppTheme
import com.devqube.drinktracker.data.Voice
import com.devqube.drinktracker.data.Vulgarity
import com.devqube.drinktracker.data.voiceFor

/** Komplet kolorów motywu – używany przez aplikację i (jako ARGB) przez widget. */
data class Palette(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val foreground: Color,
    val muted: Color,
    val primary: Color,
    val onPrimary: Color,
    val ok: Color,
    val warn: Color,
    val over: Color,
    val border: Color,
    /** Dekoracyjne cętki panterki na kartach (motyw Grażyna). */
    val leopard: Boolean = false,
)

// Paleta odwzorowana z projektu TypeScript (oklch -> sRGB).
val ClassicPalette = Palette(
    background = Color(0xFF171922),
    surface = Color(0xFF23252F),
    surface2 = Color(0xFF2E313D),
    foreground = Color(0xFFF1F2F7),
    muted = Color(0xFFA8ABB8),
    primary = Color(0xFFE3A54B),
    onPrimary = Color(0xFF2A2010),
    ok = Color(0xFF45C08A),
    warn = Color(0xFFE6B646),
    over = Color(0xFFDE5140),
    border = Color(0x33FFFFFF),
)

// Grażyna: panterka (espresso + złoto), szminkowy róż, kremowy tekst.
val GrazynaPalette = Palette(
    background = Color(0xFF1C1311),
    surface = Color(0xFF2A1C17),
    surface2 = Color(0xFF3B2820),
    foreground = Color(0xFFFFF1E6),
    muted = Color(0xFFCBA68E),
    primary = Color(0xFFFF4FA3),
    onPrimary = Color(0xFF2B0A18),
    ok = Color(0xFF7BC96F),
    warn = Color(0xFFF2B33D),
    over = Color(0xFFFF3B3B),
    border = Color(0x40F2B33D),
    leopard = true,
)

fun paletteFor(theme: AppTheme): Palette = when (theme) {
    AppTheme.CLASSIC -> ClassicPalette
    AppTheme.GRAZYNA -> GrazynaPalette
}

/** Kolory semantyczne spoza schematu Material. */
data class DrinkColors(
    val surface2: Color = ClassicPalette.surface2,
    val muted: Color = ClassicPalette.muted,
    val ok: Color = ClassicPalette.ok,
    val warn: Color = ClassicPalette.warn,
    val over: Color = ClassicPalette.over,
    val border: Color = ClassicPalette.border,
    val leopard: Boolean = false,
)

val LocalDrinkColors = staticCompositionLocalOf { DrinkColors() }

/** Teksty aktywnego motywu (nagłówki, hasła, opisy promili). */
val LocalVoice = staticCompositionLocalOf<Voice> { voiceFor(AppTheme.CLASSIC) }

@Composable
fun DrinkTrackerTheme(
    theme: AppTheme = AppTheme.CLASSIC,
    vulgarity: Vulgarity = Vulgarity.MAX,
    content: @Composable () -> Unit,
) {
    // Aplikacja jest zaprojektowana jako ciemna, niezależnie od ustawień systemu.
    val p = paletteFor(theme)
    val scheme = darkColorScheme(
        primary = p.primary,
        onPrimary = p.onPrimary,
        background = p.background,
        onBackground = p.foreground,
        surface = p.surface,
        onSurface = p.foreground,
        surfaceVariant = p.surface2,
        onSurfaceVariant = p.muted,
        error = p.over,
        outline = p.border,
    )
    val colors = DrinkColors(p.surface2, p.muted, p.ok, p.warn, p.over, p.border, p.leopard)
    val voice = remember(theme, vulgarity) { voiceFor(theme, vulgarity) }
    CompositionLocalProvider(LocalDrinkColors provides colors, LocalVoice provides voice) {
        MaterialTheme(colorScheme = scheme, typography = DrinkTypography, content = content)
    }
}
