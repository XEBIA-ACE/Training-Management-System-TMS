import { useId } from 'react';

export interface RadioOption {
  value: string;
  label: string;
  disabled?: boolean;
}

export interface RadioGroupProps {
  label: string;
  options: readonly RadioOption[];
  value: string;
  onChange: (value: string) => void;
  name?: string;
  disabled?: boolean;
}

/** Radio Group: mutually exclusive visible options. States: default, selected, focus, disabled. */
export function RadioGroup({ label, options, value, onChange, name, disabled }: RadioGroupProps) {
  const generatedName = useId();
  const groupName = name ?? generatedName;
  return (
    <fieldset className="tms-radio-group" role="radiogroup" disabled={disabled}>
      <legend className="tms-field__label">{label}</legend>
      <div className="tms-radio-group__options">
        {options.map((option) => {
          const isDisabled = disabled || option.disabled;
          return (
            <label
              key={option.value}
              className={['tms-radio', isDisabled ? 'tms-radio--disabled' : '']
                .filter(Boolean)
                .join(' ')}
            >
              <input
                type="radio"
                name={groupName}
                value={option.value}
                checked={value === option.value}
                disabled={isDisabled}
                onChange={() => onChange(option.value)}
              />
              {option.label}
            </label>
          );
        })}
      </div>
    </fieldset>
  );
}
