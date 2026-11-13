import { useId, type ReactNode } from 'react';
import { SecondaryButton } from './SecondaryButton';
import { useDialogBehavior } from './useDialogBehavior';

export interface SlideOverPanelProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  headerExtra?: ReactNode;
}

/** Slide-over Panel: read-oriented side detail view preserving list context. State: default. */
export function SlideOverPanel({
  open,
  title,
  onClose,
  children,
  headerExtra,
}: SlideOverPanelProps) {
  const titleId = useId();
  const containerRef = useDialogBehavior(open, onClose);
  if (!open) return null;
  return (
    <div
      className="tms-backdrop tms-slide-over-backdrop"
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      <aside
        ref={containerRef}
        className="tms-slide-over"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
      >
        <header className="tms-slide-over__header">
          <div className="tms-row">
            <h2 id={titleId}>{title}</h2>
            {headerExtra}
          </div>
          <SecondaryButton onClick={onClose} aria-label="Close panel">
            Close
          </SecondaryButton>
        </header>
        <div className="tms-slide-over__body">{children}</div>
      </aside>
    </div>
  );
}
