package com.devqube.drinktracker.data

/**
 * „Głos” aplikacji – wszystkie teksty zależne od motywu: nagłówki, hasła, opis promili,
 * napisy na widgecie, powiadomienia o planie, podsumowanie tygodnia i przypomnienia dzienne.
 */
interface Voice {
    val homeEyebrow: String
    val homeTitle: String
    val bacEyebrow: String
    val logTitle: String
    val addButton: String
    val recentTitle: String

    /** Nagłówek karty i widgetu („TEN TYDZIEŃ”). */
    val weekHeader: String

    /** Przycisk szybkiego dodania na dużym widgecie. */
    val quickAdd: String

    /** Podpis dymka na dużym widgecie albo `null`, gdy motyw nie ma „postaci”. */
    val speechTitle: String?

    fun statusLabel(status: Status): String
    fun daysLabel(days: Int, limit: Int, long: Boolean): String

    /** Poziom wkurwu albo `null` – wtedy zwykły pasek limitu. */
    fun meter(details: StatusDetails): Meter?

    /** Ikona narzędzia albo `null` – wtedy kropka statusu. */
    fun tool(status: Status): Tool?

    fun tagline(status: Status, seed: Int): String
    fun bacLabel(level: BacLevel): Pair<String, String>
    fun streakLine(streak: Int): String

    /** Krótka linijka na widget (jeden wiersz). */
    fun widgetLine(m: WidgetMoment): String

    /** Dłuższy tekst do dymka na dużym widgecie (2–3 wiersze). */
    fun widgetSpeech(m: WidgetMoment): String

    /** Nagłówek i treść ramki alertu na karcie tygodnia (tylko statusy WARN i OVER). */
    fun alert(details: StatusDetails): Pair<String, String>

    fun planMessage(details: StatusDetails, oneBeerUnits: Double): Pair<String, String>
    fun weeklyMessage(details: StatusDetails, streak: Int): Pair<String, String>
    fun nudge(n: Nudge): Pair<String, String>
}

fun voiceFor(theme: AppTheme, vulgarity: Vulgarity = Vulgarity.MAX): Voice = when (theme) {
    AppTheme.CLASSIC -> ClassicVoice
    AppTheme.GRAZYNA -> GrazynaVoice(vulgarity)
}

object ClassicVoice : Voice {
    override val homeEyebrow = "Twój tydzień"
    override val homeTitle = "Spożycie alkoholu"
    override val bacEyebrow = "Szacowany poziom alkoholu"
    override val logTitle = "Zaloguj trunek"
    override val addButton = "Dodaj wpis"
    override val recentTitle = "Ostatnie wpisy"
    override val weekHeader = "TEN TYDZIEŃ"
    override val quickAdd = "+ Dodaj"
    override val speechTitle: String? = null

    override fun statusLabel(status: Status) = com.devqube.drinktracker.data.statusLabel(status)
    override fun daysLabel(days: Int, limit: Int, long: Boolean) =
        if (long) "$days/$limit dni z alkoholem" else "$days/$limit dni"
    override fun meter(details: StatusDetails): Meter? = null
    override fun tool(status: Status): Tool? = null

    override fun tagline(status: Status, seed: Int) = Fancy.tagline(status, seed)
    override fun bacLabel(level: BacLevel) = level.label to level.note
    override fun streakLine(streak: Int) = Fancy.streakLine(streak)

    override fun widgetLine(m: WidgetMoment) = when (m.moment) {
        Moment.HANGOVER -> "Dziś dzień przerwy? 💧"
        Moment.EVENING_DRINKING -> "Może już wystarczy? 🌙"
        else -> Fancy.streakLine(m.streak)
    }

