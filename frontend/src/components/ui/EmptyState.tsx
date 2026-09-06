import type { ReactNode } from 'react';

interface EmptyStateProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-sand bg-fog/50 px-6 py-12 text-center">
      <p className="font-display text-lg font-semibold text-ink">{title}</p>
      {description && <p className="mt-2 max-w-sm text-sm text-ink/55">{description}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}
