import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../lib/api';
import type { TicketPriority, TicketStatus } from '../../types';
import { StatusPill, EmptyState, InlineAlert } from '../ui';

interface Props {
  token: string;
  hotelId: number;
}

export function MaintenanceTickets({ token, hotelId }: Props) {
  const queryClient = useQueryClient();
  const { data: tickets, isLoading } = useQuery({
    queryKey: ['maintenance-tickets'],
    queryFn: () => api.listMaintenanceTickets(token),
  });
  const { data: rooms } = useQuery({
    queryKey: ['room-board'],
    queryFn: () => api.listRoomBoard(token),
  });

  const [roomId, setRoomId] = useState('');
  const [title, setTitle] = useState('');
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM');
  const [ooo, setOoo] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  async function createTicket(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      await api.createMaintenanceTicket(token, {
        hotelId,
        roomId: Number(roomId),
        title,
        priority,
        markRoomOutOfOrder: ooo,
      });
      setTitle('');
      setRoomId('');
      setOoo(false);
      setShowCreate(false);
      setSuccess('Ticket created.');
      queryClient.invalidateQueries({ queryKey: ['maintenance-tickets'] });
      queryClient.invalidateQueries({ queryKey: ['room-board'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create ticket');
    }
  }

  async function updateStatus(id: number, status: TicketStatus) {
    setError('');
    try {
      await api.updateMaintenanceTicket(token, id, { status });
      queryClient.invalidateQueries({ queryKey: ['maintenance-tickets'] });
      queryClient.invalidateQueries({ queryKey: ['room-board'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      setSuccess(`Ticket #${id} → ${status.replace(/_/g, ' ')}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Update failed');
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button type="button" className="btn-primary" onClick={() => setShowCreate((v) => !v)}>
          {showCreate ? 'Close form' : 'New ticket'}
        </button>
      </div>

      {error && <InlineAlert tone="error">{error}</InlineAlert>}
      {success && <InlineAlert tone="success">{success}</InlineAlert>}

      {showCreate && (
        <form onSubmit={createTicket} className="ops-panel space-y-3 animate-panel">
          <h3 className="font-display text-lg font-semibold text-ink">Create maintenance ticket</h3>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <label className="label-caps">Room</label>
              <select
                required
                value={roomId}
                onChange={(e) => setRoomId(e.target.value)}
                className="input-field"
              >
                <option value="">Select room</option>
                {rooms?.map((r) => (
                  <option key={r.id} value={r.id}>
                    #{r.roomNumber} · {r.roomTypeName}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="label-caps">Priority</label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value as TicketPriority)}
                className="input-field"
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="URGENT">URGENT</option>
              </select>
            </div>
          </div>
          <div>
            <label className="label-caps">Issue</label>
            <input
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Issue title"
              className="input-field"
            />
          </div>
          <label className="flex items-center gap-2 text-sm text-ink/65">
            <input type="checkbox" checked={ooo} onChange={(e) => setOoo(e.target.checked)} />
            Mark room out of order (MAINTENANCE)
          </label>
          <button type="submit" className="btn-primary">
            Create ticket
          </button>
        </form>
      )}

      {isLoading ? (
        <p className="text-sm text-ink/45">Loading tickets…</p>
      ) : !tickets?.length ? (
        <EmptyState
          title="No tickets yet"
          description="Open a work order when a room needs repair or engineering attention."
          action={
            !showCreate ? (
              <button type="button" className="btn-secondary" onClick={() => setShowCreate(true)}>
                Create ticket
              </button>
            ) : undefined
          }
        />
      ) : (
        <ul className="space-y-2">
          {tickets.map((t) => (
            <li key={t.id} className="ops-panel flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="min-w-0">
                <p className="font-medium text-ink">
                  #{t.roomNumber} · {t.title}
                </p>
                <div className="mt-2 flex flex-wrap gap-2">
                  <StatusPill label={t.priority} />
                  <StatusPill label={t.status} />
                </div>
              </div>
              <select
                value={t.status}
                onChange={(e) => updateStatus(t.id, e.target.value as TicketStatus)}
                className="input-field sm:w-44"
              >
                <option value="OPEN">OPEN</option>
                <option value="IN_PROGRESS">IN_PROGRESS</option>
                <option value="RESOLVED">RESOLVED</option>
                <option value="CANCELLED">CANCELLED</option>
              </select>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
