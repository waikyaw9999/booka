import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../lib/api';
import { useAuthStore, useRagStore } from '../stores';
import { RagChat } from '../components/RagChat';
import { BlackoutDateManager } from '../components/BlackoutDateManager';
import { RoomBoard, HousekeepingBoard } from '../components/admin/RoomBoard';
import { MaintenanceTickets } from '../components/admin/MaintenanceTickets';
import { FolioPanel } from '../components/admin/FolioPanel';
import { WalkInForm } from '../components/admin/WalkInForm';
import { PropertyInventory } from '../components/admin/PropertyInventory';
import { PageHeader, MetricTile, StatusPill, EmptyState, InlineAlert, SegmentedControl } from '../components/ui';
import type { Booking, BookingStatus } from '../types';

type Tab =
  | 'dashboard'
  | 'front-office'
  | 'reservations'
  | 'housekeeping'
  | 'maintenance'
  | 'folios'
  | 'inventory'
  | 'blackouts'
  | 'assistant';

const TAB_IDS: Tab[] = [
  'dashboard',
  'front-office',
  'reservations',
  'housekeeping',
  'maintenance',
  'folios',
  'inventory',
  'blackouts',
  'assistant',
];

export function AdminPage() {
  const { user } = useAuthStore();
  const queryClient = useQueryClient();
  const { staffMessages, addStaffMessage, clearStaffMessages } = useRagStore();
  const [ragLoading, setRagLoading] = useState(false);
  const [ragAvailable, setRagAvailable] = useState<boolean | null>(null);
  const [feedback, setFeedback] = useState<{ tone: 'success' | 'error'; text: string } | null>(null);
  const [searchParams] = useSearchParams();

  const rawTab = searchParams.get('tab') ?? 'dashboard';
  const tab: Tab = TAB_IDS.includes(rawTab as Tab) ? (rawTab as Tab) : 'dashboard';

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => api.getDashboard(user!.token),
    enabled: !!user?.token,
  });

  const { data: bookings, refetch, isLoading: bookingsLoading } = useQuery({
    queryKey: ['admin-bookings'],
    queryFn: () => api.listBookings(user!.token),
    enabled: !!user?.token,
  });

  const todayBookings = useMemo(() => {
    const today = new Date().toISOString().slice(0, 10);
    const list = bookings?.content ?? [];
    return {
      arrivals: list.filter((b) => b.checkInDate === today && b.status !== 'CANCELLED' && b.status !== 'CHECKED_OUT'),
      departures: list.filter((b) => b.checkOutDate === today && b.status === 'CHECKED_IN'),
    };
  }, [bookings]);

  const occupancyPct =
    stats && stats.totalRooms > 0
      ? Math.round((stats.occupiedRooms / stats.totalRooms) * 100)
      : 0;

  async function handleStatusChange(id: number, status: BookingStatus) {
    setFeedback(null);
    try {
      await api.updateBookingStatus(user!.token, id, status);
      refetch();
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['room-board'] });
      setFeedback({ tone: 'success', text: `Booking #${id} updated to ${status.replace(/_/g, ' ')}.` });
    } catch (err) {
      setFeedback({ tone: 'error', text: getErrorMessage(err, 'Status update failed. Please try again.') });
    }
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
        suggestedQuestions: res.suggestedQuestions,
      });
    } catch (err) {
      addStaffMessage({
        role: 'assistant',
        content: getErrorMessage(err, 'Assistant unavailable. Please try again.'),
      });
    } finally {
      setRagLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6 animate-panel">
      {feedback && (
        <InlineAlert tone={feedback.tone === 'success' ? 'success' : 'error'}>{feedback.text}</InlineAlert>
      )}

      {tab === 'dashboard' && (
        <div className="space-y-6 animate-tab">
          <PageHeader
            eyebrow="Today"
            title="Property pulse"
            description="Occupancy, arrivals, and active stays at a glance."
          />

          {statsLoading && <p className="text-sm text-ink/45">Loading dashboard…</p>}

          {stats && (
            <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
              <MetricTile label="Occupancy" value={`${occupancyPct}%`} hint={`${stats.occupiedRooms} of ${stats.totalRooms} rooms`} accent="forest" />
              <MetricTile label="Arrivals today" value={stats.upcomingCheckIns} accent="lagoon" />
              <MetricTile label="Departures today" value={stats.upcomingCheckOuts} accent="gold" />
              <MetricTile label="Active bookings" value={stats.activeBookings} hint={`${stats.availableRooms} available · ${stats.maintenanceRooms} OOO`} accent="moss" />
            </div>
          )}

          <div className="grid gap-4 lg:grid-cols-2">
            <PulseList
              title="Arrivals"
              empty="No arrivals scheduled for today."
              items={todayBookings.arrivals}
            />
            <PulseList
              title="Departures"
              empty="No departures scheduled for today."
              items={todayBookings.departures}
            />
          </div>

          <ReservationsTable
            bookings={bookings?.content}
            loading={bookingsLoading}
            onStatusChange={handleStatusChange}
            compact
          />
        </div>
      )}

      {tab === 'front-office' && (
        <div className="space-y-5 animate-tab">
          <PageHeader
            eyebrow="Front office"
            title="Room board"
            description="Inventory status, housekeeping badges, and in-room guest actions."
          />
          <RoomBoard token={user!.token} />
        </div>
      )}

      {tab === 'reservations' && (
        <div className="space-y-5 animate-tab">
          <PageHeader
            eyebrow="CRS"
            title="Reservations"
            description="Manage stays and create walk-in bookings at the desk."
          />
          <WalkInForm
            token={user!.token}
            hotelId={user!.hotelId ?? 1}
            onCreated={() => {
              refetch();
              queryClient.invalidateQueries({ queryKey: ['dashboard'] });
              queryClient.invalidateQueries({ queryKey: ['room-board'] });
              setFeedback({ tone: 'success', text: 'Walk-in reservation created.' });
            }}
          />
          <ReservationsTable
            bookings={bookings?.content}
            loading={bookingsLoading}
            onStatusChange={handleStatusChange}
          />
        </div>
      )}

      {tab === 'housekeeping' && (
        <div className="space-y-5 animate-tab">
          <PageHeader
            eyebrow="Housekeeping"
            title="Room readiness"
            description="Move rooms through Dirty → Inspecting → Clean."
          />
          <HousekeepingBoard token={user!.token} />
        </div>
      )}

      {tab === 'maintenance' && (
        <div className="space-y-5 animate-tab">
          <PageHeader
            eyebrow="Engineering"
            title="Maintenance"
            description="Track work orders and mark rooms out of order when needed."
          />
          <MaintenanceTickets token={user!.token} hotelId={user!.hotelId ?? 1} />
        </div>
      )}

      {tab === 'folios' && (
        <div className="space-y-5 animate-tab">
          <PageHeader
            eyebrow="Billing"
            title="Guest folios"
            description="Search a booking, review the ledger, and post F&B charges."
          />
          <FolioPanel token={user!.token} />
        </div>
      )}

      {tab === 'inventory' && (
        <div className="space-y-5 animate-tab">
          <PageHeader
            eyebrow="Property"
            title="Inventory"
            description="Add rooms and keep hotel and room-type amenities current."
          />
          <PropertyInventory token={user!.token} hotelId={user!.hotelId ?? 1} />
        </div>
      )}

      {tab === 'blackouts' && (
        <div className="space-y-5 animate-tab">
          <PageHeader
            eyebrow="Restrictions"
            title="Blackout dates"
            description="Block new bookings for property-wide or room-type windows."
          />
          <BlackoutDateManager token={user!.token} hotelId={user!.hotelId ?? 1} />
        </div>
      )}

      {tab === 'assistant' && (
        <div className="space-y-5 animate-tab">
          <PageHeader
            eyebrow="Staff"
            title="Operations assistant"
            description="Ask about occupancy, blackouts, or booking counts — grounded in live data."
            actions={
              <button type="button" className="btn-ghost text-xs" onClick={() => clearStaffMessages()}>
                Clear thread
              </button>
            }
          />
          <RagChat
            title="Staff Assistant"
            messages={staffMessages}
            onSend={handleStaffRag}
            loading={ragLoading}
            ragAvailable={ragAvailable}
            placeholder="Ask about bookings next month, blackouts, occupancy..."
          />
        </div>
      )}
    </div>
  );
}

