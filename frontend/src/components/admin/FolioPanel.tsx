import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../../lib/api';
import { StatusPill, EmptyState, InlineAlert } from '../ui';

interface Props {
  token: string;
}

function parseNumericBookingId(value: string): number | null {
  const trimmed = value.trim();
  if (!/^\d+$/.test(trimmed)) return null;
  const id = Number(trimmed);
  return Number.isFinite(id) && id > 0 ? id : null;
}

export function FolioPanel({ token }: Props) {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState('');
  const [resolvedBookingId, setResolvedBookingId] = useState<number | null>(null);
  const [resolving, setResolving] = useState(false);
  const [description, setDescription] = useState('Restaurant charge');
  const [amount, setAmount] = useState('25.00');
  const [externalRef, setExternalRef] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const enabled = resolvedBookingId != null && resolvedBookingId > 0;
  const { data: folio, isLoading, isFetching, error: folioError } = useQuery({
    queryKey: ['folio', resolvedBookingId],
    queryFn: () => api.getAdminFolio(token, resolvedBookingId!),
    enabled,
  });

  const charges = folio?.lineItems.filter((l) => l.type !== 'PAYMENT') ?? [];
  const payments = folio?.lineItems.filter((l) => l.type === 'PAYMENT') ?? [];
  const searching = resolving || (enabled && (isLoading || isFetching));

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');
    const trimmed = query.trim();
    if (!trimmed) {
      setResolvedBookingId(null);
      setError('Enter a booking ID or confirmation code.');
      return;
    }

    const numericId = parseNumericBookingId(trimmed);
    if (numericId != null) {
      setResolvedBookingId(numericId);
      return;
    }

    setResolving(true);
    setResolvedBookingId(null);
    try {
      const booking = await api.getBookingByCode(token, trimmed);
      setResolvedBookingId(booking.id);
    } catch (err) {
      setError(getErrorMessage(err, "We couldn't find that booking."));
    } finally {
      setResolving(false);
    }
  }

  async function postCharge(e: React.FormEvent) {
    e.preventDefault();
    if (resolvedBookingId == null) return;
    setError('');
    setSuccess('');
    try {
      await api.postFolioCharge(token, resolvedBookingId, {
        type: 'POS_FB',
        description,
        amount: Number(amount),
        externalRef: externalRef || undefined,
      });
      setExternalRef('');
      queryClient.invalidateQueries({ queryKey: ['folio', resolvedBookingId] });
      setSuccess('F&B charge posted.');
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to post charge. Please try again.'));
    }
  }

  return (
    <div className="space-y-4">
      <form onSubmit={handleSearch} className="ops-panel flex flex-wrap items-end gap-3">
        <div>
          <label className="label-caps">Booking ID or confirmation</label>
          <input
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setError('');
              setSuccess('');
            }}
            className="input-field w-52"
            placeholder="e.g. 8 or BK-LEGACY-8"
            autoComplete="off"
          />
        </div>
        <button type="submit" className="btn-secondary" disabled={searching}>
          {searching ? 'Searching…' : 'Search'}
        </button>
      </form>

      {error && <InlineAlert tone="error">{error}</InlineAlert>}

      {!enabled && !error && !resolving && (
        <EmptyState
          title="Search a booking"
          description="Enter a booking ID or confirmation code, then search to open the guest ledger."
        />
      )}

      {enabled && folioError && (
        <InlineAlert tone="error">{getErrorMessage(folioError)}</InlineAlert>
      )}

      {enabled && !isLoading && !folio && !folioError && (
        <EmptyState title="Folio not found" description="Check the booking ID and try again." />
      )}

      {folio && (
        <div className="space-y-4 animate-tab">
          <div className="grid gap-3 sm:grid-cols-3">
            <div className="ops-panel">
              <p className="text-[11px] font-semibold uppercase tracking-wider text-ink/45">Status</p>
              <div className="mt-2">
                <StatusPill label={folio.status} tone={folio.status === 'OPEN' ? 'open' : 'resolved'} />
              </div>
            </div>
            <div className="ops-panel">
              <p className="text-[11px] font-semibold uppercase tracking-wider text-ink/45">Charges</p>
              <p className="mt-1 font-display text-2xl font-bold text-ink">
                ${Number(folio.chargesTotal).toFixed(2)}
              </p>
            </div>
            <div className="ops-panel">
              <p className="text-[11px] font-semibold uppercase tracking-wider text-ink/45">
                Open balance
              </p>
              <p
                className={`mt-1 font-display text-2xl font-bold ${
                  Number(folio.balance) > 0
                    ? 'text-[color:var(--color-status-unpaid)]'
                    : 'text-moss'
                }`}
              >
                ${Number(folio.balance).toFixed(2)}
              </p>
              <p className="mt-1 text-xs text-ink/45">
                Payments ${Number(folio.paymentsTotal).toFixed(2)}
              </p>
            </div>
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <LedgerColumn title="Charges" lines={charges} empty="No charges posted." />
            <LedgerColumn title="Payments" lines={payments} empty="No payments recorded." />
          </div>

          <form onSubmit={postCharge} className="ops-panel space-y-3">
            <div>
              <h3 className="font-display text-lg font-semibold text-ink">Post F&amp;B charge</h3>
              <p className="mt-1 text-sm text-ink/55">Add a POS charge to this folio.</p>
            </div>
            <div className="grid gap-3 sm:grid-cols-3">
              <div className="sm:col-span-1">
                <label className="label-caps">Description</label>
                <input
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="input-field"
                  required
                />
              </div>
              <div>
                <label className="label-caps">Amount</label>
                <input
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="input-field"
                  required
                />
              </div>
              <div>
                <label className="label-caps">POS ref</label>
                <input
                  value={externalRef}
                  onChange={(e) => setExternalRef(e.target.value)}
                  className="input-field"
                  placeholder="Optional"
                />
              </div>
            </div>
            {success && <InlineAlert tone="success">{success}</InlineAlert>}
            <button type="submit" className="btn-primary">
              Post charge
            </button>
          </form>
        </div>
      )}
    </div>
  );
}

function LedgerColumn({
  title,
  lines,
  empty,
}: {
  title: string;
  lines: { id: number; type: string; description: string; amount: number; postedAt: string }[];
  empty: string;
}) {
  return (
    <div className="ops-panel !p-0 overflow-hidden">
      <div className="border-b border-sand/70 px-4 py-3">
        <h3 className="font-display text-base font-semibold text-ink">{title}</h3>
      </div>
      {lines.length === 0 ? (
        <p className="px-4 py-6 text-sm text-ink/50">{empty}</p>
      ) : (
        <ul className="divide-y divide-sand/60">
          {lines.map((line) => (
            <li key={line.id} className="flex items-start justify-between gap-3 px-4 py-3 text-sm">
              <div className="min-w-0">
                <p className="font-medium text-ink">{line.description}</p>
                <p className="mt-0.5 text-xs text-ink/45">
                  {line.type} · {new Date(line.postedAt).toLocaleString()}
                </p>
              </div>
              <span className="shrink-0 font-semibold tabular-nums text-forest">
                ${Number(line.amount).toFixed(2)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
