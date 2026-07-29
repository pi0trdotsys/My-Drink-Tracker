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
import kotlin.math.max

private val Surface = Color(0xFF23252F)
private val Surface2 = Color(0xFF2E313D)
private val Foreground = Color(0xFFF1F2F7)
private val Muted = Color(0xFFA8ABB8)
private val PrimaryC = Color(0xFFE3A54B)
private val OkC = Color(0xFF45C08A)
private val WarnC = Color(0xFFE6B646)
private val OverC = Color(0xFFDE5140)

class AlcoholGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = Store.get(context)
        val settings = store.snapshotSettings()
        val entries = store.snapshotEntries()
        val week = buildWeek(entries, settings)
        val details = statusDetails(week)
        val streak = Fancy.dryStreak(entries)

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
                    streak = streak,
                )
            }
        }
    }
}

private fun statusColor(status: Status): Color = when (status) {
    Status.OK -> OkC
    Status.WARN -> WarnC
    Status.OVER -> OverC
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
    streak: Int,
) {
    val size = LocalSize.current
    val showBars = size.width >= 200.dp
    val showStreak = size.height >= 110.dp
    val statusCol = statusColor(status)
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Surface)
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "TEN TYDZIEŃ",
                style = TextStyle(color = ColorProvider(Muted), fontSize = 10.sp, fontWeight = FontWeight.Medium),
            )
            Spacer(GlanceModifier.defaultWeight())
            Box(GlanceModifier.size(10.dp).cornerRadius(5.dp).background(statusCol)) {}
        }
        Spacer(GlanceModifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                fmt(total),
                style = TextStyle(
                    color = ColorProvider(if (status == Status.OVER) OverC else Foreground),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(" / $limit SU", style = TextStyle(color = ColorProvider(Muted), fontSize = 13.sp))
        }
        Spacer(GlanceModifier.height(2.dp))
        Text(
            "$drinkingDays/$dayLimit dni · " +
                if (status == Status.OVER && overUnits > 0) "+${fmt(overUnits)} SU" else statusText,
            style = TextStyle(color = ColorProvider(statusCol), fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )

        if (showBars) {
            Spacer(GlanceModifier.height(8.dp))
            MiniBars(dayUnits, dailyRef, status)
        }
        if (showStreak) {
            Spacer(GlanceModifier.height(6.dp))
            Text(Fancy.streakLine(streak), style = TextStyle(color = ColorProvider(Muted), fontSize = 11.sp))
        }
    }
}

@Composable
private fun MiniBars(dayUnits: List<Double>, dailyRef: Double, status: Status) {
    val maxVal = max(dailyRef * 1.4, dayUnits.maxOrNull() ?: 0.0)
    val maxBar = 34
    val hot = if (status == Status.OK) WarnC else statusColor(status)
    Row(modifier = GlanceModifier.fillMaxWidth().height(maxBar.dp), verticalAlignment = Alignment.Bottom) {
        dayUnits.forEach { u ->
            val frac = if (maxVal > 0) (u / maxVal) else 0.0
            val h = if (u > 0) max((frac * maxBar).toInt(), 4) else 2
            val color = when {
                u <= 0 -> Surface2
                u > dailyRef -> hot
                else -> PrimaryC
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
