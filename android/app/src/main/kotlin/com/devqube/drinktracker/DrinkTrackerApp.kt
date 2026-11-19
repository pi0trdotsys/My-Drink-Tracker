package com.devqube.drinktracker

import android.app.Application
import com.devqube.drinktracker.notifications.Notifier
import com.devqube.drinktracker.notifications.WeeklySummaryWorker
import com.devqube.drinktracker.widget.MidnightRefreshWorker

class DrinkTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        WeeklySummaryWorker.schedule(this)
        MidnightRefreshWorker.schedule(this)
    }
}
