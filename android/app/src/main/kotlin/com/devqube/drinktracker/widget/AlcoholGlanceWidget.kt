package com.devqube.drinktracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.devqube.drinktracker.MainActivity
import com.devqube.drinktracker.R
import com.devqube.drinktracker.data.Fancy
import com.devqube.drinktracker.data.Meter
import com.devqube.drinktracker.data.Status
import com.devqube.drinktracker.data.Store
import com.devqube.drinktracker.data.Tool
import com.devqube.drinktracker.data.WidgetMoment
import com.devqube.drinktracker.data.buildWeek
import com.devqube.drinktracker.data.fmt
import com.devqube.drinktracker.data.momentOf
import com.devqube.drinktracker.data.statusDetails
import com.devqube.drinktracker.data.toUnits
import com.devqube.drinktracker.data.unitsOn
import com.devqube.drinktracker.data.voiceFor
import com.devqube.drinktracker.ui.components.toolDrawable
import com.devqube.drinktracker.ui.components.toolName
import com.devqube.drinktracker.ui.theme.Palette
import com.devqube.drinktracker.ui.theme.paletteFor
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min

/** Wszystko, co widget pokazuje – policzone raz w [AlcoholGlanceWidget.provideGlance]. */
private data class WidgetState(
    val total: Double,
    val limit: Int,
    val drinkingDays: Int,
    val dayLimit: Int,
    val status: Status,
    val overUnits: Double,
    val ratio: Double,
    val dayUnits: List<Double>,
    val dailyRef: Double,
    val header: String,
    val statusText: String,
    val daysText: String,
    val line: String,
    val speech: String,
    val speechTitle: String?,
    val meter: Meter?,
    val tool: Tool?,
    val quickAdd: String,
    val leopard: Boolean,
)

class AlcoholGlanceWidget : GlanceAppWidget() {

    // Rzeczywisty rozmiar widgetu w LocalSize. Domyślny SizeMode.Single podaje zawsze rozmiar
    // minimalny, przez co po powiększeniu nie pojawiały się dodatkowe elementy.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = Store.get(context)
        val settings = store.snapshotSettings()
        val entries = store.snapshotEntries()
        val week = buildWeek(entries, settings)
        val details = statusDetails(week)
        val streak = Fancy.dryStreak(entries)
        val palette = paletteFor(settings.theme)
        val voice = voiceFor(settings.theme, settings.vulgarity)

        val now = ZonedDateTime.now()
        val today = now.toLocalDate()
        val moment = momentOf(
            hour = now.hour,
            unitsToday = unitsOn(entries, today, settings.gramsPerUnit, now.zone),
            unitsYesterday = unitsOn(entries, today.minusDays(1), settings.gramsPerUnit, now.zone),
        )
        // Teksty zmieniają się co godzinę (worker przypomnień odświeża widget) i po każdym wpisie.
        val seed = (System.currentTimeMillis() / 3_600_000L).toInt() * 31 + entries.size
        val wm = WidgetMoment(
            status = details.status,
            streak = streak,
            beersLeft = Fancy.beersLeft(details.remainingUnits, toUnits(500.0, 5.0, settings.gramsPerUnit)),
            moment = moment,
            seed = seed,
        )

        val state = WidgetState(
            total = details.total,
            limit = week.unitLimit.toInt(),
            drinkingDays = details.drinkingDays,
            dayLimit = week.dayLimit,
            status = details.status,
            overUnits = details.overUnits,
            ratio = details.ratio,
            dayUnits = week.days.map { it.units },
            dailyRef = if (week.dayLimit > 0) week.unitLimit / week.dayLimit else week.unitLimit,
            header = voice.weekHeader,
            statusText = voice.statusLabel(details.status),
            daysText = voice.daysLabel(details.drinkingDays, week.dayLimit, long = false),
            line = voice.widgetLine(wm),
            speech = voice.widgetSpeech(wm),
            speechTitle = voice.speechTitle,
            meter = voice.meter(details),
            tool = voice.tool(details.status),
            quickAdd = voice.quickAdd,
            leopard = palette.leopard,
        )

