package com.devqube.drinktracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devqube.drinktracker.ui.theme.LocalDrinkColors

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalDrinkColors.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .leopardSpots()
            .border(1.dp, colors.border, RoundedCornerShape(22.dp))
            .padding(20.dp),
    ) { content() }
}

@Composable
fun Eyebrow(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = LocalDrinkColors.current
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            Text(valueText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = colors.surface2,
            ),
        )
    }
}

/**
 * Suwak ustawienia: podczas przeciągania zmienia tylko stan lokalny, a [onCommit] (zapis do
 * DataStore + odświeżenie widgetu) wywołuje raz, po puszczeniu. Wcześniej każdy ruch palcem
 * oznaczał osobny zapis i przerysowanie widgetu.
 */
@Composable
fun SettingSlider(
    label: String,
    value: Float,
    valueText: (Float) -> String,
    onCommit: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
) {
    var local by remember { mutableFloatStateOf(value) }
    var dragging by remember { mutableStateOf(false) }
    // Synchronizacja z zapisaną wartością, gdy zmieni się z zewnątrz (i nie trwa przeciąganie).
    LaunchedEffect(value) { if (!dragging) local = value }

    LabeledSlider(
        label = label,
        valueText = valueText(local),
        value = local,
        onValueChange = { dragging = true; local = it },
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = { dragging = false; onCommit(local) },
    )
}

/**
 * Cętki panterki w tle karty (motyw Grażyna). Pozycje są deterministyczne, więc wzór
 * nie „skacze” przy przerysowaniu. W innych motywach modyfikator nic nie robi.
 */
@Composable
fun Modifier.leopardSpots(): Modifier {
    if (!LocalDrinkColors.current.leopard) return this
    val gold = LocalDrinkColors.current.warn
    return this.drawBehind {
        val rnd = java.util.Random(42)
        val step = 46.dp.toPx()
        var y = 0f
        while (y < size.height + step) {
            var x = if ((y / step).toInt() % 2 == 0) 0f else step / 2
            while (x < size.width + step) {
                val cx = x + (rnd.nextFloat() - 0.5f) * step * 0.5f
                val cy = y + (rnd.nextFloat() - 0.5f) * step * 0.5f
                val r = step * (0.16f + rnd.nextFloat() * 0.08f)
                // Rozeta: jasne wnętrze i kilka ciemnych plamek dookoła.
                drawCircle(gold.copy(alpha = 0.06f), radius = r, center = Offset(cx, cy))
                repeat(5) { k ->
                    val a = k / 5f * 2f * Math.PI.toFloat() + rnd.nextFloat()
                    drawCircle(
                        Color.Black.copy(alpha = 0.18f),
                        radius = r * 0.32f,
                        center = Offset(cx + kotlin.math.cos(a) * r * 1.15f, cy + kotlin.math.sin(a) * r * 1.15f),
                    )
                }
                x += step
            }
            y += step
        }
    }
}
