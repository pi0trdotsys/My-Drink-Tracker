package com.devqube.drinktracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import com.devqube.drinktracker.data.AppSettings
import com.devqube.drinktracker.data.DEFAULT_DRINK_TYPES
import com.devqube.drinktracker.data.DrinkEntry
import com.devqube.drinktracker.data.newEntryId
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

    // Startujemy od typów domyślnych, aby ekran logowania od razu miał co zaznaczyć.
    val drinkTypes: StateFlow<List<DrinkType>> =
        store.allDrinkTypes.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_DRINK_TYPES)

    /**
     * Zapisuje trunek i zwraca utworzony wpis (np. do wysłania do Kropi przez ekran, który ma kontekst
     * aktywności). Wpis powstaje od razu; zapis, powiadomienie i odświeżenie widgetu idą w tle.
     */
    fun addDrink(name: String, volumeMl: Double, abv: Double, timestamp: Long = System.currentTimeMillis()): DrinkEntry {
        val entry = DrinkEntry(
            id = newEntryId(),
            timestamp = timestamp,
            name = name,
            volumeMl = volumeMl,
            abv = abv,
            loggedUnits = toUnits(volumeMl, abv, settings.value.gramsPerUnit),
        )
        viewModelScope.launch {
            store.addEntry(entry)
            LimitChecker.checkAndNotify(getApplication())
            AlcoholGlanceWidget().updateAll(getApplication())
        }
        return entry
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
