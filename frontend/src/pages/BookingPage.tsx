import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../lib/api';
import { useBookingStore } from '../stores';
import { InlineAlert, EmptyState } from '../components/ui';

export function BookingPage() {
  const { draft, setDraft, clearDraft } = useBookingStore();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [confirmed, setConfirmed] = useState<{
    bookingId: number;
    confirmationCode?: string;
  } | null>(null);

  const paymentState = searchParams.get('payment');
  const returnBookingId = searchParams.get('bookingId');

  // Legacy Stripe return URL support (when Checkout is re-enabled later)
  useEffect(() => {
    if (paymentState === 'success' && returnBookingId) {
      clearDraft();
    }
  }, [paymentState, returnBookingId, clearDraft]);

  if (paymentState === 'cancelled') {
    return (
      <div className="mx-auto max-w-lg px-4 py-24 text-center space-y-4">
        <h1 className="font-display text-3xl font-bold text-ink">Payment cancelled</h1>
        <p className="text-ink/65">
          Your reservation may still be pending. You can return to checkout and try again.
        </p>
        <Link to="/" className="btn-primary inline-flex">
          Back to home
        </Link>
      </div>
    );
  }

  if (paymentState === 'success' && returnBookingId) {
    return <PaymentConfirming bookingId={Number(returnBookingId)} onHome={() => navigate('/')} />;
  }

  if (confirmed) {
    return (
      <div className="mx-auto max-w-lg px-4 py-24 text-center space-y-5 animate-panel">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-moss/15 text-2xl text-moss">
          ✓
        </div>
        <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-moss">
          Booking confirmed
        </p>
        <h1 className="font-display text-3xl font-bold text-forest">You&apos;re all set</h1>
        <p className="text-ink/65">
          Confirmation code{' '}
          <span className="font-semibold text-ink">
            {confirmed.confirmationCode ?? `#${confirmed.bookingId}`}
          </span>
        </p>
        <p className="text-xs text-ink/45">
          Online payment is deferred — pay at the property or when Stripe Checkout is enabled.
        </p>
        <button onClick={() => navigate('/')} className="btn-primary">
          Back to home
        </button>
      </div>
    );
  }

  if (!draft.hotelId || !draft.selectedRoomType) {
    return (
      <div className="mx-auto max-w-lg px-4 py-24">
        <EmptyState
          title="No room selected yet"
          description="Search availability first, then continue here."
          action={
            <Link to="/" className="btn-primary inline-flex">
              Browse stays
            </Link>
          }
        />
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
      // Stripe Checkout skipped for now — booking is already CONFIRMED + N_A on the server.
      clearDraft();
      setConfirmed({
        bookingId: booking.id,
        confirmationCode: booking.confirmationCode,
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Booking failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-12 space-y-8">
      <div>
        <p className="text-xs font-semibold uppercase tracking-widest text-moss">Checkout</p>
        <h1 className="mt-2 font-display text-3xl font-bold text-ink">Complete your booking</h1>
        <ol className="mt-4 flex gap-2 text-[11px] font-semibold uppercase tracking-wide">
          <Step n={1} label="Stay" active done />
          <Step n={2} label="Guest" active />
          <Step n={3} label="Confirm" />
        </ol>
      </div>

      <section className="surface space-y-3 p-5 text-sm animate-panel">
        <p className="text-[11px] font-semibold uppercase tracking-wider text-moss">Stay summary</p>
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
      </section>

      <form onSubmit={handleSubmit} className="surface space-y-4 p-5 animate-rise-delay">
        <p className="text-[11px] font-semibold uppercase tracking-wider text-moss">Guest details</p>
        <div>
          <label className="label-caps">Full name</label>
          <input
            required
            value={draft.guestFullName}
            onChange={(e) => setDraft({ guestFullName: e.target.value })}
            className="input-field"
          />
        </div>
        <div>
          <label className="label-caps">Email</label>
          <input
            type="email"
            required
            value={draft.guestEmail}
            onChange={(e) => setDraft({ guestEmail: e.target.value })}
            className="input-field"
          />
        </div>
        <div>
          <label className="label-caps">Phone (optional)</label>
          <input
            value={draft.guestPhone}
            onChange={(e) => setDraft({ guestPhone: e.target.value })}
            className="input-field"
          />
        </div>
        {error && <InlineAlert tone="error">{error}</InlineAlert>}
        <button type="submit" disabled={loading} className="btn-primary w-full py-3">
          {loading ? 'Confirming…' : 'Confirm booking'}
        </button>
      </form>
    </div>
  );
}

function Step({ n, label, active, done }: { n: number; label: string; active?: boolean; done?: boolean }) {
  return (
    <li
      className={`flex items-center gap-1.5 rounded-lg px-2.5 py-1 ${
        active || done ? 'bg-forest/10 text-forest' : 'bg-fog text-ink/35'
      }`}
    >
      <span className="tabular-nums">{done ? '✓' : n}</span>
      {label}
    </li>
  );
}

function PaymentConfirming({ bookingId, onHome }: { bookingId: number; onHome: () => void }) {
  const [status, setStatus] = useState('Confirming payment…');
  const [paid, setPaid] = useState(false);

  useEffect(() => {
    let cancelled = false;
    let tries = 0;
    const tick = async () => {
      tries += 1;
      try {
        const booking = await api.getBooking(bookingId);
        if (cancelled) return;
        if (booking.paymentStatus === 'PAID' || booking.status === 'CONFIRMED') {
          setPaid(true);
          setStatus(`Confirmed ${booking.confirmationCode ?? '#' + bookingId}`);
          return;
        }
        setStatus(`Waiting for payment confirmation… (${booking.status} / ${booking.paymentStatus})`);
      } catch {
        if (!cancelled) setStatus('Could not load booking status yet.');
      }
      if (!cancelled && tries < 15) {
        window.setTimeout(tick, 2000);
      }
    };
    tick();
    return () => {
      cancelled = true;
    };
  }, [bookingId]);

  return (
    <div className="mx-auto max-w-lg px-4 py-24 text-center space-y-5 animate-panel">
      <div
        className={`mx-auto flex h-16 w-16 items-center justify-center rounded-full text-2xl ${
          paid ? 'bg-moss/15 text-moss' : 'bg-gold/20 text-gold animate-pulse'
        }`}
      >
        {paid ? '✓' : '…'}
      </div>
      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-moss">
        {paid ? 'Step 3 complete' : 'Payment in progress'}
      </p>
      <h1 className="font-display text-3xl font-bold text-forest">
        {paid ? 'Booking confirmed' : 'Confirming your stay'}
      </h1>
      <p className="text-ink/65">{status}</p>
      <p className="text-xs text-ink/45">
        Confirmation is finalized by Stripe webhooks when Checkout is enabled.
      </p>
      <button onClick={onHome} className="btn-primary">
        Back to home
      </button>
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
