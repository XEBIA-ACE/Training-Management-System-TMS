import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PRIMARY_NAV, SECONDARY_NAV } from './navigation/navItems';
import { ROUTES } from './routes/routes';
import { renderApp } from './test/renderApp';

/** AC-1 / T004: the scaffold renders its root route and route shell without runtime errors. */
describe('AC-1 frontend-app scaffold', () => {
  it('renders the root route (redirects to SCR-001 Login)', () => {
    renderApp('/');
    expect(screen.getByRole('heading', { name: 'ACE_For TMS' })).toBeInTheDocument();
    expect(screen.getByLabelText(/Email \/ username/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Login' })).toBeInTheDocument();
  });

  it('renders the SCR-003 Global Shell with every primary navigation item', () => {
    renderApp('/dashboard');
    const nav = screen.getByRole('navigation', { name: 'Primary navigation' });
    for (const item of [...PRIMARY_NAV, ...SECONDARY_NAV]) {
      expect(within(nav).getAllByRole('link', { name: item.label }).length).toBeGreaterThan(0);
    }
    expect(
      screen.getByRole('heading', { name: ROUTES.coordinatorDashboard.title }),
    ).toBeInTheDocument();
  });

  it.each(Object.entries(ROUTES))('renders route %s without errors', (_key, route) => {
    renderApp(route.path.replace(':userId', 'EMP-0001'));
    if (route.screenId === 'SCR-044') {
      expect(screen.getByRole('dialog', { name: 'Sign out?' })).toBeInTheDocument();
    } else {
      expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument();
      expect(
        screen.getAllByText(
          route.screenId === 'SCR-001' || route.screenId === 'SCR-002'
            ? route.title
            : route.screenId,
        ).length,
      ).toBeGreaterThan(0);
    }
  });

  it('navigates from SCR-001 to the coordinator dashboard on submit', async () => {
    const user = userEvent.setup();
    renderApp(ROUTES.login.path);
    await user.click(screen.getByRole('button', { name: 'Login' }));
    expect(screen.getByText('Email or username is required.')).toBeInTheDocument();
    await user.type(screen.getByLabelText(/Email \/ username/), 'someone');
    await user.type(screen.getByLabelText(/Password/), 'placeholder');
    await user.click(screen.getByRole('button', { name: 'Login' }));
    expect(
      screen.getByRole('heading', { name: ROUTES.coordinatorDashboard.title }),
    ).toBeInTheDocument();
  });

  it('opens the SCR-006 slide-over from a dashboard row and closes it with Escape', async () => {
    const user = userEvent.setup();
    renderApp(ROUTES.coordinatorDashboard.path);
    await user.click(screen.getByText('Placeholder Workshop A'));
    const dialog = screen.getByRole('dialog', { name: 'Placeholder Workshop A' });
    expect(within(dialog).getByText('SCR-006')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
