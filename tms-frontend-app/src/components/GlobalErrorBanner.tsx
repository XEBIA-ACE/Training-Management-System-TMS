import { SecondaryButton } from './SecondaryButton';

export type GlobalErrorBannerVariant = 'error' | 'warning' | 'info';

export interface GlobalErrorBannerProps {
  variant: GlobalErrorBannerVariant;
  message: string;
  onRetry?: () => void;
}

/** Global Error Banner: system-level partial failure / degraded state. States: error, warning, info. */
export function GlobalErrorBanner({ variant, message, onRetry }: GlobalErrorBannerProps) {
  return (
    <div
      className={`tms-banner tms-banner--${variant}`}
      role={variant === 'error' ? 'alert' : 'status'}
    >
      <span>{message}</span>
      {onRetry && <SecondaryButton onClick={onRetry}>Retry</SecondaryButton>}
    </div>
  );
}
