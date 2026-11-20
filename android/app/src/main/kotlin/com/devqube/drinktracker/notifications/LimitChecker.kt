package com.devqube.drinktracker.notifications

import android.content.Context
import com.devqube.drinktracker.data.voiceFor
import com.devqube.drinktracker.data.Store
import com.devqube.drinktracker.data.buildWeek
import com.devqube.drinktracker.data.mondayOf
import com.devqube.drinktracker.data.statusDetails
import com.devqube.drinktracker.data.toUnits
import java.time.LocalDate

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
        val (title, body) = voiceFor(settings.theme, settings.vulgarity).planMessage(details, oneBeerUnits)

        // Eskalacja (pierwszy wpis tygodnia albo gorszy status) wyskakuje nad innymi aplikacjami;
        // pozostałe wpisy grają dźwięk (o ile użytkownik tego chce) bez wyskakującego okienka.
        val monday = mondayOf(LocalDate.now())
        val escalated = AlertPolicy.shouldAlert(store.lastNotifiedStatus(), monday, details.status)
        val d = AlertPolicy.delivery(escalated, settings.soundEveryLog)

        Notifier.notifyStatus(context, title, body, details.status, d.alert, settings.theme, headsUp = d.headsUp)
        store.setLastNotifiedStatus(AlertPolicy.key(monday, details.status))
    }
}
