package com.devqube.drinktracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devqube.drinktracker.data.AppTheme
import com.devqube.drinktracker.data.DrinkType
import com.devqube.drinktracker.data.Sex
import com.devqube.drinktracker.data.fmt
import com.devqube.drinktracker.data.toUnits
import com.devqube.drinktracker.ui.AppViewModel
import com.devqube.drinktracker.ui.components.Card
import com.devqube.drinktracker.ui.components.Eyebrow
import com.devqube.drinktracker.ui.components.SettingSlider
import kotlin.math.roundToInt
import com.devqube.drinktracker.ui.theme.LocalDrinkColors

@Composable
fun SettingsScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val types by vm.drinkTypes.collectAsStateWithLifecycle()
    val colors = LocalDrinkColors.current
    var showAddType by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card {
            Text("Ustawienia limitów", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            SettingSlider(
                label = "Limit tygodniowy",
                value = settings.unitLimit.toFloat(),
                valueText = { "${it.roundToInt()} SU" },
                onCommit = { v -> vm.updateSettings { it.copy(unitLimit = v.roundToInt().toDouble()) } },
                valueRange = 0f..30f,
                steps = 29,
            )
            Spacer(Modifier.height(10.dp))
            SettingSlider(
                label = "Maks. dni z alkoholem w tygodniu",
                value = settings.dayLimit.toFloat(),
                valueText = { "${it.roundToInt()} dni" },
                onCommit = { v -> vm.updateSettings { it.copy(dayLimit = v.roundToInt()) } },
                valueRange = 0f..7f,
                steps = 6,
            )
            Spacer(Modifier.height(10.dp))
            SettingSlider(
                label = "Definicja jednostki (g czystego etanolu)",
                value = settings.gramsPerUnit.toFloat(),
                valueText = { "${it.roundToInt()} g" },
                onCommit = { v -> vm.updateSettings { it.copy(gramsPerUnit = v.roundToInt()) } },
                valueRange = 8f..14f,
                steps = 5,
            )
            Spacer(Modifier.height(10.dp))
            SettingSlider(
                label = "Próg ostrzeżenia",
                value = settings.warnRatioPercent.toFloat(),
                valueText = { p ->
                    val pct = (p / 5f).roundToInt() * 5
                    "$pct% (${fmt(settings.unitLimit * pct / 100)} SU)"
                },
                onCommit = { v -> vm.updateSettings { it.copy(warnRatioPercent = (v / 5f).roundToInt() * 5) } },
                valueRange = 50f..100f,
                steps = 9,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface2.copy(alpha = 0.7f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Powiadomienia (ostrzeżenia + podsumowanie tygodnia)", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { c -> vm.updateSettings { it.copy(notificationsEnabled = c) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface2.copy(alpha = 0.7f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Przypomnienia w ciągu dnia", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Południe, ok. 17:00 i wieczór – tylko gdy jest powód (np. wczoraj było picie, limit przekroczony).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
                Switch(
                    checked = settings.dailyNudges && settings.notificationsEnabled,
                    enabled = settings.notificationsEnabled,
                    onCheckedChange = { c -> vm.updateSettings { it.copy(dailyNudges = c) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                )
            }
        }

        ThemeCard(
            current = settings.theme,
            onPick = { theme ->
                // Wejście w Grażynę włącza też jej gderanie w ciągu dnia; powrót niczego nie wyłącza.
                vm.updateSettings { it.copy(theme = theme, dailyNudges = it.dailyNudges || theme == AppTheme.GRAZYNA) }
            },
        )

        // Sekcja BAC – masa ciała i płeć.
        Card {
            Text("Kalkulator poziomu alkoholu", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "Używane do szacowania promili we krwi (wzór Widmarka).",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
            Spacer(Modifier.height(14.dp))
            SettingSlider(
                label = "Masa ciała",
                value = settings.weightKg.toFloat(),
                valueText = { "${it.roundToInt()} kg" },
                onCommit = { v -> vm.updateSettings { it.copy(weightKg = v.roundToInt().toDouble()) } },
                valueRange = 40f..150f,
                steps = 21,
            )
            Spacer(Modifier.height(14.dp))
            Text("Płeć", style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Sex.entries.forEach { s ->
                    SegChip(
                        label = s.label,
                        selected = settings.sex == s,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.updateSettings { it.copy(sex = s) } },
                    )
                }
            }
        }

        Card {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Typy trunków")
                Text("+ Dodaj", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { showAddType = true })
            }
            Spacer(Modifier.height(8.dp))
            types.forEach { t ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(t.emoji, fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(t.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${t.volumeMl} ml · ${fmt(t.abv)}% · ${fmt(toUnits(t.volumeMl.toDouble(), t.abv, settings.gramsPerUnit))} SU",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                    if (t.custom) {
                        IconButton(onClick = { vm.deleteCustomType(t.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Usuń typ", tint = colors.muted)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    if (showAddType) {
        AddTypeDialog(
            onDismiss = { showAddType = false },
            onConfirm = { type ->
                vm.addCustomType(type)
                showAddType = false
            },
        )
    }
}

@Composable
private fun SegChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalDrinkColors.current
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else colors.surface2.copy(alpha = 0.6f))
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else colors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun AddTypeDialog(onDismiss: () -> Unit, onConfirm: (DrinkType) -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🥤") }
    var volume by remember { mutableStateOf("330") }
    var abv by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowy typ trunku") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nazwa") }, singleLine = true)
                OutlinedTextField(emoji, { emoji = it.take(2) }, label = { Text("Emoji") }, singleLine = true)
                OutlinedTextField(volume, { volume = it.filter(Char::isDigit) }, label = { Text("Objętość (ml)") }, singleLine = true)
                OutlinedTextField(abv, { abv = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Moc (%)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = volume.toIntOrNull() ?: 330
                val a = abv.toDoubleOrNull() ?: 5.0
                onConfirm(
                    DrinkType(
                        id = "custom_${System.currentTimeMillis()}",
                        name = name.ifBlank { "Własny trunek" },
                        emoji = emoji.ifBlank { "🥤" },
                        volumeMl = v,
                        abv = a,
                        custom = true,
                    )
                )
            }) { Text("Dodaj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

@Composable
private fun ThemeCard(current: AppTheme, onPick: (AppTheme) -> Unit) {
    val colors = LocalDrinkColors.current
    var confirmGrazyna by remember { mutableStateOf(false) }

    Card {
        Text("Motyw", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "Wygląd, teksty, dźwięki powiadomień i widget.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SegChip(
                label = AppTheme.CLASSIC.label,
                selected = current == AppTheme.CLASSIC,
                modifier = Modifier.weight(1f),
                onClick = { onPick(AppTheme.CLASSIC) },
            )
            SegChip(
                label = AppTheme.GRAZYNA.label,
                selected = current == AppTheme.GRAZYNA,
                modifier = Modifier.weight(1f),
                onClick = { if (current != AppTheme.GRAZYNA) confirmGrazyna = true },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "🔞 Grażyna (eksperymentalny): memiczna, wulgarna „wkurzona żona” – patelnia, kapeć i gderanie w powiadomieniach.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )
    }

    if (confirmGrazyna) {
        AlertDialog(
            onDismissRequest = { confirmGrazyna = false },
            title = { Text("Włączyć Grażynę? 🍳") },
            text = {
                Text(
                    "Eksperymentalny motyw 18+ z wulgaryzmami i memicznym humorem. Zmienia wygląd, teksty, " +
                        "dźwięki i widget oraz włącza przypomnienia w ciągu dnia. Możesz wrócić w każdej chwili."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmGrazyna = false
                    onPick(AppTheme.GRAZYNA)
                }) { Text("Dawaj, niech gdera") }
            },
            dismissButton = { TextButton(onClick = { confirmGrazyna = false }) { Text("Anuluj") } },
        )
    }
}
