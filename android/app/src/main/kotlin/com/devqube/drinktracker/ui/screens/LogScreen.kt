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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devqube.drinktracker.data.DrinkType
import com.devqube.drinktracker.data.fmt
import com.devqube.drinktracker.data.formatEntryTime
import com.devqube.drinktracker.data.toUnits
import com.devqube.drinktracker.ui.AppViewModel
import com.devqube.drinktracker.ui.components.Card
import com.devqube.drinktracker.ui.components.Eyebrow
import com.devqube.drinktracker.ui.components.LabeledSlider
import com.devqube.drinktracker.ui.theme.LocalDrinkColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val types by vm.drinkTypes.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val colors = LocalDrinkColors.current

    var selectedId by remember { mutableStateOf(types.firstOrNull()?.id) }
    var name by remember { mutableStateOf(types.firstOrNull()?.name ?: "Trunek") }
    var volume by remember { mutableStateOf((types.firstOrNull()?.volumeMl ?: 500).toFloat()) }
    var abv by remember { mutableStateOf((types.firstOrNull()?.abv ?: 5.0).toFloat()) }

    fun select(t: DrinkType) {
        selectedId = t.id
        name = t.name
        volume = t.volumeMl.toFloat()
        abv = t.abv.toFloat()
    }

    val units = toUnits(volume.toDouble(), abv.toDouble(), settings.gramsPerUnit)

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card {
            Text("Zaloguj trunek", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                types.forEach { t ->
                    DrinkChip(t, selected = t.id == selectedId, onClick = { select(t) })
                }
            }

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nazwa wpisu") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            LabeledSlider(
                label = "Objętość",
                valueText = "${volume.toInt()} ml",
                value = volume,
                onValueChange = { volume = it },
                valueRange = 25f..750f,
                steps = (750 - 25) / 25 - 1,
            )
            Spacer(Modifier.height(8.dp))
            LabeledSlider(
                label = "Zawartość alkoholu",
                valueText = "${fmt(abv.toDouble())}%",
                value = abv,
                onValueChange = { abv = it },
                valueRange = 1f..60f,
            )

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface2.copy(alpha = 0.7f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("Przelicznik")
                    Text("${fmt(units)} SU", fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            vm.addDrink(name.ifBlank { "Trunek" }, volume.toDouble(), abv.toDouble())
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text("Dodaj wpis", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Card {
            Eyebrow("Ostatnie wpisy")
            Spacer(Modifier.height(8.dp))
            if (entries.isEmpty()) {
                Text("Brak wpisów w tym tygodniu. Dodaj pierwszy trunek powyżej.", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
            } else {
                entries.take(20).forEach { e ->
                    val (day, time) = formatEntryTime(e.timestamp)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(e.name, style = MaterialTheme.typography.bodyLarge)
                            Text("$day, $time", style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                        }
                        Text("${fmt(e.units)} SU", color = colors.muted)
                        IconButton(onClick = { vm.deleteEntry(e.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Usuń", tint = colors.muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrinkChip(type: DrinkType, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalDrinkColors.current
    Column(
        Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else colors.surface2.copy(alpha = 0.6f))
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else colors.border,
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(type.emoji, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text(type.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2)
        Text(
            "${type.volumeMl} ml · ${fmt(type.abv)}%",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
    }
}
