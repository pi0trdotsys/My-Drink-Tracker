import { createFileRoute } from "@tanstack/react-router";
import { AlcoholWidget } from "@/components/AlcoholWidget";
import { LogDrinkMock } from "@/components/LogDrinkMock";
import { SettingsMock } from "@/components/SettingsMock";
import { WEEK_OK, WEEK_OVER, WEEK_WARN } from "@/lib/alcohol";

const title = "Widget spożycia alkoholu – makiety tygodniowe";
const description =
  "Makiety widgetu tygodniowego spożycia alkoholu: wykres dni, jednostki SU, limity, ostrzeżenia i rozmiary 2x1, 2x2 oraz 4x2.";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title },
      { name: "description", content: description },
      { property: "og:title", content: title },
      { property: "og:description", content: description },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Index,
});

const STATES = [
  { key: "ok", label: "W normie", week: WEEK_OK },
  { key: "warn", label: "Blisko limitu", week: WEEK_WARN },
  { key: "over", label: "Przekroczony", week: WEEK_OVER },
] as const;

function Section({
  eyebrow,
  title: heading,
  children,
}: {
  eyebrow: string;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section className="mt-14">
      <div className="text-[11px] uppercase tracking-[0.2em] text-primary">{eyebrow}</div>
      <h2 className="mt-1 font-display text-2xl tracking-tight sm:text-3xl">{heading}</h2>
      <div className="mt-5">{children}</div>
    </section>
  );
}

function Index() {
  return (
    <main className="min-h-screen bg-background px-5 py-12 font-sans text-foreground">
      <div className="mx-auto max-w-5xl">
        <header>
          <div className="text-[11px] uppercase tracking-[0.25em] text-primary">
            Makiety widgetu
          </div>
          <h1 className="mt-2 font-display text-4xl leading-tight tracking-tight sm:text-5xl">
            Spożycie alkoholu w bieżącym tygodniu
          </h1>
          <p className="mt-3 max-w-2xl text-sm text-muted-foreground">
            Jednostka standardowa (SU) = 10 g czystego etanolu. Wzór: objętość × %ABV × 0,789 / 10.
            Wszystkie dane poniżej są przykładowe.
          </p>
        </header>

        <Section eyebrow="Rozmiar 4×2" title="Widget duży">
          <div className="grid gap-5 lg:grid-cols-3">
            {STATES.map((s) => (
              <div key={s.key}>
                <div className="mb-2 text-xs text-muted-foreground">{s.label}</div>
                <AlcoholWidget week={s.week} size="4x2" />
              </div>
            ))}
          </div>
        </Section>

        <Section eyebrow="Rozmiar 2×2" title="Widget średni">
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {STATES.map((s) => (
              <div key={s.key}>
                <div className="mb-2 text-xs text-muted-foreground">{s.label}</div>
                <AlcoholWidget week={s.week} size="2x2" />
              </div>
            ))}
          </div>
        </Section>

        <Section eyebrow="Rozmiar 2×1" title="Widget minimalny – wszystko wciąż widoczne">
          <div className="grid max-w-md gap-4">
            {STATES.map((s) => (
              <div key={s.key}>
                <div className="mb-2 text-xs text-muted-foreground">{s.label}</div>
                <AlcoholWidget week={s.week} size="2x1" />
              </div>
            ))}
          </div>
        </Section>

        <Section eyebrow="Ekran aplikacji" title="Logowanie trunku i ustawienia">
          <div className="grid gap-5 lg:grid-cols-2">
            <LogDrinkMock />
            <SettingsMock />
          </div>
        </Section>

        <p className="mt-14 text-xs text-muted-foreground">
          Kolejny krok: konta i zapis wpisów w Lovable Cloud.
        </p>
      </div>
    </main>
  );
}