    override fun widgetSpeech(m: WidgetMoment) = Fancy.tagline(m.status, m.seed)
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
 * Eksperymentalny motyw 18+: memiczna, wulgarna „wkurzona żona”. Gdera, przeklina, wyzywa od
 * moczymord i ochlapusów i straszy patelnią – ale zawsze w stronę picia mniej, nigdy więcej.
 *
 * @param level jak mocno przeklina
 * @param clock źródło czasu dla losowania słów (słowa zmieniają się co minutę); w testach stałe
 */
class GrazynaVoice(
    val level: Vulgarity = Vulgarity.MAX,
    private val clock: () -> Long = System::currentTimeMillis,
) : Voice {

    private fun words(seed: Int = (clock() / 60_000).toInt()) = Words(level, seed)

    override val homeEyebrow = "Rachunek sumienia"
    override val homeTitle = "Ile znowu wychlałeś"
    override val bacEyebrow: String get() = "Ile w tobie promili, ${words().nick()}"
    override val logTitle = "Przyznaj się, co piłeś"
    override val addButton = "Przyznaję się"
    override val recentTitle = "Lista grzechów"
    override val weekHeader = "RACHUNEK SUMIENIA"
    override val quickAdd = "+ Przyznaję się"
    override val speechTitle = "Grażyna mówi:"

    override fun statusLabel(status: Status) = when (status) {
        Status.OK -> "Na razie trzeźwy"
        Status.WARN -> "Uważaj, łajzo"
        Status.OVER -> "Przechlane, kurwa"
    }

    override fun daysLabel(days: Int, limit: Int, long: Boolean) = "$days/$limit dni chlania"

    override fun meter(details: StatusDetails): Meter {
        val p = wrathPercent(details)
        return Meter(p, wrathLabel(p))
    }

    override fun tool(status: Status) = toolFor(status)

    // --- Hasła w nagłówku aplikacji ---

    private val okLines: List<Line> = listOf(
        { "No proszę, trzeźwy. Mam to wpisać do kalendarza, ${nick()}?" },
        { "Na razie się trzymasz, ${nick()}. Na razie." },
        { "Wątroba ci dziękuje. Ja nie – śmieci dalej nie wyniesione." },
        { "Grzeczny jesteś. Aż podejrzanie, ${curse()}." },
        { "Trzeźwy jak niemowlę. Kto cię podmienił, ${nick()}?" },
    )
    private val okMax: List<Line> = listOf(
        { "Nie chlałeś? Ja pierdolę, cud nad Wisłą." },
    )
    private val warnLines: List<Line> = listOf(
        { "Hamuj, ${nick()}, bo zaraz będzie awantura." },
        { "„Jeszcze jedno i do domu”? Znam ja te twoje jedno, ${nick()}." },
        { "Jeszcze trochę i śpisz na wycieraczce, ${curse()}." },
        { "Patelnia już się grzeje, ${nick()}." },
        { "Ty ${nick()}! Nie przeginaj, bo dzwonię do mamusi." },
    )
    private val warnMax: List<Line> = listOf(
        { "Jeszcze jedno piwo i wyjebię ci browary przez okno, ${nick()}." },
    )
    private val overLines: List<Line> = listOf(
        { "Ty ${nick()}! Dziś śpisz na kanapie." },
        { "Znowu ${drunk()}. Dzieci patrzą, ${curse()}!" },
        { "Teściowa miała rację co do ciebie, ${nick()}." },
        { "Limit poszedł się bujać, ${nick()}. Kanapa czeka." },
    )
    private val overMax: List<Line> = listOf(
        { "Ja pierdolę, znowu w trzy dupy. Dzwonię do mamusi." },
        { "Limit poszedł w pizdu, ${nick()}. Spierdalaj na kanapę." },
        { "${Nick()}, najebany jak stodoła. Wstyd przed sąsiadami." },
    )

    override fun tagline(status: Status, seed: Int) = words(seed).run {
        when (status) {
            Status.OK -> pick(okLines, okMax)
            Status.WARN -> pick(warnLines, warnMax)
            Status.OVER -> pick(overLines, overMax)
        }
    }

    override fun bacLabel(level: BacLevel) = words().run {
        when (level) {
            BacLevel.SOBER -> "Trzeźwy" to "Na razie. Zobaczymy, ile wytrzymasz, ${nick()}."
            BacLevel.LIGHT -> "Już ci się oczka świecą" to "Ani mi się waż siadać za kółko, ${nick()}."
            BacLevel.IMPAIRED -> hard("Najebany", "Wstawiony") to "Kluczyki oddajesz mnie. Natychmiast, ${curse()}."
            BacLevel.HIGH -> "Zalany w trupa" to "Wstyd przed sąsiadami, ${curse()}."
        }
    }

    override fun streakLine(streak: Int) = words().run {
        when (streak) {
            0 -> "Dziś już chlałeś, ${nick()}. Nie udawaj."
            1 -> "Jeden dzień na sucho. Medal ci dać, ${nick()}?"
            else -> "$streak dni na sucho. Nie przyzwyczajaj się do pochwał."
        }
    }

    // --- Widget: krótka linijka ---

    override fun widgetLine(m: WidgetMoment): String = words(m.seed).run {
        val beers = "${m.beersLeft} ${Fancy.beerWord(m.beersLeft)}"
        when (m.moment) {
            Moment.HANGOVER -> pick(
                listOf(
                    { "Kac morderca? Dobrze ci tak, ${nick()}." },
                    { "Klina nie będzie, ${nick()}." },
                    { "Łeb boli? ${Curse()}, a mówiłam." },
                ),
                listOf({ "Łeb napierdala? Zasłużyłeś, ${nick()}." }),
            )
            Moment.TEMPTATION -> pick(
                listOf(
                    { "Nawet nie myśl o Żabce, ${nick()}." },
                    { "Małpka? Po moim trupie, ${nick()}." },
                    { "Harnaś w promocji? Nie dla ciebie." },
                ),
                listOf({ "Do Żabki? Chuja, nie do Żabki." }),
            )
            Moment.EVENING_DRINKING -> pick(
                listOf(
                    { "Odstaw butelkę, ${nick()}!" },
                    { "Starczy, ${nick()}. Spać." },
                ),
                listOf({ "Odstaw tę małpkę, bo wyjebię wałkiem." }),
            )
            Moment.NONE -> when (m.status) {
                Status.OK -> pick(
                    listOf(
                        { "Trzeźwy? Podejrzane, ${nick()}." },
                        { "Nie piłeś? ${Curse()}, cud." },
                        { if (m.streak >= 2) "${m.streak} dni na sucho. Kto cię podmienił?" else "Grzeczny, ${nick()}. Na razie." },
                    ),
                    listOf({ "Trzeźwy? Ja pierdolę, nie wierzę." }),
                )
                Status.WARN -> pick(
                    listOf(
                        { "Hamuj, ${nick()}! 🩴" },
                        { "Patelnia się grzeje, ${nick()} 🍳" },
                        { if (m.beersLeft > 0) "Zostało $beers, ${nick()}." else "Ani łyka więcej, ${nick()}." },
                        { "Jeszcze jedno? ${Curse()}, nie." },
                    ),
                )
                Status.OVER -> pick(
                    listOf(
                        { "${Nick()}, koniec! 🍳" },
                        { "Kanapa czeka, ${nick()}." },
                        { "Ani kropli, ${nick()}!" },
                    ),
                    listOf(
                        { "Limit w pizdu, ${nick()}." },
                        { "Ja pierdolę, znowu?!" },
                    ),
                )
            }
        }
    }

    // --- Widget: dymek „Grażyna mówi:” ---

    override fun widgetSpeech(m: WidgetMoment): String = words(m.seed + 7).run {
        val beers = "${m.beersLeft} ${Fancy.beerWord(m.beersLeft)}"
        when (m.moment) {
            Moment.HANGOVER -> pick(
                listOf({ "Kac morderca, co? Klina nie będzie, ${nick()}. Woda, rosół i siedź cicho." }),
                listOf({ "Łeb napierdala? Dobrze ci tak, ${nick()}. Klina nie będzie, ani dziś, ani jutro." }),
            )
            Moment.TEMPTATION -> pick(
                listOf({ "Wiem, że ciągnie cię do Żabki, ${nick()}. Nawet nie próbuj, bo wałek już czeka." }),
                listOf({ "Do Żabki się wybierasz? Chuja tam, ${nick()}. Siadaj i pij herbatę." }),
            )
            Moment.EVENING_DRINKING -> pick(
                listOf({ "Dzisiaj już wystarczy, ${nick()}. Odstaw butelkę i idź spać, bo jutro znowu będziesz jęczał." }),
                listOf({ "Odstaw to, ${nick()}, bo jutro łeb będzie napierdalał i znowu będzie na mnie." }),
            )
            Moment.NONE -> when (m.status) {
                Status.OK -> pick(
                    listOf(
                        { "Na razie trzeźwy. Ale ja wszystko widzę i wszystko liczę, ${nick()}." },
                        { if (m.streak >= 2) "${m.streak} dni na sucho? ${Curse()}, nie wierzę. Pokażę mamie." else "Grzeczny dziś jesteś, ${nick()}. Zobaczymy, ile wytrzymasz." },
                    ),
                    listOf({ "Trzeźwy? Ja pierdolę, trzeba to oblać. Żartuję, ${nick()}, siedź na dupie." }),
                )
                Status.WARN -> pick(
                    listOf(
                        { if (m.beersLeft > 0) "Zostało ci $beers do końca tygodnia, ${nick()}. Jedno więcej i patelnia idzie w ruch." else "Limit prawie wychlany, ${nick()}. Jeszcze łyk i patelnia idzie w ruch." },
                        { "„Jeszcze jedno i do domu”? Ty już jesteś w domu, ${nick()}. Odstaw to." },
                    ),
                )
                Status.OVER -> pick(
                    listOf(
                        { "Limit przechlany, ${nick()}. Do poniedziałku ani kropli, a teraz marsz na kanapę." },
                        { "Znowu ${drunk()}, ${nick()}. Teściowa miała rację, trzeba było brać Zbyszka." },
                    ),
                    listOf({ "Ja pierdolę, znowu ${drunk(1)}. Limit w pizdu, ${nick()}. Spierdalaj na kanapę." }),
                )
            }
        }
    }

    // --- Ramka alertu na karcie ---

    override fun alert(details: StatusDetails): Pair<String, String> = words().run {
        when (details.status) {
            Status.OVER -> "Limit przechlany 🍳" to
                if (details.overUnits > 0) "Przekroczyłeś o ${fmt(details.overUnits)} SU. Do poniedziałku ani kropli, ${nick()}."
                else "Dni z piciem przekroczone (${details.drinkingDays}/${details.drinkingDays - details.overDays}). Do poniedziałku ani kropli, ${nick()}."
            else -> "Hamuj, ${nick()} 🩴" to
                "Zostało ci ledwo ${fmt(details.remainingUnits)} SU i ${details.remainingDays} dni. Nie przeginaj, ${curse()}."
        }
    }

    // --- Powiadomienia ---

    override fun planMessage(details: StatusDetails, oneBeerUnits: Double): Pair<String, String> = words().run {
        if (details.status == Status.OVER) {
            val why = if (details.overUnits > 0) "Przechlałeś limit o ${fmt(details.overUnits)} SU" else "Wszystkie dni z piciem wykorzystane"
            return@run "Koniec chlania, słyszysz?! 🍳" to
                "$why, ${nick()}. Do poniedziałku ani kropli, bo dostaniesz patelnią."
        }
        val remaining = details.remainingUnits
        val beers = Fancy.beersLeft(remaining, oneBeerUnits)
        val days = if (details.remainingDays > 0) " i ${details.remainingDays} dni" else ", a dni z piciem już nie masz"
        when {
            remaining <= 0.05 ->
                "To był ostatni. OSTATNI, kurwa 🍳" to
                    "Limit wychlany do zera, ${nick()}. Jeszcze raz otworzysz lodówkę po piwo, to zobaczysz."
            beers == 0 ->
                "Zostały ci ochłapy 🩴" to
                    "Mniej niż jedno piwo (${fmt(remaining)} SU)$days. Kranówkę se pij, ${nick()}, jest za darmo."
            details.status == Status.WARN ->
                "Hamuj, ${nick()} 🩴" to
                    "Zostało ci max $beers ${Fancy.beerWord(beers)} (${fmt(remaining)} SU)$days. Rozplanuj to, a nie wszystko w piątek jak zwykle, ${curse()}."
            else ->
                "Masz jeszcze zapas, ale nie przeginaj 🍺" to
                    "Max $beers ${Fancy.beerWord(beers)} (${fmt(remaining)} SU)$days do końca tygodnia. I nie myśl, że nie liczę, ${nick()}."
        }
    }

    override fun weeklyMessage(details: StatusDetails, streak: Int): Pair<String, String> = words().run {
        val body = when (details.status) {
            Status.OK -> "${fmt(details.total)} SU. Jak na ciebie to cud, ${nick()}. Nie przyzwyczajaj się do pochwał."
            Status.WARN -> "Ledwo się zmieściłeś: ${fmt(details.total)} SU w ${details.drinkingDays} dni. Następny tydzień mniej, obiecujesz? Ta, jasne, ${nick()}."
            Status.OVER -> hard(
                "Przechlałeś limit (${fmt(details.total)} SU), ${nick()}. Ja pierdolę, od poniedziałku nowe życie, jak co tydzień, nie?",
                "Przechlałeś limit (${fmt(details.total)} SU), ${nick()}. Od poniedziałku nowe życie, jak co tydzień, nie?",
            )
        }
        "Podsumowanie tygodnia 🍳" to body
    }

    override fun nudge(n: Nudge): Pair<String, String> = words().run {
        when (n) {
            is Nudge.DontDrive ->
                "Kluczyki zostaw, kurwa! 🔑" to
                    "Masz jeszcze ok. ${fmt(n.perMille)} ‰, ${nick()}. Za kółko najwcześniej za ${fmt(n.hoursToSober)} h, bo sama zadzwonię po policję."
            Nudge.AfterYesterday ->
                "Wczoraj chlałeś, dziś przerwa 🩴" to
                    "Nawet nie patrz w stronę Żabki, ${nick()}. Dzisiaj dzień bez procentów i koniec dyskusji."
            is Nudge.WeekOver ->
                "Dzisiaj nie waż się pić! 🍳" to
                    hard(
                        "Limit poszedł w pizdu, ${nick()}. Piwo z lodówki już wylałam do zlewu, ani kropli do poniedziałku.",
                        "Limit przechlany do poniedziałku, ${nick()}. Piwo z lodówki już wylałam do zlewu, ani kropli.",
                    )
            Nudge.DaysUsedUp ->
                "Dni z piciem wykorzystane 🗓️" to
                    "Dzisiaj nawet nie myśl o otwieraniu, ${nick()}. Weekend jest dla grzecznych, a ty nie jesteś."
            is Nudge.WeekendBudget ->
                "Piąteczek, wiem, wiem 🙄" to
                    "Ale max ${n.beers} ${Fancy.beerWord(n.beers)} do końca tygodnia, zrozumiano, ${nick()}? Nie „jeszcze jedno i do domu”, tylko ${n.beers} i koniec."
            is Nudge.DryStreak ->
                "No proszę, ${n.days} dni na sucho 🌱" to
                    "Nie będę cię chwalić, bo się rozbestwisz. Ale… dobrze. Tak trzymaj, ${nick()}."
            is Nudge.EveningStop ->
                "Odstaw tę butelkę! 🍳" to
                    "Dzisiaj już ${fmt(n.unitsToday)} SU, ${nick()}. Starczy, idź spać, bo jutro znowu będziesz jęczał, że ${hard("łeb napierdala", "łeb pęka")}."
            Nudge.EveningDry ->
                "Dzień bez picia zaliczony 🌱" to
                    "Nie wierzę, ${curse()}. Zrób zdjęcie, pokażę mamie."
        }
    }
}
