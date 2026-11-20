package com.devqube.drinktracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.net.URLDecoder

class KropiSyncTest {

    private fun entry(ml: Double, name: String = "Piwo", ts: Long = 1_795_000_000_000L) =
        DrinkEntry(1, ts, name, ml, 5.0, 2.0)

    /** Rozbiór adresu tak, jak robi to Kropi (NfcTag.parse): URI + URLDecoder. */
    private fun query(uri: String): Map<String, String> =
        URI(uri).rawQuery.split("&").associate { p ->
            val i = p.indexOf('=')
            URLDecoder.decode(p.substring(0, i), "UTF-8") to URLDecoder.decode(p.substring(i + 1), "UTF-8")
        }

    @Test
    fun `uri carries amount, alcohol type, time and drink name`() {
        val uri = KropiSync.uri(entry(500.0, ts = 1_795_000_000_000L))
        assertTrue(uri.startsWith("kropi://add?"))
        val q = query(uri)
        assertEquals("500", q["ml"])
        assertEquals("ALCOHOL", q["drink"])
        assertEquals("1795000000000", q["at"])
        assertEquals("Piwo", q["note"])
    }

    @Test
    fun `note with spaces and polish letters survives encoding`() {
        assertEquals("Kieliszek wódki & cola", query(KropiSync.uri(entry(50.0, "Kieliszek wódki & cola")))["note"])
    }

    @Test
    fun `amount is fitted to the range accepted by Kropi`() {
        assertEquals("10", query(KropiSync.uri(entry(5.0)))["ml"])
        assertEquals("1500", query(KropiSync.uri(entry(3000.0)))["ml"])
        assertEquals("333", query(KropiSync.uri(entry(332.6)))["ml"])
    }

    @Test
    fun `backdated means more than five minutes ago`() {
        val now = 1_795_000_000_000L
        assertFalse(KropiSync.isBackdated(entry(500.0, ts = now - 60_000), now))
        assertTrue(KropiSync.isBackdated(entry(500.0, ts = now - 3_600_000), now))
    }

    @Test
    fun `decision depends on installation and version`() {
        assertEquals(KropiSync.Result.NotInstalled, KropiSync.decide(installedVersion = null, backdated = false))
        assertEquals(KropiSync.Result.NeedsUpdateForBackdated, KropiSync.decide(installedVersion = 14, backdated = true))
        assertNull(KropiSync.decide(installedVersion = 14, backdated = false))   // „teraz” działa też w starszej Kropi
        assertNull(KropiSync.decide(installedVersion = 15, backdated = true))
    }
}
