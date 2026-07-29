## Cel

Strona-makieta prezentująca widget tygodniowego spożycia alkoholu w trzech rozmiarach, plus makiety ekranu logowania trunku i ustawień limitów. Wszystko na danych przykładowych (bez bazy) — Lovable Cloud dołączamy w kolejnym kroku, gdy zaakceptujesz wygląd.

## Strona `/` — galeria makiet

Ciemny, "night-out" motyw: głęboki granat/grafit, akcent bursztynowy (whisky), stan alarmu w czerwieni. Typografia: kondensowany nagłówek + czytelny grotesk. Kafle widgetu w stylu iOS/Android (zaokrąglone, subtelny szum/gradient) prezentowane na siatce.

### Widget 4x2 (duży)
- Nagłówek: „Ten tydzień" + zakres dat
- Duża liczba: `8,5 SU` z podpisem `/ 14 SU limit`
- Wykres słupkowy pon–nd: wysokość = jednostki dnia, słupki ponad limit dzienny w kolorze ostrzegawczym, dziś podświetlony
- Pasek postępu limitu tygodniowego + `3/4 dni z alkoholem`
- Pasek statusu: OK / Uwaga / Przekroczono

### Widget 2x2 (średni)
- Kompaktowy wykres 7 słupków, liczba SU, mini-pasek limitu, kropka statusu

### Widget 2x1 (minimalny)
- Jeden wiersz: 7 mikro-słupków (sparkline), `8,5/14 SU`, `3/4 dni`, kolorowy pasek statusu z lewej krawędzi
- Zasada: te 4 informacje muszą być czytelne także w tym rozmiarze

Każdy rozmiar pokazany w trzech stanach: w normie, blisko limitu, przekroczony.

### Makieta logowania trunku
Siatka kafelków typów: piwo 0,5 l (2,0 SU), lampka wina 150 ml (1,5 SU), kieliszek wódki 50 ml (1,6 SU), whisky 50 ml, gin z tonikiem, drink, „+ własny". Po wyborze: suwaki objętości i % alkoholu z podglądem przeliczenia na SU w czasie rzeczywistym.

### Makieta ustawień
- Limit tygodniowy w SU (suwak)
- Limit dni z alkoholem w tygodniu
- Definicja jednostki (10 g etanolu — edytowalna)
- Edytor typów trunków: nazwa, domyślna objętość, %, ikona
- Próg ostrzeżenia (np. 80% limitu)

## Szczegóły techniczne

- Wzór: `SU = objętość_ml × (%ABV/100) × 0,789 / 10`
- Wykres: własne słupki na CSS/flex (bez biblioteki) — pełna kontrola nad skalowaniem do 2x1
- Skalowanie widgetu: jeden komponent `<AlcoholWidget size="2x1"|"2x2"|"4x2" />` z wariantami przez `cva`, dane wstrzykiwane propsem z pliku `src/lib/mock-week.ts`
- Kolory i stany statusu jako tokeny semantyczne w `src/styles.css` (oklch), zero hardkodowanych kolorów
- Metadane `head()` dla `/`

## Poza zakresem tego kroku

Konto użytkownika, tabele w Lovable Cloud, realny zapis logów — do zrobienia po akceptacji makiet.
