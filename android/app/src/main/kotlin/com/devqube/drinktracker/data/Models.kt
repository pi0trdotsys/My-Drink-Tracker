package com.devqube.drinktracker.data

/** Pojedynczy zalogowany trunek. */
data class DrinkEntry(
    val id: Long,
    val timestamp: Long,
    val name: String,
    val volumeMl: Double,
    val abv: Double,
    /** SU wyliczone w chwili zapisu – tylko dla zgodności formatu, nie używać w obliczeniach. */
    val loggedUnits: Double,
) {
    val grams: Double get() = toGrams(volumeMl, abv)

    /** SU według bieżącej definicji jednostki – zmiana ustawień przelicza całą historię spójnie. */
    fun unitsFor(gramsPerUnit: Int): Double = toUnits(volumeMl, abv, gramsPerUnit)
}

/**
 * Losowe, dodatnie ID wpisu. Znacznik czasu w milisekundach mógł się powtórzyć przy
 * szybkim podwójnym dodaniu, a wtedy usunięcie jednego wpisu kasowało oba.
 */
fun newEntryId(): Long = java.util.UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE

/** Motyw aplikacji: wygląd, teksty, dźwięki powiadomień i widget. */
enum class AppTheme(val label: String) {
    CLASSIC("Klasyczny"),

    /** Eksperymentalny, 18+: wulgarne, memiczne komentarze „wkurzonej żony”. */
    GRAZYNA("Grażyna 🍳"),
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
    val theme: AppTheme = AppTheme.CLASSIC,
    /** Sensowne przypomnienia w ciągu dnia (południe / 17:00 / wieczór). */
    val dailyNudges: Boolean = false,
) {
    val warnRatio: Double get() = warnRatioPercent / 100.0
}