        provideContent {
            GlanceTheme { WidgetBody(state, palette) }
        }
    }
}

private fun statusColor(status: Status, p: Palette): Color = when (status) {
    Status.OK -> p.ok
    Status.WARN -> p.warn
    Status.OVER -> p.over
}

private fun WidgetState.statusLine() =
    "$daysText · " + if (status == Status.OVER && overUnits > 0) "+${fmt(overUnits)} SU" else statusText

/** Wersja dla 2×1: same liczby dni, żeby zmieścił się cały status. */
private fun WidgetState.compactStatusLine() =
    "$drinkingDays/$dayLimit · " + if (status == Status.OVER && overUnits > 0) "+${fmt(overUnits)} SU" else statusText

private val PAD = 12.dp

@Composable
private fun WidgetBody(s: WidgetState, p: Palette) {
    val size = LocalSize.current
    val context = LocalContext.current
    val compact = size.height < 120.dp  // 2×1 – na większości telefonów ok. 100–115 dp wysokości
    val large = !compact && size.width >= 250.dp && size.height >= 150.dp
    val frame = if (s.leopard) 2.dp else 0.dp

    // Zewnętrzna warstwa = różowa ramka (Grażyna), wewnętrzna = panterka albo gładkie tło.
    Box(
        GlanceModifier
            .fillMaxSize()
            .cornerRadius(22.dp)
            .background(if (s.leopard) p.primary else p.surface)
            .padding(frame)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        val inner = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .let {
                if (s.leopard) it.background(ImageProvider(R.drawable.widget_bg_leopard), ContentScale.FillBounds)
                else it.background(p.surface)
            }
            .padding(PAD)
        val contentWidth = size.width - PAD * 2 - frame * 2
        when {
            compact -> Compact(s, p, inner, size.height)
            large -> Large(s, p, inner, contentWidth, context)
            else -> Medium(s, p, inner, contentWidth, size.width, size.height)
        }
    }
}

// --- Rozmiary ---

@Composable
private fun Compact(s: WidgetState, p: Palette, modifier: GlanceModifier, height: Dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        ToolOrDot(s, p, 36.dp)
        Spacer(GlanceModifier.width(10.dp))
        Column(GlanceModifier.defaultWeight()) {
            NumberRow(s, p, 22.sp)
            Text(s.compactStatusLine(), style = textStyle(statusColor(s.status, p), 11.sp, bold = true), maxLines = 1)
            if (height >= 70.dp) Text(s.line, style = textStyle(p.muted, 10.sp), maxLines = 1)
        }
    }
}

@Composable
private fun Medium(s: WidgetState, p: Palette, modifier: GlanceModifier, contentWidth: Dp, width: Dp, height: Dp) {
    Column(modifier) {
        Header(s, p, 24.dp)
        NumberRow(s, p, 26.sp)
        Text(s.statusLine(), style = textStyle(statusColor(s.status, p), 11.sp, bold = true), maxLines = 1)
        if (width >= 180.dp && height >= 125.dp) {
            Spacer(GlanceModifier.height(6.dp))
            MiniBars(s, p, 28)
        }
        if (height >= 140.dp) {
            Spacer(GlanceModifier.height(6.dp))
            MeterOrLimit(s, p, contentWidth)
        }
        if (height >= 175.dp) {
            Spacer(GlanceModifier.height(4.dp))
            Text(s.line, style = textStyle(p.muted, 11.sp), maxLines = 1)
        }
    }
}

