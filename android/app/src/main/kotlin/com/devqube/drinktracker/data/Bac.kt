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

/**
 * Szacuje aktualne stężenie alkoholu we krwi wzorem Widmarka:
 *   BAC(‰) = gramy_etanolu / (masa_kg × r) − β × godziny_od_pierwszego_drinka
 *
 * @param gramsToday suma gramów czystego etanolu wypitych dzisiaj
 * @param hoursSinceFirst godziny od pierwszego dzisiejszego trunku
 */
fun estimateBac(
    gramsToday: Double,
    hoursSinceFirst: Double,
    weightKg: Double,
    sex: Sex,
): BacEstimate {
    if (gramsToday <= 0 || weightKg <= 0) {
        return BacEstimate(0.0, 0.0, BacLevel.SOBER)
    }
    val peak = gramsToday / (weightKg * sex.r)
    val current = max(peak - ELIMINATION_PER_HOUR * max(hoursSinceFirst, 0.0), 0.0)
    val hoursToSober = current / ELIMINATION_PER_HOUR
    val level = when {
        current <= 0.05 -> BacLevel.SOBER
        current < 0.5 -> BacLevel.LIGHT
        current < 1.5 -> BacLevel.IMPAIRED
        else -> BacLevel.HIGH
    }
    return BacEstimate(current, hoursToSober, level)
}
