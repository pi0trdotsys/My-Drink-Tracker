package com.devqube.drinktracker.data

/**
 * „Głos” aplikacji – wszystkie teksty zależne od motywu: nagłówki, hasła, opis promili,
 * powiadomienia o planie, podsumowanie tygodnia, przypomnienia dzienne i dopisek na widgecie.
 */
interface Voice {
    val homeEyebrow: String
    val homeTitle: String
    val bacEyebrow: String
    val logTitle: String
    val addButton: String
    val recentTitle: String

    fun tagline(status: Status, seed: Int): String
    fun bacLabel(level: BacLevel): Pair<String, String>
    fun streakLine(streak: Int): String

    /** Krótka linijka na widget (musi się zmieścić w jednym wierszu). */
    fun widgetLine(status: Status, streak: Int): String

    /** Nagłówek i treść ramki alertu na karcie tygodnia (tylko statusy WARN i OVER). */
    fun alert(details: StatusDetails): Pair<String, String>

    fun planMessage(details: StatusDetails, oneBeerUnits: Double): Pair<String, String>
    fun weeklyMessage(details: StatusDetails, streak: Int): Pair<String, String>
    fun nudge(n: Nudge): Pair<String, String>
}

fun voiceFor(theme: AppTheme): Voice = when (theme) {
    AppTheme.CLASSIC -> ClassicVoice
    AppTheme.GRAZYNA -> GrazynaVoice
}

private fun pick(pool: List<String>, seed: Int) = pool[(seed % pool.size + pool.size) % pool.size]

object ClassicVoice : Voice {
    override val homeEyebrow = "Twój tydzień"
    override val homeTitle = "Spożycie alkoholu"
    override val bacEyebrow = "Szacowany poziom alkoholu"
    override val logTitle = "Zaloguj trunek"
    override val addButton = "Dodaj wpis"
    override val recentTitle = "Ostatnie wpisy"

    override fun tagline(status: Status, seed: Int) = Fancy.tagline(status, seed)
    override fun bacLabel(level: BacLevel) = level.label to level.note
    override fun streakLine(streak: Int) = Fancy.streakLine(streak)
    override fun widgetLine(status: Status, streak: Int) = Fancy.streakLine(streak)
    override fun alert(details: StatusDetails) = details.headline to details.message
    override fun planMessage(details: StatusDetails, oneBeerUnits: Double) = Fancy.planMessage(details, oneBeerUnits)
    override fun weeklyMessage(details: StatusDetails, streak: Int) = Fancy.weeklyNotification(details, streak)

    override fun nudge(n: Nudge): Pair<String, String> = when (n) {
        is Nudge.DontDrive ->
            "Nie siadaj za kierownicę 🚗" to
                "Szacunkowo ${fmt(n.perMille)} ‰. Trzeźwość ok. za ${fmt(n.hoursToSober)} h."
        Nudge.AfterYesterday ->
            "Dzień przerwy? 💧" to "Wczoraj był alkohol – dzisiejszy dzień bez niego dobrze zrobi."
        is Nudge.WeekOver ->
            "Dziś bez alkoholu 🛑" to "Tygodniowy limit jest już przekroczony. Reset w poniedziałek."
        Nudge.DaysUsedUp ->
            "Dni z alkoholem wykorzystane 🗓️" to "Na ten tydzień to już wszystko – dziś odpuść."
        is Nudge.WeekendBudget ->
            "Weekendowy plan 🍺" to
                "Na resztę tygodnia zostało maks. ${n.beers} ${Fancy.beerWord(n.beers)} (${fmt(n.remainingUnits)} SU). Rozłóż to rozsądnie."
        is Nudge.DryStreak ->
            "${n.days} dni bez alkoholu ${Fancy.streakBadge(n.days)}" to "Świetna seria – tak trzymaj!"
        is Nudge.EveningStop ->
            "Może już wystarczy? 🌙" to "Dziś to już ${fmt(n.unitsToday)} SU – więcej niż dzienna norma."
        Nudge.EveningDry ->
            "Dzień bez alkoholu ✨" to "Kolejny trzeźwy dzień zaliczony. Dobra robota!"
    }
}

