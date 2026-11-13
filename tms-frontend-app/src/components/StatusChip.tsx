export type StatusChipState = 'confirmed' | 'unconfirmed' | 'pending' | 'error';

export interface StatusChipProps {
  state: StatusChipState;
  /** Always shown: status must not be conveyed by color alone. */
  label: string;
}

/** Status Chip / Tag. States: confirmed, unconfirmed, pending, error. */
export function StatusChip({ state, label }: StatusChipProps) {
  return <span className={`tms-chip tms-chip--${state}`}>{label}</span>;
}
