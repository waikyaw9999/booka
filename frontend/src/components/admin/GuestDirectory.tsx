import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../../lib/api';
import { money } from '../../lib/format';
import { EmptyState, InlineAlert, StatusPill } from '../ui';
import type { GuestProfile, VipTier } from '../../types';

interface Props {
  token: string;
  initialGuestId?: number | null;
}

const TIERS: VipTier[] = ['NONE', 'SILVER', 'GOLD', 'PLATINUM'];

export function GuestDirectory({ token, initialGuestId }: Props) {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState('');
  const [submitted, setSubmitted] = useState('');
  const [selectedId, setSelectedId] = useState<number | null>(initialGuestId ?? null);
  const [draft, setDraft] = useState({ nationality: '', vipTier: 'NONE' as VipTier, notes: '', blacklisted: false });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const { data: guests, isLoading } = useQuery({
    queryKey: ['guests', submitted],
    queryFn: () => api.searchGuests(token, submitted),
  });

  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: ['guest', selectedId],
    queryFn: () => api.getGuest(token, selectedId!),
    enabled: selectedId != null,
  });

  useEffect(() => {
    if (!profile) return;
    setDraft({
      nationality: profile.nationality ?? '',
      vipTier: profile.vipTier,
      notes: profile.notes ?? '',
      blacklisted: profile.blacklisted,
    });
  }, [profile]);

  async function save(e: React.FormEvent) {
    e.preventDefault();
    if (!profile) return;
    setError('');
    setSuccess('');
    try {
      await api.updateGuest(token, profile.id, draft);
      queryClient.invalidateQueries({ queryKey: ['guest', profile.id] });
      queryClient.invalidateQueries({ queryKey: ['guests'] });
      setSuccess('Guest profile saved.');
    } catch (err) {
      setError(getErrorMessage(err, 'Could not save the guest profile.'));
    }
  }

  return (
    <div className="grid gap-4 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.2fr)]">
      <div className="space-y-3">
        <form
          className="ops-panel flex gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            setSubmitted(query.trim());
          }}
        >
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="input-field"
            placeholder="Name, email, or phone"
          />
          <button type="submit" className="btn-secondary shrink-0">
            Search
          </button>
        </form>
        <div className="ops-panel !p-0 overflow-hidden">
          {isLoading ? (
            <p className="px-4 py-6 text-sm text-ink/45">Loading guests…</p>
          ) : !guests?.length ? (
            <div className="p-4">
              <EmptyState title="No guests" description="Search by name, email, or phone, or clear the search to list the property." />
            </div>
          ) : (
            <ul className="divide-y divide-sand/70">
              {guests.map((guest) => (
                <li key={guest.id}>
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedId(guest.id);
                      setSuccess('');
                      setError('');
                    }}
                    className={`flex w-full items-center justify-between gap-3 px-4 py-3 text-left text-sm transition hover:bg-fog ${
                      selectedId === guest.id ? 'bg-moss/8' : ''
                    }`}
                  >
                    <span className="min-w-0">
                      <span className="block truncate font-medium text-ink">{guest.fullName}</span>
                      <span className="block truncate text-xs text-ink/45">{guest.email}</span>
                    </span>
                    <span className="flex shrink-0 flex-col items-end gap-1">
                      {guest.vipTier !== 'NONE' && <StatusPill label={guest.vipTier} />}
                      {guest.blacklisted && <StatusPill label="Blocked" tone="cancelled" />}
                      <span className="text-xs text-ink/45">{guest.stays} stays</span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <div>
        {selectedId == null && (
          <EmptyState title="Select a guest" description="Stay history, VIP tier, and a block flag live on the profile." />
        )}
        {selectedId != null && profileLoading && <p className="text-sm text-ink/45">Loading profile…</p>}
        {profile && <ProfileCard profile={profile} draft={draft} setDraft={setDraft} onSave={save} error={error} success={success} />}
      </div>
    </div>
  );
}

function ProfileCard({
  profile,
  draft,
  setDraft,
  onSave,
  error,
  success,
}: {
  profile: GuestProfile;
  draft: { nationality: string; vipTier: VipTier; notes: string; blacklisted: boolean };
  setDraft: (next: { nationality: string; vipTier: VipTier; notes: string; blacklisted: boolean }) => void;
  onSave: (e: React.FormEvent) => void;
  error: string;
  success: string;
}) {
  return (
    <div className="space-y-4">
      <div className="ops-panel">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="font-display text-2xl font-bold text-ink">{profile.fullName}</h2>
            <p className="mt-1 text-sm text-ink/55">
              {profile.email}
              {profile.phone ? ` · ${profile.phone}` : ''}
            </p>
          </div>
          <div className="text-right text-sm text-ink/60">
            <p>{profile.stays} stays · {profile.nights} nights</p>
            <p className="font-semibold text-forest">{money(profile.lifetimeRevenue)} lifetime</p>
          </div>
        </div>
        <form onSubmit={onSave} className="mt-5 grid gap-3 sm:grid-cols-2">
          <div>
            <label className="label-caps">Nationality</label>
            <input
              value={draft.nationality}
              onChange={(e) => setDraft({ ...draft, nationality: e.target.value })}
              className="input-field"
              placeholder="e.g. Myanmar"
            />
          </div>
          <div>
            <label className="label-caps">VIP tier</label>
            <select
              value={draft.vipTier}
              onChange={(e) => setDraft({ ...draft, vipTier: e.target.value as VipTier })}
              className="input-field"
            >
              {TIERS.map((tier) => (
                <option key={tier} value={tier}>{tier}</option>
              ))}
            </select>
          </div>
          <div className="sm:col-span-2">
            <label className="label-caps">Notes</label>
            <textarea
              value={draft.notes}
              onChange={(e) => setDraft({ ...draft, notes: e.target.value })}
              className="input-field min-h-20"
              placeholder="Pillow preference, company account, quiet room…"
            />
          </div>
          <label className="flex items-center gap-2 text-sm text-ink/75 sm:col-span-2">
            <input
              type="checkbox"
              checked={draft.blacklisted}
              onChange={(e) => setDraft({ ...draft, blacklisted: e.target.checked })}
            />
            Block new reservations for this guest
          </label>
          {error && <div className="sm:col-span-2"><InlineAlert tone="error">{error}</InlineAlert></div>}
          {success && <div className="sm:col-span-2"><InlineAlert tone="success">{success}</InlineAlert></div>}
          <div>
            <button type="submit" className="btn-primary">Save profile</button>
          </div>
        </form>
      </div>

      <div className="ops-panel !p-0 overflow-hidden">
        <div className="border-b border-sand/70 px-4 py-3">
          <h3 className="font-display text-lg font-semibold text-ink">Stay history</h3>
        </div>
        {profile.history.length === 0 ? (
          <p className="px-4 py-6 text-sm text-ink/50">No stays on file.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Dates</th>
                  <th>Room</th>
                  <th>Value</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {profile.history.map((stay) => (
                  <tr key={stay.bookingId}>
                    <td className="font-mono text-xs">{stay.confirmationCode}</td>
                    <td className="whitespace-nowrap text-xs">{stay.checkInDate} → {stay.checkOutDate}</td>
                    <td className="text-xs">{stay.roomTypeName}</td>
                    <td className="text-xs tabular-nums">{money(stay.totalPrice)}</td>
                    <td><StatusPill label={stay.status} /></td>
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
