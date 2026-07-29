package com.devqube.drinktracker.data

import java.util.Locale
import kotlin.math.max

/** Gęstość etanolu (g/ml). */
const val ETHANOL_DENSITY = 0.789

/** Gramy czystego etanolu na jedną jednostkę standardową (SU). */
const val GRAMS_PER_UNIT_DEFAULT = 10

/** Typ trunku – łatwy do dostosowania w ustawieniach. */
data class DrinkType(
    val id: String,
    val name: String,
    val emoji: String,
    val volumeMl: Int,
    val abv: Double,
    /** true dla typów dodanych przez użytkownika. */
    val custom: Boolean = false,
)

val DEFAULT_DRINK_TYPES: List<DrinkType> = listOf(
    DrinkType("beer", "Piwo", "🍺", 500, 5.0),
    DrinkType("wine", "Lampka wina", "🍷", 150, 12.0),
    DrinkType("vodka", "Kieliszek wódki", "🥃", 50, 40.0),
    DrinkType("whisky", "Whisky", "🥃", 50, 43.0),
    DrinkType("gin", "Gin z tonikiem", "🍸", 250, 9.0),
    DrinkType("drink", "Drink", "🍹", 200, 14.0),
    DrinkType("prosecco", "Prosecco", "🍾", 120, 11.0),
)

/** Przelicza objętość i moc na jednostki standardowe (SU). */
fun toUnits(volumeMl: Double, abv: Double, gramsPerUnit: Int = GRAMS_PER_UNIT_DEFAULT): Double =
    (volumeMl * (abv / 100.0) * ETHANOL_DENSITY) / gramsPerUnit

/** Gramy czystego etanolu z objętości i mocy. */
fun toGrams(volumeMl: Double, abv: Double): Double = volumeMl * (abv / 100.0) * ETHANOL_DENSITY

/** Formatowanie z jednym miejscem po przecinku i polskim separatorem. */
fun fmt(n: Double): String = String.format(Locale.US, "%.1f", n).replace(".", ",")

enum class Status { OK, WARN, OVER }

fun statusLabel(status: Status): String = when (status) {
    Status.OK -> "W normie"
    Status.WARN -> "Uwaga"
    Status.OVER -> "Przekroczono"
}

data class DayEntry(val label: String, val units: Double)

data class WeekData(
    val range: String,
    val days: List<DayEntry>,
    val todayIndex: Int,
    val unitLimit: Double,
    val dayLimit: Int,
    val warnRatio: Double,
)

data class WeekSummary(val total: Double, val drinkingDays: Int, val status: Status)

fun weekSummary(week: WeekData): WeekSummary {
    val total = week.days.sumOf { it.units }
    val drinkingDays = week.days.count { it.units > 0 }
    val status = when {
        total > week.unitLimit || drinkingDays > week.dayLimit -> Status.OVER
        total >= week.unitLimit * week.warnRatio || drinkingDays >= week.dayLimit -> Status.WARN
        else -> Status.OK
    }
    return WeekSummary(total, drinkingDays, status)
}

data class StatusDetails(
    val total: Double,
    val drinkingDays: Int,
    val status: Status,
    val ratio: Double,
    val overUnits: Double,
    val remainingUnits: Double,
    val overDays: Int,
    val remainingDays: Int,
    val headline: String,
    val message: String,
    val reasons: List<String>,
)

fun statusDetails(week: WeekData): StatusDetails {
    val (total, drinkingDays, status) = weekSummary(week)
    val overUnits = max(total - week.unitLimit, 0.0)
    val remainingUnits = max(week.unitLimit - total, 0.0)
    val overDays = max(drinkingDays - week.dayLimit, 0)
    val remainingDays = max(week.dayLimit - drinkingDays, 0)
    val ratio = if (week.unitLimit > 0) total / week.unitLimit else 0.0

    val reasons = mutableListOf<String>()
    if (overUnits > 0) reasons.add("+${fmt(overUnits)} SU ponad limit")
    if (overDays > 0) reasons.add("+$overDays dzień z alkoholem ponad limit")
    if (status == Status.WARN) {
        if (overUnits == 0.0 && remainingUnits <= week.unitLimit * (1 - week.warnRatio))
            reasons.add("zostało ${fmt(remainingUnits)} SU")
        if (overDays == 0 && remainingDays == 0) reasons.add("wykorzystane wszystkie dni")
    }

    val headline: String
    val message: String
    when (status) {
        Status.OVER -> {
            headline = "Limit przekroczony"
            message = if (overUnits > 0)
                "Przekroczono tygodniowy limit o ${fmt(overUnits)} SU. Zrób przerwę do końca tygodnia."
            else
                "Przekroczono limit dni z alkoholem ($drinkingDays/${week.dayLimit}). Zrób przerwę do końca tygodnia."
        }
        Status.WARN -> {
            headline = "Blisko limitu"
            message = if (remainingUnits > 0)
                "Zostało tylko ${fmt(remainingUnits)} SU i $remainingDays dni z alkoholem."
            else
                "Wykorzystano cały limit dni z alkoholem w tym tygodniu."
        }
        Status.OK -> {
            headline = "W normie"
            message = "Zostało ${fmt(remainingUnits)} SU i $remainingDays dni z alkoholem."
        }
    }

    return StatusDetails(
        total, drinkingDays, status, ratio, overUnits, remainingUnits,
        overDays, remainingDays, headline, message, reasons,
    )
}
