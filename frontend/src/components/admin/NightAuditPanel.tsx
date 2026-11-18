import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../../lib/api';
import { money } from '../../lib/format';
import { ConfirmAction, EmptyState, InlineAlert, MetricTile, StatusPill } from '../ui';

interface Props {
  token: string;
}

export function NightAuditPanel({ token }: Props) {
  const queryClient = useQueryClient();
  const [note, setNote] = useState('');
  const [retain, setRetain] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['night-audit'],
    queryFn: () => api.getNightAudit(token),
  });

  async function closeNight() {
    setError('');
    setSuccess('');
    try {
      const closed = await api.closeNightAudit(token, {
        note: note.trim() || undefined,
        retainNoShowNight: retain,
      });
      setNote('');
      setSuccess(`Closed ${closed.businessDate}. The desk is now on the next business date.`);
      queryClient.invalidateQueries({ queryKey: ['night-audit'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['admin-bookings'] });
      queryClient.invalidateQueries({ queryKey: ['room-board'] });
    } catch (err) {
      setError(getErrorMessage(err, 'Night audit could not close.'));
    }
  }

  if (isLoading || !data) {
    return <p className="text-sm text-ink/45">Loading night audit…</p>;
  }

  return (
    <div className="space-y-5">
      {error && <InlineAlert tone="error">{error}</InlineAlert>}
      {success && <InlineAlert tone="success">{success}</InlineAlert>}

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <MetricTile
          label="Business date"
          value={data.businessDate}
          hint={data.alreadyClosed ? 'Already closed' : 'Open'}
          accent="forest"
          dense
        />
        <MetricTile label="Occupancy" value={`${data.occupancyPercent}%`} hint={`${data.roomsSold} sold · ${data.sellableRooms} sellable`} accent="moss" />
        <MetricTile label="ADR" value={money(data.adr)} hint="Room revenue / rooms sold" accent="lagoon" />
        <MetricTile label="RevPAR" value={money(data.revpar)} hint={`Room revenue ${money(data.roomRevenue)}`} accent="gold" />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <StayList
          title="Due out"
          empty="No departures left in house."
          items={data.dueOutList}
          warn={data.dueOuts > 0}
        />
        <StayList
          title="No-show candidates"
          empty="Every expected arrival is resolved."
          items={data.noShowList}
        />
      </div>

      <form className="ops-panel space-y-3" onSubmit={(e) => e.preventDefault()}>
        <div>
          <h2 className="font-display text-lg font-semibold text-ink">Close the night</h2>
          <p className="mt-1 text-sm text-ink/55">
            Due-outs must be checked out or extended first. Expected arrivals that are still unarrived become no-shows, and the business date rolls forward.
          </p>
        </div>
        <label className="label-caps">Manager note</label>
        <input
          value={note}
          onChange={(e) => setNote(e.target.value)}
          className="input-field"
          placeholder="Weather, city event, or anything that explains the day"
        />
        <label className="flex items-center gap-2 text-sm text-ink/75">
          <input type="checkbox" checked={retain} onChange={(e) => setRetain(e.target.checked)} />
          Retain one night on each no-show and waive the rest of the stay
        </label>
        <ConfirmAction
          label="Close business date"
          confirmLabel="Close night"
          tone="danger"
          disabled={data.alreadyClosed || data.dueOuts > 0}
          onConfirm={closeNight}
        />
        {data.dueOuts > 0 && (
          <p className="text-sm text-[color:var(--color-danger)]">
            {data.dueOuts === 1
              ? '1 in-house departure still needs a checkout or an extended stay.'
              : `${data.dueOuts} in-house departures still need a checkout or an extended stay.`}
          </p>
        )}
      </form>

      <div className="ops-panel !p-0 overflow-hidden">
        <div className="border-b border-sand/70 px-4 py-3">
          <h2 className="font-display text-lg font-semibold text-ink">Recent closes</h2>
        </div>
        {data.history.length === 0 ? (
          <div className="p-4">
            <EmptyState title="No nights closed yet" description="The first close stores occupancy, ADR, and RevPAR for the morning report." />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Occ.</th>
                  <th>ADR</th>
                  <th>RevPAR</th>
                  <th>Revenue</th>
                  <th>No-shows</th>
                  <th>Note</th>
                </tr>
              </thead>
              <tbody>
                {data.history.map((row) => (
                  <tr key={row.id}>
                    <td className="whitespace-nowrap text-xs">{row.businessDate}</td>
                    <td className="text-xs">{row.occupancyPercent}%</td>
                    <td className="text-xs tabular-nums">{money(row.adr)}</td>
                    <td className="text-xs tabular-nums">{money(row.revpar)}</td>
                    <td className="text-xs tabular-nums">{money(row.roomRevenue)}</td>
                    <td className="text-xs">{row.noShows}</td>
                    <td className="max-w-xs truncate text-xs text-ink/60">{row.note || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

function StayList({
  title,
  empty,
  items,
  warn,
}: {
  title: string;
  empty: string;
  items: { bookingId: number; confirmationCode: string; guestName: string; roomNumber: string | null; status: string }[];
  warn?: boolean;
}) {
  return (
    <div className="ops-panel">
      <h2 className="font-display text-lg font-semibold text-ink">{title}</h2>
      {items.length === 0 ? (
        <p className="mt-3 text-sm text-ink/50">{empty}</p>
      ) : (
        <ul className="mt-3 divide-y divide-sand/70">
          {items.map((item) => (
            <li key={item.bookingId} className="flex items-center justify-between gap-3 py-2.5 text-sm">
              <div className="min-w-0">
                <p className="truncate font-medium text-ink">{item.guestName}</p>
                <p className="truncate text-xs text-ink/45">
                  {item.confirmationCode}
                  {item.roomNumber ? ` · Room ${item.roomNumber}` : ' · Unassigned'}
                </p>
              </div>
              <StatusPill label={item.status} tone={warn ? 'cancelled' : undefined} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
