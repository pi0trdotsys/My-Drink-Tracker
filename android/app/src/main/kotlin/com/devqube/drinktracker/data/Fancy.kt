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
