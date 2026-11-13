import { useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router';
import {
  GlobalErrorBanner,
  PrimaryButton,
  SecondaryButton,
  StatusChip,
  TextInput,
} from '../../components';
import { ROUTES } from '../../routes/routes';
import { ScreenHeader } from '../common/ScreenHeader';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * SCR-008 Admin – User Creation & Edit (AUTH-01, F-10).
 * Employee ID is read-only in edit mode.
 * TODO(SCR-008): optional fields are [TBD]; uniqueness validation across active/inactive accounts
 * and persistence require the admin API (future work).
 */
export function UserFormScreen() {
  const { userId } = useParams();
  const isEdit = userId !== undefined;
  const navigate = useNavigate();
  const [employeeId, setEmployeeId] = useState(userId ?? '');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const [notice, setNotice] = useState<string | null>(null);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors = {
      employeeId: employeeId.trim() ? undefined : 'Employee ID is required.',
      name: name.trim() ? undefined : 'Name is required.',
      email: EMAIL_PATTERN.test(email.trim()) ? undefined : 'Enter a valid email address.',
    };
    setErrors(nextErrors);
    if (Object.values(nextErrors).some(Boolean)) return;
    setNotice('Saving is not wired yet (F-10). No data was persisted.');
  };

  return (
    <section className="tms-stack">
      <ScreenHeader
        route={ROUTES.adminUserCreate}
        actions={isEdit && <StatusChip state="confirmed" label="Active" />}
      />
      {notice && <GlobalErrorBanner variant="info" message={notice} />}
      <form className="tms-card tms-stack" onSubmit={handleSubmit} noValidate>
        <TextInput
          label="Employee ID"
          required
          value={employeeId}
          disabled={isEdit}
          helperText={isEdit ? 'Employee ID cannot be changed after creation.' : undefined}
          onChange={(event) => setEmployeeId(event.target.value)}
          error={errors.employeeId}
        />
        <TextInput
          label="Name"
          required
          value={name}
          onChange={(event) => setName(event.target.value)}
          error={errors.name}
        />
        <TextInput
          label="Email"
          type="email"
          required
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={errors.email}
        />
        <div className="tms-row">
          <PrimaryButton type="submit">Save</PrimaryButton>
          <SecondaryButton onClick={() => navigate(ROUTES.adminUserList.path)}>
            Cancel
          </SecondaryButton>
        </div>
      </form>
    </section>
  );
}
