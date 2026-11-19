import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../../lib/api';
import { EmptyState, InlineAlert, StatusPill } from '../ui';

interface Props {
  token: string;
  hotelId: number;
}

export function TeamAccounts({ token, hotelId }: Props) {
  const queryClient = useQueryClient();
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [resetForId, setResetForId] = useState<number | null>(null);
  const [resetPassword, setResetPassword] = useState('');

  const { data: staff, isLoading } = useQuery({
    queryKey: ['staff', hotelId],
    queryFn: () => api.listStaff(token),
  });

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['staff', hotelId] });
  }

  const inviteMutation = useMutation({
    mutationFn: () =>
      api.inviteStaff(token, {
        email: email.trim(),
        fullName: fullName.trim(),
        password,
      }),
    onSuccess: () => {
      invalidate();
      setEmail('');
      setFullName('');
      setPassword('');
      setError('');
      setSuccess('Front-desk account created for this property.');
    },
    onError: (err) => {
      setSuccess('');
      setError(getErrorMessage(err, 'Unable to create team member.'));
    },
  });

  const activeMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      api.setStaffActive(token, id, active),
    onSuccess: (_, vars) => {
      invalidate();
      setError('');
      setSuccess(vars.active ? 'Account reactivated.' : 'Account deactivated.');
    },
    onError: (err) => {
      setSuccess('');
      setError(getErrorMessage(err, 'Unable to update account.'));
    },
  });

  const resetMutation = useMutation({
    mutationFn: ({ id, newPassword }: { id: number; newPassword: string }) =>
      api.resetStaffPassword(token, id, newPassword),
    onSuccess: () => {
      invalidate();
      setResetForId(null);
      setResetPassword('');
      setError('');
      setSuccess('Password updated.');
    },
    onError: (err) => {
      setSuccess('');
      setError(getErrorMessage(err, 'Unable to reset password.'));
    },
  });

  return (
    <div className="space-y-6">
      {error && <InlineAlert tone="error">{error}</InlineAlert>}
      {success && <InlineAlert tone="success">{success}</InlineAlert>}

      <div className="surface-elevated p-5">
        <p className="mb-3 text-[11px] font-semibold uppercase tracking-[0.14em] text-moss">
          Invite front desk
        </p>
        <p className="mb-4 text-sm text-ink/55">
          New STAFF accounts can only see this property’s operations data.
        </p>
        <form
          className="grid gap-3 sm:grid-cols-2"
          onSubmit={(e) => {
            e.preventDefault();
            inviteMutation.mutate();
          }}
        >
          <div>
            <label className="label-caps">Full name</label>
            <input
              required
              className="input-field"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
            />
          </div>
          <div>
            <label className="label-caps">Email</label>
            <input
              type="email"
              required
              className="input-field"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div>
            <label className="label-caps">Temporary password</label>
            <input
              type="password"
              required
              minLength={6}
              className="input-field"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <div className="flex items-end">
            <button type="submit" className="btn-primary" disabled={inviteMutation.isPending}>
              {inviteMutation.isPending ? 'Creating…' : 'Add team member'}
            </button>
          </div>
        </form>
      </div>

      <div className="surface-elevated overflow-hidden">
        <div className="border-b border-sand/80 px-5 py-3">
          <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-moss">
            Property team
          </p>
        </div>
        {isLoading && <p className="p-5 text-sm text-ink/50">Loading…</p>}
        {!isLoading && (!staff || staff.length === 0) && (
          <EmptyState title="No accounts yet" description="Invite a front-desk user to get started." />
        )}
        {staff && staff.length > 0 && (
          <ul className="divide-y divide-sand/70">
            {staff.map((member) => (
              <li key={member.id} className="space-y-3 px-5 py-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="font-medium text-ink">{member.fullName}</p>
                    <p className="text-sm text-ink/55">{member.email}</p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusPill
                      label={member.role === 'ADMIN' ? 'Manager' : 'Front desk'}
                      tone={member.role === 'ADMIN' ? 'confirmed' : 'default'}
                    />
                    <StatusPill
                      label={member.active ? 'Active' : 'Inactive'}
                      tone={member.active ? 'clean' : 'cancelled'}
                    />
                  </div>
                </div>
                {member.role === 'STAFF' && (
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      className="btn-secondary !px-3 !py-1.5 text-xs"
                      disabled={activeMutation.isPending}
                      onClick={() =>
                        activeMutation.mutate({ id: member.id, active: !member.active })
                      }
                    >
                      {member.active ? 'Deactivate' : 'Reactivate'}
                    </button>
                    <button
                      type="button"
                      className="btn-ghost !px-3 !py-1.5 text-xs"
                      onClick={() => {
                        setResetForId(member.id);
                        setResetPassword('');
                      }}
                    >
                      Reset password
                    </button>
                  </div>
                )}
                {resetForId === member.id && (
                  <form
                    className="flex flex-wrap items-end gap-2"
                    onSubmit={(e) => {
                      e.preventDefault();
                      resetMutation.mutate({ id: member.id, newPassword: resetPassword });
                    }}
                  >
                    <div className="min-w-[12rem] flex-1">
                      <label className="label-caps">New password</label>
                      <input
                        type="password"
                        required
                        minLength={6}
                        className="input-field"
                        value={resetPassword}
                        onChange={(e) => setResetPassword(e.target.value)}
                      />
                    </div>
                    <button type="submit" className="btn-primary !py-2 text-xs" disabled={resetMutation.isPending}>
                      Save
                    </button>
                    <button
                      type="button"
                      className="btn-ghost !py-2 text-xs"
                      onClick={() => setResetForId(null)}
                    >
                      Cancel
                    </button>
                  </form>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
