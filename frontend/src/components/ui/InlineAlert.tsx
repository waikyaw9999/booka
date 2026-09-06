type AlertTone = 'error' | 'success' | 'warning' | 'info';

interface InlineAlertProps {
  tone?: AlertTone;
  children: React.ReactNode;
  className?: string;
}

const TONE: Record<AlertTone, string> = {
  error: 'border-[color:var(--color-danger)]/25 bg-[color:var(--color-danger)]/8 text-[color:var(--color-danger)]',
  success: 'border-moss/25 bg-moss/10 text-moss',
  warning: 'border-gold/35 bg-gold/12 text-[color:var(--color-warning)]',
  info: 'border-lagoon/25 bg-lagoon/10 text-lagoon',
};

export function InlineAlert({ tone = 'info', children, className = '' }: InlineAlertProps) {
  return (
    <div
      role="alert"
      className={`rounded-xl border px-3 py-2.5 text-sm ${TONE[tone]} ${className}`}
    >
      {children}
    </div>
  );
}
