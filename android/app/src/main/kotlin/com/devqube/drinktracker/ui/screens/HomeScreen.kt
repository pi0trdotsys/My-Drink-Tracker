package com.devqube.drinktracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devqube.drinktracker.data.AppSettings
import com.devqube.drinktracker.data.BacEstimate
import com.devqube.drinktracker.data.DrinkEntry
import com.devqube.drinktracker.data.Fancy
import com.devqube.drinktracker.data.Status
import com.devqube.drinktracker.data.buildWeek
import com.devqube.drinktracker.data.currentBac
import com.devqube.drinktracker.data.fmt
import com.devqube.drinktracker.data.statusDetails
import com.devqube.drinktracker.ui.AppViewModel
import com.devqube.drinktracker.ui.components.AlcoholWidgetCard
import com.devqube.drinktracker.ui.components.Card
import com.devqube.drinktracker.ui.components.Eyebrow
import com.devqube.drinktracker.ui.components.WidgetSize
import com.devqube.drinktracker.ui.theme.LocalDrinkColors

@Composable
fun HomeScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()

    val week = buildWeek(entries, settings)
    val details = statusDetails(week)
    val bac = currentBac(entries, settings)
    val streak = Fancy.dryStreak(entries)
    val colors = LocalDrinkColors.current

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Nagłówek z osobowością.
        Column {
            Eyebrow("Twój tydzień")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Spożycie alkoholu",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(Fancy.emblem(details.status), fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    Fancy.tagline(details.status, streak),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }

        AlcoholWidgetCard(week = week, size = WidgetSize.S4x2)

        BacCard(bac, settings)

        StatRow(details.total, week.unitLimit, details.drinkingDays, week.dayLimit, streak)

        // Podgląd widgetu w mniejszych rozmiarach (skalowalny do 2x1).
        Column {
            Eyebrow("Podgląd widgetu")
            Spacer(Modifier.height(10.dp))
            AlcoholWidgetCard(week = week, size = WidgetSize.S2x2)
            Spacer(Modifier.height(12.dp))
            AlcoholWidgetCard(week = week, size = WidgetSize.S2x1)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BacCard(bac: BacEstimate, settings: AppSettings) {
    val colors = LocalDrinkColors.current
    val accent = when {
        bac.perMille <= 0.05 -> colors.ok
        bac.perMille < 0.5 -> colors.warn
        else -> colors.over
    }
    Card {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Eyebrow("Szacowany poziom alkoholu")
            Text("${settings.sex.label}, ${settings.weightKg.toInt()} kg", style = MaterialTheme.typography.labelMedium, color = colors.muted)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                fmt(bac.perMille),
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = accent,
            )
            Text(" ‰", fontSize = 18.sp, color = colors.muted, modifier = Modifier.padding(bottom = 6.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.padding(bottom = 6.dp)) {
                Text(bac.level.label, style = MaterialTheme.typography.titleMedium, color = accent)
                Text(bac.level.note, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            }
        }
        if (bac.perMille > 0.05) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Trzeźwość ok. za ${fmt(bac.hoursToSober)} h (wzór Widmarka, wartość orientacyjna).",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun StatRow(total: Double, unitLimit: Double, days: Int, dayLimit: Int, streak: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile("${fmt(total)}", "/ ${unitLimit.toInt()} SU", Modifier.weight(1f))
        StatTile("$days", "/ $dayLimit dni", Modifier.weight(1f))
        StatTile("${Fancy.streakBadge(streak)} $streak", "trzeźwych dni", Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = LocalDrinkColors.current
    Card(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
    }
}
