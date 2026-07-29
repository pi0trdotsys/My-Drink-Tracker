import { useState } from "react";
import { Slider } from "@/components/ui/slider";
import { cn } from "@/lib/utils";
import { DRINK_TYPES, RECENT_LOG, fmt, toUnits } from "@/lib/alcohol";

export function LogDrinkMock() {
  const [selected, setSelected] = useState(DRINK_TYPES[0]);
  const [volume, setVolume] = useState(DRINK_TYPES[0].volumeMl);
  const [abv, setAbv] = useState(DRINK_TYPES[0].abv);
  const units = toUnits(volume, abv);

  return (
    <div className="rounded-[22px] border border-border bg-card p-5">
      <h3 className="font-display text-xl tracking-tight">Zaloguj trunek</h3>
      <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-4">
        {DRINK_TYPES.map((d) => (
          <button
            key={d.id}
            type="button"
            onClick={() => {
              setSelected(d);
              setVolume(d.volumeMl);
              setAbv(d.abv);
            }}
            className={cn(
              "rounded-2xl border border-border bg-surface-2/60 p-3 text-left transition-colors hover:bg-accent",
              selected.id === d.id && "border-primary bg-accent",
            )}
          >
            <div className="inline-flex rounded-md bg-primary/15 px-1.5 py-0.5 font-display text-[11px] tracking-wide text-primary">
              {d.emoji}
            </div>
            <div className="mt-1.5 text-xs font-medium leading-tight">{d.name}</div>
            <div className="text-[11px] text-muted-foreground">
              {d.volumeMl} ml · {d.abv}% · {fmt(toUnits(d.volumeMl, d.abv))} SU
            </div>
          </button>
        ))}
        <button
          type="button"
          className="rounded-2xl border border-dashed border-border p-3 text-left text-xs text-muted-foreground transition-colors hover:bg-accent"
        >
          <div className="font-display text-base leading-none text-primary">+</div>
          <div className="mt-1.5 font-medium">Własny typ</div>
        </button>
      </div>

      <div className="mt-5 grid gap-5 sm:grid-cols-2">
        <div>
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>Objętość</span>
            <span className="text-foreground">{volume} ml</span>
          </div>
          <Slider
            className="mt-3"
            value={[volume]}
            min={25}
            max={750}
            step={25}
            onValueChange={([v]) => setVolume(v)}
          />
        </div>
        <div>
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>Zawartość alkoholu</span>
            <span className="text-foreground">{abv}%</span>
          </div>
          <Slider
            className="mt-3"
            value={[abv]}
            min={1}
            max={60}
            step={0.5}
            onValueChange={([v]) => setAbv(v)}
          />
        </div>
      </div>

      <div className="mt-5 flex items-center justify-between rounded-2xl bg-surface-2/70 p-4">
        <div>
          <div className="text-[11px] uppercase tracking-widest text-muted-foreground">
            Przelicznik
          </div>
          <div className="font-display text-3xl leading-none tracking-tight">{fmt(units)} SU</div>
        </div>
        <span className="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground">
          Dodaj wpis
        </span>
      </div>

      <ul className="mt-4 divide-y divide-border text-sm">
        {RECENT_LOG.map((e, i) => (
          <li key={i} className="flex items-center justify-between py-2">
            <span>
              {e.name}
              <span className="ml-2 text-xs text-muted-foreground">
                {e.day}, {e.time}
              </span>
            </span>
            <span className="text-muted-foreground">{fmt(e.units)} SU</span>
          </li>
        ))}
      </ul>
    </div>
  );
}