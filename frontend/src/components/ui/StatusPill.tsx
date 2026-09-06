const TONE_CLASSES: Record<string, string> = {
  occupied: 'bg-forest/10 text-forest border-forest/20',
  available: 'bg-moss/10 text-moss border-moss/25',
  clean: 'bg-moss/12 text-moss border-moss/25',
  dirty: 'bg-[color:var(--color-status-dirty)]/12 text-[color:var(--color-status-dirty)] border-[color:var(--color-status-dirty)]/25',
  inspecting: 'bg-[color:var(--color-status-inspecting)]/12 text-[color:var(--color-status-inspecting)] border-[color:var(--color-status-inspecting)]/25',
  maintenance: 'bg-[color:var(--color-status-maintenance)]/12 text-[color:var(--color-status-maintenance)] border-[color:var(--color-status-maintenance)]/25',
  pending: 'bg-gold/15 text-[color:var(--color-status-pending)] border-gold/30',
  paid: 'bg-moss/12 text-moss border-moss/25',
  unpaid: 'bg-[color:var(--color-status-unpaid)]/12 text-[color:var(--color-status-unpaid)] border-[color:var(--color-status-unpaid)]/25',
  confirmed: 'bg-lagoon/12 text-lagoon border-lagoon/25',
  checked_in: 'bg-forest/10 text-forest border-forest/20',
  checked_out: 'bg-sand/80 text-ink/60 border-sand',
  cancelled: 'bg-[color:var(--color-danger)]/10 text-[color:var(--color-danger)] border-[color:var(--color-danger)]/20',
  low: 'bg-sand/80 text-ink/55 border-sand',
  medium: 'bg-gold/15 text-[color:var(--color-warning)] border-gold/30',
  high: 'bg-[color:var(--color-status-unpaid)]/12 text-[color:var(--color-status-unpaid)] border-[color:var(--color-status-unpaid)]/25',
  urgent: 'bg-[color:var(--color-danger)]/12 text-[color:var(--color-danger)] border-[color:var(--color-danger)]/25',
  open: 'bg-gold/15 text-[color:var(--color-warning)] border-gold/30',
  in_progress: 'bg-lagoon/12 text-lagoon border-lagoon/25',
  resolved: 'bg-moss/12 text-moss border-moss/25',
  default: 'bg-fog text-ink/65 border-sand',
};

export type StatusTone = keyof typeof TONE_CLASSES | string;

interface StatusPillProps {
  label: string;
  tone?: StatusTone;
  className?: string;
}

export function StatusPill({ label, tone, className = '' }: StatusPillProps) {
  const key = (tone ?? label).toLowerCase().replace(/\s+/g, '_');
  const classes = TONE_CLASSES[key] ?? TONE_CLASSES.default;
  return (
    <span
      className={`inline-flex items-center rounded-md border px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wide ${classes} ${className}`}
    >
      {label.replace(/_/g, ' ')}
    </span>
  );
}
