import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import {
  GlobalErrorBanner,
  PrimaryButton,
  RadioGroup,
  SecondaryButton,
  Select,
  TextInput,
} from '../../components';
import {
  TRAINER_STATUS_ACTIVE,
  checkDuplicateTrainer,
  createTrainer,
  type TrainerInput,
  type TrainerType,
} from '../../api/trainerApi';
import { ROUTES, trainerAuditPath } from '../../routes/routes';
import { addTrainer } from '../../state/trainerStore';
import { ScreenHeader } from '../common/ScreenHeader';
import { validateTrainer, type TrainerFormErrors, type TrainerFormValues } from './trainerValidation';

/**
 * TODO(location): no shared location source exists in the SPA yet; this list is a placeholder.
 * The server derives the time zone from the chosen location (US-003-ac-5).
 */
export const LOCATION_OPTIONS = [
  { value: 'Pune', label: 'Pune' },
  { value: 'Bengaluru', label: 'Bengaluru' },
  { value: 'Delhi', label: 'Delhi' },
];

const TYPE_OPTIONS = [
  { value: 'internal', label: 'Internal' },
  { value: 'external', label: 'External' },
];

const INITIAL: TrainerFormValues = {
  trainerType: 'internal',
  name: '',
  employeeId: '',
  email: '',
  location: '',
};

/**
 * Create Trainer Profile at /trainers/new (US-003, FR-US-003-001..013).
 * Access: sits inside the Global Shell like every other screen.
 * TODO(NAV-01, artifact-SVC-005-c3): RBAC is not implemented in the SPA yet; the backend enforces
 * it. Add the route guard here once the shared pattern exists (FR-US-003-002).
 * Audit (US-003-ac-6): navigation approach — the audit screen owns displaying the create entry.
 * Privacy: employee ID / email / location are never logged.
 */
export function TrainerCreateScreen() {
  const navigate = useNavigate();
  const [values, setValues] = useState<TrainerFormValues>(INITIAL);
  const [errors, setErrors] = useState<TrainerFormErrors>({});
  const [submitting, setSubmitting] = useState(false);
  const [bannerError, setBannerError] = useState<string | null>(null);

  const setField = <K extends keyof TrainerFormValues>(key: K, value: TrainerFormValues[K]) => {
    const next = { ...values, [key]: value };
    setValues(next);
    // Clear a field's error once its corrected value passes validation (FR-US-003-004).
    if (errors[key]) {
      const fresh = validateTrainer(next);
      setErrors((current) => ({ ...current, [key]: fresh[key] }));
    }
  };

  const handleTypeChange = (type: string) => {
    const next = { ...values, trainerType: type as TrainerType };
    setValues(next);
    if (Object.keys(errors).length) {
      const fresh = validateTrainer(next);
      setErrors((current) => ({
        ...current,
        employeeId: fresh.employeeId,
        email: fresh.email,
      }));
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (submitting) return;
    setBannerError(null);
    const found = validateTrainer(values);
    setErrors(found);
    if (Object.keys(found).length) return;

    const input: TrainerInput = {
      trainerType: values.trainerType,
      name: values.name.trim(),
      location: values.location,
      ...(values.trainerType === 'internal' || values.employeeId.trim()
        ? { employeeId: values.employeeId.trim() }
        : {}),
      ...(values.email.trim() ? { email: values.email.trim() } : {}),
    };

    setSubmitting(true);
    try {
      const duplicates = await checkDuplicateTrainer(input);
      if (duplicates.employeeId || duplicates.email) {
        setErrors({
          ...(duplicates.employeeId && {
            employeeId: 'A trainer with this employee ID already exists.',
          }),
          ...(duplicates.email && { email: 'A trainer with this email already exists.' }),
        });
        return;
      }
      const created = await createTrainer(input);
      if (created.status !== TRAINER_STATUS_ACTIVE) {
        // Non-Active status is out of scope for US-003 (FR-US-003-008): report safely, no store update.
        setBannerError(
          'The trainer was created but is not Active, so it was not added to trainer lists.',
        );
        return;
      }
      addTrainer(created); // includes server-derived timeZone (FR-US-003-010)
      navigate(trainerAuditPath(created.id));
    } catch (error) {
      setBannerError(
        error instanceof Error ? error.message : 'The trainer could not be created.',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const isInternal = values.trainerType === 'internal';

  return (
    <section className="tms-stack">
      <ScreenHeader route={ROUTES.trainerCreate} />
      {bannerError && <GlobalErrorBanner variant="error" message={bannerError} />}
      <form className="tms-card tms-stack" onSubmit={handleSubmit} noValidate>
        <RadioGroup
          label="Trainer type"
          name="trainerType"
          options={TYPE_OPTIONS}
          value={values.trainerType}
          onChange={handleTypeChange}
          disabled={submitting}
        />
        <TextInput
          label="Name"
          required
          maxLength={200}
          value={values.name}
          disabled={submitting}
          onChange={(event) => setField('name', event.target.value)}
          error={errors.name}
        />
        <TextInput
          label="Employee ID"
          required={isInternal}
          maxLength={50}
          value={values.employeeId}
          disabled={submitting}
          onChange={(event) => setField('employeeId', event.target.value)}
          error={errors.employeeId}
        />
        <TextInput
          label="Email"
          type="email"
          required={!isInternal}
          maxLength={254}
          value={values.email}
          disabled={submitting}
          onChange={(event) => setField('email', event.target.value)}
          error={errors.email}
        />
        <Select
          label="Location"
          required
          placeholder="Select a location"
          options={LOCATION_OPTIONS}
          value={values.location}
          disabled={submitting}
          onChange={(event) => setField('location', event.target.value)}
          error={errors.location}
          helperText="Time zone is derived from the location."
        />
        <div className="tms-row">
          <PrimaryButton type="submit" disabled={submitting}>
            Create trainer
          </PrimaryButton>
          <SecondaryButton onClick={() => navigate(ROUTES.trainerList.path)} disabled={submitting}>
            Cancel
          </SecondaryButton>
        </div>
      </form>
    </section>
  );
}
