import { cn } from "@/lib/utils";
import {
  fmt,
  STATUS_LABEL,
  weekSummary,
  type Status,
  type WeekData,
} from "@/lib/alcohol";

const statusBg: Record<Status, string> = {
  ok: "bg-ok",
  warn: "bg-warn",
  over: "bg-over",
};
const statusText: Record<Status, string> = {
  ok: "text-ok",
  warn: "text-warn",
  over: "text-over",
};

type Size = "2x1" | "2x2" | "4x2";

function Bars({
  week,
  status,
  height,
  showLabels,
}: {
  week: WeekData;
  status: Status;
  height: string;
  showLabels: boolean;
}) {
  const dailyRef = week.unitLimit / week.dayLimit;
  const max = Math.max(dailyRef * 1.4, ...week.days.map((d) => d.units));
  return (
    <div className={cn("flex w-full items-end gap-1", height)}>
      {week.days.map((d, i) => {
        const pct = max > 0 ? (d.units / max) * 100 : 0;
        const hot = d.units > dailyRef;
        return (
          <div key={d.label} className="flex h-full flex-1 flex-col items-center justify-end gap-1">
            <div className="flex w-full flex-1 items-end rounded-[3px] bg-surface-2/70">
              <div
                className={cn(
                  "w-full rounded-[3px] transition-all",
                  d.units === 0
                    ? "bg-transparent"
                    : hot
                      ? statusBg[status === "ok" ? "warn" : status]
                      : "bg-primary",
                  i === week.todayIndex && d.units > 0 && "ring-1 ring-foreground/60",
                )}
                style={{ height: `${Math.max(pct, d.units > 0 ? 8 : 0)}%` }}
              />
            </div>
            {showLabels && (
              <span
                className={cn(
                  "text-[10px] leading-none",
                  i === week.todayIndex ? "font-semibold text-foreground" : "text-muted-foreground",
                )}
              >
                {d.label}
              </span>
            )}
          </div>
        );
      })}
    </div>
  );
}

function LimitBar({ ratio, status }: { ratio: number; status: Status }) {
  return (
    <div className="h-1.5 w-full overflow-hidden rounded-full bg-surface-2">
      <div
        className={cn("h-full rounded-full", statusBg[status])}
        style={{ width: `${Math.min(ratio * 100, 100)}%` }}
      />
    </div>
  );
}

export function AlcoholWidget({
  week,
  size = "4x2",
  className,
}: {
  week: WeekData;
  size?: Size;
  className?: string;
}) {
  const { total, drinkingDays, status } = weekSummary(week);
  const ratio = total / week.unitLimit;

  const shell = cn(
    "relative overflow-hidden rounded-[22px] border border-border bg-card p-4 shadow-lg",
    "before:pointer-events-none before:absolute before:inset-0 before:bg-[radial-gradient(120%_100%_at_100%_0%,color-mix(in_oklab,var(--primary)_14%,transparent),transparent_60%)]",
    className,
  );

  if (size === "2x1") {
    return (
      <div className={cn(shell, "flex items-center gap-3 py-3 pl-5")}>
        <span className={cn("absolute left-0 top-0 h-full w-1.5", statusBg[status])} />
        <div className="min-w-0">
          <div className="font-display text-xl leading-none tracking-tight">
            {fmt(total)}
            <span className="text-sm text-muted-foreground">/{week.unitLimit} SU</span>
          </div>
          <div className={cn("mt-1 text-[11px] leading-none", statusText[status])}>
            {drinkingDays}/{week.dayLimit} dni · {STATUS_LABEL[status]}
          </div>
        </div>
        <div className="ml-auto w-[46%]">
          <Bars week={week} status={status} height="h-8" showLabels={false} />
        </div>
      </div>
    );
  }

  if (size === "2x2") {
    return (
      <div className={cn(shell, "flex flex-col gap-2")}>
        <div className="flex items-start justify-between">
          <div>
            <div className="text-[11px] uppercase tracking-widest text-muted-foreground">
              Ten tydzień
            </div>
            <div className="font-display text-3xl leading-tight tracking-tight">
              {fmt(total)}
              <span className="text-base text-muted-foreground"> / {week.unitLimit} SU</span>
            </div>
          </div>
          <span className={cn("mt-1 size-3 rounded-full", statusBg[status])} />
        </div>
        <Bars week={week} status={status} height="h-16" showLabels />
        <LimitBar ratio={ratio} status={status} />
        <div className="flex justify-between text-[11px] text-muted-foreground">
          <span>
            {drinkingDays}/{week.dayLimit} dni
          </span>
          <span className={statusText[status]}>{STATUS_LABEL[status]}</span>
        </div>
      </div>
    );
  }

  return (
    <div className={cn(shell, "flex flex-col gap-3 p-5")}>
      <div className="flex items-baseline justify-between">
        <div className="text-[11px] uppercase tracking-widest text-muted-foreground">
          Ten tydzień
        </div>
        <div className="text-[11px] text-muted-foreground">{week.range}</div>
      </div>
      <div className="flex items-end justify-between gap-4">
        <div>
          <div className="font-display text-5xl leading-none tracking-tight">{fmt(total)}</div>
          <div className="mt-1 text-xs text-muted-foreground">/ {week.unitLimit} SU limitu</div>
        </div>
        <div className="flex-1">
          <Bars week={week} status={status} height="h-24" showLabels />
        </div>
      </div>
      <LimitBar ratio={ratio} status={status} />
      <div className="flex items-center justify-between text-xs">
        <span className="text-muted-foreground">
          {drinkingDays}/{week.dayLimit} dni z alkoholem
        </span>
        <span
          className={cn(
            "rounded-full px-2.5 py-1 text-[11px] font-semibold",
            status === "ok" && "bg-ok text-ok-foreground",
            status === "warn" && "bg-warn text-warn-foreground",
            status === "over" && "bg-over text-over-foreground",
          )}
        >
          {STATUS_LABEL[status]}
        </span>
      </div>
      {status !== "ok" && (
        <p className={cn("text-[11px]", statusText[status])}>
          {status === "over"
            ? `Limit przekroczony o ${fmt(Math.max(total - week.unitLimit, 0))} SU.`
            : `Zostało ${fmt(week.unitLimit - total)} SU do limitu.`}
        </p>
      )}
    </div>
  );
}