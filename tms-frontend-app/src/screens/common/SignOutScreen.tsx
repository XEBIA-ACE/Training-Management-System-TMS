import { useNavigate } from 'react-router';
import { Modal, PrimaryButton, SecondaryButton } from '../../components';
import { ROUTES } from '../../routes/routes';

/**
 * Sign Out (navigation.md -> SCR-044) rendered as the "Sign-out Confirmation" Modal / Dialog
 * listed in references/components.md.
 * TODO(SCR-044, AUTH-02): SCR-044 has no exported screen spec; real session termination is
 * future work. Confirming only routes back to SCR-001.
 */
export function SignOutScreen() {
  const navigate = useNavigate();
  const cancel = () => navigate(-1);
  return (
    <Modal
      open
      title="Sign out?"
      onClose={cancel}
      footer={
        <>
          <SecondaryButton onClick={cancel}>Cancel</SecondaryButton>
          <PrimaryButton onClick={() => navigate(ROUTES.login.path, { replace: true })}>
            Sign Out
          </PrimaryButton>
        </>
      }
    >
      <p>You will need to sign in again to continue.</p>
    </Modal>
  );
}
