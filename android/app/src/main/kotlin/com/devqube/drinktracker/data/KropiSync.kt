package com.devqube.drinktracker.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * Integracja z Kropi (aplikacja do nawodnienia). Alkohol zalogowany w Drink Trackerze trafia też do Kropi,
 * żeby nie wpisywać go dwa razy. Kropi liczy alkohol jako 0% nawodnienia i podnosi dzienny cel wody
 * (połowa wypitego alkoholu, maks. 600 ml) – więc wpis ma tam realny skutek.
 *
 * Kanał: publiczny adres Kropi `kropi://add?ml=…&drink=ALCOHOL` obsługiwany przez jej bezokienkową
 * „trampolinę” (ta sama, której używają skróty i tagi NFC). Od Kropi 1.14.0 adres przyjmuje też
 * `at` (czas wpisu, ms) i `note` (nazwa trunku) – dzięki temu wpis wsteczny trafia na właściwy dzień.
 */
object KropiSync {
    const val PACKAGE = "com.kropi.hydration"

    /** Kropi 1.14.0 (versionCode 15) jako pierwsza obsługuje parametry `at` i `note`. */
    const val MIN_VERSION_WITH_TIME = 15L

    /** Zakres ilości akceptowany przez Kropi (tam przycinany; tu tylko dopasowujemy). */
    private const val MIN_ML = 10
    private const val MAX_ML = 1500

    /** Wpis wsteczny różni się od „teraz” o więcej niż tyle – wtedy potrzebny jest parametr `at`. */
    private const val BACKDATED_AFTER_MS = 5 * 60_000L

    fun uri(entry: DrinkEntry): String {
        val ml = entry.volumeMl.roundToInt().coerceIn(MIN_ML, MAX_ML)
        val note = URLEncoder.encode(entry.name.trim().take(60), "UTF-8")
        return "kropi://add?ml=$ml&drink=ALCOHOL&at=${entry.timestamp}&note=$note"
    }

    fun isBackdated(entry: DrinkEntry, nowMillis: Long = System.currentTimeMillis()) =
        nowMillis - entry.timestamp > BACKDATED_AFTER_MS

    /** Wersja Kropi albo `null`, gdy nie jest zainstalowana. */
    fun installedVersion(context: Context): Long? = runCatching {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(PACKAGE, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") context.packageManager.getPackageInfo(PACKAGE, 0)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    }.getOrNull()

    sealed interface Result {
        data object Sent : Result
        data object NotInstalled : Result

        /** Starsza Kropi zapisałaby wpis wsteczny z dzisiejszą datą – lepiej go pominąć. */
        data object NeedsUpdateForBackdated : Result
        data object Failed : Result
    }

    /** Decyzja bez skutków ubocznych – testowalna. */
    fun decide(installedVersion: Long?, backdated: Boolean): Result? = when {
        installedVersion == null -> Result.NotInstalled
        backdated && installedVersion < MIN_VERSION_WITH_TIME -> Result.NeedsUpdateForBackdated
        else -> null // można wysyłać
    }

    /** Wysyła wpis do Kropi. Wołać z kontekstu aktywności (aplikacja jest wtedy na pierwszym planie). */
    fun send(context: Context, entry: DrinkEntry): Result {
        decide(installedVersion(context), isBackdated(entry))?.let { return it }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri(entry)))
            .setPackage(PACKAGE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            Result.Sent
        } catch (e: ActivityNotFoundException) {
            Result.NotInstalled
        } catch (e: SecurityException) {
            Result.Failed
        }
    }
}
