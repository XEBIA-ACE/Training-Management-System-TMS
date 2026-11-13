import { PRODUCT_NAME } from '../../navigation/navItems';
import { ROUTES } from '../../routes/routes';
import { LoginForm } from './LoginForm';
import './auth.css';

/** SCR-001 Login & Session Management (AUTH-02). */
export function LoginScreen() {
  return (
    <main className="tms-auth">
      <section className="tms-card tms-auth__card tms-stack">
        <header>
          <h1 className="tms-auth__brand">{PRODUCT_NAME}</h1>
          <span className="tms-muted">{ROUTES.login.title}</span>
        </header>
        <LoginForm />
      </section>
    </main>
  );
}
