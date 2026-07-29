import { useState } from "react";
import { Slider } from "@/components/ui/slider";
import { Switch } from "@/components/ui/switch";
import { DRINK_TYPES, fmt, toUnits } from "@/lib/alcohol";

export function SettingsMock() {
  const [unitLimit, setUnitLimit] = useState(14);
  const [dayLimit, setDayLimit] = useState(4);
  const [gramsPerUnit, setGramsPerUnit] = useState(10);
  const [warn, setWarn] = useState(80);

  return (
    <div className="rounded-[22px] border border-border bg-card p-5">
      <h3 className="font-display text-xl tracking-tight">Ustawienia limitów</h3>

      <div className="mt-4 space-y-5">
        <div>
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>Limit tygodniowy</span>
            <span className="text-foreground">{unitLimit} SU</span>
          </div>
          <Slider
            className="mt-3"
            value={[unitLimit]}
            min={0}
            max={30}
            step={1}
            onValueChange={([v]) => setUnitLimit(v)}
          />
        </div>
        <div>
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>Maks. dni z alkoholem w tygodniu</span>
            <span className="text-foreground">{dayLimit} dni</span>
          </div>
          <Slider
            className="mt-3"
            value={[dayLimit]}
            min={0}
            max={7}
            step={1}
            onValueChange={([v]) => setDayLimit(v)}
          />
        </div>
        <div>
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>Definicja jednostki (g czystego etanolu)</span>
            <span className="text-foreground">{gramsPerUnit} g</span>
          </div>
          <Slider
            className="mt-3"
            value={[gramsPerUnit]}
            min={8}
            max={14}
            step={1}
            onValueChange={([v]) => setGramsPerUnit(v)}
          />
        </div>
        <div>
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>Próg ostrzeżenia</span>
            <span className="text-foreground">
              {warn}% ({fmt((unitLimit * warn) / 100)} SU)
            </span>
          </div>
          <Slider
            className="mt-3"
            value={[warn]}
            min={50}
            max={100}
            step={5}
            onValueChange={([v]) => setWarn(v)}
          />
        </div>
        <div className="flex items-center justify-between rounded-2xl bg-surface-2/70 p-3">
          <span className="text-sm">Powiadomienia po przekroczeniu limitu</span>
          <Switch defaultChecked />
        </div>
      </div>

      <div className="mt-6">
        <div className="text-[11px] uppercase tracking-widest text-muted-foreground">
          Typy trunków
        </div>
        <ul className="mt-2 divide-y divide-border text-sm">
          {DRINK_TYPES.map((d) => (
            <li key={d.id} className="flex items-center gap-3 py-2">
              <span>{d.emoji}</span>
              <span className="flex-1">{d.name}</span>
              <span className="text-xs text-muted-foreground">
                {d.volumeMl} ml · {d.abv}% ·{" "}
                {fmt(toUnits(d.volumeMl, d.abv, gramsPerUnit))} SU
              </span>
              <span className="text-xs text-primary">Edytuj</span>
            </li>
          ))}
        </ul>
        <button
          type="button"
          className="mt-3 w-full rounded-2xl border border-dashed border-border py-2 text-sm text-muted-foreground transition-colors hover:bg-accent"
        >
          ＋ Dodaj własny typ
        </button>
      </div>
    </div>
  );
}