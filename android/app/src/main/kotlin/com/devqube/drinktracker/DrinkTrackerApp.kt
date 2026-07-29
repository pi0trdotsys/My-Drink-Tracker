package com.devqube.drinktracker

import android.app.Application
import com.devqube.drinktracker.notifications.Notifier
import com.devqube.drinktracker.notifications.WeeklySummaryWorker

class DrinkTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        WeeklySummaryWorker.schedule(this)
    }
}
