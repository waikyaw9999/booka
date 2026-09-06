interface MetricTileProps {
  label: string;
  value: string | number;
  hint?: string;
  accent?: 'forest' | 'moss' | 'gold' | 'lagoon' | 'danger';
}

const ACCENT: Record<NonNullable<MetricTileProps['accent']>, string> = {
  forest: 'text-forest',
  moss: 'text-moss',
  gold: 'text-gold',
  lagoon: 'text-lagoon',
  danger: 'text-[color:var(--color-danger)]',
};

export function MetricTile({ label, value, hint, accent = 'forest' }: MetricTileProps) {
  return (
    <div className="ops-panel !p-4">
      <p className="text-[11px] font-semibold uppercase tracking-wider text-ink/45">{label}</p>
      <p className={`mt-1.5 font-display text-3xl font-bold tabular-nums ${ACCENT[accent]}`}>
        {value}
      </p>
      {hint && <p className="mt-1 text-xs text-ink/50">{hint}</p>}
    </div>
  );
}
