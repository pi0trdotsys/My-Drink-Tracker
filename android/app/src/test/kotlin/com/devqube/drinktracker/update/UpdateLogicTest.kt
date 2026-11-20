package com.devqube.drinktracker.update

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateLogicTest {
    private val sha = "a".repeat(64)

    private fun release(
        tag: String = "v1.3.0",
        draft: Boolean = false,
        pre: Boolean = false,
        url: String = "https://github.com/pi0trdotsys/My-Drink-Tracker/releases/download/v1.3.0/DrinkTracker-v1.3.0.apk",
    ) = JSONObject(
        """{"tag_name":"$tag","name":"Drink Tracker v1.3.0 (Android)","body":"notes","draft":$draft,"prerelease":$pre,
           "assets":[{"name":"DrinkTracker-v1.3.0.apk","browser_download_url":"$url","size":18000000,"digest":"sha256:$sha"}]}""",
    )

    @Test
    fun `parses latest release`() {
        val r = UpdateLogic.parseRelease(release())
        assertNotNull(r)
        assertEquals("1.3.0", r!!.version)
        assertEquals("Drink Tracker v1.3.0 (Android)", r.title)
        assertEquals(sha, r.sha256)
        assertEquals(18_000_000L, r.size)
    }

    @Test
    fun `skips drafts, pre-releases and odd tags`() {
        assertNull(UpdateLogic.parseRelease(release(draft = true)))
        assertNull(UpdateLogic.parseRelease(release(pre = true)))
        assertNull(UpdateLogic.parseRelease(release(tag = "nightly")))
        assertNull(UpdateLogic.parseRelease(null))
    }

    @Test
    fun `only accepts apks from this repo, case-insensitively`() {
        assertTrue(UpdateLogic.allowedUrl("https://github.com/pi0trdotsys/my-drink-tracker/releases/download/v1/a.apk"))
        assertFalse(UpdateLogic.allowedUrl("https://github.com/pi0trdotsys/Hydration-Buddy/releases/download/v1/a.apk"))
        assertFalse(UpdateLogic.allowedUrl("https://github.com/pi0trdotsys/My-Drink-Tracker/releases/download/v1/a.zip"))
        assertFalse(UpdateLogic.allowedUrl("https://github.com/pi0trdotsys/My-Drink-Tracker/releases/download/../x.apk"))
        assertNull(UpdateLogic.parseRelease(release(url = "https://evil.example/drinktracker.apk")))
    }

    @Test
    fun `compares versions numerically`() {
        assertEquals(1, UpdateLogic.compare("1.10.0", "1.9.3"))
        assertEquals(0, UpdateLogic.compare("v1.3", "1.3.0"))
        assertEquals(-1, UpdateLogic.compare("1.2.1", "1.3.0"))
    }

    @Test
    fun `normalizes digests`() {
        assertEquals(sha, UpdateLogic.normalizeSha("sha256:${sha.uppercase()}"))
        assertEquals("", UpdateLogic.normalizeSha("sha1:abc"))
        assertEquals("", UpdateLogic.normalizeSha(null))
    }

    @Test
    fun `release notes are shown as plain text`() {
        val md = "Wydanie poprawkowe.\r\n\r\n## Zmiany\n- 🧩 **Pełna zawartość** w `2×2`\n  * podpunkt\n\n\n\n> 🔞 Uwaga"
        assertEquals(
            "Wydanie poprawkowe.\n\nZmiany\n• 🧩 Pełna zawartość w 2×2\n  • podpunkt\n\n🔞 Uwaga",
            UpdateLogic.plainNotes(md),
        )
    }
}

