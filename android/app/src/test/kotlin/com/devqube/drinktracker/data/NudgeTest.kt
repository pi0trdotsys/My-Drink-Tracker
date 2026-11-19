package com.devqube.drinktracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class NudgeTest {

    private val sober = BacEstimate(0.0, 0.0, BacLevel.SOBER)
    private val tipsy = BacEstimate(0.6, 4.0, BacLevel.IMPAIRED)

    private fun details(vararg units: Double) = statusDetails(
        WeekData(
            range = "",
            days = units.map { DayEntry("", it) } + List(7 - units.size) { DayEntry("", 0.0) },
            todayIndex = 3,
            unitLimit = 14.0,
            dayLimit = 4,
            warnRatio = 0.8,
        )
    )

    private fun input(
        slot: NudgeSlot,
        details: StatusDetails = details(),
        today: Double = 0.0,
        yesterday: Double = 0.0,
        bac: BacEstimate = sober,
        streak: Int = 0,
        day: DayOfWeek = DayOfWeek.WEDNESDAY,
    ) = NudgeInput(slot, details, today, yesterday, bac, streak, day, oneBeerUnits = 2.0, dailyRef = 3.5)

    @Test
    fun `slots cover midday, afternoon and evening only`() {
        assertNull(NudgeSlot.forHour(8))
        assertEquals(NudgeSlot.MIDDAY, NudgeSlot.forHour(12))
        assertEquals(NudgeSlot.AFTERNOON, NudgeSlot.forHour(17))
        assertEquals(NudgeSlot.EVENING, NudgeSlot.forHour(21))
        assertNull(NudgeSlot.forHour(23))
        assertNull(NudgeSlot.forHour(2))
    }

    @Test
    fun `midday - alcohol in blood beats everything`() {
        assertTrue(decideNudge(input(NudgeSlot.MIDDAY, bac = tipsy, yesterday = 4.0)) is Nudge.DontDrive)
    }

    @Test
    fun `midday - drank yesterday means today is a break`() {
        assertEquals(Nudge.AfterYesterday, decideNudge(input(NudgeSlot.MIDDAY, yesterday = 4.0)))
    }

    @Test
    fun `midday - nothing to say stays silent`() {
        assertNull(decideNudge(input(NudgeSlot.MIDDAY)))
    }

    @Test
    fun `afternoon - weekly limit exceeded says do not drink today`() {
        val n = decideNudge(input(NudgeSlot.AFTERNOON, details = details(8.0, 8.0)))
        assertTrue(n is Nudge.WeekOver)
        assertEquals(2.0, (n as Nudge.WeekOver).overUnits, 1e-9)
    }

    @Test
    fun `afternoon - all drinking days used`() {
        val d = details(1.0, 1.0, 1.0, 1.0) // 4/4 dni, mało SU
        assertEquals(Nudge.DaysUsedUp, decideNudge(input(NudgeSlot.AFTERNOON, details = d)))
    }

    @Test
    fun `afternoon - friday shows how many beers are left`() {
        val n = decideNudge(input(NudgeSlot.AFTERNOON, details = details(2.0), day = DayOfWeek.FRIDAY))
        assertEquals(Nudge.WeekendBudget(beers = 6, remainingUnits = 12.0), n)
    }

    @Test
    fun `afternoon - dry streak gets praise on a regular day`() {
        assertEquals(Nudge.DryStreak(3), decideNudge(input(NudgeSlot.AFTERNOON, streak = 3)))
        assertNull(decideNudge(input(NudgeSlot.AFTERNOON, streak = 1)))
    }

    @Test
    fun `evening - stop above daily norm, praise when dry, silent otherwise`() {
        assertEquals(Nudge.EveningStop(5.0), decideNudge(input(NudgeSlot.EVENING, today = 5.0)))
        assertEquals(Nudge.EveningDry, decideNudge(input(NudgeSlot.EVENING, streak = 1)))
        assertNull(decideNudge(input(NudgeSlot.EVENING, today = 2.0)))
    }

    @Test
    fun `input is built from real entries`() {
        val zone = ZoneId.of("Europe/Warsaw")
        val now = LocalDateTime.of(2026, 11, 19, 12, 0).atZone(zone) // czwartek
        val yesterdayBeer = DrinkEntry(
            1, LocalDateTime.of(2026, 11, 18, 20, 0).atZone(zone).toInstant().toEpochMilli(),
            "Piwo", 500.0, 5.0, 2.0,
        )
        val i = nudgeInputFor(listOf(yesterdayBeer), AppSettings(), NudgeSlot.MIDDAY, now)

        assertEquals(toUnits(500.0, 5.0), i.unitsYesterday, 1e-9)
        assertEquals(0.0, i.unitsToday, 1e-9)
        assertEquals(DayOfWeek.THURSDAY, i.dayOfWeek)
        assertEquals(Nudge.AfterYesterday, decideNudge(i))
    }
}
