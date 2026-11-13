import { useState, type FormEvent, type ReactNode } from 'react';
import { TextInput } from '../../components';
import type { CustomerInput } from '../../api/customerApi';

interface CustomerFormProps {
  id: string;
  initial?: CustomerInput;
  disabled?: boolean;
  onSubmit: (values: CustomerInput) => void;
  children: ReactNode;
}

/** Customer name/city form (US-001 create and update). `children` renders the action buttons. */
export function CustomerForm({ id, initial, disabled, onSubmit, children }: CustomerFormProps) {
  const [name, setName] = useState(initial?.name ?? '');
  const [city, setCity] = useState(initial?.city ?? '');
  const [errors, setErrors] = useState<{ name?: string; city?: string }>({});

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors = {
      name: name.trim() ? undefined : 'Name is required.',
      city: city.trim() ? undefined : 'City is required.',
    };
    setErrors(nextErrors);
    if (nextErrors.name || nextErrors.city) return;
    onSubmit({ name: name.trim(), city: city.trim() });
  };

  return (
    <form id={id} className="tms-stack" onSubmit={handleSubmit} noValidate>
      <TextInput
        label="Name"
        required
        maxLength={200}
        value={name}
        disabled={disabled}
        onChange={(event) => setName(event.target.value)}
        error={errors.name}
      />
      <TextInput
        label="City"
        required
        maxLength={120}
        value={city}
        disabled={disabled}
        onChange={(event) => setCity(event.target.value)}
        error={errors.city}
      />
      {children}
    </form>
  );
}
