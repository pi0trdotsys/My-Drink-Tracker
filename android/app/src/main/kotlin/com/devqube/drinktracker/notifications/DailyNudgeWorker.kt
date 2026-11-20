package com.devqube.drinktracker.notifications

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.devqube.drinktracker.data.NudgeSlot
import com.devqube.drinktracker.data.Store
import com.devqube.drinktracker.data.decideNudge
import com.devqube.drinktracker.data.nudgeInputFor
import com.devqube.drinktracker.data.voiceFor
import com.devqube.drinktracker.update.UpdateManager
import com.devqube.drinktracker.widget.AlcoholGlanceWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Przypomnienia w ciągu dnia. Worker budzi się co godzinę, ale powiadomienie wysyła tylko:
 * - w oknie jednej z pór dnia ([NudgeSlot]),
 * - gdy jest sensowny powód ([decideNudge] nie zwraca `null`),
 * - najwyżej raz na porę dnia (rezerwacja slotu w [Store.claimNudgeSlot]).
 */
class DailyNudgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Co godzinę odświeżamy widget: rotacja tekstów i komentarze zależne od pory dnia.
        runCatching { AlcoholGlanceWidget().updateAll(applicationContext) }

        // Nowa wersja na GitHubie: UpdateManager pyta najwyżej co 6 h i zwraca wersję raz (powiadomienie raz na wersję).
        withContext(Dispatchers.IO) { UpdateManager.backgroundCheck(applicationContext) }
            ?.let { Notifier.notifyUpdate(applicationContext, it) }

        val store = Store.get(applicationContext)
        val settings = store.snapshotSettings()
        if (!settings.notificationsEnabled || !settings.dailyNudges) return Result.success()

        val now = ZonedDateTime.now()
        val slot = NudgeSlot.forHour(now.hour) ?: return Result.success()

        val nudge = decideNudge(nudgeInputFor(store.snapshotEntries(), settings, slot, now))
            ?: return Result.success()
        if (!store.claimNudgeSlot("${now.toLocalDate()}|${slot.name}")) return Result.success()

        val (title, body) = voiceFor(settings.theme, settings.vulgarity).nudge(nudge)
        Notifier.notifyNudge(applicationContext, title, body, settings.theme)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_nudges"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyNudgeWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
