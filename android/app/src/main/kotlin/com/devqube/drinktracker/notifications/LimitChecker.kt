package com.devqube.drinktracker.notifications

import android.content.Context
import com.devqube.drinktracker.data.Fancy
import com.devqube.drinktracker.data.Store
import com.devqube.drinktracker.data.buildWeek
import com.devqube.drinktracker.data.statusDetails
import com.devqube.drinktracker.data.toUnits

/**
 * Po zalogowaniu (również wstecznym) trunku wysyła powiadomienie "planujące":
 * ile jeszcze możesz wypić w tym tygodniu, wyrażone w konkretnych piwach.
 * Ton i priorytet zależą od statusu (spokojny plan / ostrzeżenie / stop).
 */
object LimitChecker {

    suspend fun checkAndNotify(context: Context) {
        val store = Store.get(context)
        val settings = store.snapshotSettings()
        if (!settings.notificationsEnabled) return

        val entries = store.snapshotEntries()
        val week = buildWeek(entries, settings)
        val details = statusDetails(week)

        // Referencja: jedno "standardowe" piwo (0,5 l, 5%) w bieżącej definicji jednostki.
        val oneBeerUnits = toUnits(500.0, 5.0, settings.gramsPerUnit)
        val (title, body) = Fancy.planMessage(details, oneBeerUnits)

        Notifier.notifyStatus(context, title, body, details.status)
        store.setLastNotifiedStatus(Notifier.statusKey(details))
    }
}
