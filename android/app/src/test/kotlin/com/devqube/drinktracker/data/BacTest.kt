package com.devqube.drinktracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class BacTest {

    private val zone = ZoneId.of("Europe/Warsaw")
    private val hour = 3_600_000L
    private val beerGrams = toGrams(500.0, 5.0) // 19,725 g
    private val male80Distribution = 80 * Sex.MALE.r // 54,4

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    private fun bac(drinks: List<Pair<Long, Double>>, now: Long, weight: Double = 80.0, sex: Sex = Sex.MALE) =
        bacAt(drinks, weight, sex, now)

    @Test
    fun `no drinks means sober`() {
        val r = bac(emptyList(), now = 0)
        assertEquals(0.0, r.perMille, 1e-9)
        assertEquals(BacLevel.SOBER, r.level)
    }

    @Test
    fun `single beer right after drinking matches Widmark peak`() {
        val t = at(2026, 11, 19, 20)
        assertEquals(beerGrams / male80Distribution, bac(listOf(t to beerGrams), now = t).perMille, 1e-9)
    }

    @Test
    fun `body eliminates 0_15 per mille per hour`() {
        val t = at(2026, 11, 19, 20)
        val expected = beerGrams / male80Distribution - 0.15
        assertEquals(expected, bac(listOf(t to beerGrams), now = t + hour).perMille, 1e-9)
    }

    @Test
    fun `drinks from before midnight still count after midnight`() {
        // Regresja: wcześniej liczone były tylko trunki z bieżącego dnia, więc o 01:00 BAC wynosił 0.
        val evening = at(2026, 11, 19, 23)
        val drinks = List(4) { evening to beerGrams }
        val r = bac(drinks, now = at(2026, 11, 20, 1))

        val expected = 4 * beerGrams / male80Distribution - 2 * 0.15
        assertEquals(expected, r.perMille, 1e-9)
        assertNotEquals(BacLevel.SOBER, r.level)
    }

    @Test
    fun `elimination between drinks never goes below zero`() {
        // Pierwsze piwo jest w pełni spalone po 5 h; drugie nie może startować od wartości ujemnej.
        val t = at(2026, 11, 19, 12)
        val r = bac(listOf(t to beerGrams, (t + 5 * hour) to beerGrams), now = t + 5 * hour)
        assertEquals(beerGrams / male80Distribution, r.perMille, 1e-9)
    }

    @Test
    fun `drinks logged in the future are ignored`() {
        val now = at(2026, 11, 19, 12)
        assertEquals(0.0, bac(listOf((now + hour) to beerGrams), now = now).perMille, 1e-9)
    }

    @Test
    fun `women reach a higher concentration for the same drink`() {
        val t = at(2026, 11, 19, 20)
        val male = bac(listOf(t to beerGrams), now = t).perMille
        val female = bac(listOf(t to beerGrams), now = t, sex = Sex.FEMALE).perMille
        assertTrue(female > male)
    }

    @Test
    fun `hours to sober follow the elimination rate`() {
        val t = at(2026, 11, 19, 20)
        val r = bac(listOf(t to beerGrams), now = t)
        assertEquals(r.perMille / 0.15, r.hoursToSober, 1e-9)
    }

    @Test
    fun `levels follow thresholds`() {
        assertEquals(BacLevel.SOBER, bacLevel(0.05))
        assertEquals(BacLevel.LIGHT, bacLevel(0.3))
        assertEquals(BacLevel.IMPAIRED, bacLevel(0.5))
        assertEquals(BacLevel.HIGH, bacLevel(1.5))
    }

    @Test
    fun `currentBac uses entry timestamps across midnight`() {
        val evening = at(2026, 11, 19, 23)
        val entries = List(4) { DrinkEntry(it.toLong(), evening, "Piwo", 500.0, 5.0, loggedUnits = 2.0) }
        val r = currentBac(entries, AppSettings(weightKg = 80.0, sex = Sex.MALE), nowMillis = at(2026, 11, 20, 1))
        assertTrue(r.perMille > 1.0)
    }
}
