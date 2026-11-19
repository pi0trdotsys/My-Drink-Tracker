package com.devqube.drinktracker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devqube.drinktracker.data.Status
import com.devqube.drinktracker.data.WeekData
import com.devqube.drinktracker.data.fmt
import com.devqube.drinktracker.data.statusDetails
import com.devqube.drinktracker.data.statusLabel
import com.devqube.drinktracker.ui.theme.LocalDrinkColors
import com.devqube.drinktracker.ui.theme.LocalVoice
import kotlin.math.max
import kotlin.math.min

enum class WidgetSize { S2x1, S2x2, S4x2 }

@Composable
private fun statusColor(status: Status): Color {
    val c = LocalDrinkColors.current
    return when (status) {
        Status.OK -> c.ok
        Status.WARN -> c.warn
        Status.OVER -> c.over
    }
}

@Composable
fun AlcoholWidgetCard(week: WeekData, size: WidgetSize, modifier: Modifier = Modifier) {
    val d = statusDetails(week)
    val statusCol = statusColor(d.status)
    val colors = LocalDrinkColors.current

    val shell = modifier
        .clip(RoundedCornerShape(22.dp))
        .background(MaterialTheme.colorScheme.surface)
        .leopardSpots()
        .border(
            width = if (d.status == Status.OK) 1.dp else 1.5.dp,
            color = when (d.status) {
                Status.OK -> colors.border
                Status.WARN -> colors.warn.copy(alpha = 0.5f)
                Status.OVER -> colors.over.copy(alpha = 0.6f)
            },
            shape = RoundedCornerShape(22.dp),
        )

    when (size) {
        WidgetSize.S2x1 -> Small2x1(week, d.status, shell)
        WidgetSize.S2x2 -> Medium2x2(week, d.status, shell)
        WidgetSize.S4x2 -> Large4x2(week, d.status, shell)
    }
}

@Composable
private fun Small2x1(week: WeekData, status: Status, shell: Modifier) {
    val d = statusDetails(week)
    val statusCol = statusColor(status)
    val colors = LocalDrinkColors.current
    Box(shell) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(6.dp)
                .background(statusCol)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 12.dp, bottom = 12.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        fmt(d.total),
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = if (status == Status.OVER) colors.over else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "/${week.unitLimit.toInt()} SU",
                        fontSize = 13.sp,
                        color = colors.muted,
                        modifier = Modifier.padding(start = 2.dp, bottom = 1.dp),
                    )
                }
                Text(
                    "${d.drinkingDays}/${week.dayLimit} dni · " +
                        if (status == Status.OVER && d.overUnits > 0) "+${fmt(d.overUnits)} SU" else statusLabel(status),
                    fontSize = 11.sp,
                    color = statusCol,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Box(Modifier.width(70.dp)) {
                Bars(week, status, height = 32.dp, showLabels = false)
            }
        }
    }
}

@Composable
private fun Medium2x2(week: WeekData, status: Status, shell: Modifier) {
    val d = statusDetails(week)
    val colors = LocalDrinkColors.current
    Column(shell.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("TEN TYDZIEŃ", style = MaterialTheme.typography.labelSmall, color = colors.muted)
                ShimmerTotal(d.total, week.unitLimit.toInt(), status, big = false)
            }
            StatusDot(status)
        }
        Bars(week, status, height = 64.dp, showLabels = true)
        LimitBar(d.ratio, status)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${d.drinkingDays}/${week.dayLimit} dni", fontSize = 11.sp, color = colors.muted)
            Text(statusLabel(status), fontSize = 11.sp, color = statusColor(status))
        }
        if (status != Status.OK) LocalVoice.current.alert(d).let { (h, m) -> AlertBox(status, h, m, d.reasons, compact = true) }
    }
}

@Composable
private fun Large4x2(week: WeekData, status: Status, shell: Modifier) {
    val d = statusDetails(week)
    val colors = LocalDrinkColors.current
    Column(shell.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TEN TYDZIEŃ", style = MaterialTheme.typography.labelSmall, color = colors.muted)
            Text(week.range, fontSize = 11.sp, color = colors.muted)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column {
                ShimmerTotal(d.total, week.unitLimit.toInt(), status, big = true)
                Text("/ ${week.unitLimit.toInt()} SU limitu", fontSize = 12.sp, color = colors.muted, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.width(16.dp))
            Box(Modifier.weight(1f)) {
                Bars(week, status, height = 88.dp, showLabels = true)
            }
        }
        LimitBar(d.ratio, status)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${d.drinkingDays}/${week.dayLimit} dni z alkoholem", fontSize = 12.sp, color = colors.muted)
            StatusPill(status)
        }
        if (status != Status.OK) LocalVoice.current.alert(d).let { (h, m) -> AlertBox(status, h, m, d.reasons, compact = false) }
    }
}

