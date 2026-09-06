import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../lib/api';
import { InlineAlert } from '../ui';

interface Props {
  token: string;
  hotelId: number;
  onCreated?: () => void;
}

export function WalkInForm({ token, hotelId, onCreated }: Props) {
  const [roomTypeId, setRoomTypeId] = useState('');
  const [roomId, setRoomId] = useState('');
  const [checkInDate, setCheckInDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [checkOutDate, setCheckOutDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d.toISOString().slice(0, 10);
  });
  const [numGuests, setNumGuests] = useState(1);
  const [guestFullName, setGuestFullName] = useState('');
  const [guestEmail, setGuestEmail] = useState('');
  const [guestPhone, setGuestPhone] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const { data: roomTypes } = useQuery({
    queryKey: ['room-types', hotelId],
    queryFn: () => api.getRoomTypes(hotelId),
  });

  const { data: rooms } = useQuery({
    queryKey: ['room-board'],
    queryFn: () => api.listRoomBoard(token),
  });

  const roomsForType = rooms?.filter(
    (r) => (!roomTypeId || String(r.roomTypeId) === roomTypeId) && r.status === 'AVAILABLE',
  );

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');
    setBusy(true);
    try {
      const booking = await api.createWalkIn(token, {
        hotelId,
        roomTypeId: Number(roomTypeId),
        roomId: roomId ? Number(roomId) : undefined,
        checkInDate,
        checkOutDate,
        numGuests,
        guestEmail,
        guestFullName,
        guestPhone: guestPhone || undefined,
      });
      setSuccess(`Walk-in created: ${booking.confirmationCode}`);
      setGuestFullName('');
      setGuestEmail('');
      setGuestPhone('');
      setRoomId('');
      onCreated?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Walk-in failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="ops-panel space-y-4">
      <div>
        <h2 className="font-display text-lg font-semibold text-ink">Walk-in reservation</h2>
        <p className="mt-1 text-sm text-ink/55">Create a same-day stay at the front desk.</p>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <label className="label-caps">Check-in</label>
          <input
            type="date"
            required
            value={checkInDate}
            onChange={(e) => setCheckInDate(e.target.value)}
            className="input-field"
          />
        </div>
        <div>
          <label className="label-caps">Check-out</label>
          <input
            type="date"
            required
            value={checkOutDate}
            onChange={(e) => setCheckOutDate(e.target.value)}
            className="input-field"
          />
        </div>
        <div>
          <label className="label-caps">Room type</label>
          <select
            required
            value={roomTypeId}
            onChange={(e) => {
              setRoomTypeId(e.target.value);
              setRoomId('');
            }}
            className="input-field"
          >
            <option value="">Select type</option>
            {roomTypes?.map((rt) => (
              <option key={rt.id} value={rt.id}>
                {rt.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="label-caps">Assign room (optional)</label>
          <select value={roomId} onChange={(e) => setRoomId(e.target.value)} className="input-field">
            <option value="">Auto / later</option>
            {roomsForType?.map((r) => (
              <option key={r.id} value={r.id}>
                #{r.roomNumber} · {r.housekeepingStatus}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="label-caps">Guests</label>
          <input
            type="number"
            min={1}
            max={10}
            value={numGuests}
            onChange={(e) => setNumGuests(Number(e.target.value))}
            className="input-field"
          />
        </div>
        <div>
          <label className="label-caps">Guest name</label>
          <input
            required
            value={guestFullName}
            onChange={(e) => setGuestFullName(e.target.value)}
            className="input-field"
            placeholder="Full name"
          />
        </div>
        <div>
          <label className="label-caps">Email</label>
          <input
            type="email"
            required
            value={guestEmail}
            onChange={(e) => setGuestEmail(e.target.value)}
            className="input-field"
            placeholder="guest@email.com"
          />
        </div>
        <div>
          <label className="label-caps">Phone</label>
          <input
            value={guestPhone}
            onChange={(e) => setGuestPhone(e.target.value)}
            className="input-field"
            placeholder="Optional"
          />
        </div>
      </div>

      {error && <InlineAlert tone="error">{error}</InlineAlert>}
      {success && <InlineAlert tone="success">{success}</InlineAlert>}

      <button type="submit" disabled={busy} className="btn-primary">
        {busy ? 'Creating…' : 'Create walk-in'}
      </button>
    </form>
  );
}
