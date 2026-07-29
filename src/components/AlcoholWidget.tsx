import { cn } from "@/lib/utils";
import {
  fmt,
  STATUS_LABEL,
  statusDetails,
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
  const filled = Math.min(ratio, 1) * 100;
  const over = Math.min(Math.max(ratio - 1, 0), 1) * 100;
  return (
    <div className="relative h-1.5 w-full overflow-hidden rounded-full bg-surface-2">
      <div
        className={cn("h-full rounded-full transition-all", statusBg[status])}
        style={{ width: `${filled}%` }}
      />
      {over > 0 && (
        <div
          className="absolute inset-y-0 right-0 rounded-full bg-over/60 mix-blend-screen"
          style={{ width: `${Math.max(over, 6)}%` }}
        />
      )}
    </div>
  );
}

function StatusPill({ status, label, pulse }: { status: Status; label: string; pulse?: boolean }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-semibold",
        status === "ok" && "bg-ok text-ok-foreground",
        status === "warn" && "bg-warn text-warn-foreground",
        status === "over" && "bg-over text-over-foreground",
      )}
    >
      {pulse && status !== "ok" && (
        <span className="relative flex size-1.5">
          <span className="absolute inline-flex size-full animate-ping rounded-full bg-current opacity-70" />
          <span className="relative inline-flex size-1.5 rounded-full bg-current" />
        </span>
      )}
      {label}
    </span>
  );
}

function AlertBox({
  status,
  headline,
  message,
  reasons,
  compact,
}: {
  status: Status;
  headline: string;
  message: string;
  reasons: string[];
  compact?: boolean;
}) {
  if (status === "ok") return null;
  return (
    <div
      className={cn(
        "rounded-xl border px-3 py-2",
        status === "over"
          ? "border-over/50 bg-over/12"
          : "border-warn/50 bg-warn/12",
      )}
    >
      <div className={cn("flex items-center gap-1.5 text-[11px] font-semibold", statusText[status])}>
        <span aria-hidden className="text-xs leading-none">
          {status === "over" ? "!!" : "!"}
        </span>
        {headline}
      </div>
      {!compact && (
        <p className="mt-0.5 text-[11px] leading-snug text-muted-foreground">{message}</p>
      )}
      {!compact && reasons.length > 0 && (
        <div className="mt-1.5 flex flex-wrap gap-1">
          {reasons.map((r) => (
            <span
              key={r}
              className={cn(
                "rounded-full border px-2 py-0.5 text-[10px] leading-none",
                status === "over" ? "border-over/50 text-over" : "border-warn/50 text-warn",
              )}
            >
              {r}
            </span>
          ))}
        </div>
      )}
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
  const { total, drinkingDays, status, ratio, overUnits, headline, message, reasons } =
    statusDetails(week);

  const shell = cn(
    "relative overflow-hidden rounded-[22px] border border-border bg-card p-4 shadow-lg",
    "before:pointer-events-none before:absolute before:inset-0 before:bg-[radial-gradient(120%_100%_at_100%_0%,color-mix(in_oklab,var(--primary)_14%,transparent),transparent_60%)]",
    status === "over" && "border-over/60 shadow-[0_0_0_1px_color-mix(in_oklab,var(--over)_45%,transparent)]",
    status === "warn" && "border-warn/50",
    className,
  );

  if (size === "2x1") {
    return (
      <div className={cn(shell, "flex items-center gap-3 py-3 pl-5")}>
        <span
          className={cn(
            "absolute left-0 top-0 h-full w-1.5",
            statusBg[status],
            status === "over" && "animate-pulse",
          )}
        />
        <div className="min-w-0">
          <div
            className={cn(
              "font-display text-xl leading-none tracking-tight",
              status === "over" && "text-over",
            )}
          >
            {fmt(total)}
            <span className="text-sm text-muted-foreground">/{week.unitLimit} SU</span>
          </div>
          <div className={cn("mt-1 flex items-center gap-1 text-[11px] leading-none", statusText[status])}>
            {status !== "ok" && <span aria-hidden>{status === "over" ? "!!" : "!"}</span>}
            <span className="truncate">
              {drinkingDays}/{week.dayLimit} dni ·{" "}
              {status === "over" && overUnits > 0
                ? `+${fmt(overUnits)} SU ponad limit`
                : STATUS_LABEL[status]}
            </span>
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
            <div
              className={cn(
                "font-display text-3xl leading-tight tracking-tight",
                status === "over" && "text-over",
              )}
            >
              {fmt(total)}
              <span className="text-base text-muted-foreground"> / {week.unitLimit} SU</span>
            </div>
          </div>
          <span
            className={cn(
              "mt-1 size-3 rounded-full",
              statusBg[status],
              status !== "ok" && "animate-pulse",
            )}
          />
        </div>
        <Bars week={week} status={status} height="h-16" showLabels />
        <LimitBar ratio={ratio} status={status} />
        <div className="flex justify-between text-[11px] text-muted-foreground">
          <span>
            {drinkingDays}/{week.dayLimit} dni
          </span>
          <span className={statusText[status]}>{STATUS_LABEL[status]}</span>
        </div>
        <AlertBox status={status} headline={headline} message={message} reasons={reasons} compact />
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
          <div
            className={cn(
              "font-display text-5xl leading-none tracking-tight",
              status === "over" && "text-over",
            )}
          >
            {fmt(total)}
          </div>
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
        <StatusPill status={status} label={STATUS_LABEL[status]} pulse />
      </div>
      <AlertBox status={status} headline={headline} message={message} reasons={reasons} />
    </div>
  );
}