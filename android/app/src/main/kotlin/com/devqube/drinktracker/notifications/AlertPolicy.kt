package com.devqube.drinktracker.notifications

import com.devqube.drinktracker.data.Status
import java.time.LocalDate

/**
 * Decyduje, czy powiadomienie o planie/statusie ma zagrać dźwięk, czy tylko cicho się zaktualizować.
 *
 * Dźwięk gra przy pierwszym wpisie w danym tygodniu oraz przy eskalacji statusu
 * (W normie → Uwaga → Przekroczono). Kolejne wpisy w tym samym statusie aktualizują treść bez dźwięku,
 * żeby pięć drinków wieczorem nie oznaczało pięciu głośnych powiadomień.
 */
object AlertPolicy {

    private fun rank(status: Status): Int = when (status) {
        Status.OK -> 0
        Status.WARN -> 1
        Status.OVER -> 2
    }

    /** Klucz zapisywany po powiadomieniu, np. „2026-11-16|WARN". */
    fun key(weekMonday: LocalDate, status: Status): String = "$weekMonday|${status.name}"

    fun shouldAlert(lastKey: String?, weekMonday: LocalDate, status: Status): Boolean {
        val parts = lastKey?.split("|")
        if (parts == null || parts.size != 2 || parts[0] != weekMonday.toString()) return true
        val lastStatus = runCatching { Status.valueOf(parts[1]) }.getOrNull() ?: return true
        return rank(status) > rank(lastStatus)
    }

    /**
     * Jak dostarczyć powiadomienie po wpisie.
     * - [alert]: czy zagrać dźwięk (domyślnie przy każdym wpisie – użytkownik oczekuje potwierdzenia),
     * - [headsUp]: czy wyskoczyć nad innymi aplikacjami (tylko przy eskalacji, żeby nie spamować).
     *
     * Przy wyłączonym [soundEveryLog] wracamy do zachowania z v1.0.2–1.2.1: dźwięk tylko przy eskalacji.
     */
    data class Delivery(val alert: Boolean, val headsUp: Boolean)

    fun delivery(escalated: Boolean, soundEveryLog: Boolean): Delivery =
        Delivery(alert = escalated || soundEveryLog, headsUp = escalated)
}
