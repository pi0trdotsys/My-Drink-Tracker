package com.devqube.drinktracker.data

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Słownik motywu Grażyna. Ksywki są w wołaczu („Hamuj, ochlapusie”), bo Grażyna zawsze
 * zwraca się bezpośrednio. Przekleństwa i stany upojenia mają dwa poziomy – [Vulgarity.SHARP]
 * bez najcięższych słów i [Vulgarity.MAX] ze wszystkim.
 *
 * Zasada: obelgi dotyczą wyłącznie picia – nic o pochodzeniu, orientacji, wyglądzie czy chorobie.
 */
object GrazynaLexicon {

    val nicknames = listOf(
        "pijusie", "moczymordo", "ochlapusie", "opoju", "pijaczyno", "chlejusie", "alkusie",
        "menelu", "żulu", "łajzo", "łachudro", "gnoju", "gamoniu", "cymbale", "ćwoku",
        "darmozjadzie", "żłopaczu", "gąbko", "beczko bez dna", "kuflu z nogami",
        "piwoszu z bożej łaski", "browarniku", "Januszu", "Mirku", "Sebixie",
    )

    val cursesSharp = listOf("kurwa", "kurwa mać", "do cholery", "psia mać", "jasna cholera")
    val cursesMax = cursesSharp + listOf("ja pierdolę", "do chuja pana", "ożeż kurwa", "kurwa jego mać")

    val drunkSharp = listOf(
        "nawalony", "nachlany", "narąbany", "zalany w trupa", "pijany jak świnia",
        "w sztok pijany", "na bani", "zrobiony", "wstawiony",
    )
    val drunkMax = drunkSharp + listOf("najebany", "w trzy dupy", "najebany jak stodoła")

    /** Rdzenie słów zarezerwowanych dla [Vulgarity.MAX] (używane też w testach). */
    val hardRoots = listOf("pierdol", "pierdal", "chuj", "pizd", "jeb")
}

private fun String.cap() = replaceFirstChar { it.uppercase() }

/**
 * Losowanie słów z [GrazynaLexicon] dla danego ziarna. To samo ziarno daje te same słowa,
 * a różne indeksy [i] – różne słowa w jednym zdaniu.
 */
class Words(val level: Vulgarity, val seed: Int) {
    private fun <T> at(list: List<T>, i: Int): T = list[Math.floorMod(seed * 31 + i * 17, list.size)]

    fun nick(i: Int = 0) = at(GrazynaLexicon.nicknames, i)
    fun Nick(i: Int = 0) = nick(i).cap()
    fun curse(i: Int = 0) = at(if (level == Vulgarity.MAX) GrazynaLexicon.cursesMax else GrazynaLexicon.cursesSharp, i)
    fun Curse(i: Int = 0) = curse(i).cap()
    fun drunk(i: Int = 0) = at(if (level == Vulgarity.MAX) GrazynaLexicon.drunkMax else GrazynaLexicon.drunkSharp, i)

    /** Wariant zależny od poziomu: ostrzejszy przy MAX, łagodniejszy przy SHARP. */
    fun hard(max: String, sharp: String) = if (level == Vulgarity.MAX) max else sharp
}

typealias Line = Words.() -> String

/** Wybiera linijkę z puli; przy [Vulgarity.MAX] dokłada pulę [extra] z najcięższymi tekstami. */
fun Words.pick(base: List<Line>, extra: List<Line> = emptyList(), salt: Int = 0): String {
    val pool = if (level == Vulgarity.MAX) base + extra else base
    return pool[Math.floorMod(seed + salt, pool.size)](this)
}

// ---------------------------------------------------------------------------------------------
// Poziom wkurwu, narzędzie Grażyny i pora dnia – wspólne dla widgetu i ekranu aplikacji.
// ---------------------------------------------------------------------------------------------

data class Meter(val percent: Int, val mood: String)

/** 0–100%: większy z udziałów (SU/limit, dni/limit dni); przekroczenie = 100%. */
fun wrathPercent(d: StatusDetails): Int {
    if (d.status == Status.OVER) return 100
    val dayLimit = d.drinkingDays + d.remainingDays
    val days = if (dayLimit > 0) d.drinkingDays.toDouble() / dayLimit else 0.0
    return (max(d.ratio, days) * 100).roundToInt().coerceIn(0, 99)
}

fun wrathLabel(percent: Int): String = when {
    percent >= 100 -> "dzwoni do teściowej"
    percent >= 75 -> "patelnia w dłoni"
    percent >= 50 -> "szuka wałka"
    percent >= 25 -> "łypie krzywo"
    else -> "spokojna"
}

/** Czym Grażyna grozi przy danym statusie (ikona na widgecie i karcie). */
enum class Tool { KAPEC, PATELNIA, WALEK }

fun toolFor(status: Status): Tool = when (status) {
    Status.OK -> Tool.KAPEC
    Status.WARN -> Tool.PATELNIA
    Status.OVER -> Tool.WALEK
}

/** Pora dnia, która zmienia komentarz na widgecie. */
enum class Moment { HANGOVER, TEMPTATION, EVENING_DRINKING, NONE }

fun momentOf(hour: Int, unitsToday: Double, unitsYesterday: Double): Moment = when {
    hour in 5..11 && unitsYesterday > 0 && unitsToday == 0.0 -> Moment.HANGOVER
    hour in 20..23 && unitsToday > 0 -> Moment.EVENING_DRINKING
    hour in 16..19 && unitsToday == 0.0 -> Moment.TEMPTATION
    else -> Moment.NONE
}

/** Wszystko, czego potrzeba do tekstu na widgecie. [seed] zmienia się co godzinę i po każdym wpisie. */
data class WidgetMoment(
    val status: Status,
    val streak: Int,
    val beersLeft: Int,
    val moment: Moment,
    val seed: Int,
)
