package com.devqube.drinktracker.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class WeekTest {

    private val zone = ZoneId.of("Europe/Warsaw")
    private val thursday = LocalDate.of(2026, 11, 19)

    private fun beerAt(dt: LocalDateTime, loggedUnits: Double = 1.97) = DrinkEntry(
        id = dt.hashCode().toLong(),
        timestamp = dt.atZone(zone).toInstant().toEpochMilli(),
        name = "Piwo",
        volumeMl = 500.0,
        abv = 5.0,
        loggedUnits = loggedUnits,
    )

    @Test
    fun `monday of the week`() {
        assertEquals(LocalDate.of(2026, 11, 16), mondayOf(thursday))
        assertEquals(LocalDate.of(2026, 11, 16), mondayOf(LocalDate.of(2026, 11, 16)))
        assertEquals(LocalDate.of(2026, 11, 16), mondayOf(LocalDate.of(2026, 11, 22)))
    }

    @Test
    fun `entries land on their weekday and other weeks are excluded`() {
        val entries = listOf(
            beerAt(LocalDateTime.of(2026, 11, 16, 20, 0)), // Pn
            beerAt(LocalDateTime.of(2026, 11, 22, 23, 59)), // Nd
            beerAt(LocalDateTime.of(2026, 11, 15, 23, 59)), // poprzedni tydzień
            beerAt(LocalDateTime.of(2026, 11, 23, 0, 1)), // następny tydzień
        )
        val week = buildWeek(entries, AppSettings(), zone, today = thursday)
        val oneBeer = toUnits(500.0, 5.0, 10)

        assertEquals(oneBeer, week.days[0].units, 1e-9)
        assertEquals(oneBeer, week.days[6].units, 1e-9)
        assertEquals(2 * oneBeer, week.days.sumOf { it.units }, 1e-9)
        assertEquals(3, week.todayIndex)
    }

    @Test
    fun `units follow the current unit definition, not the one at log time`() {
        // Regresja: wpis zapisany przy 10 g/SU musi zostać przeliczony po zmianie na 8 g/SU.
        val entry = beerAt(LocalDateTime.of(2026, 11, 17, 20, 0), loggedUnits = toUnits(500.0, 5.0, 10))
        val week = buildWeek(listOf(entry), AppSettings(gramsPerUnit = 8), zone, today = thursday)
        assertEquals(toUnits(500.0, 5.0, 8), week.days[1].units, 1e-9)
    }

    @Test
    fun `millis until next midnight`() {
        val now = LocalDateTime.of(2026, 11, 19, 23, 0).atZone(zone)
        assertEquals(3_600_000L + 30_000L, millisUntilNextMidnight(now))
    }
}
