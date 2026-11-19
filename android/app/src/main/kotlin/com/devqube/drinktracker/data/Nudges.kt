package com.devqube.drinktracker.data

import java.time.DayOfWeek

/**
 * Pora dnia, w której może przyjść przypomnienie. Maksymalnie jedno na slot, więc najwyżej
 * trzy dziennie – a zwykle mniej, bo slot bez sensownego powodu jest pomijany.
 *
 * @param fromHour pierwsza godzina okna (włącznie), [toHour] – koniec okna (wyłącznie)
 */
enum class NudgeSlot(val fromHour: Int, val toHour: Int) {
    /** Kac i kierownica, „wczoraj piłeś”. */
    MIDDAY(11, 14),

    /** Godzina pokusy: po pracy, przed zakupami. */
    AFTERNOON(16, 19),

    /** Wieczór: „odstaw butelkę” albo pochwała za dzień bez picia. */
    EVENING(20, 22);

    companion object {
        fun forHour(hour: Int): NudgeSlot? = entries.firstOrNull { hour >= it.fromHour && hour < it.toHour }
    }
}

/** Powód przypomnienia – tekst dobiera [Voice] aktywnego motywu. */
sealed interface Nudge {
    /** We krwi wciąż jest alkohol – nie prowadź. */
    data class DontDrive(val perMille: Double, val hoursToSober: Double) : Nudge

    /** Wczoraj było picie – dziś przerwa. */
    data object AfterYesterday : Nudge

    /** Tygodniowy limit już przekroczony – dziś ani kropli. */
    data class WeekOver(val overUnits: Double, val overDays: Int) : Nudge

    /** Wszystkie dni z alkoholem wykorzystane, a dziś jeszcze nic – niech tak zostanie. */
    data object DaysUsedUp : Nudge

    /** Piątek/sobota – ile maksymalnie można dziś wypić. */
    data class WeekendBudget(val beers: Int, val remainingUnits: Double) : Nudge

    /** Kilka dni bez alkoholu z rzędu – pochwała (motywacja). */
    data class DryStreak(val days: Int) : Nudge

    /** Dziś już ponad dzienną normę – odstaw. */
    data class EveningStop(val unitsToday: Double) : Nudge

    /** Wieczór, a dziś bez alkoholu – pochwała. */
    data object EveningDry : Nudge
}

data class NudgeInput(
    val slot: NudgeSlot,
    val details: StatusDetails,
    val unitsToday: Double,
    val unitsYesterday: Double,
    val bac: BacEstimate,
    val dryStreak: Int,
    val dayOfWeek: DayOfWeek,
    val oneBeerUnits: Double,
    /** Dzienna „norma” = limit tygodniowy / limit dni. */
    val dailyRef: Double,
)

/** SU wypite danego dnia (wg bieżącej definicji jednostki). */
fun unitsOn(entries: List<DrinkEntry>, day: java.time.LocalDate, gramsPerUnit: Int, zone: java.time.ZoneId): Double =
    entries.filter { java.time.Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() == day }
        .sumOf { it.unitsFor(gramsPerUnit) }

/** Zbiera stan aplikacji w chwili [now] w dane wejściowe dla [decideNudge]. */
fun nudgeInputFor(
    entries: List<DrinkEntry>,
    settings: AppSettings,
    slot: NudgeSlot,
    now: java.time.ZonedDateTime,
): NudgeInput {
    val zone = now.zone
    val today = now.toLocalDate()
    val week = buildWeek(entries, settings, zone, today)
    return NudgeInput(
        slot = slot,
        details = statusDetails(week),
        unitsToday = unitsOn(entries, today, settings.gramsPerUnit, zone),
        unitsYesterday = unitsOn(entries, today.minusDays(1), settings.gramsPerUnit, zone),
        bac = currentBac(entries, settings, now.toInstant().toEpochMilli()),
        dryStreak = Fancy.dryStreak(entries, zone, today),
        dayOfWeek = today.dayOfWeek,
        oneBeerUnits = toUnits(500.0, 5.0, settings.gramsPerUnit),
        dailyRef = if (settings.dayLimit > 0) settings.unitLimit / settings.dayLimit else settings.unitLimit,
    )
}

/** Wybiera najbardziej sensowne przypomnienie dla danego slotu albo `null`, gdy nie ma powodu. */
fun decideNudge(i: NudgeInput): Nudge? {
    val drunk = i.bac.perMille > 0.2
    return when (i.slot) {
        NudgeSlot.MIDDAY -> when {
            drunk -> Nudge.DontDrive(i.bac.perMille, i.bac.hoursToSober)
            i.unitsYesterday > 0 && i.unitsToday == 0.0 -> Nudge.AfterYesterday
            else -> null
        }

        NudgeSlot.AFTERNOON -> when {
            drunk -> Nudge.DontDrive(i.bac.perMille, i.bac.hoursToSober)
            i.details.status == Status.OVER -> Nudge.WeekOver(i.details.overUnits, i.details.overDays)
            i.details.remainingDays == 0 && i.unitsToday == 0.0 -> Nudge.DaysUsedUp
            i.dayOfWeek == DayOfWeek.FRIDAY || i.dayOfWeek == DayOfWeek.SATURDAY -> {
                val beers = Fancy.beersLeft(i.details.remainingUnits, i.oneBeerUnits)
                if (beers > 0) Nudge.WeekendBudget(beers, i.details.remainingUnits) else null
            }
            i.dryStreak >= 2 -> Nudge.DryStreak(i.dryStreak)
            else -> null
        }

        NudgeSlot.EVENING -> when {
            i.unitsToday > i.dailyRef -> Nudge.EveningStop(i.unitsToday)
            i.unitsToday == 0.0 && i.dryStreak >= 1 -> Nudge.EveningDry
            else -> null
        }
    }
}