/**
 * Eksperymentalny motyw 18+: memiczna, wulgarna „wkurzona żona”. Gdera, przeklina i straszy
 * patelnią – ale zawsze w stronę picia mniej, nigdy więcej.
 */
object GrazynaVoice : Voice {
    override val homeEyebrow = "Rachunek sumienia"
    override val homeTitle = "Ile znowu wychlałeś"
    override val bacEyebrow = "Ile w tobie promili, pijusie"
    override val logTitle = "Przyznaj się, co piłeś"
    override val addButton = "Przyznaję się"
    override val recentTitle = "Lista grzechów"

    private val ok = listOf(
        "No proszę, trzeźwy. Mam to wpisać do kalendarza?",
        "Na razie się trzymasz. Na razie, cwaniaku.",
        "Wątroba ci dziękuje. Ja nie – śmieci dalej nie wyniesione.",
        "Grzeczny jesteś. Aż podejrzanie, kurwa.",
    )
    private val warn = listOf(
        "Hamuj, pijusie, bo zaraz będzie awantura.",
        "„Jeszcze jedno i do domu”? Znam ja te twoje jedno.",
        "Jeszcze trochę i śpisz na wycieraczce.",
    )
    private val over = listOf(
        "Ty moczymordo jedna! Dziś śpisz na kanapie.",
        "Znowu nawalony. Dzieci patrzą, kurwa!",
        "Teściowa miała rację co do ciebie.",
    )

    override fun tagline(status: Status, seed: Int) = pick(
        when (status) {
            Status.OK -> ok
            Status.WARN -> warn
            Status.OVER -> over
        },
        seed,
    )

    override fun bacLabel(level: BacLevel) = when (level) {
        BacLevel.SOBER -> "Trzeźwy" to "Na razie. Zobaczymy, ile wytrzymasz."
        BacLevel.LIGHT -> "Już ci się oczka świecą" to "Ani mi się waż siadać za kółko."
        BacLevel.IMPAIRED -> "Wstawiony" to "Kluczyki oddajesz mnie. Natychmiast."
        BacLevel.HIGH -> "Zalany w trupa" to "Wstyd przed sąsiadami, kurwa mać."
    }

    override fun streakLine(streak: Int) = when (streak) {
        0 -> "Dziś już chlałeś. Nie udawaj."
        1 -> "Jeden dzień na sucho. Medal ci dać?"
        else -> "$streak dni na sucho. Nie przyzwyczajaj się do pochwał."
    }

    override fun widgetLine(status: Status, streak: Int) = when (status) {
        Status.OK -> if (streak >= 2) "$streak dni na sucho. Podejrzane." else "Trzeźwy? Na razie."
        Status.WARN -> "Hamuj, pijusie! 🩴"
        Status.OVER -> "Moczymordo, koniec! 🍳"
    }

    override fun alert(details: StatusDetails): Pair<String, String> = when (details.status) {
        Status.OVER -> "Limit przechlany 🍳" to
            if (details.overUnits > 0) "Przekroczyłeś o ${fmt(details.overUnits)} SU. Do poniedziałku ani kropli, moczymordo."
            else "Dni z piciem przekroczone (${details.drinkingDays}/${details.drinkingDays - details.overDays}). Do poniedziałku ani kropli."
        else -> "Hamuj, pijusie 🩴" to
            "Zostało ci ledwo ${fmt(details.remainingUnits)} SU i ${details.remainingDays} dni. Nie przeginaj."
    }

