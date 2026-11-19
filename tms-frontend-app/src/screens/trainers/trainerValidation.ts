import type { TrainerType } from '../../api/trainerApi';

export interface TrainerFormValues {
  trainerType: TrainerType;
  name: string;
  employeeId: string;
  email: string;
  location: string;
}

export type TrainerFormErrors = Partial<Record<keyof TrainerFormValues, string>>;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const EMPLOYEE_ID_PATTERN = /^[A-Za-z0-9._-]+$/;

/** Client-side rules (US-003-ac-2): employee ID required for internal, email for external. */
export function validateTrainer(values: TrainerFormValues): TrainerFormErrors {
  const errors: TrainerFormErrors = {};
  const email = values.email.trim();
  const employeeId = values.employeeId.trim();
  if (!values.name.trim()) errors.name = 'Name is required.';
  if (!values.location) errors.location = 'Location is required.';
  if (values.trainerType === 'internal') {
    if (!employeeId) errors.employeeId = 'Employee ID is required for internal trainers.';
  }
  if (employeeId && !EMPLOYEE_ID_PATTERN.test(employeeId)) {
    errors.employeeId = 'Employee ID may contain only letters, digits, ".", "_" and "-".';
  }
  if (values.trainerType === 'external' && !email) {
    errors.email = 'Email is required for external trainers.';
  } else if (email && !EMAIL_PATTERN.test(email)) {
    errors.email = 'Enter a valid email address.';
  }
  return errors;
}
