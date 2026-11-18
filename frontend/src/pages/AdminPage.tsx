import { useEffect, useMemo, useState, type ReactNode } from 'react';
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
import { GuestDirectory } from '../components/admin/GuestDirectory';
import { GuestRequests } from '../components/admin/GuestRequests';
import { NightAuditPanel } from '../components/admin/NightAuditPanel';
import { PageHeader, MetricTile, StatusPill, EmptyState, InlineAlert, SegmentedControl } from '../components/ui';
import { money } from '../lib/format';
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
  | 'guests'
  | 'requests'
  | 'night-audit'
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
  'guests',
  'requests',
  'night-audit',
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

  useEffect(() => {
    setFeedback(null);
  }, [tab]);

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['dashboard', user?.hotelId],
    queryFn: () => api.getDashboard(user!.token),
    enabled: !!user?.token,
  });

  const { data: bookings, refetch, isLoading: bookingsLoading } = useQuery({
    queryKey: ['admin-bookings', user?.hotelId],
    queryFn: () => api.listBookings(user!.token),
    enabled: !!user?.token,
  });

  const businessDate = stats?.businessDate ?? '';

  const todayBookings = useMemo(() => {
    const list = bookings?.content ?? [];
    if (!businessDate) {
      return { arrivals: [] as Booking[], departures: [] as Booking[] };
    }
    return {
      arrivals: list.filter(
        (b) =>
          (b.status === 'CONFIRMED' || b.status === 'PENDING') && b.checkInDate <= businessDate,
      ),
      departures: list.filter((b) => b.status === 'CHECKED_IN' && b.checkOutDate <= businessDate),
    };
  }, [bookings, businessDate]);

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

  async function handleNoShow(id: number) {
    setFeedback(null);
    try {
      await api.markNoShow(user!.token, id, true);
      refetch();
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['night-audit'] });
      queryClient.invalidateQueries({ queryKey: ['room-board'] });
      setFeedback({ tone: 'success', text: `Booking #${id} marked as a no-show. One night was retained.` });
    } catch (err) {
      setFeedback({ tone: 'error', text: getErrorMessage(err, 'Could not mark that arrival as a no-show.') });
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
            eyebrow={businessDate ? `Business date ${businessDate}` : 'Today'}
            title="Property pulse"
            description="Occupancy, ADR, and RevPAR for the rooms in house, plus who still needs the desk."
          />

          {statsLoading && <p className="text-sm text-ink/45">Loading dashboard…</p>}

          {stats && (
            <>
              <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
                <MetricTile
                  label="Occupancy"
                  value={`${stats.occupancyPercent}%`}
                  hint={`${stats.inHouse} in house · ${stats.maintenanceRooms} out of order`}
                  accent="forest"
                />
                <MetricTile label="ADR" value={money(stats.adr)} hint="Average daily rate" accent="lagoon" />
                <MetricTile label="RevPAR" value={money(stats.revpar)} hint={`Room revenue ${money(stats.roomRevenue)}`} accent="gold" />
                <MetricTile
                  label="Open balances"
                  value={money(stats.openBalance)}
                  hint={`${stats.dueOuts} due out · ${stats.noShowCandidates} no-show candidates`}
                  accent={stats.openBalance > 0 ? 'danger' : 'moss'}
                />
              </div>
              {stats.channels.length > 0 && (
                <div className="ops-panel">
                  <h2 className="font-display text-lg font-semibold text-ink">In-house by channel</h2>
                  <ul className="mt-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                    {stats.channels.map((channel) => (
                      <li key={channel.channel} className="rounded-xl border border-sand/80 bg-fog/60 px-3 py-2.5">
                        <p className="text-[11px] font-semibold uppercase tracking-wider text-ink/45">
                          {channel.channel.replace(/_/g, ' ')}
                        </p>
                        <p className="mt-1 font-display text-xl font-bold text-forest">{money(channel.roomRevenue)}</p>
                        <p className="text-xs text-ink/50">{channel.rooms} room{channel.rooms === 1 ? '' : 's'}</p>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </>
          )}

          <div className="grid gap-4 lg:grid-cols-2">
            <PulseList
              title="Arrivals"
              empty="No arrivals left on the business date."
              items={todayBookings.arrivals}
              action={(booking) => (
                <button type="button" className="btn-ghost !px-2 !py-1 text-xs" onClick={() => handleNoShow(booking.id)}>
                  No-show
                </button>
              )}
            />
            <PulseList
              title="Due out"
              empty="No in-house departures."
              items={todayBookings.departures}
            />
          </div>

          <ReservationsTable
            bookings={bookings?.content}
            loading={bookingsLoading}
            onStatusChange={handleStatusChange}
            onNoShow={handleNoShow}
            businessDate={businessDate}
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
            onNoShow={handleNoShow}
            businessDate={businessDate}
          />
        </div>
      )}

      {tab === 'guests' && (
        <div className="space-y-5 animate-tab">
          <PageHeader
            eyebrow="CRM"
            title="Guest profiles"
            description="Returning guests, VIP tier, stay history, and a block on new reservations."
          />
          <GuestDirectory
            token={user!.token}
            initialGuestId={Number(searchParams.get('guestId')) || null}
          />
        </div>
      )}

      {tab === 'requests' && (
        <div className="space-y-5 animate-tab">
          <PageHeader
            eyebrow="Front desk"
            title="Guest requests"
            description="Traces the desk promised — late checkout, airport, extra bed — until someone closes them."
          />
          <GuestRequests token={user!.token} />
        </div>
      )}

      {tab === 'night-audit' && (
        <div className="space-y-5 animate-tab">
          <PageHeader
            eyebrow="Accounting"
            title="Night audit"
            description="Review the day, clear due-outs, post no-shows, and roll the business date."
          />
          <NightAuditPanel token={user!.token} />
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
            description="Search a booking, review the ledger, post F&B charges, and take cash, card, or transfer."
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
  action,
}: {
  title: string;
  empty: string;
  items: Booking[];
  action?: (booking: Booking) => ReactNode;
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
              <div className="flex shrink-0 items-center gap-2">
                {action?.(b)}
                <StatusPill label={b.status} />
              </div>
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
  onNoShow,
  businessDate,
  compact,
}: {
  bookings?: Booking[];
  loading: boolean;
  onStatusChange: (id: number, status: BookingStatus) => void;
  onNoShow: (id: number) => void;
  businessDate?: string;
  compact?: boolean;
}) {
  const [filter, setFilter] = useState<'all' | 'active' | 'today'>('all');
  const today = businessDate || '';

  const filtered = useMemo(() => {
    const list = bookings ?? [];
    if (filter === 'active') {
      return list.filter((b) => !['CANCELLED', 'CHECKED_OUT', 'NO_SHOW'].includes(b.status));
    }
    if (filter === 'today') {
      if (!today) return [];
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
                <th>
                  <span className="block">Override</span>
                  <span className="font-normal normal-case tracking-normal text-ink/40">
                    Prefer Front Office for check-in/out
                  </span>
                </th>
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
                    <div className="flex items-center gap-2">
                      {(b.status === 'CONFIRMED' || b.status === 'PENDING') && (
                        <button
                          type="button"
                          className="btn-ghost !px-2 !py-1 text-xs"
                          onClick={() => onNoShow(b.id)}
                        >
                          No-show
                        </button>
                      )}
                      <select
                        value={b.status}
                        onChange={(e) => onStatusChange(b.id, e.target.value as BookingStatus)}
                        className="input-field !w-auto !py-1.5 !px-2 text-xs"
                        title="Manual override — use Front Office for arrivals and departures"
                      >
                        <option value="PENDING">PENDING</option>
                        <option value="CONFIRMED">CONFIRMED</option>
                        <option value="CHECKED_IN">CHECKED_IN</option>
                        <option value="CHECKED_OUT">CHECKED_OUT</option>
                        <option value="CANCELLED">CANCELLED</option>
                        {b.status === 'NO_SHOW' && <option value="NO_SHOW">NO_SHOW</option>}
                      </select>
                    </div>
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
