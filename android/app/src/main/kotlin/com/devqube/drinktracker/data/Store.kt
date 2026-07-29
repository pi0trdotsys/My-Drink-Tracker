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
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            unitLimit = p[Keys.UNIT_LIMIT] ?: 14.0,
            dayLimit = p[Keys.DAY_LIMIT] ?: 4,
            gramsPerUnit = p[Keys.GRAMS_PER_UNIT] ?: GRAMS_PER_UNIT_DEFAULT,
            warnRatioPercent = p[Keys.WARN_PERCENT] ?: 80,
            notificationsEnabled = p[Keys.NOTIFS] ?: true,
            weightKg = p[Keys.WEIGHT] ?: 80.0,
            sex = runCatching { Sex.valueOf(p[Keys.SEX] ?: "MALE") }.getOrDefault(Sex.MALE),
        )
    }

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

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val current = settings.first()
        val next = transform(current)
        context.dataStore.edit { p ->
            p[Keys.UNIT_LIMIT] = next.unitLimit
            p[Keys.DAY_LIMIT] = next.dayLimit
            p[Keys.GRAMS_PER_UNIT] = next.gramsPerUnit
            p[Keys.WARN_PERCENT] = next.warnRatioPercent
            p[Keys.NOTIFS] = next.notificationsEnabled
            p[Keys.WEIGHT] = next.weightKg
            p[Keys.SEX] = next.sex.name
        }
    }

    /** Zapamiętuje ostatnio zgłoszony status, by nie spamować powiadomieniami. */
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
                    units = o.getDouble("units"),
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
                    .put("units", e.units)
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
