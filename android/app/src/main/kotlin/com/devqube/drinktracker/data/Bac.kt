package com.devqube.drinktracker.data

import kotlin.math.max

enum class Sex(val label: String, val r: Double) {
    MALE("Mężczyzna", 0.68),
    FEMALE("Kobieta", 0.55),
}

/** Tempo eliminacji alkoholu z krwi (‰ na godzinę). */
const val ELIMINATION_PER_HOUR = 0.15

data class BacEstimate(
    /** Szacowane stężenie we krwi w promilach (‰). */
    val perMille: Double,
    /** Godziny do trzeźwości (BAC = 0). */
    val hoursToSober: Double,
    val level: BacLevel,
)

enum class BacLevel(val label: String, val note: String) {
    SOBER("Trzeźwy", "Brak alkoholu we krwi"),
    LIGHT("Lekki rausz", "Możliwe pogorszenie refleksu"),
    IMPAIRED("Nie prowadź", "Wyraźny wpływ na organizm"),
    HIGH("Bardzo dużo", "Zadbaj o bezpieczeństwo"),
}

private const val MILLIS_PER_HOUR = 3_600_000.0

fun bacLevel(perMille: Double): BacLevel = when {
    perMille <= 0.05 -> BacLevel.SOBER
    perMille < 0.5 -> BacLevel.LIGHT
    perMille < 1.5 -> BacLevel.IMPAIRED
    else -> BacLevel.HIGH
}

/**
 * Szacuje stężenie alkoholu we krwi w chwili [nowMillis] (model Widmarka z eliminacją).
 *
 * Trunki przetwarzane są chronologicznie: każdy dodaje `gramy / (masa × r)` ‰, a między
 * kolejnymi trunkami organizm spala [ELIMINATION_PER_HOUR] ‰/h (nie schodząc poniżej zera).
 * Dzięki temu długa przerwa „zeruje" stan, a picie przez północ liczy się poprawnie.
 * Trunki z przyszłości (względem [nowMillis]) są pomijane.
 *
 * @param drinks pary (czas w ms, gramy czystego etanolu)
 */
fun bacAt(
    drinks: List<Pair<Long, Double>>,
    weightKg: Double,
    sex: Sex,
    nowMillis: Long,
): BacEstimate {
    if (weightKg <= 0) return BacEstimate(0.0, 0.0, BacLevel.SOBER)
    val distribution = weightKg * sex.r

    var bac = 0.0
    var lastTime: Long? = null
    drinks.filter { it.first <= nowMillis && it.second > 0 }
        .sortedBy { it.first }
        .forEach { (time, grams) ->
            lastTime?.let { bac = max(bac - ELIMINATION_PER_HOUR * (time - it) / MILLIS_PER_HOUR, 0.0) }
            bac += grams / distribution
            lastTime = time
        }
    lastTime?.let { bac = max(bac - ELIMINATION_PER_HOUR * (nowMillis - it) / MILLIS_PER_HOUR, 0.0) }

    return BacEstimate(bac, bac / ELIMINATION_PER_HOUR, bacLevel(bac))
}
