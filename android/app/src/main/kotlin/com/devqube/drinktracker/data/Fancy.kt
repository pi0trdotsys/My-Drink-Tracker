package com.devqube.drinktracker.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * "Fancy smaczek" – lekki, dowcipny charakter aplikacji: rotujące hasła,
 * licznik trzeźwych dni oraz teksty powiadomień z pazurem.
 */
object Fancy {

    private val okTaglines = listOf(
        "Twoja wątroba przybija Ci piątkę 🖐️",
        "Forma mistrzowska. Tak trzymaj! ✨",
        "Spokojna głowa – trzymasz się planu 🧊",
        "Nawodnienie > procenty. Dobra robota 💧",
    )
    private val warnTaglines = listOf(
        "Ostatnia prosta – może dziś woda? 💧",
        "Balansujesz na krawędzi limitu 🎚️",
        "Jeszcze chwila i limit powie „dość” ⏳",
    )
    private val overTaglines = listOf(
        "Czas na tryb detoks 🌿",
        "Limit macha białą flagą 🏳️",
        "Dziś wygrywa herbata 🍵",
    )

    fun tagline(status: Status, seed: Int): String {
        val pool = when (status) {
            Status.OK -> okTaglines
            Status.WARN -> warnTaglines
            Status.OVER -> overTaglines
        }
        return pool[(seed % pool.size + pool.size) % pool.size]
    }

    /** Emblemat statusu na widgecie/nagłówku. */
    fun emblem(status: Status): String = when (status) {
        Status.OK -> "🟢"
        Status.WARN -> "🟡"
        Status.OVER -> "🔴"
    }

    /**
     * Liczba kolejnych najświeższych dni bez alkoholu (dry streak), licząc wstecz
     * od dziś aż do ostatniego dnia z alkoholem. Bez historii picia zwraca 0.
     */
    fun dryStreak(entries: List<DrinkEntry>, zone: ZoneId = ZoneId.systemDefault()): Int {
        if (entries.isEmpty()) return 0
        val drinkingDays = entries
            .map { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
            .toSet()
        val lastDrink = drinkingDays.max()
        var streak = 0
        var day = LocalDate.now(zone)
        // Dzisiaj liczy się tylko, jeśli nic dziś nie wypito.
        while (day !in drinkingDays && !day.isBefore(lastDrink)) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    fun streakBadge(streak: Int): String = when {
        streak >= 30 -> "🏆"
        streak >= 14 -> "🥇"
        streak >= 7 -> "🔥"
        streak >= 3 -> "⭐"
        streak >= 1 -> "🌱"
        else -> "🍻"
    }

    fun streakLine(streak: Int): String = when (streak) {
        0 -> "Dziś już z procentami – jutro nowe otwarcie"
        1 -> "1 trzeźwy dzień – dobry początek!"
        else -> "$streak trzeźwych dni z rzędu ${streakBadge(streak)}"
    }

    // --- System "planowania" tygodnia: ile jeszcze możesz wypić ---

    /** Polska odmiana rzeczownika „piwo": 1 piwo, 2–4 piwa, 5+ piw. */
    fun beerWord(n: Int): String {
        val m10 = n % 10
        val m100 = n % 100
        return when {
            n == 1 -> "piwo"
            m10 in 2..4 && m100 !in 12..14 -> "piwa"
            else -> "piw"
        }
    }

    /** Ile piw (dla podanego przelicznika 1 piwa w SU) mieści się w pozostałym limicie. */
    fun beersLeft(remainingUnits: Double, oneBeerUnits: Double): Int {
        if (oneBeerUnits <= 0) return 0
        return kotlin.math.floor(remainingUnits / oneBeerUnits).toInt().coerceAtLeast(0)
    }

    /**
     * Komunikat "planujący" po zalogowaniu trunku: ile jeszcze możesz wypić,
     * wyrażone również w konkretnych piwach, z podpowiedzią by dobrze to rozplanować.
     */
    fun planMessage(details: StatusDetails, oneBeerUnits: Double): Pair<String, String> {
        if (details.status == Status.OVER) {
            val title = "Stop na ten tydzień 🛑"
            val body = if (details.overUnits > 0)
                "W tym tygodniu już więcej nie pij — jesteś ${fmt(details.overUnits)} SU ponad plan. Reset w poniedziałek 🔁"
            else
                "Wykorzystano wszystkie dni z alkoholem (${details.drinkingDays}/${details.drinkingDays + details.remainingDays}). Do poniedziałku odpuść 🔁"
            return title to body
        }

        val remaining = details.remainingUnits
        val beers = beersLeft(remaining, oneBeerUnits)
        val daysTail = if (details.remainingDays > 0) " i ${details.remainingDays} dni z alkoholem" else ", ale dni z alkoholem masz już wykorzystane"

        return when {
            remaining <= 0.05 ->
                "To był ostatni w tym tygodniu 🎯" to
                    "Limit dokładnie wykorzystany. Resztę tygodnia zaplanuj na spokojnie 🧘"
            beers == 0 ->
                "Ostatnie krople 🤏" to
                    "Zostało Ci mniej niż jedno piwo (${fmt(remaining)} SU)$daysTail. Trzymaj się planu!"
            else -> {
                val head = if (details.status == Status.WARN) "Blisko limitu ${emblem(details.status)}" else "Plan tygodnia 🍺"
                head to
                    "Zostało Ci maksymalnie $beers ${beerWord(beers)} (${fmt(remaining)} SU)$daysTail. Zaplanuj to dobrze 📅"
            }
        }
    }

    // --- Teksty powiadomień z charakterem ---

    fun warnNotification(details: StatusDetails): Pair<String, String> =
        "Blisko limitu 🟡" to
            "${tagline(Status.WARN, details.drinkingDays)} Zostało ${fmt(details.remainingUnits)} SU."

    fun overNotification(details: StatusDetails): Pair<String, String> =
        "Limit przekroczony 🔴" to
            "${tagline(Status.OVER, details.drinkingDays)} ${details.message}"

    fun weeklyNotification(details: StatusDetails, streak: Int): Pair<String, String> {
        val title = "Podsumowanie tygodnia ${emblem(details.status)}"
        val verdict = when (details.status) {
            Status.OK -> "Tydzień w normie – ${fmt(details.total)} SU. ${streakLine(streak)}"
            Status.WARN -> "Blisko granicy – ${fmt(details.total)} SU w ${details.drinkingDays} dni."
            Status.OVER -> "Przekroczono limit o ${fmt(details.overUnits)} SU. Nowy tydzień = nowy start."
        }
        return title to verdict
    }
}
