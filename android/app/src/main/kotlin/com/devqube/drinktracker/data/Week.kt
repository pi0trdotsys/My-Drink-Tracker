package com.devqube.drinktracker.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val DAY_LABELS = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")
private val PL = Locale("pl", "PL")

/** Milisekundy do najbliższej północy (+30 s zapasu, by trafić już w nowy dzień). */
fun millisUntilNextMidnight(now: java.time.ZonedDateTime): Long {
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return java.time.Duration.between(now, nextMidnight).toMillis() + 30_000
}

/** Poniedziałek tygodnia, do którego należy dany dzień. */
fun mondayOf(day: LocalDate): LocalDate = day.minusDays((day.dayOfWeek.value - 1).toLong())

/**
 * Buduje dane bieżącego tygodnia (poniedziałek–niedziela) z listy wpisów.
 * SU liczone są według bieżącej definicji jednostki, więc zmiana ustawień przelicza historię spójnie.
 */
fun buildWeek(
    entries: List<DrinkEntry>,
    settings: AppSettings,
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone),
): WeekData {
    val monday = mondayOf(today)
    val sunday = monday.plusDays(6)

    val perDay = DoubleArray(7)
    entries.forEach { e ->
        val d = Instant.ofEpochMilli(e.timestamp).atZone(zone).toLocalDate()
        if (!d.isBefore(monday) && !d.isAfter(sunday)) {
            val idx = ChronoUnit.DAYS.between(monday, d).toInt()
            if (idx in 0..6) perDay[idx] += e.unitsFor(settings.gramsPerUnit)
        }
    }

    val days = (0..6).map { DayEntry(DAY_LABELS[it], perDay[it]) }
    val todayIndex = ChronoUnit.DAYS.between(monday, today).toInt().coerceIn(0, 6)

    return WeekData(
        range = formatRange(monday, sunday),
        days = days,
        todayIndex = todayIndex,
        unitLimit = settings.unitLimit,
        dayLimit = settings.dayLimit,
        warnRatio = settings.warnRatio,
    )
}

private fun formatRange(from: LocalDate, to: LocalDate): String {
    val m1 = from.month.getDisplayName(TextStyle.SHORT, PL).take(3)
    val m2 = to.month.getDisplayName(TextStyle.SHORT, PL).take(3)
    return "${from.dayOfMonth} $m1 – ${to.dayOfMonth} $m2"
}

/**
 * Szacowane BAC w chwili [nowMillis]. Nie zależy od daty kalendarzowej: trunek z 23:00
 * nadal się liczy o 01:00 (wcześniej o północy promile spadały do zera).
 */
fun currentBac(
    entries: List<DrinkEntry>,
    settings: AppSettings,
    nowMillis: Long = System.currentTimeMillis(),
): BacEstimate = bacAt(
    drinks = entries.map { it.timestamp to it.grams },
    weightKg = settings.weightKg,
    sex = settings.sex,
    nowMillis = nowMillis,
)

/** Krótka etykieta wybranego momentu, np. „Dziś, 14:09" lub „28 lip, 21:40". */
fun formatWhenLabel(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val dt = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()
    val time = String.format(Locale.US, "%02d:%02d", dt.hour, dt.minute)
    val today = LocalDate.now(zone)
    return if (dt.toLocalDate() == today) {
        "Dziś, $time"
    } else {
        val m = dt.month.getDisplayName(TextStyle.SHORT, PL).take(3)
        "${dt.dayOfMonth} $m, $time"
    }
}

/** Łączy datę (UTC z DatePickera) z godziną/minutą w strefie lokalnej → epoch millis. */
fun combineDateTime(dateUtcMillis: Long, hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
    val date = Instant.ofEpochMilli(dateUtcMillis).atZone(ZoneId.of("UTC")).toLocalDate()
    return date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
}

fun formatEntryTime(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): Pair<String, String> {
    val dt: LocalDateTime = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDateTime()
    val day = dt.dayOfWeek.getDisplayName(TextStyle.FULL, PL).replaceFirstChar { it.uppercase(PL) }
    val time = String.format(Locale.US, "%02d:%02d", dt.hour, dt.minute)
    return day to time
}
