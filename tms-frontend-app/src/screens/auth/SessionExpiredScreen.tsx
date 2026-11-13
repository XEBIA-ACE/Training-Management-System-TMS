import { GlobalErrorBanner } from '../../components';
import { PRODUCT_NAME } from '../../navigation/navItems';
import { ROUTES } from '../../routes/routes';
import { LoginForm } from './LoginForm';
import './auth.css';

/**
 * SCR-002 Session Expired / Re-authentication (AUTH-02).
 * TODO(SCR-002): "summary of last attempted action" is [TBD]; idle timer wiring uses
 * config.sessionTimeoutMinutes once the timeout policy is defined.
 */
export function SessionExpiredScreen() {
  return (
    <main className="tms-auth">
      <section className="tms-card tms-auth__card tms-stack">
        <header>
          <h1 className="tms-auth__brand">{PRODUCT_NAME}</h1>
          <span className="tms-muted">{ROUTES.sessionExpired.title}</span>
        </header>
        <GlobalErrorBanner
          variant="warning"
          message="Your session has expired. Please sign in again to continue."
        />
        <LoginForm />
      </section>
    </main>
  );
}
