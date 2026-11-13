import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { GlobalErrorBanner, PrimaryButton, TextInput } from '../../components';
import { ROUTES } from '../../routes/routes';

interface FieldErrors {
  username?: string;
  password?: string;
}

/**
 * Shared credential form used by SCR-001 and its session-timeout variant SCR-002.
 * TODO(AUTH-02): no authentication is performed. Submitting with non-empty fields routes to the
 * coordinator dashboard; role-based landing (SCR-004 vs SCR-005, NAV-01) is future work.
 */
export function LoginForm() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors: FieldErrors = {
      username: username.trim() ? undefined : 'Email or username is required.',
      password: password ? undefined : 'Password is required.',
    };
    setErrors(nextErrors);
    if (nextErrors.username || nextErrors.password) {
      setFormError('Please correct the highlighted fields.');
      return;
    }
    setFormError(null);
    navigate(ROUTES.coordinatorDashboard.path);
  };

  return (
    <form className="tms-stack" onSubmit={handleSubmit} noValidate>
      {formError && <GlobalErrorBanner variant="error" message={formError} />}
      <TextInput
        label="Email / username"
        name="username"
        autoComplete="username"
        required
        value={username}
        onChange={(event) => setUsername(event.target.value)}
        error={errors.username}
      />
      <TextInput
        label="Password"
        name="password"
        type="password"
        autoComplete="current-password"
        required
        value={password}
        onChange={(event) => setPassword(event.target.value)}
        error={errors.password}
      />
      <PrimaryButton type="submit">Login</PrimaryButton>
      {/* TODO(SCR-001): password support mechanism is [TBD]. */}
      <span className="tms-muted">Password help: [TBD]</span>
    </form>
  );
}
