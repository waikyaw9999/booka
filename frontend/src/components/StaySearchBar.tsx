import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { StaySearchParams } from '../types';
import { addDaysIso, staySearchQuery, todayIso } from '../lib/search';

interface Props {
  initial?: Partial<StaySearchParams>;
  compact?: boolean;
  className?: string;
  /** When set, dates update in place instead of navigating to /search */
  onSubmit?: (next: StaySearchParams) => void;
}

export function StaySearchBar({
  initial,
  compact = false,
  className = '',
  onSubmit,
}: Props) {
  const navigate = useNavigate();
  const today = todayIso();
  const [city, setCity] = useState(initial?.city ?? '');
  const [checkIn, setCheckIn] = useState(initial?.checkIn ?? today);
  const [checkOut, setCheckOut] = useState(
    initial?.checkOut ?? addDaysIso(initial?.checkIn ?? today, 2),
  );
  const [guests, setGuests] = useState(initial?.guests ?? 2);

  const checkoutMin = checkIn ? addDaysIso(checkIn, 1) : addDaysIso(today, 1);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!checkIn || !checkOut || checkOut <= checkIn) return;
    const params: StaySearchParams = {
      city: city.trim(),
      checkIn,
      checkOut,
      guests: Math.max(1, guests),
    };
    if (onSubmit) {
      onSubmit(params);
      return;
    }
    navigate(`/search?${staySearchQuery(params)}`);
  }

  return (
    <form
      onSubmit={handleSubmit}
      className={`grid gap-3 rounded-2xl border border-sand/80 bg-white/95 p-3 shadow-[0_20px_50px_-28px_rgba(28,50,44,0.45)] backdrop-blur-md sm:grid-cols-[1.2fr_1fr_1fr_0.7fr_auto] sm:items-end sm:gap-2 sm:p-3.5 ${className}`}
    >
      <div>
        {!compact && <label className="label-caps">Destination</label>}
        <input
          className="input-field"
          placeholder="City or property"
          value={city}
          onChange={(e) => setCity(e.target.value)}
          aria-label="Destination"
        />
      </div>
      <div>
        {!compact && <label className="label-caps">Check-in</label>}
        <input
          type="date"
          required
          min={today}
          className="input-field"
          value={checkIn}
          onChange={(e) => {
            const next = e.target.value;
            setCheckIn(next);
            if (checkOut <= next) setCheckOut(addDaysIso(next, 1));
          }}
          aria-label="Check-in"
        />
      </div>
      <div>
        {!compact && <label className="label-caps">Check-out</label>}
        <input
          type="date"
          required
          min={checkoutMin}
          className="input-field"
          value={checkOut}
          onChange={(e) => setCheckOut(e.target.value)}
          aria-label="Check-out"
        />
      </div>
      <div>
        {!compact && <label className="label-caps">Guests</label>}
        <input
          type="number"
          min={1}
          max={10}
          required
          className="input-field"
          value={guests}
          onChange={(e) => setGuests(Number(e.target.value) || 1)}
          aria-label="Guests"
        />
      </div>
      <button type="submit" className="btn-primary w-full sm:w-auto sm:self-end">
        Search
      </button>
    </form>
  );
}
