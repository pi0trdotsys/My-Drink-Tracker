package com.devqube.drinktracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import com.devqube.drinktracker.data.AppSettings
import com.devqube.drinktracker.data.DrinkEntry
import com.devqube.drinktracker.data.DrinkType
import com.devqube.drinktracker.data.Store
import com.devqube.drinktracker.data.toUnits
import com.devqube.drinktracker.notifications.LimitChecker
import com.devqube.drinktracker.widget.AlcoholGlanceWidget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store.get(app)

    val settings: StateFlow<AppSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val entries: StateFlow<List<DrinkEntry>> =
        store.entries.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val drinkTypes: StateFlow<List<DrinkType>> =
        store.allDrinkTypes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addDrink(name: String, volumeMl: Double, abv: Double, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val gramsPerUnit = store.snapshotSettings().gramsPerUnit
            val entry = DrinkEntry(
                id = System.currentTimeMillis(),
                timestamp = timestamp,
                name = name,
                volumeMl = volumeMl,
                abv = abv,
                units = toUnits(volumeMl, abv, gramsPerUnit),
            )
            store.addEntry(entry)
            LimitChecker.checkAndNotify(getApplication())
            AlcoholGlanceWidget().updateAll(getApplication())
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            store.deleteEntry(id)
            AlcoholGlanceWidget().updateAll(getApplication())
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            store.updateSettings(transform)
            AlcoholGlanceWidget().updateAll(getApplication())
        }
    }

    fun addCustomType(type: DrinkType) {
        viewModelScope.launch { store.addCustomType(type) }
    }

    fun deleteCustomType(id: String) {
        viewModelScope.launch { store.deleteCustomType(id) }
    }
}
