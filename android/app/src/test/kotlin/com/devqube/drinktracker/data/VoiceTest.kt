package com.devqube.drinktracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTest {

    private val seeds = 0..120

    private fun grazyna(level: Vulgarity, seed: Int) = GrazynaVoice(level, clock = { seed * 60_000L })

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

    // OK, WARN (blisko), tuż pod limitem, dokładnie limit, OVER (SU), OVER (dni)
    private val weeks = listOf(
        details(2.0), details(4.0, 4.0, 3.8), details(13.5), details(14.0), details(8.0, 8.0),
        details(0.5, 0.5, 0.5, 0.5, 0.5),
    )

    /** Wszystkie teksty, jakie dany głos potrafi wyprodukować przy danym ziarnie. */
    private fun allTexts(v: Voice, seed: Int): List<String> = buildList {
        addAll(listOf(v.homeEyebrow, v.homeTitle, v.bacEyebrow, v.logTitle, v.addButton, v.recentTitle, v.weekHeader, v.quickAdd))
        v.speechTitle?.let(::add)
        Status.entries.forEach { s ->
            add(v.statusLabel(s))
            add(v.tagline(s, seed))
            Moment.entries.forEach { m ->
                listOf(0, 3).forEach { streak ->
                    listOf(0, 1, 3).forEach { beers ->
                        val wm = WidgetMoment(s, streak, beers, m, seed)
                        add(v.widgetLine(wm)); add(v.widgetSpeech(wm))
                    }
                }
            }
        }
        add(v.daysLabel(2, 4, long = true)); add(v.daysLabel(2, 4, long = false))
        BacLevel.entries.forEach { addAll(v.bacLabel(it).toList()) }
        listOf(0, 1, 7).forEach { add(v.streakLine(it)) }
        weeks.forEach { d ->
            addAll(v.planMessage(d, 2.0).toList())
            addAll(v.weeklyMessage(d, 2).toList())
            if (d.status != Status.OK) addAll(v.alert(d).toList())
            v.meter(d)?.let { add(it.mood) }
        }
        allNudges.forEach { addAll(v.nudge(it).toList()) }
    }

    private fun assertProper(text: String) {
        assertTrue("pusty tekst", text.isNotBlank())
        assertFalse("„null” w tekście: $text", text.contains("null"))
        assertFalse("nieuzupełniony szablon: $text", text.contains("\${"))
    }

    @Test
    fun `every voice produces proper text everywhere`() {
        seeds.step(10).forEach { seed ->
            listOf(ClassicVoice, grazyna(Vulgarity.SHARP, seed), grazyna(Vulgarity.MAX, seed))
                .forEach { v -> allTexts(v, seed).forEach(::assertProper) }
        }
    }

    @Test
    fun `sharp level never uses the heaviest words`() {
        seeds.forEach { seed ->
            allTexts(grazyna(Vulgarity.SHARP, seed), seed).forEach { text ->
                GrazynaLexicon.hardRoots.forEach { root ->
                    assertFalse("„$root” w poziomie Ostry: $text", text.lowercase().contains(root))
                }
            }
        }
    }

    @Test
    fun `max level really goes without brakes`() {
        val all = seeds.flatMap { allTexts(grazyna(Vulgarity.MAX, it), it) }.joinToString(" ").lowercase()
        GrazynaLexicon.hardRoots.forEach { root -> assertTrue("brak „$root” w poziomie MAX", all.contains(root)) }
    }

    @Test
    fun `grazyna uses many nicknames, not only pijus`() {
        val all = seeds.flatMap { allTexts(grazyna(Vulgarity.MAX, it), it) }.joinToString(" ")
        val used = GrazynaLexicon.nicknames.filter { all.contains(it) }
        assertTrue("użyte ksywki: $used", used.size >= 20)
    }

    @Test
    fun `grazyna sounds different from classic`() {
        val g = grazyna(Vulgarity.MAX, 1)
        assertNotEquals(ClassicVoice.homeTitle, g.homeTitle)
        assertNotEquals(ClassicVoice.weekHeader, g.weekHeader)
        allNudges.forEach { assertNotEquals(ClassicVoice.nudge(it), g.nudge(it)) }
    }

    @Test
    fun `grazyna forbids drinking today when the week is over`() {
        val g = grazyna(Vulgarity.MAX, 3)
        assertEquals("Dzisiaj nie waż się pić! 🍳", g.nudge(Nudge.WeekOver(2.0, 0)).first)
        assertTrue(g.planMessage(details(8.0, 8.0), 2.0).second.contains("ani kropli"))
    }

    @Test
    fun `beer plural is correct in grazyna messages`() {
        val g = grazyna(Vulgarity.MAX, 5)
        assertTrue(g.nudge(Nudge.WeekendBudget(1, 2.0)).second.contains("max 1 piwo "))
        assertTrue(g.nudge(Nudge.WeekendBudget(3, 6.0)).second.contains("max 3 piwa "))
        assertTrue(g.planMessage(details(2.0), 2.0).second.contains("Max 6 piw "))
    }

    @Test
    fun `classic has no meter, tool or speech title`() {
        assertEquals(null, ClassicVoice.meter(details(4.0)))
        assertEquals(null, ClassicVoice.tool(Status.OVER))
        assertEquals(null, ClassicVoice.speechTitle)
    }

    @Test
    fun `theme picks its voice and grazyna defaults to max vulgarity`() {
        assertEquals(ClassicVoice, voiceFor(AppTheme.CLASSIC))
        assertEquals(Vulgarity.MAX, (voiceFor(AppTheme.GRAZYNA) as GrazynaVoice).level)
        assertEquals(Vulgarity.SHARP, (voiceFor(AppTheme.GRAZYNA, Vulgarity.SHARP) as GrazynaVoice).level)
        assertEquals(Vulgarity.MAX, AppSettings().vulgarity)
    }
}