function PulseList({
  title,
  empty,
  items,
}: {
  title: string;
  empty: string;
  items: Booking[];
}) {
  return (
    <div className="ops-panel">
      <h2 className="font-display text-lg font-semibold text-ink">{title}</h2>
      {items.length === 0 ? (
        <p className="mt-3 text-sm text-ink/50">{empty}</p>
      ) : (
        <ul className="mt-3 divide-y divide-sand/70">
          {items.map((b) => (
            <li key={b.id} className="flex items-center justify-between gap-3 py-2.5 text-sm">
              <div className="min-w-0">
                <p className="truncate font-medium text-ink">{b.guestName}</p>
                <p className="truncate text-xs text-ink/45">
                  {b.confirmationCode} · {b.roomTypeName}
                  {b.roomNumber ? ` #${b.roomNumber}` : ''}
                </p>
              </div>
              <StatusPill label={b.status} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function ReservationsTable({
  bookings,
  loading,
  onStatusChange,
  compact,
}: {
  bookings?: Booking[];
  loading: boolean;
  onStatusChange: (id: number, status: BookingStatus) => void;
  compact?: boolean;
}) {
  const [filter, setFilter] = useState<'all' | 'active' | 'today'>('all');
  const today = new Date().toISOString().slice(0, 10);

  const filtered = useMemo(() => {
    const list = bookings ?? [];
    if (filter === 'active') {
      return list.filter((b) => !['CANCELLED', 'CHECKED_OUT'].includes(b.status));
    }
    if (filter === 'today') {
      return list.filter((b) => b.checkInDate === today || b.checkOutDate === today);
    }
    return list;
  }, [bookings, filter, today]);

  return (
    <div className="ops-panel !p-0 overflow-hidden">
      <div className="flex flex-col gap-3 border-b border-sand/70 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
        <h2 className="font-display text-lg font-semibold text-ink">
          {compact ? 'Recent reservations' : 'All reservations'}
        </h2>
        {!compact && (
          <SegmentedControl
            value={filter}
            onChange={setFilter}
            options={[
              { id: 'all', label: 'All' },
              { id: 'active', label: 'Active' },
              { id: 'today', label: 'Today' },
            ]}
          />
        )}
      </div>

      {loading ? (
        <p className="px-4 py-8 text-sm text-ink/45">Loading reservations…</p>
      ) : filtered.length === 0 ? (
        <div className="p-4">
          <EmptyState
            title="No reservations"
            description="Create a walk-in at the desk or wait for direct / OTA bookings."
          />
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Guest</th>
                <th>Room</th>
                <th>Dates</th>
                <th>Channel</th>
                <th>Pay</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((b) => (
                <tr key={b.id}>
                  <td className="font-mono text-xs">{b.confirmationCode ?? b.id}</td>
                  <td>
                    <div className="font-medium text-ink">{b.guestName}</div>
                    <div className="text-xs text-ink/45">{b.guestEmail}</div>
                  </td>
                  <td className="text-xs">
                    {b.roomTypeName}
                    {b.roomNumber ? ` #${b.roomNumber}` : ' · unassigned'}
                  </td>
                  <td className="whitespace-nowrap text-xs text-ink/65">
                    {b.checkInDate} → {b.checkOutDate}
                  </td>
                  <td>
                    <StatusPill label={b.channel} tone="default" />
                  </td>
                  <td>
                    <StatusPill label={b.paymentStatus} />
                  </td>
                  <td>
                    <StatusPill label={b.status} />
                  </td>
                  <td>
                    <select
                      value={b.status}
                      onChange={(e) => onStatusChange(b.id, e.target.value as BookingStatus)}
                      className="input-field !py-1.5 !px-2 text-xs"
                    >
                      <option value="PENDING">PENDING</option>
                      <option value="CONFIRMED">CONFIRMED</option>
                      <option value="CHECKED_IN">CHECKED_IN</option>
                      <option value="CHECKED_OUT">CHECKED_OUT</option>
                      <option value="CANCELLED">CANCELLED</option>
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
