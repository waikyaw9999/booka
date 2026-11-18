import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../../lib/api';
import { useAuthStore } from '../../stores';
import { EmptyState, InlineAlert, SegmentedControl, StatusPill } from '../ui';
import type { GuestRequestCategory, GuestRequestStatus } from '../../types';

interface Props {
  token: string;
}

const CATEGORIES: { id: GuestRequestCategory; label: string }[] = [
  { id: 'LATE_CHECKOUT', label: 'Late checkout' },
  { id: 'EXTRA_BED', label: 'Extra bed' },
  { id: 'AIRPORT', label: 'Airport' },
  { id: 'AMENITY', label: 'Amenity' },
  { id: 'OTHER', label: 'Other' },
];

export function GuestRequests({ token }: Props) {
  const queryClient = useQueryClient();
  const hotelId = useAuthStore((s) => s.user?.hotelId);
  const [filter, setFilter] = useState<'OPEN' | 'DONE' | 'all'>('OPEN');
  const [code, setCode] = useState('');
  const [category, setCategory] = useState<GuestRequestCategory>('LATE_CHECKOUT');
  const [note, setNote] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const { data: requests, isLoading } = useQuery({
    queryKey: ['guest-requests', hotelId],
    queryFn: () => api.listGuestRequests(token),
  });

  const visible = (requests ?? []).filter((item) => filter === 'all' || item.status === filter);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      const booking = await api.getBookingByCode(token, code.trim());
      await api.createGuestRequest(token, { bookingId: booking.id, category, note: note.trim() });
      setNote('');
      setCode('');
      queryClient.invalidateQueries({ queryKey: ['guest-requests'] });
      setSuccess(`Request logged for ${booking.guestName}.`);
    } catch (err) {
      setError(getErrorMessage(err, 'Could not log that request.'));
    }
  }

  async function setStatus(id: number, status: GuestRequestStatus) {
    setError('');
    try {
      await api.updateGuestRequest(token, id, status);
      queryClient.invalidateQueries({ queryKey: ['guest-requests'] });
    } catch (err) {
      setError(getErrorMessage(err, 'Could not update the request.'));
    }
  }

  return (
    <div className="space-y-4">
      <form onSubmit={create} className="ops-panel grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <label className="label-caps">Confirmation</label>
          <input value={code} onChange={(e) => setCode(e.target.value)} className="input-field" placeholder="BK-…" required />
        </div>
        <div>
          <label className="label-caps">Category</label>
          <select value={category} onChange={(e) => setCategory(e.target.value as GuestRequestCategory)} className="input-field">
            {CATEGORIES.map((item) => (
              <option key={item.id} value={item.id}>{item.label}</option>
            ))}
          </select>
        </div>
        <div className="sm:col-span-2 lg:col-span-2">
          <label className="label-caps">Note</label>
          <input value={note} onChange={(e) => setNote(e.target.value)} className="input-field" placeholder="What the desk promised" required />
        </div>
        <div className="sm:col-span-2 lg:col-span-4 flex flex-wrap items-center gap-3">
          <button type="submit" className="btn-primary">Log request</button>
          {error && <div className="min-w-0 flex-1"><InlineAlert tone="error">{error}</InlineAlert></div>}
          {success && <div className="min-w-0 flex-1"><InlineAlert tone="success">{success}</InlineAlert></div>}
        </div>
      </form>

      <div className="ops-panel !p-0 overflow-hidden">
        <div className="flex flex-col gap-3 border-b border-sand/70 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="font-display text-lg font-semibold text-ink">Open traces</h2>
          <SegmentedControl
            value={filter}
            onChange={setFilter}
            options={[
              { id: 'OPEN', label: 'Open' },
              { id: 'DONE', label: 'Done' },
              { id: 'all', label: 'All' },
            ]}
          />
        </div>
        {isLoading ? (
          <p className="px-4 py-8 text-sm text-ink/45">Loading requests…</p>
        ) : visible.length === 0 ? (
          <div className="p-4">
            <EmptyState title="Nothing waiting" description="Late checkouts, airport pickups, and extra beds show up here." />
          </div>
        ) : (
          <ul className="divide-y divide-sand/70">
            {visible.map((item) => (
              <li key={item.id} className="flex flex-col gap-3 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                  <p className="font-medium text-ink">{item.guestName}</p>
                  <p className="text-xs text-ink/45">{item.confirmationCode} · {item.category.replace(/_/g, ' ')}</p>
                  <p className="mt-1 text-sm text-ink/70">{item.note}</p>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <StatusPill label={item.status} />
                  {item.status === 'OPEN' && (
                    <>
                      <button type="button" className="btn-secondary !px-3 !py-1.5 text-xs" onClick={() => setStatus(item.id, 'DONE')}>
                        Done
                      </button>
                      <button type="button" className="btn-ghost !px-2 !py-1.5 text-xs" onClick={() => setStatus(item.id, 'CANCELLED')}>
                        Cancel
                      </button>
                    </>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
