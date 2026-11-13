import type { ReactNode } from 'react';

export interface EmptyStateProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

/** Empty State Pattern: text guidance (plus optional action) when no data is available. */
export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="tms-empty-state">
      <span className="tms-empty-state__title">{title}</span>
      {description && <span>{description}</span>}
      {action}
    </div>
  );
}
