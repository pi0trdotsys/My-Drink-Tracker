package com.devqube.drinktracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AlcoholAndFancyTest {

    private fun week(vararg units: Double, limit: Double = 14.0, dayLimit: Int = 4) = WeekData(
        range = "",
        days = units.mapIndexed { i, u -> DayEntry("d$i", u) } + List(7 - units.size) { DayEntry("x$it", 0.0) },
        todayIndex = 3,
        unitLimit = limit,
        dayLimit = dayLimit,
        warnRatio = 0.8,
    )

    @Test
    fun `beer converts to about 2 standard units`() {
        assertEquals(1.9725, toUnits(500.0, 5.0), 1e-9)
        assertEquals(2.465625, toUnits(500.0, 5.0, gramsPerUnit = 8), 1e-9)
    }

    @Test
    fun `status thresholds`() {
        assertEquals(Status.OK, statusDetails(week(2.6, 3.2, 4.0)).status)
        assertEquals(Status.WARN, statusDetails(week(4.0, 4.0, 3.8)).status) // 11,8 ≥ 11,2
        val over = statusDetails(week(6.0, 6.0, 3.8)) // 15,8
        assertEquals(Status.OVER, over.status)
        assertEquals(1.8, over.overUnits, 1e-9)
    }

    @Test
    fun `too many drinking days exceed the limit even with few units`() {
        val d = statusDetails(week(0.5, 0.5, 0.5, 0.5, 0.5))
        assertEquals(Status.OVER, d.status)
        assertEquals(1, d.overDays)
    }

    @Test
    fun `polish plural of beer`() {
        val cases = mapOf(
            0 to "piw", 1 to "piwo", 2 to "piwa", 4 to "piwa", 5 to "piw",
            12 to "piw", 13 to "piw", 14 to "piw", 22 to "piwa", 25 to "piw", 112 to "piw", 122 to "piwa",
        )
        cases.forEach { (n, word) -> assertEquals("n=$n", word, Fancy.beerWord(n)) }
    }

    @Test
    fun `beers left rounds down`() {
        assertEquals(6, Fancy.beersLeft(12.0, 2.0))
        assertEquals(0, Fancy.beersLeft(1.2, 2.0))
        assertEquals(0, Fancy.beersLeft(-1.0, 2.0))
    }

    @Test
    fun `plan message speaks in beers and says stop when over`() {
        val (_, okBody) = Fancy.planMessage(statusDetails(week(2.0)), oneBeerUnits = 2.0)
        assertTrue(okBody, okBody.contains("maksymalnie 6 piw"))

        val (overTitle, overBody) = Fancy.planMessage(statusDetails(week(8.0, 8.0)), oneBeerUnits = 2.0)
        assertEquals("Stop na ten tydzień 🛑", overTitle)
        assertTrue(overBody, overBody.contains("już więcej nie pij"))
    }

    @Test
    fun `dry streak counts days since last drink`() {
        val zone = ZoneId.of("Europe/Warsaw")
        val today = LocalDate.of(2026, 11, 19)
        val tuesday = LocalDateTime.of(2026, 11, 17, 21, 0).atZone(zone).toInstant().toEpochMilli()
        val entry = DrinkEntry(1, tuesday, "Piwo", 500.0, 5.0, 2.0)

        assertEquals(2, Fancy.dryStreak(listOf(entry), zone, today))
        assertEquals(0, Fancy.dryStreak(listOf(entry), zone, LocalDate.of(2026, 11, 17)))
        assertEquals(0, Fancy.dryStreak(emptyList(), zone, today))
    }

    @Test
    fun `entry ids are unique and positive`() {
        val ids = List(10_000) { newEntryId() }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it >= 0 })
    }
}
