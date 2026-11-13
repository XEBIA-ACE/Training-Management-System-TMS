import type { ReactNode } from 'react';

interface FormFieldProps {
  id: string;
  label: string;
  required?: boolean;
  helperText?: string;
  error?: string;
  children: ReactNode;
}

/** Internal label/helper/error wrapper shared by Text Input and Dropdown / Select. */
export function FormField({ id, label, required, helperText, error, children }: FormFieldProps) {
  return (
    <div className={['tms-field', error ? 'tms-field--error' : ''].filter(Boolean).join(' ')}>
      <label className="tms-field__label" htmlFor={id}>
        {label}
        {required && (
          <span className="tms-field__required" aria-hidden="true">
            {' '}
            *
          </span>
        )}
      </label>
      {children}
      {error ? (
        <span id={`${id}-error`} className="tms-field__error" role="alert">
          {error}
        </span>
      ) : (
        helperText && (
          <span id={`${id}-helper`} className="tms-field__helper">
            {helperText}
          </span>
        )
      )}
    </div>
  );
}
