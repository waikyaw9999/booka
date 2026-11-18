export function money(value: number | null | undefined): string {
  const amount = Number(value ?? 0);
  return `$${amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}