@Composable
private fun ShimmerTotal(total: Double, limit: Int, status: Status, big: Boolean) {
    val colors = LocalDrinkColors.current
    val baseColor = if (status == Status.OVER) colors.over else MaterialTheme.colorScheme.onSurface
    val size = if (big) 52.sp else 30.sp

    // "Fancy smaczek": złoty połysk przesuwający się po głównej liczbie.
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -1.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, MaterialTheme.colorScheme.primary, baseColor),
        start = androidx.compose.ui.geometry.Offset(x * 120f, 0f),
        end = androidx.compose.ui.geometry.Offset((x + 1f) * 120f, 0f),
    )
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            fmt(total),
            fontWeight = FontWeight.Black,
            fontSize = size,
            style = MaterialTheme.typography.displayLarge.copy(brush = brush, fontSize = size),
        )
        if (!big) {
            Text(
                " / $limit SU",
                fontSize = 15.sp,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

@Composable
private fun Bars(week: WeekData, status: Status, height: androidx.compose.ui.unit.Dp, showLabels: Boolean) {
    val colors = LocalDrinkColors.current
    val dailyRef = if (week.dayLimit > 0) week.unitLimit / week.dayLimit else week.unitLimit
    val maxVal = max(dailyRef * 1.4, week.days.maxOfOrNull { it.units } ?: 0.0)
    val hotColor = if (status == Status.OK) colors.warn else statusColor(status)

    Column {
        Row(
            Modifier.fillMaxWidth().height(height),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            week.days.forEachIndexed { i, day ->
                val pct = if (maxVal > 0) (day.units / maxVal).toFloat() else 0f
                val filled = if (day.units > 0) max(pct, 0.08f) else 0f
                val hot = day.units > dailyRef
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.surface2.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (day.units > 0) {
                        val barColor = if (hot) hotColor else MaterialTheme.colorScheme.primary
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(filled)
                                .clip(RoundedCornerShape(3.dp))
                                .background(barColor)
                                .then(
                                    if (i == week.todayIndex)
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                                    else Modifier
                                )
                        )
                    }
                }
            }
        }
        if (showLabels) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.days.forEachIndexed { i, day ->
                    Text(
                        day.label,
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp,
                        color = if (i == week.todayIndex) MaterialTheme.colorScheme.onSurface else colors.muted,
                        fontWeight = if (i == week.todayIndex) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun LimitBar(ratio: Double, status: Status) {
    val colors = LocalDrinkColors.current
    val filled = min(ratio, 1.0).toFloat()
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(colors.surface2),
    ) {
        Box(
            Modifier
                .fillMaxWidth(filled)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(statusColor(status)),
        )
    }
}

@Composable
private fun StatusDot(status: Status) {
    Box(
        Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(statusColor(status)),
    )
}

@Composable
private fun StatusPill(status: Status) {
    val statusCol = statusColor(status)
    Box(
        Modifier
            .clip(CircleShape)
            .background(statusCol)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            statusLabel(status),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = when (status) {
                Status.OK -> Color(0xFF0E2C20)
                Status.WARN -> Color(0xFF2A2008)
                Status.OVER -> Color(0xFFFFFFFF)
            },
        )
    }
}

@Composable
fun AlertBox(status: Status, headline: String, message: String, reasons: List<String>, compact: Boolean) {
    if (status == Status.OK) return
    val colors = LocalDrinkColors.current
    val accent = statusColor(status)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (status == Status.OVER) "‼️" else "⚠️", fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Text(headline, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent)
        }
        if (!compact) {
            Text(message, fontSize = 11.sp, color = colors.muted, modifier = Modifier.padding(top = 2.dp))
            if (reasons.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    reasons.take(2).forEach { r ->
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .border(1.dp, accent.copy(alpha = 0.5f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(r, fontSize = 10.sp, color = accent)
                        }
                    }
                }
            }
        }
    }
}
