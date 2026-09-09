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

/** Deep link into hotel detail with dates (and optional room type) for booking. */
export function availabilityBookingPath(
  option: Pick<
    AvailabilityOption,
    'hotelId' | 'checkInDate' | 'checkOutDate' | 'roomTypeId'
  >,
  guests = 2,
): string {
  const params = new URLSearchParams({
    checkIn: option.checkInDate,
    checkOut: option.checkOutDate,
    guests: String(guests),
  });
  if (option.roomTypeId != null) {
    params.set('roomTypeId', String(option.roomTypeId));
  }
  return `/hotels/${option.hotelId}?${params.toString()}#availability`;
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
  const { draft, setDraft } = useBookingStore();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!input.trim() || loading) return;
    const msg = input.trim();
    setInput('');
    await onSend(msg);
  }

  function applyAvailability(option: AvailabilityOption) {
    const numGuests = draft.numGuests > 0 ? draft.numGuests : 2;
    setDraft({
      hotelId: option.hotelId,
      hotelName: option.hotelName,
      checkInDate: option.checkInDate,
      checkOutDate: option.checkOutDate,
      numGuests,
      selectedRoomType: null,
    });
    return availabilityBookingPath(option, numGuests);
  }

  function onAvailabilityClick(option: AvailabilityOption, e?: React.MouseEvent) {
    e?.stopPropagation();
    applyAvailability(option);
  }

  return (
    <div className="overflow-hidden rounded-2xl border border-sand/80 bg-gradient-to-b from-white/90 to-fog/40 shadow-[0_16px_40px_-28px_rgba(16,32,27,0.4)]">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="flex w-full items-center justify-between border-b border-sand/60 bg-forest px-4 py-3.5 text-left text-white"
      >
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-gold/90">
            Concierge
          </p>
          <span className="font-display text-lg font-semibold">{title}</span>
        </div>
        <span className="text-xs text-sand/70">
          {ragAvailable === false ? 'Offline mode' : ragAvailable ? 'Live' : open ? 'Collapse' : 'Open'}
        </span>
      </button>

      {open && (
        <div className="animate-tab space-y-4 p-4">
          <div className="h-72 space-y-3 overflow-y-auto pr-1">
            {messages.length === 0 && (
              <p className="rounded-xl border border-dashed border-sand bg-fog/50 px-3 py-4 text-sm text-ink/55">
                Ask about amenities, policies, blackouts, or room availability.
              </p>
            )}
            {messages.map((msg, i) => {
              const guests = draft.numGuests > 0 ? draft.numGuests : 2;
              const openHotelPath = msg.availability?.[0]
                ? availabilityBookingPath(msg.availability[0], guests)
                : msg.hotelPath
                  ? msg.hotelPath.includes('#')
                    ? msg.hotelPath
                    : `${msg.hotelPath}#availability`
                  : null;

              return (
                <div
                  key={i}
                  className={`rounded-xl px-3 py-2 text-sm ${
                    msg.role === 'user'
                      ? 'ml-8 bg-forest text-white'
                      : 'mr-8 border border-sand/70 bg-white/90 text-ink'
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
                      guests={guests}
                      onSelect={onAvailabilityClick}
                    />
                  )}
                  {openHotelPath && (
                    <Link
                      to={openHotelPath}
                      onClick={(e) => {
                        e.stopPropagation();
                        const first = msg.availability?.[0];
                        if (first) applyAvailability(first);
                      }}
                      className="relative z-10 mt-2 inline-block text-xs font-medium text-moss hover:underline"
                    >
                      Open hotel booking page →
                    </Link>
                  )}
                  {msg.citations && msg.citations.length > 0 && (
                    <Citations citations={msg.citations} />
                  )}
                  {msg.suggestedQuestions && msg.suggestedQuestions.length > 0 && (
                    <SuggestedQuestions
                      questions={msg.suggestedQuestions}
                      disabled={loading}
                      onPick={(q) => void onSend(q)}
                    />
                  )}
                </div>
              );
            })}
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
  guests,
  onSelect,
}: {
  options: AvailabilityOption[];
  guests: number;
  onSelect: (option: AvailabilityOption, e?: React.MouseEvent) => void;
}) {
  return (
    <ul className="mt-2 space-y-2">
      {options.map((o) => {
        const href = availabilityBookingPath(o, guests);
        return (
          <li
            key={`${o.hotelId}-${o.roomTypeId}-${o.checkInDate}`}
            className="rounded-lg border border-sand bg-white/90 px-3 py-2 text-xs text-ink"
          >
            <div className="font-medium">
              {o.hotelName} — {o.roomTypeName}
            </div>
            <div className="mt-0.5 text-ink/55">
              {o.checkInDate} → {o.checkOutDate} · {o.availableCount} left · $
              {o.totalPrice.toFixed(2)} total
            </div>
            <Link
              to={href}
              onClick={(e) => onSelect(o, e)}
              className="relative z-10 mt-1 inline-block font-medium text-moss hover:underline"
            >
              View & book
            </Link>
          </li>
        );
      })}
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

function SuggestedQuestions({
  questions,
  disabled,
  onPick,
}: {
  questions: string[];
  disabled: boolean;
  onPick: (question: string) => void;
}) {
  return (
    <div className="mt-2 flex flex-col gap-1.5">
      <p className="text-[10px] font-semibold uppercase tracking-[0.12em] text-ink/40">
        Suggested
      </p>
      {questions.map((q) => (
        <button
          key={q}
          type="button"
          disabled={disabled}
          onClick={() => onPick(q)}
          className="rounded-lg border border-moss/30 bg-moss/5 px-2.5 py-1.5 text-left text-xs font-medium text-forest transition hover:border-moss/50 hover:bg-moss/10 disabled:opacity-50"
        >
          {q}
        </button>
      ))}
    </div>
  );
}
