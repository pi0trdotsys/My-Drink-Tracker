export const ETHANOL_DENSITY = 0.789;
/** grams of pure ethanol per standard unit (SU) */
export const GRAMS_PER_UNIT = 10;

export type DrinkType = {
  id: string;
  name: string;
  emoji: string;
  volumeMl: number;
  abv: number;
};

export const DRINK_TYPES: DrinkType[] = [
  { id: "beer", name: "Piwo", emoji: "PIW", volumeMl: 500, abv: 5 },
  { id: "wine", name: "Lampka wina", emoji: "WIN", volumeMl: 150, abv: 12 },
  { id: "vodka", name: "Kieliszek wódki", emoji: "WÓD", volumeMl: 50, abv: 40 },
  { id: "whisky", name: "Whisky", emoji: "WHI", volumeMl: 50, abv: 43 },
  { id: "gin", name: "Gin z tonikiem", emoji: "GIN", volumeMl: 250, abv: 9 },
  { id: "drink", name: "Drink", emoji: "DRK", volumeMl: 200, abv: 14 },
  { id: "prosecco", name: "Prosecco", emoji: "PRO", volumeMl: 120, abv: 11 },
];

export function toUnits(volumeMl: number, abv: number, gramsPerUnit = GRAMS_PER_UNIT) {
  return (volumeMl * (abv / 100) * ETHANOL_DENSITY) / gramsPerUnit;
}

export const fmt = (n: number) => n.toFixed(1).replace(".", ",");

export type DayEntry = { label: string; units: number };

export type WeekData = {
  range: string;
  days: DayEntry[];
  todayIndex: number;
  unitLimit: number;
  dayLimit: number;
  warnRatio: number;
};

export type Status = "ok" | "warn" | "over";

export const STATUS_LABEL: Record<Status, string> = {
  ok: "W normie",
  warn: "Uwaga",
  over: "Przekroczono",
};

export function weekSummary(week: WeekData) {
  const total = week.days.reduce((s, d) => s + d.units, 0);
  const drinkingDays = week.days.filter((d) => d.units > 0).length;
  const status: Status =
    total > week.unitLimit || drinkingDays > week.dayLimit
      ? "over"
      : total >= week.unitLimit * week.warnRatio || drinkingDays >= week.dayLimit
        ? "warn"
        : "ok";
  return { total, drinkingDays, status };
}

export type StatusDetails = {
  total: number;
  drinkingDays: number;
  status: Status;
  ratio: number;
  /** SU above the limit (0 when within limit) */
  overUnits: number;
  /** SU left to the limit (0 when exceeded) */
  remainingUnits: number;
  /** drinking days above the day limit */
  overDays: number;
  remainingDays: number;
  /** short headline for the widget alert */
  headline: string;
  /** full sentence explaining the status */
  message: string;
  /** compact chips, e.g. "+2,4 SU", "+1 dzień" */
  reasons: string[];
};

export function statusDetails(week: WeekData): StatusDetails {
  const { total, drinkingDays, status } = weekSummary(week);
  const overUnits = Math.max(total - week.unitLimit, 0);
  const remainingUnits = Math.max(week.unitLimit - total, 0);
  const overDays = Math.max(drinkingDays - week.dayLimit, 0);
  const remainingDays = Math.max(week.dayLimit - drinkingDays, 0);
  const ratio = week.unitLimit > 0 ? total / week.unitLimit : 0;

  const reasons: string[] = [];
  if (overUnits > 0) reasons.push(`+${fmt(overUnits)} SU ponad limit`);
  if (overDays > 0) reasons.push(`+${overDays} dzień z alkoholem ponad limit`);
  if (status === "warn") {
    if (overUnits === 0 && remainingUnits <= week.unitLimit * (1 - week.warnRatio))
      reasons.push(`zostało ${fmt(remainingUnits)} SU`);
    if (overDays === 0 && remainingDays === 0) reasons.push("wykorzystane wszystkie dni");
  }

  let headline: string;
  let message: string;
  if (status === "over") {
    headline = "Limit przekroczony";
    message =
      overUnits > 0
        ? `Przekroczono tygodniowy limit o ${fmt(overUnits)} SU. Zrób przerwę do końca tygodnia.`
        : `Przekroczono limit dni z alkoholem (${drinkingDays}/${week.dayLimit}). Zrób przerwę do końca tygodnia.`;
  } else if (status === "warn") {
    headline = "Blisko limitu";
    message =
      remainingUnits > 0
        ? `Zostało tylko ${fmt(remainingUnits)} SU i ${remainingDays} dni z alkoholem.`
        : `Wykorzystano cały limit dni z alkoholem w tym tygodniu.`;
  } else {
    headline = "W normie";
    message = `Zostało ${fmt(remainingUnits)} SU i ${remainingDays} dni z alkoholem.`;
  }

  return {
    total,
    drinkingDays,
    status,
    ratio,
    overUnits,
    remainingUnits,
    overDays,
    remainingDays,
    headline,
    message,
    reasons,
  };
}

const DAYS = ["Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd"];
const base = { range: "27 lip – 2 sie", todayIndex: 4, unitLimit: 14, dayLimit: 4, warnRatio: 0.8 };

const mkDays = (u: number[]): DayEntry[] => u.map((units, i) => ({ label: DAYS[i], units }));

export const WEEK_OK: WeekData = { ...base, days: mkDays([0, 2, 0, 0, 2.6, 0, 0]) };
export const WEEK_WARN: WeekData = { ...base, days: mkDays([0, 2, 0, 3.2, 6.1, 0, 0]) };
export const WEEK_OVER: WeekData = { ...base, days: mkDays([2, 3.2, 0, 4, 7.4, 2.6, 0]) };

export const RECENT_LOG = [
  { day: "Piątek", time: "21:40", name: "Whisky 50 ml", units: toUnits(50, 43) },
  { day: "Piątek", time: "20:10", name: "Piwo 0,5 l", units: toUnits(500, 5) },
  { day: "Czwartek", time: "19:25", name: "Lampka wina", units: toUnits(150, 12) },
];