import { useState, type ReactNode } from 'react';

interface ConfirmActionProps {
  label: string;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: 'primary' | 'danger';
  onConfirm: () => void | Promise<void>;
  disabled?: boolean;
  children?: ReactNode;
}

export function ConfirmAction({
  label,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  tone = 'primary',
  onConfirm,
  disabled,
  children,
}: ConfirmActionProps) {
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);

  async function handleConfirm() {
    setBusy(true);
    try {
      await onConfirm();
      setOpen(false);
    } finally {
      setBusy(false);
    }
  }

  if (!open) {
    return (
      <button
        type="button"
        disabled={disabled}
        onClick={() => setOpen(true)}
        className={tone === 'danger' ? 'btn-danger text-xs !py-1.5 !px-3' : 'btn-secondary text-xs !py-1.5 !px-3'}
      >
        {label}
      </button>
    );
  }

  return (
    <div className="inline-flex flex-wrap items-center gap-2 rounded-xl border border-sand bg-fog/90 px-2 py-1.5">
      {children && <span className="text-xs text-ink/65">{children}</span>}
      <button
        type="button"
        disabled={busy}
        onClick={handleConfirm}
        className={tone === 'danger' ? 'btn-danger text-xs !py-1 !px-2.5' : 'btn-primary text-xs !py-1 !px-2.5'}
      >
        {busy ? '…' : confirmLabel}
      </button>
      <button
        type="button"
        disabled={busy}
        onClick={() => setOpen(false)}
        className="btn-ghost text-xs !py-1 !px-2"
      >
        {cancelLabel}
      </button>
    </div>
  );
}
