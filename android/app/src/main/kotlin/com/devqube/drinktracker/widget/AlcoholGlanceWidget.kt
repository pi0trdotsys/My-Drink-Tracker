package com.devqube.drinktracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.devqube.drinktracker.MainActivity
import com.devqube.drinktracker.data.Fancy
import com.devqube.drinktracker.data.Status
import com.devqube.drinktracker.data.Store
import com.devqube.drinktracker.data.buildWeek
import com.devqube.drinktracker.data.fmt
import com.devqube.drinktracker.data.statusDetails
import com.devqube.drinktracker.data.statusLabel
import com.devqube.drinktracker.data.voiceFor
import com.devqube.drinktracker.ui.theme.Palette
import com.devqube.drinktracker.ui.theme.paletteFor
import kotlin.math.max

class AlcoholGlanceWidget : GlanceAppWidget() {

    // Rzeczywisty rozmiar widgetu w LocalSize. Domyślny SizeMode.Single podaje zawsze rozmiar
    // minimalny, przez co po powiększeniu nie pojawiały się słupki ani dopisek.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = Store.get(context)
        val settings = store.snapshotSettings()
        val entries = store.snapshotEntries()
        val week = buildWeek(entries, settings)
        val details = statusDetails(week)
        val streak = Fancy.dryStreak(entries)
        val palette = paletteFor(settings.theme)
        val line = voiceFor(settings.theme).widgetLine(details.status, streak)

        provideContent {
            GlanceTheme {
                WidgetBody(
                    total = details.total,
                    limit = week.unitLimit.toInt(),
                    drinkingDays = details.drinkingDays,
                    dayLimit = week.dayLimit,
                    status = details.status,
                    statusText = statusLabel(details.status),
                    overUnits = details.overUnits,
                    dayUnits = week.days.map { it.units },
                    dailyRef = if (week.dayLimit > 0) week.unitLimit / week.dayLimit else week.unitLimit,
                    p = palette,
                    line = line,
                )
            }
        }
    }
}

private fun statusColor(status: Status, p: Palette): Color = when (status) {
    Status.OK -> p.ok
    Status.WARN -> p.warn
    Status.OVER -> p.over
}

@Composable
private fun WidgetBody(
    total: Double,
    limit: Int,
    drinkingDays: Int,
    dayLimit: Int,
    status: Status,
    statusText: String,
    overUnits: Double,
    dayUnits: List<Double>,
    dailyRef: Double,
    p: Palette,
    line: String,
) {
    val size = LocalSize.current
    val showBars = size.width >= 200.dp
    val showLine = size.height >= 110.dp
    val statusCol = statusColor(status, p)
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(p.surface)
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "TEN TYDZIEŃ",
                style = TextStyle(color = ColorProvider(p.muted), fontSize = 10.sp, fontWeight = FontWeight.Medium),
            )
            Spacer(GlanceModifier.defaultWeight())
            Box(GlanceModifier.size(10.dp).cornerRadius(5.dp).background(statusCol)) {}
        }
        Spacer(GlanceModifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                fmt(total),
                style = TextStyle(
                    color = ColorProvider(if (status == Status.OVER) p.over else p.foreground),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(" / $limit SU", style = TextStyle(color = ColorProvider(p.muted), fontSize = 13.sp))
        }
        Spacer(GlanceModifier.height(2.dp))
        Text(
            "$drinkingDays/$dayLimit dni · " +
                if (status == Status.OVER && overUnits > 0) "+${fmt(overUnits)} SU" else statusText,
            style = TextStyle(color = ColorProvider(statusCol), fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )

        if (showBars) {
            Spacer(GlanceModifier.height(8.dp))
            MiniBars(dayUnits, dailyRef, status, p)
        }
        if (showLine) {
            Spacer(GlanceModifier.height(6.dp))
            Text(line, style = TextStyle(color = ColorProvider(p.muted), fontSize = 11.sp))
        }
    }
}

@Composable
private fun MiniBars(dayUnits: List<Double>, dailyRef: Double, status: Status, p: Palette) {
    val maxVal = max(dailyRef * 1.4, dayUnits.maxOrNull() ?: 0.0)
    val maxBar = 34
    val hot = if (status == Status.OK) p.warn else statusColor(status, p)
    Row(modifier = GlanceModifier.fillMaxWidth().height(maxBar.dp), verticalAlignment = Alignment.Bottom) {
        dayUnits.forEach { u ->
            val frac = if (maxVal > 0) (u / maxVal) else 0.0
            val h = if (u > 0) max((frac * maxBar).toInt(), 4) else 2
            val color = when {
                u <= 0 -> p.surface2
                u > dailyRef -> hot
                else -> p.primary
            }
            Box(
                modifier = GlanceModifier.defaultWeight().height(maxBar.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    GlanceModifier
                        .fillMaxWidth()
                        .height(h.dp)
                        .padding(horizontal = 1.dp)
                        .cornerRadius(3.dp)
                        .background(color),
                ) {}
            }
        }
    }
}
