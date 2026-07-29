package com.devqube.drinktracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.devqube.drinktracker.R
import com.devqube.drinktracker.data.Status
import com.devqube.drinktracker.data.StatusDetails

object Notifier {

    const val CHANNEL_LIMITS = "limits"
    const val CHANNEL_WEEKLY = "weekly"

    private const val ID_LIMIT = 1001
    private const val ID_WEEKLY = 1002

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_LIMITS, "Ostrzeżenia o limicie", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Powiadomienia gdy zbliżasz się do limitu lub go przekraczasz."
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_WEEKLY, "Podsumowanie tygodnia", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Cotygodniowe podsumowanie spożycia alkoholu."
            }
        )
    }

    private fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun notifyLimit(context: Context, title: String, body: String, over: Boolean) {
        if (!canPost(context)) return
        val n = NotificationCompat.Builder(context, CHANNEL_LIMITS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(if (over) 0xFFDE5140.toInt() else 0xFFE6B646.toInt())
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_LIMIT, n)
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
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_WEEKLY, n)
    }

    /** Zwraca klucz statusu do porównań (unikanie duplikatów powiadomień). */
    fun statusKey(details: StatusDetails): String = when (details.status) {
        Status.OK -> "ok"
        Status.WARN -> "warn"
        Status.OVER -> "over"
    }
}
