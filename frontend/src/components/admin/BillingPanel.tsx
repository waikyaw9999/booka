import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../../lib/api';
import { InlineAlert, StatusPill } from '../ui';
import { useState } from 'react';

interface Props {
  token: string;
  hotelId: number;
}

export function BillingPanel({ token, hotelId }: Props) {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['billing', hotelId],
    queryFn: () => api.getBilling(token),
  });

  const activate = useMutation({
    mutationFn: () => api.activateStarterPlan(token),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['billing', hotelId] });
      queryClient.invalidateQueries({ queryKey: ['hotel', hotelId] });
      setError('');
      setSuccess('Starter plan activated (payment stub).');
    },
    onError: (err) => {
      setSuccess('');
      setError(getErrorMessage(err, 'Unable to activate plan.'));
    },
  });

  return (
    <div className="space-y-4">
      {error && <InlineAlert tone="error">{error}</InlineAlert>}
      {success && <InlineAlert tone="success">{success}</InlineAlert>}
      {isLoading && <p className="text-sm text-ink/50">Loading billing…</p>}
      {data && (
        <div className="surface-elevated space-y-4 p-5">
          <div className="flex flex-wrap items-center gap-3">
            <StatusPill label={data.subscriptionPlan} tone="confirmed" />
            <StatusPill label={data.subscriptionStatus} tone="default" />
          </div>
          <p className="text-sm text-ink/65">{data.billingNote}</p>
          <dl className="grid gap-2 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-ink/45">Trial ends</dt>
              <dd className="font-medium text-ink">
                {data.trialEndsAt ? new Date(data.trialEndsAt).toLocaleString() : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-ink/45">Renews</dt>
              <dd className="font-medium text-ink">
                {data.subscriptionRenewsAt
                  ? new Date(data.subscriptionRenewsAt).toLocaleString()
                  : '—'}
              </dd>
            </div>
          </dl>
          {data.subscriptionStatus !== 'ACTIVE' && (
            <button
              type="button"
              className="btn-primary"
              disabled={activate.isPending}
              onClick={() => activate.mutate()}
            >
              {activate.isPending ? 'Activating…' : 'Activate Starter (stub payment)'}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
