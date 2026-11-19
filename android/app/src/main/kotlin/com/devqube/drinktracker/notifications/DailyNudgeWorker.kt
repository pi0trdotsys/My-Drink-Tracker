package com.devqube.drinktracker.notifications

import android.content.Context
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
        val store = Store.get(applicationContext)
        val settings = store.snapshotSettings()
        if (!settings.notificationsEnabled || !settings.dailyNudges) return Result.success()

        val now = ZonedDateTime.now()
        val slot = NudgeSlot.forHour(now.hour) ?: return Result.success()

        val nudge = decideNudge(nudgeInputFor(store.snapshotEntries(), settings, slot, now))
            ?: return Result.success()
        if (!store.claimNudgeSlot("${now.toLocalDate()}|${slot.name}")) return Result.success()

        val (title, body) = voiceFor(settings.theme).nudge(nudge)
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
