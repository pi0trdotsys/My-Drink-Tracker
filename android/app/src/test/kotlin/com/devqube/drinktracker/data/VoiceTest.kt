package com.devqube.drinktracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTest {

    private val voices = listOf(ClassicVoice, GrazynaVoice)

    private val allNudges = listOf(
        Nudge.DontDrive(0.6, 4.0),
        Nudge.AfterYesterday,
        Nudge.WeekOver(2.0, 0),
        Nudge.DaysUsedUp,
        Nudge.WeekendBudget(1, 2.0),
        Nudge.WeekendBudget(3, 6.0),
        Nudge.DryStreak(5),
        Nudge.EveningStop(5.0),
        Nudge.EveningDry,
    )

    private fun details(vararg units: Double) = statusDetails(
        WeekData("", units.map { DayEntry("", it) } + List(7 - units.size) { DayEntry("", 0.0) }, 3, 14.0, 4, 0.8)
    )

    // OK, WARN (blisko), tuż pod limitem, dokładnie limit, OVER
    private val weeks = listOf(details(2.0), details(4.0, 4.0, 3.8), details(13.5), details(14.0), details(8.0, 8.0))

    private fun assertProper(text: String) {
        assertTrue("pusty tekst", text.isNotBlank())
        assertFalse("„null” w tekście: $text", text.contains("null"))
        assertFalse("nieuzupełniony szablon: $text", text.contains("\${"))
    }

    @Test
    fun `every voice has text for every nudge`() {
        voices.forEach { v ->
            allNudges.forEach { n ->
                val (title, body) = v.nudge(n)
                assertProper(title); assertProper(body)
            }
        }
    }

    @Test
    fun `every voice covers every status, level and streak`() {
        voices.forEach { v ->
            listOf(v.homeEyebrow, v.homeTitle, v.bacEyebrow, v.logTitle, v.addButton, v.recentTitle).forEach(::assertProper)
            Status.entries.forEach { s ->
                repeat(5) { seed -> assertProper(v.tagline(s, seed)) }
                listOf(0, 1, 2, 10).forEach { streak -> assertProper(v.widgetLine(s, streak)) }
            }
            BacLevel.entries.forEach { l -> v.bacLabel(l).toList().forEach(::assertProper) }
            listOf(0, 1, 7).forEach { assertProper(v.streakLine(it)) }
            weeks.forEach { d ->
                v.planMessage(d, 2.0).toList().forEach(::assertProper)
                v.weeklyMessage(d, 2).toList().forEach(::assertProper)
                if (d.status != Status.OK) v.alert(d).toList().forEach(::assertProper)
            }
        }
    }

    @Test
    fun `grazyna sounds different from classic`() {
        assertNotEquals(ClassicVoice.homeTitle, GrazynaVoice.homeTitle)
        allNudges.forEach { assertNotEquals(ClassicVoice.nudge(it), GrazynaVoice.nudge(it)) }
    }

    @Test
    fun `grazyna forbids drinking today when the week is over`() {
        val (title, _) = GrazynaVoice.nudge(Nudge.WeekOver(2.0, 0))
        assertEquals("Dzisiaj nie waż się pić! 🍳", title)
        val (_, body) = GrazynaVoice.planMessage(details(8.0, 8.0), 2.0)
        assertTrue(body, body.contains("ani kropli"))
    }

    @Test
    fun `beer plural is correct in grazyna messages`() {
        assertTrue(GrazynaVoice.nudge(Nudge.WeekendBudget(1, 2.0)).second.contains("max 1 piwo "))
        assertTrue(GrazynaVoice.nudge(Nudge.WeekendBudget(3, 6.0)).second.contains("max 3 piwa "))
        assertTrue(GrazynaVoice.planMessage(details(2.0), 2.0).second.contains("Max 6 piw "))
    }

    @Test
    fun `theme picks its voice`() {
        assertEquals(ClassicVoice, voiceFor(AppTheme.CLASSIC))
        assertEquals(GrazynaVoice, voiceFor(AppTheme.GRAZYNA))
    }
}