    override fun planMessage(details: StatusDetails, oneBeerUnits: Double): Pair<String, String> {
        if (details.status == Status.OVER) {
            val why = if (details.overUnits > 0) "Przechlałeś limit o ${fmt(details.overUnits)} SU" else "Wszystkie dni z piciem wykorzystane"
            return "Koniec chlania, słyszysz?! 🍳" to
                "$why. Do poniedziałku ani kropli, bo dostaniesz patelnią."
        }
        val remaining = details.remainingUnits
        val beers = Fancy.beersLeft(remaining, oneBeerUnits)
        val days = if (details.remainingDays > 0) " i ${details.remainingDays} dni" else ", a dni z piciem już nie masz"
        return when {
            remaining <= 0.05 ->
                "To był ostatni. OSTATNI, kurwa 🍳" to
                    "Limit wychlany do zera. Jeszcze raz otworzysz lodówkę po piwo, to zobaczysz."
            beers == 0 ->
                "Zostały ci ochłapy 🩴" to
                    "Mniej niż jedno piwo (${fmt(remaining)} SU)$days. Kranówkę se pij, jest za darmo."
            details.status == Status.WARN ->
                "Hamuj, pijusie 🩴" to
                    "Zostało ci max $beers ${Fancy.beerWord(beers)} (${fmt(remaining)} SU)$days. Rozplanuj to, a nie wszystko w piątek jak zwykle."
            else ->
                "Masz jeszcze zapas, ale nie przeginaj 🍺" to
                    "Max $beers ${Fancy.beerWord(beers)} (${fmt(remaining)} SU)$days do końca tygodnia. I nie myśl, że nie liczę."
        }
    }

    override fun weeklyMessage(details: StatusDetails, streak: Int): Pair<String, String> {
        val title = "Podsumowanie tygodnia 🍳"
        val body = when (details.status) {
            Status.OK -> "${fmt(details.total)} SU. Jak na ciebie to cud. Nie przyzwyczajaj się do pochwał."
            Status.WARN -> "Ledwo się zmieściłeś: ${fmt(details.total)} SU w ${details.drinkingDays} dni. Następny tydzień mniej, obiecujesz? Ta, jasne."
            Status.OVER -> "Przechlałeś limit (${fmt(details.total)} SU). Od poniedziałku nowe życie, jak co tydzień, nie?"
        }
        return title to body
    }

    override fun nudge(n: Nudge): Pair<String, String> = when (n) {
        is Nudge.DontDrive ->
            "Kluczyki zostaw, kurwa! 🔑" to
                "Masz jeszcze ok. ${fmt(n.perMille)} ‰. Za kółko najwcześniej za ${fmt(n.hoursToSober)} h, bo sama zadzwonię po policję."
        Nudge.AfterYesterday ->
            "Wczoraj chlałeś, dziś przerwa 🩴" to
                "Nawet nie patrz w stronę Żabki. Dzisiaj dzień bez procentów i koniec dyskusji."
        is Nudge.WeekOver ->
            "Dzisiaj nie waż się pić! 🍳" to
                "Limit przechlany do poniedziałku. Piwo z lodówki już wylałam do zlewu."
        Nudge.DaysUsedUp ->
            "Dni z piciem wykorzystane 🗓️" to
                "Dzisiaj nawet nie myśl o otwieraniu. Weekend jest dla grzecznych, a ty nie jesteś."
        is Nudge.WeekendBudget ->
            "Piąteczek, wiem, wiem 🙄" to
                "Ale max ${n.beers} ${Fancy.beerWord(n.beers)} do końca tygodnia, zrozumiano? Nie „jeszcze jedno i do domu”, tylko ${n.beers} i koniec."
        is Nudge.DryStreak ->
            "No proszę, ${n.days} dni na sucho 🌱" to
                "Nie będę cię chwalić, bo się rozbestwisz. Ale… dobrze. Tak trzymaj, pijusie."
        is Nudge.EveningStop ->
            "Odstaw tę butelkę! 🍳" to
                "Dzisiaj już ${fmt(n.unitsToday)} SU. Starczy, idź spać, bo jutro znowu będziesz jęczał, że łeb napierdala."
        Nudge.EveningDry ->
            "Dzień bez picia zaliczony 🌱" to
                "Nie wierzę. Zrób zdjęcie, pokażę mamie."
    }
}
