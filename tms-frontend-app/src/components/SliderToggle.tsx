import { useId } from 'react';

export interface SliderToggleProps {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  disabled?: boolean;
  onLabel?: string;
  offLabel?: string;
}

/** Slider Toggle (Yes/No): binary flag. States: on, off, focus, disabled. */
export function SliderToggle({
  label,
  checked,
  onChange,
  disabled,
  onLabel = 'Yes',
  offLabel = 'No',
}: SliderToggleProps) {
  const labelId = useId();
  return (
    <div className="tms-toggle">
      <button
        type="button"
        role="switch"
        className="tms-toggle__switch"
        aria-checked={checked}
        aria-labelledby={labelId}
        disabled={disabled}
        onClick={() => onChange(!checked)}
      />
      <span className="tms-toggle__state" aria-hidden="true">
        {checked ? onLabel : offLabel}
      </span>
      <span id={labelId}>{label}</span>
    </div>
  );
}
