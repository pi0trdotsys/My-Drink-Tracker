package com.devqube.drinktracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "drink_tracker")

/** Jedyne źródło prawdy dla ustawień, wpisów i własnych typów trunków. */
class Store(private val context: Context) {

    private object Keys {
        val UNIT_LIMIT = doublePreferencesKey("unit_limit")
        val DAY_LIMIT = intPreferencesKey("day_limit")
        val GRAMS_PER_UNIT = intPreferencesKey("grams_per_unit")
        val WARN_PERCENT = intPreferencesKey("warn_percent")
        val NOTIFS = booleanPreferencesKey("notifications")
        val WEIGHT = doublePreferencesKey("weight_kg")
        val SEX = stringPreferencesKey("sex")
        val ENTRIES = stringPreferencesKey("entries_json")
        val CUSTOM_TYPES = stringPreferencesKey("custom_types_json")
        val LAST_NOTIFIED_STATUS = stringPreferencesKey("last_notified_status")
        val THEME = stringPreferencesKey("theme")
        val DAILY_NUDGES = booleanPreferencesKey("daily_nudges")
        val SENT_NUDGES = stringPreferencesKey("sent_nudges")
    }

    private fun Preferences.toSettings() = AppSettings(
        unitLimit = this[Keys.UNIT_LIMIT] ?: 14.0,
        dayLimit = this[Keys.DAY_LIMIT] ?: 4,
        gramsPerUnit = this[Keys.GRAMS_PER_UNIT] ?: GRAMS_PER_UNIT_DEFAULT,
        warnRatioPercent = this[Keys.WARN_PERCENT] ?: 80,
        notificationsEnabled = this[Keys.NOTIFS] ?: true,
        weightKg = this[Keys.WEIGHT] ?: 80.0,
        sex = runCatching { Sex.valueOf(this[Keys.SEX] ?: "MALE") }.getOrDefault(Sex.MALE),
        theme = runCatching { AppTheme.valueOf(this[Keys.THEME] ?: "CLASSIC") }.getOrDefault(AppTheme.CLASSIC),
        dailyNudges = this[Keys.DAILY_NUDGES] ?: false,
    )

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    val entries: Flow<List<DrinkEntry>> = context.dataStore.data.map { p ->
        parseEntries(p[Keys.ENTRIES])
    }

    val customTypes: Flow<List<DrinkType>> = context.dataStore.data.map { p ->
        parseTypes(p[Keys.CUSTOM_TYPES])
    }

    val allDrinkTypes: Flow<List<DrinkType>> = context.dataStore.data.map { p ->
        DEFAULT_DRINK_TYPES + parseTypes(p[Keys.CUSTOM_TYPES])
    }

    suspend fun snapshotSettings(): AppSettings = settings.first()
    suspend fun snapshotEntries(): List<DrinkEntry> = entries.first()

    suspend fun addEntry(entry: DrinkEntry) {
        context.dataStore.edit { p ->
            val list = parseEntries(p[Keys.ENTRIES]).toMutableList()
            list.add(0, entry)
            p[Keys.ENTRIES] = encodeEntries(list)
        }
    }

    suspend fun deleteEntry(id: Long) {
        context.dataStore.edit { p ->
            val list = parseEntries(p[Keys.ENTRIES]).filterNot { it.id == id }
            p[Keys.ENTRIES] = encodeEntries(list)
        }
    }

    suspend fun addCustomType(type: DrinkType) {
        context.dataStore.edit { p ->
            val list = parseTypes(p[Keys.CUSTOM_TYPES]).toMutableList()
            list.add(type.copy(custom = true))
            p[Keys.CUSTOM_TYPES] = encodeTypes(list)
        }
    }

    suspend fun deleteCustomType(id: String) {
        context.dataStore.edit { p ->
            val list = parseTypes(p[Keys.CUSTOM_TYPES]).filterNot { it.id == id }
            p[Keys.CUSTOM_TYPES] = encodeTypes(list)
        }
    }

    /** Odczyt i zapis w jednej transakcji – równoległe zmiany się nie nadpisują. */
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val next = transform(p.toSettings())
            p[Keys.UNIT_LIMIT] = next.unitLimit
            p[Keys.DAY_LIMIT] = next.dayLimit
            p[Keys.GRAMS_PER_UNIT] = next.gramsPerUnit
            p[Keys.WARN_PERCENT] = next.warnRatioPercent
            p[Keys.NOTIFS] = next.notificationsEnabled
            p[Keys.WEIGHT] = next.weightKg
            p[Keys.SEX] = next.sex.name
            p[Keys.THEME] = next.theme.name
            p[Keys.DAILY_NUDGES] = next.dailyNudges
        }
    }

    /**
     * Atomowo rezerwuje slot przypomnienia (np. „2026-11-19|AFTERNOON”).
     * Zwraca false, jeśli w tym slocie już coś wysłano – chroni przed podwójnym powiadomieniem.
     */
    suspend fun claimNudgeSlot(key: String): Boolean {
        var claimed = false
        context.dataStore.edit { p ->
            val sent = p[Keys.SENT_NUDGES].orEmpty().split(",").filter { it.isNotBlank() }
            if (key !in sent) {
                // Trzymamy tylko kilka ostatnich kluczy (z ostatnich dni).
                p[Keys.SENT_NUDGES] = (sent + key).takeLast(12).joinToString(",")
                claimed = true
            }
        }
        return claimed
    }

    /**
     * Ostatnio zgłoszony status w formacie „<poniedziałek tygodnia>|<status>",
     * aby dźwięk grał tylko przy zmianie statusu (i na start nowego tygodnia).
     */
    suspend fun lastNotifiedStatus(): String? = context.dataStore.data.first()[Keys.LAST_NOTIFIED_STATUS]

    suspend fun setLastNotifiedStatus(value: String) {
        context.dataStore.edit { it[Keys.LAST_NOTIFIED_STATUS] = value }
    }

    private fun parseEntries(json: String?): List<DrinkEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DrinkEntry(
                    id = o.getLong("id"),
                    timestamp = o.getLong("timestamp"),
                    name = o.getString("name"),
                    volumeMl = o.getDouble("volumeMl"),
                    abv = o.getDouble("abv"),
                    loggedUnits = o.optDouble("units", 0.0),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeEntries(list: List<DrinkEntry>): String {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("timestamp", e.timestamp)
                    .put("name", e.name)
                    .put("volumeMl", e.volumeMl)
                    .put("abv", e.abv)
                    .put("units", e.loggedUnits)
            )
        }
        return arr.toString()
    }

    private fun parseTypes(json: String?): List<DrinkType> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DrinkType(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    emoji = o.optString("emoji", "🥤"),
                    volumeMl = o.getInt("volumeMl"),
                    abv = o.getDouble("abv"),
                    custom = true,
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeTypes(list: List<DrinkType>): String {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("name", t.name)
                    .put("emoji", t.emoji)
                    .put("volumeMl", t.volumeMl)
                    .put("abv", t.abv)
            )
        }
        return arr.toString()
    }

    companion object {
        @Volatile
        private var instance: Store? = null

        fun get(context: Context): Store =
            instance ?: synchronized(this) {
                instance ?: Store(context.applicationContext).also { instance = it }
            }
    }
}
