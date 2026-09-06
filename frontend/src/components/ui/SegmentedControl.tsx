interface SegmentOption<T extends string> {
  id: T;
  label: string;
}

interface SegmentedControlProps<T extends string> {
  options: SegmentOption<T>[];
  value: T;
  onChange: (value: T) => void;
  className?: string;
}

export function SegmentedControl<T extends string>({
  options,
  value,
  onChange,
  className = '',
}: SegmentedControlProps<T>) {
  return (
    <div
      className={`inline-flex flex-wrap gap-1 rounded-xl border border-sand bg-fog/80 p-1 ${className}`}
      role="tablist"
    >
      {options.map((opt) => {
        const active = opt.id === value;
        return (
          <button
            key={opt.id}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onChange(opt.id)}
            className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
              active
                ? 'bg-forest text-white shadow-sm'
                : 'text-ink/60 hover:bg-white/80 hover:text-forest'
            }`}
          >
            {opt.label}
          </button>
        );
      })}
    </div>
  );
}
