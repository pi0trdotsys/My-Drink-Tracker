package com.devqube.drinktracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.devqube.drinktracker.data.AppSettings
import com.devqube.drinktracker.data.DrinkEntry
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
        val initialSettings = store.snapshotSettings()
        val initialEntries = store.snapshotEntries()

        provideContent {
            // provideGlance wykonuje się raz na sesję widgetu – dane subskrybujemy w treści, żeby każda
            // zmiana (nowy wpis, motyw, limit) od razu przerysowała widget. Wcześniej stan był liczony
            // tylko raz, więc przy żywej sesji widget pokazywał nieaktualne dane (np. stary motyw).
            val settings by store.settings.collectAsState(initial = initialSettings)
            val entries by store.entries.collectAsState(initial = initialEntries)
            val (state, palette) = buildWidgetState(settings, entries)
            GlanceTheme { WidgetBody(state, palette) }
        }
    }
}

/** Liczy wszystko, co widget pokazuje, dla bieżących danych i chwili [nowMillis]. */
private fun buildWidgetState(
    settings: AppSettings,
    entries: List<DrinkEntry>,
    nowMillis: Long = System.currentTimeMillis(),
): Pair<WidgetState, Palette> {
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
    val seed = (nowMillis / 3_600_000L).toInt() * 31 + entries.size
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
    return state to palette
}

private fun statusColor(status: Status, p: Palette): Color = when (status) {
    Status.OK -> p.ok
    Status.WARN -> p.warn
    Status.OVER -> p.over
}

private fun WidgetState.statusLine() =
    "$daysText · " + if (status == Status.OVER && overUnits > 0) "+${fmt(overUnits)} SU" else statusText

/** Wersja dla 2×2: „1/4 dni · status” – krótsza od pełnej, żeby zmieściła się w wąskiej kolumnie. */
private fun WidgetState.mediumStatusLine() =
    "$drinkingDays/$dayLimit dni · " + if (status == Status.OVER && overUnits > 0) "+${fmt(overUnits)} SU" else statusText

/** Wersja dla 2×1: same liczby dni, żeby zmieścił się cały status. */
private fun WidgetState.compactStatusLine() =
    "$drinkingDays/$dayLimit · " + if (status == Status.OVER && overUnits > 0) "+${fmt(overUnits)} SU" else statusText

private val PAD = 12.dp

/** Przybliżona wysokość jednej linii tekstu 10,5 sp w dymku i stała część dymka (tytuł + odstępy), w dp. */
private const val LINE = 14f
private const val BUBBLE_CHROME = 26f

/** Średnia szerokość znaku tekstu 10,5 sp (dp) i wewnętrzne marginesy dymka – do szacowania łamania linii. */
private const val CHAR_WIDTH = 5.6f
private const val BUBBLE_PADDING = 16f

/** Ile linii zajmie [text] w dymku o szerokości [width] (szacunek z zapasem). */
private fun linesNeeded(text: String, width: Dp): Int {
    val perLine = ((width.value - BUBBLE_PADDING) / CHAR_WIDTH).toInt().coerceAtLeast(1)
    return (text.length + perLine - 1) / perLine
}

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
            else -> Medium(s, p, inner, contentWidth, size.height - PAD * 2 - frame * 2, context)
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

/**
 * 2×2 – pełna zawartość jak w 4×2, ułożona w jednej kolumnie: nagłówek z ikoną, liczba z przyciskiem „+”,
 * status, słupki, poziom wkurwu i dymek z komentarzem. Dymek dostaje tyle linii, ile zmieści się
 * w dostępnej wysokości; dłuższa wypowiedź trafia do niego tylko, jeśli zmieści się bez ucięcia.
 */
@Composable
private fun Medium(s: WidgetState, p: Palette, modifier: GlanceModifier, contentWidth: Dp, contentHeight: Dp, context: Context) {
    // Szacunkowe wysokości stałych elementów (dp): nagłówek, liczba, status, słupki, pasek (+ podpis).
    val fixed = 22 + 30 + 14 + (4 + 22) + (4 + 6) + (if (s.meter != null) 15 else 0)
    val left = contentHeight.value - fixed - 6
    val lines = ((left - BUBBLE_CHROME) / LINE).toInt().coerceIn(0, 4)

    Column(modifier) {
        Header(s, p, 22.dp)
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.defaultWeight()) { NumberRow(s, p, 24.sp) }
            PlusButton(s, p, context)
        }
        Text(s.mediumStatusLine(), style = textStyle(statusColor(s.status, p), 11.sp, bold = true), maxLines = 1)
        Spacer(GlanceModifier.height(4.dp))
        MiniBars(s, p, 22)
        Spacer(GlanceModifier.height(4.dp))
        MeterOrLimit(s, p, contentWidth)
        when {
            lines >= 1 -> {
                Spacer(GlanceModifier.height(6.dp))
                SpeechBubble(
                    s, p, GlanceModifier.fillMaxWidth(),
                    // Dłuższa wypowiedź tylko wtedy, gdy zmieści się w całości – inaczej krótka zaczepka (bez ucinania).
                    text = if (linesNeeded(s.speech, contentWidth) <= lines) s.speech else s.line,
                    maxLines = lines,
                )
            }
            left >= LINE -> {
                Spacer(GlanceModifier.height(4.dp))
                Text(s.line, style = textStyle(p.muted, 10.5.sp), maxLines = 1)
            }
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

/** Okrągły przycisk „+” (2×2) – otwiera ekran logowania, tak jak „+ Przyznaję się” w 4×2. */
@Composable
private fun PlusButton(s: WidgetState, p: Palette, context: Context) {
    Box(
        GlanceModifier
            .size(28.dp)
            .cornerRadius(14.dp)
            .background(p.primary)
            .clickable(actionStartActivity(MainActivity.logIntent(context))),
        contentAlignment = Alignment.Center,
    ) {
        Text("+", style = textStyle(p.onPrimary, 18.sp, bold = true))
    }
}

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
private fun SpeechBubble(s: WidgetState, p: Palette, modifier: GlanceModifier, text: String = s.speech, maxLines: Int = 4) {
    Column(
        modifier
            .cornerRadius(12.dp)
            .background(p.primary.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        s.speechTitle?.let { Text(it, style = textStyle(p.primary, 10.sp, bold = true), maxLines = 1) }
        Text(text, style = textStyle(p.foreground, 10.5.sp), maxLines = maxLines)
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
