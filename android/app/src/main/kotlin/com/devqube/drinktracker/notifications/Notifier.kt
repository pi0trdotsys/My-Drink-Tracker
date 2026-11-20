package com.devqube.drinktracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.devqube.drinktracker.MainActivity
import com.devqube.drinktracker.R
import com.devqube.drinktracker.data.AppTheme
import com.devqube.drinktracker.data.Status

object Notifier {

    /** Rodzaj powiadomienia – razem z motywem wyznacza kanał (a więc i dźwięk). */
    enum class Kind { LIMITS, PLAN, WEEKLY, DAILY }

    /**
     * Kanał systemowy. Android przypisuje dźwięk do kanału na stałe, więc każdy motyw ma
     * własny komplet kanałów (ID są wersjonowane – zmiana dźwięku wymaga nowego ID).
     */
    private data class Channel(
        val id: String,
        val name: String,
        val description: String,
        val importance: Int,
        val sound: Int,
        val vibrate: Boolean = false,
    )

    private fun channel(theme: AppTheme, kind: Kind): Channel = when (theme) {
        AppTheme.CLASSIC -> when (kind) {
            Kind.LIMITS -> Channel("limits_v2", "Ostrzeżenia o limicie", "Gdy zbliżasz się do limitu lub go przekraczasz.", NotificationManager.IMPORTANCE_HIGH, R.raw.clink_cheers, vibrate = true)
            Kind.PLAN -> Channel("plan_v2", "Plan tygodnia", "Ile jeszcze możesz wypić w tym tygodniu.", NotificationManager.IMPORTANCE_DEFAULT, R.raw.clink_cheers)
            Kind.WEEKLY -> Channel("weekly_v2", "Podsumowanie tygodnia", "Cotygodniowe podsumowanie spożycia alkoholu.", NotificationManager.IMPORTANCE_DEFAULT, R.raw.clink_cheers)
            Kind.DAILY -> Channel("daily_v1", "Przypomnienia w ciągu dnia", "Sensowne przypomnienia: południe, popołudnie, wieczór.", NotificationManager.IMPORTANCE_DEFAULT, R.raw.clink_cheers)
        }
        AppTheme.GRAZYNA -> when (kind) {
            Kind.LIMITS -> Channel("grazyna_limits_v1", "Grażyna: awantura o limit", "Patelnia, gdy zbliżasz się do limitu lub go przekraczasz.", NotificationManager.IMPORTANCE_HIGH, R.raw.patelnia, vibrate = true)
            Kind.PLAN -> Channel("grazyna_plan_v1", "Grażyna: ile jeszcze wolno", "Kapciem po głowie: ile zostało do limitu.", NotificationManager.IMPORTANCE_DEFAULT, R.raw.kapec)
            Kind.WEEKLY -> Channel("grazyna_weekly_v1", "Grażyna: rachunek tygodnia", "Cotygodniowe rozliczenie z patelnią w tle.", NotificationManager.IMPORTANCE_DEFAULT, R.raw.patelnia)
            Kind.DAILY -> Channel("grazyna_daily_v1", "Grażyna: gderanie w ciągu dnia", "„Dzisiaj nie waż się pić!” i inne dobre rady.", NotificationManager.IMPORTANCE_DEFAULT, R.raw.kapec)
        }
    }

    private const val ID_STATUS = 1001
    private const val ID_WEEKLY = 1002
    private const val ID_DAILY = 1003
    private const val ID_UPDATE = 1004
    private const val CHANNEL_UPDATES = "updates_v1"

    private fun soundUri(context: Context, res: Int): Uri = Uri.parse("android.resource://${context.packageName}/$res")

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)

        // Sprzątanie starych kanałów bez własnego dźwięku.
        listOf("limits", "weekly").forEach { runCatching { nm.deleteNotificationChannel(it) } }

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_UPDATES, "Aktualizacje", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Informacja, że na GitHubie czeka nowa wersja Drink Trackera."
            }
        )

        AppTheme.entries.forEach { theme ->
            Kind.entries.forEach { kind ->
                val c = channel(theme, kind)
                nm.createNotificationChannel(
                    NotificationChannel(c.id, c.name, c.importance).apply {
                        description = c.description
                        setSound(soundUri(context, c.sound), attrs)
                        enableVibration(c.vibrate)
                    }
                )
            }
        }
    }

    private fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun accent(status: Status): Int = when (status) {
        Status.OK -> 0xFF45C08A.toInt()
        Status.WARN -> 0xFFE6B646.toInt()
        Status.OVER -> 0xFFDE5140.toInt()
    }

    private fun post(
        context: Context,
        id: Int,
        theme: AppTheme,
        kind: Kind,
        title: String,
        body: String,
        color: Int,
        alert: Boolean = true,
    ) {
        if (!canPost(context)) return
        val c = channel(theme, kind)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, c.id)
            .setSmallIcon(if (theme == AppTheme.GRAZYNA) R.drawable.ic_notification_pan else R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (c.importance >= NotificationManager.IMPORTANCE_HIGH) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setColor(color)
            .setSound(soundUri(context, c.sound))
            .setContentIntent(open)
            // Bez dźwięku, wibracji i heads-upu – treść aktualizuje się po cichu.
            .setSilent(!alert)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }

    /** Plan/status po wpisie – kanał zależny od tego, jak blisko limitu jesteś. */
    fun notifyStatus(
        context: Context,
        title: String,
        body: String,
        status: Status,
        alert: Boolean,
        theme: AppTheme,
        headsUp: Boolean = status != Status.OK,
    ) =
        // Kanał LIMITS (wysoka ważność) wyskakuje nad innymi aplikacjami – tylko przy eskalacji ostrzeżenia.
        // Zwykłe wpisy idą kanałem PLAN: grają ten sam dźwięk motywu, ale bez wyskakującego okienka.
        post(context, ID_STATUS, theme, if (headsUp && status != Status.OK) Kind.LIMITS else Kind.PLAN, title, body, accent(status), alert)

    fun notifyWeekly(context: Context, title: String, body: String, theme: AppTheme) =
        post(context, ID_WEEKLY, theme, Kind.WEEKLY, title, body, 0xFFE3A54B.toInt())

    fun notifyNudge(context: Context, title: String, body: String, theme: AppTheme) =
        post(context, ID_DAILY, theme, Kind.DAILY, title, body, if (theme == AppTheme.GRAZYNA) 0xFFFF4FA3.toInt() else 0xFFE3A54B.toInt())

    /** „Dostępna wersja X” – cicho, raz na wersję; kliknięcie otwiera kartę aktualizacji w Ustawieniach. */
    fun notifyUpdate(context: Context, version: String) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context, 1, MainActivity.updatesIntent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Dostępna aktualizacja Drink Trackera")
            .setContentText("Wersja $version czeka – dotknij, by pobrać i zainstalować.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(0xFFE3A54B.toInt())
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_UPDATE, n)
    }
}
