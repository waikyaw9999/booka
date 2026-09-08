import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../lib/api';
import type { BlackoutDate } from '../types';
import { EmptyState, InlineAlert, ConfirmAction } from './ui';

interface BlackoutDateManagerProps {
  token: string;
  hotelId: number;
}

export function BlackoutDateManager({ token, hotelId }: BlackoutDateManagerProps) {
  const queryClient = useQueryClient();
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [roomTypeId, setRoomTypeId] = useState<string>('');
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const { data: blackouts, isLoading } = useQuery({
    queryKey: ['admin-blackouts'],
    queryFn: () => api.listAdminBlackoutDates(token),
  });

  const { data: roomTypes } = useQuery({
    queryKey: ['room-types', hotelId],
    queryFn: () => api.getRoomTypes(hotelId),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      api.createBlackoutDate(token, {
        hotelId,
        roomTypeId: roomTypeId ? Number(roomTypeId) : null,
        startDate,
        endDate,
        reason,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-blackouts'] });
      setStartDate('');
      setEndDate('');
      setRoomTypeId('');
      setReason('');
      setError('');
      setSuccess('Blackout date added.');
    },
    onError: (err: Error) => {
      setSuccess('');
      setError(getErrorMessage(err));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.deleteBlackoutDate(token, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-blackouts'] });
      setSuccess('Blackout removed.');
    },
    onError: (err: Error) => setError(getErrorMessage(err)),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');
    createMutation.mutate();
  }

  return (
    <div className="space-y-4">
      {error && <InlineAlert tone="error">{error}</InlineAlert>}
      {success && <InlineAlert tone="success">{success}</InlineAlert>}

      <form onSubmit={handleSubmit} className="ops-panel grid gap-3 md:grid-cols-2">
        <div>
          <label className="label-caps">Start date</label>
          <input
            type="date"
            required
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="input-field"
          />
        </div>
        <div>
          <label className="label-caps">End date (exclusive)</label>
          <input
            type="date"
            required
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="input-field"
          />
        </div>
        <div>
          <label className="label-caps">Scope</label>
          <select
            value={roomTypeId}
            onChange={(e) => setRoomTypeId(e.target.value)}
            className="input-field"
          >
            <option value="">Entire property</option>
            {roomTypes?.map((rt) => (
              <option key={rt.id} value={rt.id}>
                {rt.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="label-caps">Reason</label>
          <input
            required
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="e.g. Holiday closure, renovation"
            className="input-field"
          />
        </div>
        <div className="md:col-span-2">
          <button
            type="submit"
            disabled={createMutation.isPending}
            className="btn-primary"
          >
            {createMutation.isPending ? 'Adding…' : 'Add blackout'}
          </button>
        </div>
      </form>

      <div className="ops-panel !p-0 overflow-hidden">
        {isLoading ? (
          <p className="p-4 text-sm text-ink/45">Loading blackout dates…</p>
        ) : !blackouts?.length ? (
          <div className="p-4">
            <EmptyState
              title="No blackout dates"
              description="Add a window when the property or a room type cannot accept new bookings."
            />
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Dates</th>
                <th>Scope</th>
                <th>Reason</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {blackouts.map((b: BlackoutDate) => (
                <tr key={b.id}>
                  <td className="whitespace-nowrap text-xs">
                    {b.startDate} → {b.endDate}
                  </td>
                  <td>{b.roomTypeName ?? 'Entire property'}</td>
                  <td className="text-ink/65">{b.reason}</td>
                  <td className="text-right">
                    <ConfirmAction
                      label="Delete"
                      tone="danger"
                      confirmLabel="Delete"
                      onConfirm={() => deleteMutation.mutateAsync(b.id)}
                    >
                      Remove this blackout?
                    </ConfirmAction>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
