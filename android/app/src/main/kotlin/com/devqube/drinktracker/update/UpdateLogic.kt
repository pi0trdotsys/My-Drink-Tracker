package com.devqube.drinktracker.update

import org.json.JSONObject
import java.util.Locale

/**
 * Czysta logika aktualizacji z GitHuba (bez Androida, testowana na JVM) – ta sama co w Kropi:
 * kanałem jest ostatnie wydanie repozytorium (nie szkic, nie pre-release), a pobieramy wyłącznie
 * APK z wydań tego repozytorium.
 */
object UpdateLogic {
    const val REPO = "pi0trdotsys/My-Drink-Tracker"
    const val RELEASES_API = "https://api.github.com/repos/$REPO/releases/latest"
    const val ALLOWED_PREFIX = "https://github.com/$REPO/releases/download/"

    data class Release(
        /** „1.3.0” – tag bez początkowego „v”. */
        val version: String,
        val title: String,
        val notes: String,
        val apkUrl: String,
        val apkName: String,
        val size: Long,
        /** SHA-256 pliku (hex małymi literami) podawany przez GitHuba albo „”. */
        val sha256: String,
    )

    /** GitHub zwraca nazwę repo w różnej wielkości liter (my-drink-tracker / My-Drink-Tracker). */
    fun allowedUrl(url: String?): Boolean {
        if (url == null) return false
        val u = url.lowercase(Locale.ROOT)
        return u.startsWith(ALLOWED_PREFIX.lowercase(Locale.ROOT)) && u.endsWith(".apk") && !url.contains("..")
    }

    /** „sha256:abc…” albo „abc…” → hex małymi literami, albo „” gdy brak/nie SHA-256. */
    fun normalizeSha(digest: String?): String {
        var d = digest?.trim()?.lowercase(Locale.ROOT) ?: return ""
        if (d.startsWith("sha256:")) d = d.removePrefix("sha256:")
        return if (Regex("[0-9a-f]{64}").matches(d)) d else ""
    }

    /** Porównanie numeryczne wersji kropkowanych: -1 / 0 / 1 („1.10.0” > „1.9.3”). */
    fun compare(a: String, b: String): Int {
        val pa = a.removePrefix("v").removePrefix("V").split(".")
        val pb = b.removePrefix("v").removePrefix("V").split(".")
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrNull(i)?.trim()?.toIntOrNull() ?: 0
            val y = pb.getOrNull(i)?.trim()?.toIntOrNull() ?: 0
            if (x != y) return if (x > y) 1 else -1
        }
        return 0
    }

    /** Wydanie jako aktualizacja albo null (szkic, pre-release, dziwny tag, brak dozwolonego APK). */
    fun parseRelease(r: JSONObject?): Release? {
        if (r == null || r.optBoolean("draft") || r.optBoolean("prerelease")) return null
        val version = r.optString("tag_name", "").removePrefix("v").removePrefix("V")
        if (!Regex("\\d+(\\.\\d+)*").matches(version)) return null
        val assets = r.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.optJSONObject(i) ?: continue
            val url = a.optString("browser_download_url")
            if (!allowedUrl(url)) continue
            return Release(
                version = version,
                title = r.optString("name").ifBlank { "Drink Tracker $version" },
                notes = r.optString("body"),
                apkUrl = url,
                apkName = a.optString("name", "drinktracker.apk"),
                size = a.optLong("size", 0L),
                sha256 = normalizeSha(a.optString("digest", "")),
            )
        }
        return null
    }

    /**
     * Notatki wydania z GitHuba są w Markdownie – na ekranie pokazujemy zwykły tekst:
     * bez znaczników nagłówków, pogrubień i cytatów, z punktorami „•” zamiast „- ”.
     */
    fun plainNotes(markdown: String): String =
        markdown.replace("\r\n", "\n").lines().joinToString("\n") { raw ->
            var line = raw.trimEnd()
            line = line.replace(Regex("^\\s{0,3}#{1,6}\\s*"), "")      // ## Nagłówek
            line = line.replace(Regex("^\\s*>\\s?"), "")              // > cytat
            line = line.replace(Regex("^(\\s*)[-*+]\\s+"), "$1• ")     // - punkt
            line = line.replace("**", "").replace("__", "")       // pogrubienia
            line.replace(Regex("`([^`]*)`"), "$1")                 // `kod`
        }.replace(Regex("\n{3,}"), "\n\n").trim()
}

