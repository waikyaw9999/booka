import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import { useBookingStore } from '../stores';

export function BookingPage() {
  const { draft, setDraft, clearDraft } = useBookingStore();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [confirmed, setConfirmed] = useState<number | null>(null);

  if (!draft.hotelId || !draft.selectedRoomType) {
    return (
      <div className="mx-auto max-w-lg px-4 py-24 text-center space-y-4">
        <p className="font-display text-2xl text-ink">No room selected yet</p>
        <p className="text-ink/60">Search availability first, then continue here.</p>
        <Link to="/" className="btn-primary inline-flex">
          Browse stays
        </Link>
      </div>
    );
  }

  const { selectedRoomType } = draft;
  const tax = selectedRoomType.totalPrice * 0.1;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const booking = await api.createBooking({
        hotelId: draft.hotelId!,
        roomTypeId: selectedRoomType!.roomType.id,
        checkInDate: draft.checkInDate,
        checkOutDate: draft.checkOutDate,
        numGuests: draft.numGuests,
        guestEmail: draft.guestEmail,
        guestFullName: draft.guestFullName,
        guestPhone: draft.guestPhone || undefined,
      });
      setConfirmed(booking.id);
      clearDraft();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Booking failed');
    } finally {
      setLoading(false);
    }
  }

  if (confirmed) {
    return (
      <div className="mx-auto max-w-lg px-4 py-24 text-center space-y-5">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-moss/15 text-2xl text-moss">
          ✓
        </div>
        <h1 className="font-display text-3xl font-bold text-forest">Booking confirmed</h1>
        <p className="text-ink/65">Confirmation #{confirmed}</p>
        <button onClick={() => navigate('/')} className="btn-primary">
          Back to home
        </button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-12 space-y-8">
      <div>
        <p className="text-xs font-semibold uppercase tracking-widest text-moss">Checkout</p>
        <h1 className="mt-2 font-display text-3xl font-bold text-ink">Complete your booking</h1>
      </div>

      <div className="surface space-y-3 p-5 text-sm">
        <Row label="Hotel" value={draft.hotelName ?? ''} />
        <Row label="Room" value={selectedRoomType.roomType.name} />
        <Row label="Dates" value={`${draft.checkInDate} → ${draft.checkOutDate}`} />
        <Row label="Guests" value={String(draft.numGuests)} />
        <div className="my-2 h-px bg-sand" />
        <Row label="Subtotal" value={`$${selectedRoomType.totalPrice.toFixed(2)}`} />
        <Row label="Tax (10%)" value={`$${tax.toFixed(2)}`} />
        <p className="flex items-center justify-between pt-1 font-display text-xl font-bold text-forest">
          <span>Total</span>
          <span>${(selectedRoomType.totalPrice + tax).toFixed(2)}</span>
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink/55">
            Full name
          </label>
          <input
            required
            value={draft.guestFullName}
            onChange={(e) => setDraft({ guestFullName: e.target.value })}
            className="input-field"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink/55">
            Email
          </label>
          <input
            type="email"
            required
            value={draft.guestEmail}
            onChange={(e) => setDraft({ guestEmail: e.target.value })}
            className="input-field"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink/55">
            Phone (optional)
          </label>
          <input
            value={draft.guestPhone}
            onChange={(e) => setDraft({ guestPhone: e.target.value })}
            className="input-field"
          />
        </div>
        {error && <p className="text-sm text-red-700">{error}</p>}
        <button type="submit" disabled={loading} className="btn-primary w-full py-3">
          {loading ? 'Confirming...' : 'Confirm booking'}
        </button>
      </form>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <p className="flex items-center justify-between gap-4 text-ink/80">
      <span className="text-ink/50">{label}</span>
      <span className="font-medium text-ink text-right">{value}</span>
    </p>
  );
}
