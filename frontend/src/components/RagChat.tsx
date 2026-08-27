import { Link } from 'react-router-dom';
import { useState } from 'react';
import type {
  AvailabilityOption,
  BlackoutOption,
  BookingPeriodSummary,
  PropertySummary,
  RagCitation,
  RagMessage,
} from '../types';
import { useBookingStore } from '../stores';

interface RagChatProps {
  title: string;
  messages: RagMessage[];
  onSend: (message: string) => Promise<void>;
  loading: boolean;
  ragAvailable: boolean | null;
  placeholder?: string;
}

export function RagChat({
  title,
  messages,
  onSend,
  loading,
  ragAvailable,
  placeholder = 'Ask a question...',
}: RagChatProps) {
  const [input, setInput] = useState('');
  const [open, setOpen] = useState(true);
  const { setDraft } = useBookingStore();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!input.trim() || loading) return;
    const msg = input.trim();
    setInput('');
    await onSend(msg);
  }

  function applyAvailability(option: AvailabilityOption) {
    setDraft({
      hotelId: option.hotelId,
      hotelName: option.hotelName,
      checkInDate: option.checkInDate,
      checkOutDate: option.checkOutDate,
      numGuests: 2,
      selectedRoomType: null,
    });
  }

  return (
    <div className="surface overflow-hidden">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="flex w-full items-center justify-between bg-forest/5 px-4 py-3 font-medium text-forest"
      >
        <span className="font-display text-lg">{title}</span>
        <span className="text-xs text-moss">
          {ragAvailable === false ? 'Offline mode' : ragAvailable ? 'Live assistant' : ''}
        </span>
      </button>

      {open && (
        <div className="space-y-4 p-4">
          <div className="h-72 space-y-3 overflow-y-auto pr-1">
            {messages.length === 0 && (
              <p className="text-sm text-ink/55">
                Ask about amenities, policies, blackouts, or room availability.
              </p>
            )}
            {messages.map((msg, i) => (
              <div
                key={i}
                className={`rounded-xl px-3 py-2 text-sm ${
                  msg.role === 'user'
                    ? 'ml-8 bg-forest text-white'
                    : 'mr-8 bg-sand/60 text-ink'
                }`}
              >
                <p className="whitespace-pre-wrap">{msg.content}</p>
                {msg.propertySummary && <SummaryCard summary={msg.propertySummary} />}
                {msg.bookingSummary && <BookingSummaryCard summary={msg.bookingSummary} />}
                {msg.blackouts && msg.blackouts.length > 0 && (
                  <BlackoutCards options={msg.blackouts} />
                )}
                {msg.availability && msg.availability.length > 0 && (
                  <AvailabilityCards
                    options={msg.availability}
                    onSelect={applyAvailability}
                  />
                )}
                {msg.hotelPath && (
                  <Link
                    to={msg.hotelPath}
                    className="mt-2 inline-block text-xs font-medium text-moss hover:underline"
                  >
                    Open hotel booking page →
                  </Link>
                )}
                {msg.citations && msg.citations.length > 0 && (
                  <Citations citations={msg.citations} />
                )}
              </div>
            ))}
            {loading && (
              <p className="animate-pulse text-sm text-ink/40">Thinking...</p>
            )}
          </div>

          {!ragAvailable && ragAvailable !== null && (
            <p className="rounded-lg bg-gold/15 px-2 py-1 text-xs text-ink/70">
              Groq API unavailable — answers use retrieved documents / live property data only.
            </p>
          )}

          <form onSubmit={handleSubmit} className="flex gap-2">
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={placeholder}
              disabled={loading}
              className="input-field flex-1"
            />
            <button
              type="submit"
              disabled={loading || !input.trim()}
              className="btn-primary disabled:opacity-50"
            >
              Send
            </button>
          </form>
        </div>
      )}
    </div>
  );
}

function SummaryCard({ summary }: { summary: PropertySummary }) {
  return (
    <div className="mt-2 grid grid-cols-2 gap-2 text-xs">
      <Stat label="Occupied" value={summary.occupiedRooms} />
      <Stat label="Available" value={summary.availableRooms} />
      <Stat label="Check-ins today" value={summary.upcomingCheckIns} />
      <Stat label="Check-outs today" value={summary.upcomingCheckOuts} />
      <Stat label="Active bookings" value={summary.activeBookings} />
      <Stat label="Maintenance" value={summary.maintenanceRooms} />
    </div>
  );
}

function BookingSummaryCard({ summary }: { summary: BookingPeriodSummary }) {
  return (
    <div className="mt-2 space-y-2 text-xs">
      <div className="font-medium text-forest">
        {summary.periodLabel}: {summary.fromDate} → {summary.toDate}
      </div>
      <div className="grid grid-cols-2 gap-2">
        <Stat label="Total" value={summary.totalBookings} />
        <Stat label="Active" value={summary.activeBookings} />
        <Stat label="Confirmed" value={summary.confirmed} />
        <Stat label="Pending" value={summary.pending} />
        <Stat label="Checked in" value={summary.checkedIn} />
        <Stat label="Cancelled" value={summary.cancelledBookings} />
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-sand bg-white/80 px-2 py-1.5">
      <div className="text-ink/50">{label}</div>
      <div className="font-semibold text-forest">{value}</div>
    </div>
  );
}

function BlackoutCards({ options }: { options: BlackoutOption[] }) {
  return (
    <ul className="mt-2 space-y-2">
      {options.map((o) => (
        <li
          key={o.id}
          className="rounded-lg border border-gold/30 bg-gold/10 px-3 py-2 text-xs text-ink"
        >
          <div className="font-medium">
            {o.startDate} → {o.endDate} · {o.roomTypeName}
          </div>
          <div className="mt-0.5 text-ink/70">{o.reason}</div>
        </li>
      ))}
    </ul>
  );
}

function AvailabilityCards({
  options,
  onSelect,
}: {
  options: AvailabilityOption[];
  onSelect: (option: AvailabilityOption) => void;
}) {
  return (
    <ul className="mt-2 space-y-2">
      {options.map((o) => (
        <li
          key={`${o.hotelId}-${o.roomTypeId}-${o.checkInDate}`}
          className="rounded-lg border border-sand bg-white/90 px-3 py-2 text-xs text-ink"
        >
          <div className="font-medium">
            {o.hotelName} — {o.roomTypeName}
          </div>
          <div className="mt-0.5 text-ink/55">
            {o.checkInDate} → {o.checkOutDate} · {o.availableCount} left · ${o.totalPrice.toFixed(2)} total
          </div>
          <Link
            to={`/hotels/${o.hotelId}?checkIn=${o.checkInDate}&checkOut=${o.checkOutDate}`}
            onClick={() => onSelect(o)}
            className="mt-1 inline-block font-medium text-moss hover:underline"
          >
            View & book
          </Link>
        </li>
      ))}
    </ul>
  );
}

function Citations({ citations }: { citations: RagCitation[] }) {
  return (
    <ul className="mt-2 space-y-1 border-t border-sand/80 pt-2 text-xs text-ink/45">
      {citations.map((c, i) => (
        <li key={i}>
          Source: {c.documentTitle}
          {c.section ? ` — ${c.section}` : ''}
        </li>
      ))}
    </ul>
  );
}
