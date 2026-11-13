import { useId, type ReactNode } from 'react';
import { useDialogBehavior } from './useDialogBehavior';

export interface ModalProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
}

/** Modal / Dialog: blocking confirmation/form overlay. States: default, backdrop. */
export function Modal({ open, title, onClose, children, footer }: ModalProps) {
  const titleId = useId();
  const containerRef = useDialogBehavior(open, onClose);
  if (!open) return null;
  return (
    <div
      className="tms-backdrop"
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      <div
        ref={containerRef}
        className="tms-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
      >
        <h2 id={titleId}>{title}</h2>
        <div>{children}</div>
        {footer && <div className="tms-modal__footer">{footer}</div>}
      </div>
    </div>
  );
}
