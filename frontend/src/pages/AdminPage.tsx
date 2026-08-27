import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../lib/api';
import { useAuthStore, useRagStore } from '../stores';
import { RagChat } from '../components/RagChat';
import { BlackoutDateManager } from '../components/BlackoutDateManager';
import type { BookingStatus, Booking } from '../types';

export function AdminPage() {
  const { user, isStaff, logout } = useAuthStore();
  const queryClient = useQueryClient();
  const { staffMessages, addStaffMessage, clearStaffMessages } = useRagStore();
  const [ragLoading, setRagLoading] = useState(false);
  const [ragAvailable, setRagAvailable] = useState<boolean | null>(null);

  const { data: stats } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => api.getDashboard(user!.token),
    enabled: !!user?.token && isStaff(),
  });

  const { data: bookings, refetch } = useQuery({
    queryKey: ['admin-bookings'],
    queryFn: () => api.listBookings(user!.token),
    enabled: !!user?.token && isStaff(),
  });

  if (!user || !isStaff()) {
    return <Navigate to="/login" replace />;
  }

  async function handleStatusChange(id: number, status: BookingStatus) {
    await api.updateBookingStatus(user!.token, id, status);
    refetch();
    queryClient.invalidateQueries({ queryKey: ['dashboard'] });
  }

  async function handleStaffRag(message: string) {
    addStaffMessage({ role: 'user', content: message });
    setRagLoading(true);
    try {
      const history = staffMessages.map((m) => ({ role: m.role, content: m.content }));
      const res = await api.staffRagChat(user!.token, message, user!.hotelId, history);
      setRagAvailable(res.ragAvailable);
      addStaffMessage({
        role: 'assistant',
        content: res.answer,
        citations: res.citations,
        availability: res.availability,
        hotelPath: res.hotelPath,
        blackouts: res.blackouts,
        propertySummary: res.propertySummary,
        bookingSummary: res.bookingSummary,
      });
    } catch (err) {
      addStaffMessage({
        role: 'assistant',
        content: err instanceof Error ? err.message : 'Assistant unavailable',
      });
    } finally {
      setRagLoading(false);
    }
  }

  function handleLogout() {
    clearStaffMessages();
    logout();
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 space-y-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Admin Dashboard</h1>
        <button onClick={handleLogout} className="text-sm text-red-600 hover:underline">
          Logout & clear staff chat
        </button>
      </div>

      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard label="Total rooms" value={stats.totalRooms} />
          <StatCard label="Occupied" value={stats.occupiedRooms} />
          <StatCard label="Available" value={stats.availableRooms} />
          <StatCard label="Check-ins today" value={stats.upcomingCheckIns} />
          <StatCard label="Check-outs today" value={stats.upcomingCheckOuts} />
          <StatCard label="Active bookings" value={stats.activeBookings} />
          <StatCard label="Maintenance" value={stats.maintenanceRooms} />
        </div>
      )}

      <div className="grid lg:grid-cols-2 gap-8">
        <div className="space-y-8">
          <div>
            <h2 className="text-lg font-semibold mb-4">Bookings</h2>
          <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-left">
                <tr>
                  <th className="px-4 py-2">Guest</th>
                  <th className="px-4 py-2">Room</th>
                  <th className="px-4 py-2">Dates</th>
                  <th className="px-4 py-2">Status</th>
                  <th className="px-4 py-2">Actions</th>
                </tr>
              </thead>
              <tbody>
                {bookings?.content.map((b: Booking) => (
                  <tr key={b.id} className="border-t border-slate-100">
                    <td className="px-4 py-2">{b.guestName}</td>
                    <td className="px-4 py-2">{b.roomTypeName} #{b.roomNumber}</td>
                    <td className="px-4 py-2 text-xs">
                      {b.checkInDate} → {b.checkOutDate}
                    </td>
                    <td className="px-4 py-2">
                      <span className="bg-slate-100 px-2 py-0.5 rounded text-xs">{b.status}</span>
                    </td>
                    <td className="px-4 py-2">
                      <select
                        value={b.status}
                        onChange={(e) => handleStatusChange(b.id, e.target.value as BookingStatus)}
                        className="text-xs border border-slate-300 rounded px-1 py-0.5"
                      >
                        <option value="CONFIRMED">CONFIRMED</option>
                        <option value="CHECKED_IN">CHECKED_IN</option>
                        <option value="CHECKED_OUT">CHECKED_OUT</option>
                        <option value="CANCELLED">CANCELLED</option>
                      </select>
                    </td>
                  </tr>
                ))}
                {bookings?.content.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-4 py-6 text-center text-slate-500">
                      No bookings yet
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          </div>

          <BlackoutDateManager token={user.token} hotelId={user.hotelId ?? 1} />
        </div>

        <RagChat
          title="Staff Assistant"
          messages={staffMessages}
          onSend={handleStaffRag}
          loading={ragLoading}
          ragAvailable={ragAvailable}
          placeholder="Ask about bookings next month, blackouts, occupancy..."
        />
      </div>
    </div>
  );
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4">
      <p className="text-slate-500 text-xs">{label}</p>
      <p className="text-2xl font-bold text-indigo-700">{value}</p>
    </div>
  );
}
