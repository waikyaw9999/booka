import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { BlackoutDate } from '../types';

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
    },
    onError: (err: Error) => setError(err.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.deleteBlackoutDate(token, id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-blackouts'] }),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    createMutation.mutate();
  }

  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold">Blackout Dates</h2>
      <p className="text-slate-500 text-sm">
        Block booking for date ranges — property-wide or per room type.
      </p>

      <form onSubmit={handleSubmit} className="bg-white border border-slate-200 rounded-xl p-4 grid md:grid-cols-2 gap-3">
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Start date</label>
          <input
            type="date"
            required
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">End date (exclusive)</label>
          <input
            type="date"
            required
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Scope</label>
          <select
            value={roomTypeId}
            onChange={(e) => setRoomTypeId(e.target.value)}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
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
          <label className="block text-xs font-medium text-slate-600 mb-1">Reason</label>
          <input
            required
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="e.g. Holiday closure, renovation"
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
          />
        </div>
        {error && <p className="md:col-span-2 text-red-600 text-sm">{error}</p>}
        <div className="md:col-span-2">
          <button
            type="submit"
            disabled={createMutation.isPending}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium disabled:opacity-50"
          >
            {createMutation.isPending ? 'Adding...' : 'Add blackout'}
          </button>
        </div>
      </form>

      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        {isLoading ? (
          <p className="p-4 text-slate-500 text-sm">Loading blackout dates...</p>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left">
              <tr>
                <th className="px-4 py-2">Dates</th>
                <th className="px-4 py-2">Scope</th>
                <th className="px-4 py-2">Reason</th>
                <th className="px-4 py-2"></th>
              </tr>
            </thead>
            <tbody>
              {blackouts?.map((b: BlackoutDate) => (
                <tr key={b.id} className="border-t border-slate-100">
                  <td className="px-4 py-2 text-xs">
                    {b.startDate} → {b.endDate}
                  </td>
                  <td className="px-4 py-2">
                    {b.roomTypeName ?? 'Entire property'}
                  </td>
                  <td className="px-4 py-2 text-slate-600">{b.reason}</td>
                  <td className="px-4 py-2 text-right">
                    <button
                      onClick={() => deleteMutation.mutate(b.id)}
                      disabled={deleteMutation.isPending}
                      className="text-red-600 hover:underline text-xs"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
              {blackouts?.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-4 py-6 text-center text-slate-500">
                    No blackout dates configured
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
