import { useId, type SelectHTMLAttributes } from 'react';
import { describedBy } from './describedBy';
import { FormField } from './FormField';

export interface SelectOption {
  value: string;
  label: string;
}

export interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'id'> {
  label: string;
  options: readonly SelectOption[];
  id?: string;
  placeholder?: string;
  helperText?: string;
  error?: string;
}

/**
 * Dropdown / Select: single-select from a constrained list.
 * States: default, hover, focus, disabled, error.
 * TODO: chip-based multi-select variant (references/components.md) is not scaffolded yet.
 */
export function Select({
  label,
  options,
  id,
  placeholder,
  helperText,
  error,
  required,
  ...props
}: SelectProps) {
  const generatedId = useId();
  const selectId = id ?? generatedId;
  return (
    <FormField
      id={selectId}
      label={label}
      required={required}
      helperText={helperText}
      error={error}
    >
      <select
        id={selectId}
        className="tms-field__control"
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(selectId, error, helperText)}
        {...props}
      >
        {placeholder !== undefined && <option value="">{placeholder}</option>}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </FormField>
  );
}
