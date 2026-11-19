package com.devqube.drinktracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.devqube.drinktracker.R
import com.devqube.drinktracker.data.Status

object Notifier {

    // Wersjonowane ID kanałów – zmiana dźwięku wymaga nowego kanału.
    const val CHANNEL_LIMITS = "limits_v2"
    const val CHANNEL_PLAN = "plan_v2"
    const val CHANNEL_WEEKLY = "weekly_v2"

    // Jedno ID dla powiadomień o statusie/planie, aby aktualizowały się, a nie mnożyły.
    private const val ID_STATUS = 1001
    private const val ID_WEEKLY = 1002

    private fun soundUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.clink_cheers}")

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)

        // Sprzątanie starych kanałów bez własnego dźwięku.
        listOf("limits", "weekly").forEach { runCatching { nm.deleteNotificationChannel(it) } }

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sound = soundUri(context)

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_LIMITS, "Ostrzeżenia o limicie", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Gdy zbliżasz się do limitu lub go przekraczasz."
                setSound(sound, attrs)
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PLAN, "Plan tygodnia", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Ile jeszcze możesz wypić w tym tygodniu."
                setSound(sound, attrs)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_WEEKLY, "Podsumowanie tygodnia", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Cotygodniowe podsumowanie spożycia alkoholu."
                setSound(sound, attrs)
            }
        )
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

    /** Powiadomienie o statusie/planie – kanał zależny od tego, jak blisko limitu jesteś. */
    fun notifyStatus(context: Context, title: String, body: String, status: Status, alert: Boolean) {
        if (!canPost(context)) return
        val channel = if (status == Status.OK) CHANNEL_PLAN else CHANNEL_LIMITS
        val priority = if (status == Status.OK) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setColor(accent(status))
            .setSound(soundUri(context))
            // Bez dźwięku, wibracji i heads-upu – treść aktualizuje się po cichu.
            .setSilent(!alert)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_STATUS, n)
    }

    fun notifyWeekly(context: Context, title: String, body: String) {
        if (!canPost(context)) return
        val n = NotificationCompat.Builder(context, CHANNEL_WEEKLY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(0xFFE3A54B.toInt())
            .setSound(soundUri(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_WEEKLY, n)
    }
}
