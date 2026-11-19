package com.devqube.drinktracker.notifications

import com.devqube.drinktracker.data.Status
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AlertPolicyTest {

    private val monday = LocalDate.of(2026, 11, 16)

    @Test
    fun `first notification ever plays sound`() {
        assertTrue(AlertPolicy.shouldAlert(null, monday, Status.OK))
    }

    @Test
    fun `first notification of a new week plays sound even if status dropped`() {
        val lastWeek = AlertPolicy.key(monday.minusWeeks(1), Status.OVER)
        assertTrue(AlertPolicy.shouldAlert(lastWeek, monday, Status.OK))
    }

    @Test
    fun `same status in the same week updates silently`() {
        val last = AlertPolicy.key(monday, Status.OK)
        assertFalse(AlertPolicy.shouldAlert(last, monday, Status.OK))
    }

    @Test
    fun `escalation plays sound`() {
        assertTrue(AlertPolicy.shouldAlert(AlertPolicy.key(monday, Status.OK), monday, Status.WARN))
        assertTrue(AlertPolicy.shouldAlert(AlertPolicy.key(monday, Status.WARN), monday, Status.OVER))
    }

    @Test
    fun `de-escalation is silent`() {
        assertFalse(AlertPolicy.shouldAlert(AlertPolicy.key(monday, Status.OVER), monday, Status.WARN))
    }

    @Test
    fun `legacy or corrupted value is treated as no history`() {
        // Starsze wersje zapisywały samo "warn" – po aktualizacji pierwszy wpis powinien zagrać.
        assertTrue(AlertPolicy.shouldAlert("warn", monday, Status.OK))
        assertTrue(AlertPolicy.shouldAlert("2026-11-16|BOGUS", monday, Status.OK))
    }
}
