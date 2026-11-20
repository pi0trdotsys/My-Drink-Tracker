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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devqube.drinktracker.data.DrinkType
import com.devqube.drinktracker.data.KropiSync
import com.devqube.drinktracker.data.combineDateTime
import com.devqube.drinktracker.data.fmt
import com.devqube.drinktracker.data.formatEntryTime
import com.devqube.drinktracker.data.formatWhenLabel
import com.devqube.drinktracker.data.toUnits
import com.devqube.drinktracker.ui.AppViewModel
import com.devqube.drinktracker.ui.components.Card
import com.devqube.drinktracker.ui.components.Eyebrow
import com.devqube.drinktracker.ui.components.LabeledSlider
import com.devqube.drinktracker.ui.theme.LocalDrinkColors
import com.devqube.drinktracker.ui.theme.LocalVoice

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val types by vm.drinkTypes.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val colors = LocalDrinkColors.current
    val context = LocalContext.current

    var selectedId by remember { mutableStateOf(types.firstOrNull()?.id) }
    var name by remember { mutableStateOf(types.firstOrNull()?.name ?: "Trunek") }
    var volume by remember { mutableStateOf((types.firstOrNull()?.volumeMl ?: 500).toFloat()) }
    var abv by remember { mutableStateOf((types.firstOrNull()?.abv ?: 5.0).toFloat()) }

    // Kiedy: null = teraz; wartość = wybrany moment w przeszłości (logowanie wstecz).
    var whenMillis by remember { mutableStateOf<Long?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var pickedDateUtc by remember { mutableStateOf<Long?>(null) }

    fun select(t: DrinkType) {
        selectedId = t.id
        name = t.name
        volume = t.volumeMl.toFloat()
        abv = t.abv.toFloat()
    }

    if (showDate) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = whenMillis ?: System.currentTimeMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis <= System.currentTimeMillis() + 24L * 3600 * 1000
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateUtc = dateState.selectedDateMillis
                    showDate = false
                    showTime = true
                }) { Text("Dalej") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Anuluj") } },
        ) { DatePicker(state = dateState) }
    }

    if (showTime) {
        val now = remember { java.util.Calendar.getInstance() }
        val timeState = rememberTimePickerState(
            initialHour = now.get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = now.get(java.util.Calendar.MINUTE),
            is24Hour = true,
        )
        DatePickerDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    val base = pickedDateUtc ?: System.currentTimeMillis()
                    whenMillis = combineDateTime(base, timeState.hour, timeState.minute)
                        .coerceAtMost(System.currentTimeMillis())
                    showTime = false
                }) { Text("Ustaw") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Anuluj") } },
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Godzina", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                TimePicker(state = timeState)
            }
        }
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
            Text(LocalVoice.current.logTitle, style = MaterialTheme.typography.headlineSmall)
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
            // "Kiedy" – logowanie wstecz: domyślnie teraz, można wybrać przeszłą datę/godzinę.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Kiedy", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                Row(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface2.copy(alpha = 0.6f))
                        .border(1.dp, if (whenMillis != null) MaterialTheme.colorScheme.primary else colors.border, RoundedCornerShape(14.dp))
                        .clickable { showDate = true }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        whenMillis?.let { formatWhenLabel(it) } ?: "Teraz",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (whenMillis != null) {
                    IconButton(onClick = { whenMillis = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Wyczyść", tint = colors.muted)
                    }
                }
            }

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
                            val entry = vm.addDrink(
                                name.ifBlank { "Trunek" },
                                volume.toDouble(),
                                abv.toDouble(),
                                timestamp = whenMillis ?: System.currentTimeMillis(),
                            )
                            whenMillis = null
                            // Ten sam trunek do Kropi (nawodnienie). Sukces potwierdza sama Kropi swoim
                            // komunikatem; tu informujemy tylko, gdy wpis nie mógł zostać przekazany.
                            if (settings.kropiSync) {
                                when (KropiSync.send(context, entry)) {
                                    KropiSync.Result.NeedsUpdateForBackdated -> Toast.makeText(
                                        context, "Kropi: zaktualizuj aplikację, by przekazywać wpisy wsteczne", Toast.LENGTH_LONG,
                                    ).show()
                                    KropiSync.Result.Failed -> Toast.makeText(
                                        context, "Nie udało się przekazać wpisu do Kropi", Toast.LENGTH_SHORT,
                                    ).show()
                                    else -> Unit
                                }
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(LocalVoice.current.addButton, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Card {
            Eyebrow(LocalVoice.current.recentTitle)
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
                        Text("${fmt(e.unitsFor(settings.gramsPerUnit))} SU", color = colors.muted)
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
