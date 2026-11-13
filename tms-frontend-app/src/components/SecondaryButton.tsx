import type { ButtonHTMLAttributes } from 'react';

export type SecondaryButtonProps = ButtonHTMLAttributes<HTMLButtonElement>;

/** Secondary Button: outlined medium-emphasis action (Cancel, Back, neutral options). */
export function SecondaryButton({ className, type = 'button', ...props }: SecondaryButtonProps) {
  return (
    <button
      type={type}
      className={['tms-btn', 'tms-btn--secondary', className].filter(Boolean).join(' ')}
      {...props}
    />
  );
}
