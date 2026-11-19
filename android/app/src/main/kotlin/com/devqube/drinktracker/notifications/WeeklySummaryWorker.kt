package com.devqube.drinktracker.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.devqube.drinktracker.data.Fancy
import com.devqube.drinktracker.data.Store
import com.devqube.drinktracker.data.voiceFor
import com.devqube.drinktracker.data.buildWeek
import com.devqube.drinktracker.data.statusDetails
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/** Cotygodniowe podsumowanie – niedziela wieczorem. */
class WeeklySummaryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = Store.get(applicationContext)
        val settings = store.snapshotSettings()
        if (!settings.notificationsEnabled) return Result.success()

        val entries = store.snapshotEntries()
        val week = buildWeek(entries, settings)
        val details = statusDetails(week)
        val streak = Fancy.dryStreak(entries)
        val (title, body) = voiceFor(settings.theme).weeklyMessage(details, streak)
        Notifier.notifyWeekly(applicationContext, title, body, settings.theme)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "weekly_summary"

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            // Cel: najbliższa niedziela o 20:00.
            var target = now.withHour(20).withMinute(0).withSecond(0).withNano(0)
            while (target.dayOfWeek.value != 7 || target.isBefore(now)) {
                target = target.plusDays(1)
            }
            val initialDelay = ChronoUnit.MINUTES.between(now, target).coerceAtLeast(1)

            val request = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
