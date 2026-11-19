package com.devqube.drinktracker.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.devqube.drinktracker.data.millisUntilNextMidnight
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Odświeża widget tuż po północy. Bez tego (updatePeriodMillis = 0) widget odświeżał się
 * tylko po dodaniu wpisu, więc np. w poniedziałek rano nadal pokazywał zeszły tydzień.
 */
class MidnightRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        AlcoholGlanceWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "midnight_widget_refresh"

        /** KEEP: kolejne uruchomienia aplikacji nie przesuwają już zaplanowanego cyklu. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MidnightRefreshWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(millisUntilNextMidnight(ZonedDateTime.now()), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
