package com.devqube.drinktracker.data

/** Pojedynczy zalogowany trunek. */
data class DrinkEntry(
    val id: Long,
    val timestamp: Long,
    val name: String,
    val volumeMl: Double,
    val abv: Double,
    val units: Double,
) {
    val grams: Double get() = toGrams(volumeMl, abv)
}

/** Wszystkie ustawienia użytkownika. */
data class AppSettings(
    val unitLimit: Double = 14.0,
    val dayLimit: Int = 4,
    val gramsPerUnit: Int = GRAMS_PER_UNIT_DEFAULT,
    val warnRatioPercent: Int = 80,
    val notificationsEnabled: Boolean = true,
    val weightKg: Double = 80.0,
    val sex: Sex = Sex.MALE,
) {
    val warnRatio: Double get() = warnRatioPercent / 100.0
}