@Composable
private fun Large(s: WidgetState, p: Palette, modifier: GlanceModifier, contentWidth: Dp, context: Context) {
    val half = (contentWidth - 8.dp) / 2
    Column(modifier) {
        Header(s, p, 26.dp)
        Row(GlanceModifier.fillMaxWidth()) {
            Column(GlanceModifier.width(half)) {
                NumberRow(s, p, 26.sp)
                Text(s.statusLine(), style = textStyle(statusColor(s.status, p), 11.sp, bold = true), maxLines = 1)
                Spacer(GlanceModifier.height(6.dp))
                MeterOrLimit(s, p, half)
            }
            Spacer(GlanceModifier.width(8.dp))
            SpeechBubble(s, p, GlanceModifier.width(half))
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Box(GlanceModifier.defaultWeight()) { MiniBars(s, p, 26) }
            Spacer(GlanceModifier.width(8.dp))
            Box(
                GlanceModifier
                    .cornerRadius(14.dp)
                    .background(p.primary)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .clickable(actionStartActivity(MainActivity.logIntent(context))),
            ) {
                Text(s.quickAdd, style = textStyle(p.onPrimary, 11.sp, bold = true), maxLines = 1)
            }
        }
    }
}

// --- Elementy ---

private fun textStyle(color: Color, size: TextUnit, bold: Boolean = false) =
    TextStyle(color = ColorProvider(color), fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

@Composable
private fun Header(s: WidgetState, p: Palette, iconSize: Dp) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(s.header, style = textStyle(if (s.leopard) p.primary else p.muted, 10.sp, bold = true), maxLines = 1)
        Spacer(GlanceModifier.defaultWeight())
        ToolOrDot(s, p, iconSize)
    }
}

@Composable
private fun NumberRow(s: WidgetState, p: Palette, size: TextUnit) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            fmt(s.total),
            style = textStyle(if (s.status == Status.OVER) p.over else p.foreground, size, bold = true),
        )
        Text(" / ${s.limit} SU", style = textStyle(p.muted, 12.sp))
    }
}

@Composable
private fun ToolOrDot(s: WidgetState, p: Palette, size: Dp) {
    val tool = s.tool
    if (tool != null) {
        Image(
            provider = ImageProvider(toolDrawable(tool)),
            contentDescription = toolName(tool),
            modifier = GlanceModifier.size(size),
        )
    } else {
        Box(GlanceModifier.size(10.dp).cornerRadius(5.dp).background(statusColor(s.status, p))) {}
    }
}

/** Poziom wkurwu Grażyny (z podpisem) albo zwykły pasek limitu w motywie klasycznym. */
@Composable
private fun MeterOrLimit(s: WidgetState, p: Palette, width: Dp) {
    val pct = s.meter?.percent ?: (min(s.ratio, 1.0) * 100).toInt()
    val filled = width * (pct.coerceIn(0, 100) / 100f)
    Column {
        Box(GlanceModifier.width(width).height(6.dp).cornerRadius(3.dp).background(p.muted.copy(alpha = 0.3f))) {
            if (pct > 0) {
                Box(GlanceModifier.width(max(filled.value, 6f).dp).height(6.dp).cornerRadius(3.dp).background(statusColor(s.status, p))) {}
            }
        }
        s.meter?.let {
            Spacer(GlanceModifier.height(3.dp))
            Text("Wkurw: ${it.percent}% · ${it.mood}", style = textStyle(p.muted, 10.sp), maxLines = 1)
        }
    }
}

@Composable
private fun SpeechBubble(s: WidgetState, p: Palette, modifier: GlanceModifier) {
    Column(
        modifier
            .cornerRadius(12.dp)
            .background(p.primary.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        s.speechTitle?.let { Text(it, style = textStyle(p.primary, 10.sp, bold = true), maxLines = 1) }
        Text(s.speech, style = textStyle(p.foreground, 11.sp), maxLines = 4)
    }
}

@Composable
private fun MiniBars(s: WidgetState, p: Palette, maxBar: Int) {
    val maxVal = max(s.dailyRef * 1.4, s.dayUnits.maxOrNull() ?: 0.0)
    val hot = if (s.status == Status.OK) p.warn else statusColor(s.status, p)
    Row(modifier = GlanceModifier.fillMaxWidth().height(maxBar.dp), verticalAlignment = Alignment.Bottom) {
        s.dayUnits.forEach { u ->
            val frac = if (maxVal > 0) (u / maxVal) else 0.0
            val h = if (u > 0) max((frac * maxBar).toInt(), 4) else 3
            val color = when {
                u <= 0 -> p.muted.copy(alpha = 0.3f)
                u > s.dailyRef -> hot
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
