package com.devqube.drinktracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrazynaLexiconTest {

    private fun details(vararg units: Double) = statusDetails(
        WeekData("", units.map { DayEntry("", it) } + List(7 - units.size) { DayEntry("", 0.0) }, 3, 14.0, 4, 0.8)
    )

    @Test
    fun `wrath meter takes the bigger of units and days ratios`() {
        assertEquals(25, wrathPercent(details(2.0)))                // 1/4 dni > 2/14 SU
        assertEquals(60, wrathPercent(details(4.2, 4.2)))           // 8,4/14 SU > 2/4 dni
        assertEquals(99, wrathPercent(details(0.5, 0.5, 0.5, 0.5))) // 4/4 dni – pełne, ale jeszcze nie przekroczone
        assertEquals(100, wrathPercent(details(8.0, 8.0)))          // przekroczony limit SU
        assertEquals(0, wrathPercent(details()))
    }

    @Test
    fun `wrath labels`() {
        assertEquals("spokojna", wrathLabel(0))
        assertEquals("łypie krzywo", wrathLabel(25))
        assertEquals("szuka wałka", wrathLabel(60))
        assertEquals("patelnia w dłoni", wrathLabel(99))
        assertEquals("dzwoni do teściowej", wrathLabel(100))
    }

    @Test
    fun `tool escalates with status`() {
        assertEquals(Tool.KAPEC, toolFor(Status.OK))
        assertEquals(Tool.PATELNIA, toolFor(Status.WARN))
        assertEquals(Tool.WALEK, toolFor(Status.OVER))
    }

    @Test
    fun `moment of day`() {
        assertEquals(Moment.HANGOVER, momentOf(9, unitsToday = 0.0, unitsYesterday = 4.0))
        assertEquals(Moment.NONE, momentOf(9, unitsToday = 0.0, unitsYesterday = 0.0))
        assertEquals(Moment.TEMPTATION, momentOf(17, unitsToday = 0.0, unitsYesterday = 0.0))
        assertEquals(Moment.NONE, momentOf(17, unitsToday = 2.0, unitsYesterday = 0.0))
        assertEquals(Moment.EVENING_DRINKING, momentOf(21, unitsToday = 2.0, unitsYesterday = 0.0))
        assertEquals(Moment.NONE, momentOf(21, unitsToday = 0.0, unitsYesterday = 0.0))
        assertEquals(Moment.NONE, momentOf(3, unitsToday = 5.0, unitsYesterday = 5.0))
    }

    @Test
    fun `words are deterministic per seed and come from the lexicon`() {
        val a = Words(Vulgarity.MAX, 42)
        val b = Words(Vulgarity.MAX, 42)
        assertEquals(a.nick(), b.nick())
        assertEquals(a.curse(1), b.curse(1))
        assertTrue(a.nick() in GrazynaLexicon.nicknames)
        assertTrue(Words(Vulgarity.SHARP, 7).curse() in GrazynaLexicon.cursesSharp)
        assertTrue(Words(Vulgarity.SHARP, 7).drunk() in GrazynaLexicon.drunkSharp)
    }

    @Test
    fun `sharp lexicon has no hard words`() {
        (GrazynaLexicon.cursesSharp + GrazynaLexicon.drunkSharp + GrazynaLexicon.nicknames).forEach { w ->
            GrazynaLexicon.hardRoots.forEach { root -> assertTrue("„$root” w „$w”", !w.lowercase().contains(root)) }
        }
    }
}
