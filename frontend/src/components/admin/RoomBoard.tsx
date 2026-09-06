import { useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../lib/api';
import type { HousekeepingStatus, RoomBoardItem } from '../../types';
import { StatusPill, EmptyState, InlineAlert, ConfirmAction } from '../ui';

interface Props {
  token: string;
}

export function RoomBoard({ token }: Props) {
  const queryClient = useQueryClient();
  const [actionError, setActionError] = useState('');
  const [actionOk, setActionOk] = useState('');
  const [selectedId, setSelectedId] = useState<number | null>(null);

  const { data: rooms, isLoading, error } = useQuery({
    queryKey: ['room-board'],
    queryFn: () => api.listRoomBoard(token),
  });

  const selected = useMemo(
    () => rooms?.find((r) => r.id === selectedId) ?? null,
    [rooms, selectedId],
  );

  async function refresh() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['room-board'] }),
      queryClient.invalidateQueries({ queryKey: ['admin-bookings'] }),
      queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
    ]);
  }

  async function checkIn(room: RoomBoardItem, force = false) {
    if (!room.currentBookingId) return;
    setActionError('');
    setActionOk('');
    try {
      await api.checkInBooking(token, room.currentBookingId, force);
      await refresh();
      setActionOk(`Checked in room #${room.roomNumber}`);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Check-in failed');
    }
  }

  async function checkOut(room: RoomBoardItem) {
    if (!room.currentBookingId) return;
    setActionError('');
    setActionOk('');
    try {
      await api.checkOutBooking(token, room.currentBookingId);
      await refresh();
      setActionOk(`Checked out room #${room.roomNumber}`);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Check-out failed');
    }
  }

  async function assign(bookingId: number, roomId: number) {
    setActionError('');
    setActionOk('');
    try {
      await api.assignRoom(token, bookingId, roomId);
      await refresh();
      setActionOk(`Assigned booking #${bookingId}`);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Assign failed');
    }
  }

  if (isLoading) return <p className="text-sm text-ink/45">Loading room board…</p>;
  if (error) return <InlineAlert tone="error">{(error as Error).message}</InlineAlert>;
  if (!rooms?.length) {
    return (
      <EmptyState title="No rooms" description="Room inventory has not been seeded for this property." />
    );
  }

  return (
    <div className="space-y-4">
      {actionError && <InlineAlert tone="error">{actionError}</InlineAlert>}
      {actionOk && <InlineAlert tone="success">{actionOk}</InlineAlert>}

      <div className="flex flex-wrap gap-3 text-xs text-ink/55">
        <Legend swatch="room-cell-available" label="Available" />
        <Legend swatch="room-cell-occupied" label="Occupied" />
        <Legend swatch="room-cell-maintenance" label="Maintenance" />
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-5">
          {rooms.map((room) => {
            const cellClass =
              room.status === 'OCCUPIED'
                ? 'room-cell-occupied'
                : room.status === 'MAINTENANCE'
                  ? 'room-cell-maintenance'
                  : 'room-cell-available';
            const selectedRing = selectedId === room.id ? 'ring-2 ring-lagoon ring-offset-2' : '';
            return (
              <button
                key={room.id}
                type="button"
                onClick={() => setSelectedId(room.id)}
                className={`rounded-xl border p-3 text-left transition hover:-translate-y-0.5 ${cellClass} ${selectedRing}`}
              >
                <div className="flex items-start justify-between gap-1">
                  <span className="font-display text-lg font-bold text-ink">#{room.roomNumber}</span>
                  <StatusPill label={room.housekeepingStatus} />
                </div>
                <p className="mt-1 truncate text-xs text-ink/55">{room.roomTypeName}</p>
                <p className="mt-2 truncate text-xs font-medium text-forest">
                  {room.currentGuestName ?? room.status.replace(/_/g, ' ')}
                </p>
              </button>
            );
          })}
        </div>

        <aside className="ops-panel h-fit lg:sticky lg:top-6">
          {!selected ? (
            <p className="text-sm text-ink/50">Select a room for guest details and actions.</p>
          ) : (
            <RoomDetail
              room={selected}
              onCheckIn={checkIn}
              onCheckOut={checkOut}
              onAssign={assign}
            />
          )}
        </aside>
      </div>
    </div>
  );
}

function RoomDetail({
  room,
  onCheckIn,
  onCheckOut,
  onAssign,
}: {
  room: RoomBoardItem;
  onCheckIn: (room: RoomBoardItem, force?: boolean) => Promise<void>;
  onCheckOut: (room: RoomBoardItem) => Promise<void>;
  onAssign: (bookingId: number, roomId: number) => Promise<void>;
}) {
  return (
    <div className="space-y-4 animate-tab">
      <div>
        <p className="text-[11px] font-semibold uppercase tracking-wider text-moss">Room detail</p>
        <h3 className="mt-1 font-display text-2xl font-bold text-ink">#{room.roomNumber}</h3>
        <p className="text-sm text-ink/55">
          {room.roomTypeName}
          {room.floor != null ? ` · Floor ${room.floor}` : ''}
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        <StatusPill label={room.status} />
        <StatusPill label={room.housekeepingStatus} />
      </div>

      {room.currentGuestName ? (
        <div className="rounded-xl border border-sand bg-fog/60 px-3 py-3 text-sm">
          <p className="font-medium text-ink">{room.currentGuestName}</p>
          <p className="mt-0.5 font-mono text-xs text-ink/50">{room.confirmationCode}</p>
          <p className="mt-1 text-xs text-ink/45">Booking #{room.currentBookingId}</p>
        </div>
      ) : (
        <p className="text-sm text-ink/50">No guest currently linked to this room.</p>
      )}

      {room.notes && <p className="text-xs text-ink/55">Note: {room.notes}</p>}

      <div className="flex flex-wrap gap-2 border-t border-sand/70 pt-3">
        {room.currentBookingId && room.status !== 'OCCUPIED' && (
          <ConfirmAction
            label="Check in"
            confirmLabel={room.housekeepingStatus !== 'CLEAN' ? 'Force check-in' : 'Confirm'}
            onConfirm={() => onCheckIn(room, room.housekeepingStatus !== 'CLEAN')}
          >
            {room.housekeepingStatus !== 'CLEAN' ? 'Room not clean — force?' : 'Confirm arrival?'}
          </ConfirmAction>
        )}
        {room.status === 'OCCUPIED' && room.currentBookingId && (
          <ConfirmAction label="Check out" tone="danger" onConfirm={() => onCheckOut(room)}>
            Complete departure?
          </ConfirmAction>
        )}
      </div>

      {!room.currentBookingId && room.status === 'AVAILABLE' && (
        <AssignForm roomId={room.id} onAssign={onAssign} />
      )}
    </div>
  );
}

