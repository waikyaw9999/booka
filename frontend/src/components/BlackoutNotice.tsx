import type { BlackoutDate } from '../types';

interface BlackoutNoticeProps {
  blackouts: BlackoutDate[];
}

export function BlackoutNotice({ blackouts }: BlackoutNoticeProps) {
  if (!blackouts.length) return null;

  return (
    <div className="rounded-2xl border border-gold/30 bg-gold/10 p-5 space-y-2">
      <h3 className="font-display text-lg font-semibold text-ink">Upcoming blackout periods</h3>
      <ul className="space-y-2">
        {blackouts.map((b) => (
          <li key={b.id} className="text-sm text-ink/75">
            <span className="font-semibold text-ink">
              {b.startDate} → {b.endDate}
            </span>
            {' · '}
            {b.roomTypeName ? `${b.roomTypeName} only` : 'Entire property'}
            {' — '}
            {b.reason}
          </li>
        ))}
      </ul>
      <p className="text-xs text-ink/55">Bookings cannot overlap these dates.</p>
    </div>
  );
}
