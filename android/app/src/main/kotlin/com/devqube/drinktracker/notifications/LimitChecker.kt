package com.devqube.drinktracker.notifications

import android.content.Context
import com.devqube.drinktracker.data.Fancy
import com.devqube.drinktracker.data.Status
import com.devqube.drinktracker.data.Store
import com.devqube.drinktracker.data.buildWeek
import com.devqube.drinktracker.data.statusDetails

/**
 * Po zalogowaniu trunku sprawdza status tygodnia i wysyła powiadomienie,
 * gdy status wzrósł (ok -> warn -> over). Nie powtarza tego samego statusu.
 */
object LimitChecker {

    suspend fun checkAndNotify(context: Context) {
        val store = Store.get(context)
        val settings = store.snapshotSettings()
        if (!settings.notificationsEnabled) return

        val entries = store.snapshotEntries()
        val week = buildWeek(entries, settings)
        val details = statusDetails(week)
        val key = Notifier.statusKey(details)
        val last = store.lastNotifiedStatus()

        // Reaguj tylko na eskalację względem poprzedniego powiadomienia.
        val rank = mapOf("ok" to 0, "warn" to 1, "over" to 2)
        val escalated = (rank[key] ?: 0) > (rank[last] ?: 0)

        when (details.status) {
            Status.WARN -> if (escalated) {
                val (t, b) = Fancy.warnNotification(details)
                Notifier.notifyLimit(context, t, b, over = false)
            }
            Status.OVER -> if (escalated || last != "over") {
                val (t, b) = Fancy.overNotification(details)
                Notifier.notifyLimit(context, t, b, over = true)
            }
            Status.OK -> Unit
        }
        store.setLastNotifiedStatus(key)
    }
}