function AssignForm({
  roomId,
  onAssign,
}: {
  roomId: number;
  onAssign: (bookingId: number, roomId: number) => Promise<void>;
}) {
  return (
    <form
      className="space-y-2 border-t border-sand/70 pt-3"
      onSubmit={async (e) => {
        e.preventDefault();
        const fd = new FormData(e.currentTarget);
        const bookingId = Number(fd.get('bookingId'));
        if (!bookingId) return;
        await onAssign(bookingId, roomId);
        e.currentTarget.reset();
      }}
    >
      <label className="label-caps">Assign booking ID</label>
      <div className="flex gap-2">
        <input
          name="bookingId"
          type="number"
          required
          placeholder="e.g. 12"
          className="input-field flex-1"
        />
        <button type="submit" className="btn-secondary shrink-0">
          Assign
        </button>
      </div>
    </form>
  );
}

function Legend({ swatch, label }: { swatch: string; label: string }) {
  return (
    <span className="inline-flex items-center gap-1.5">
      <span className={`h-3 w-3 rounded border ${swatch}`} />
      {label}
    </span>
  );
}

export function HousekeepingBoard({ token }: Props) {
  const queryClient = useQueryClient();
  const [actionError, setActionError] = useState('');
  const [actionOk, setActionOk] = useState('');
  const { data: rooms, isLoading } = useQuery({
    queryKey: ['room-board'],
    queryFn: () => api.listRoomBoard(token),
  });

  async function setStatus(roomId: number, housekeepingStatus: HousekeepingStatus, force = false) {
    setActionError('');
    setActionOk('');
    try {
      await api.updateHousekeeping(token, roomId, housekeepingStatus, force);
      queryClient.invalidateQueries({ queryKey: ['room-board'] });
      setActionOk(`Room updated to ${housekeepingStatus}`);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Update failed');
    }
  }

  const groups: { key: HousekeepingStatus; title: string; hint: string }[] = [
    { key: 'DIRTY', title: 'Dirty', hint: 'Awaiting service' },
    { key: 'INSPECTING', title: 'Inspecting', hint: 'Ready for QC' },
    { key: 'CLEAN', title: 'Clean', hint: 'Ready for arrival' },
  ];

  if (isLoading) return <p className="text-sm text-ink/45">Loading housekeeping board…</p>;

  return (
    <div className="space-y-4">
      {actionError && <InlineAlert tone="error">{actionError}</InlineAlert>}
      {actionOk && <InlineAlert tone="success">{actionOk}</InlineAlert>}

      <div className="grid gap-4 lg:grid-cols-3">
        {groups.map((g) => {
          const items = rooms?.filter((r) => r.housekeepingStatus === g.key) ?? [];
          return (
            <div key={g.key} className="ops-panel space-y-3">
              <div className="flex items-center justify-between gap-2">
                <div>
                  <h3 className="font-display text-lg font-semibold text-ink">{g.title}</h3>
                  <p className="text-xs text-ink/45">{g.hint}</p>
                </div>
                <span className="font-display text-2xl font-bold text-forest tabular-nums">
                  {items.length}
                </span>
              </div>

              {items.length === 0 ? (
                <EmptyState title={`No ${g.title.toLowerCase()} rooms`} />
              ) : (
                <ul className="space-y-2">
                  {items.map((room) => (
                    <li
                      key={room.id}
                      className="rounded-xl border border-sand/80 bg-white/80 px-3 py-2.5"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <p className="font-semibold text-ink">#{room.roomNumber}</p>
                          <p className="text-xs text-ink/50">{room.roomTypeName}</p>
                        </div>
                        <StatusPill label={room.status} />
                      </div>
                      <div className="mt-2 flex flex-wrap gap-1.5">
                        {g.key !== 'DIRTY' && (
                          <button
                            type="button"
                            className="btn-ghost !px-2 !py-1 text-xs hk-dirty"
                            onClick={() => setStatus(room.id, 'DIRTY')}
                          >
                            Dirty
                          </button>
                        )}
                        {g.key !== 'INSPECTING' && (
                          <button
                            type="button"
                            className="btn-ghost !px-2 !py-1 text-xs hk-inspecting"
                            onClick={() => setStatus(room.id, 'INSPECTING')}
                          >
                            Inspect
                          </button>
                        )}
                        {g.key !== 'CLEAN' && (
                          <button
                            type="button"
                            className="btn-ghost !px-2 !py-1 text-xs hk-clean"
                            onClick={() =>
                              setStatus(room.id, 'CLEAN', room.housekeepingStatus !== 'INSPECTING')
                            }
                          >
                            Clean
                          </button>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
