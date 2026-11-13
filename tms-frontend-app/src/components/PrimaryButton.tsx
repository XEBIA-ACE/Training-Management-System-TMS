import type { ButtonHTMLAttributes } from 'react';

export type PrimaryButtonProps = ButtonHTMLAttributes<HTMLButtonElement>;

/** Primary Button: high-emphasis main action. Only one per context. */
export function PrimaryButton({ className, type = 'button', ...props }: PrimaryButtonProps) {
  return (
    <button
      type={type}
      className={['tms-btn', 'tms-btn--primary', className].filter(Boolean).join(' ')}
      {...props}
    />
  );
}
