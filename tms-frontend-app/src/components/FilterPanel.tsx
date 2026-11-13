import type { ReactNode } from 'react';

export interface FilterChipOption {
  id: string;
  label: string;
}

export interface FilterChipsProps {
  label: string;
  options: readonly FilterChipOption[];
  activeIds: readonly string[];
  onToggle: (id: string) => void;
}

/** Chip Filters. States: filter_chip_default, filter_chip_active. */
export function FilterChips({ label, options, activeIds, onToggle }: FilterChipsProps) {
  return (
    <div className="tms-filter-chips" role="group" aria-label={label}>
      {options.map((option) => (
        <button
          key={option.id}
          type="button"
          className="tms-filter-chip"
          aria-pressed={activeIds.includes(option.id)}
          onClick={() => onToggle(option.id)}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}

export interface FilterPanelProps {
  label: string;
  children: ReactNode;
}

/** Filter Panel: container for filter controls placed above tables and dashboards. */
export function FilterPanel({ label, children }: FilterPanelProps) {
  return (
    <section className="tms-filter-panel tms-card" aria-label={label}>
      {children}
    </section>
  );
}
