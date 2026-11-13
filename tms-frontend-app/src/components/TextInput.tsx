import { useId, type InputHTMLAttributes } from 'react';
import { describedBy } from './describedBy';
import { FormField } from './FormField';

export interface TextInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  label: string;
  id?: string;
  helperText?: string;
  error?: string;
}

/** Text Input: single-line entry. States: default, hover, focus, error, disabled. */
export function TextInput({ label, id, helperText, error, required, ...props }: TextInputProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  return (
    <FormField id={inputId} label={label} required={required} helperText={helperText} error={error}>
      <input
        id={inputId}
        className="tms-field__control"
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(inputId, error, helperText)}
        {...props}
      />
    </FormField>
  );
}
