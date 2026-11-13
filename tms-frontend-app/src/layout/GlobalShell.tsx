import { NavLink, Outlet } from 'react-router';
import { config } from '../config/env';
import {
  PRIMARY_NAV,
  PRODUCT_NAME,
  SECONDARY_NAV,
  pathFor,
  type NavItem,
} from '../navigation/navItems';
import { ROUTES } from '../routes/routes';
import './GlobalShell.css';

function NavEntry({ item, child = false }: { item: NavItem; child?: boolean }) {
  const className = ({ isActive }: { isActive: boolean }) =>
    ['tms-shell__nav-link', child ? 'tms-shell__nav-link--child' : '', isActive ? 'active' : '']
      .filter(Boolean)
      .join(' ');
  return (
    <li>
      <NavLink to={pathFor(item.route)} className={className} end data-icon={item.icon}>
        {item.label}
      </NavLink>
      {item.children && (
        <ul>
          {item.children.map((childItem) => (
            <NavEntry key={childItem.label} item={childItem} child />
          ))}
        </ul>
      )}
    </li>
  );
}

/**
 * SCR-003 Global Shell & Role-Based Navigation Layout (NAV-01).
 * TODO(NAV-01): filter PRIMARY_NAV by the signed-in user's rights once RBAC is defined (SCR-009).
 * TODO(SCR-003): breadcrumbs / context-preserving back navigation pattern is [TBD].
 * TODO(SCR-003): user profile/access menu details are [TBD]; links only for now.
 */
export function GlobalShell() {
  return (
    <div className="tms-shell">
      <header className="tms-shell__header">
        <div>
          <NavLink to={ROUTES.coordinatorDashboard.path} className="tms-shell__brand">
            {PRODUCT_NAME}
          </NavLink>
          {config.appEnv && <span className="tms-shell__env">{config.appEnv}</span>}
        </div>
        <nav className="tms-shell__user" aria-label="User menu">
          {SECONDARY_NAV.map((item) => (
            <NavLink key={item.label} to={pathFor(item.route)}>
              {item.label}
            </NavLink>
          ))}
        </nav>
      </header>
      <nav className="tms-shell__nav" aria-label="Primary navigation">
        <ul>
          {PRIMARY_NAV.map((item) => (
            <NavEntry key={item.label} item={item} />
          ))}
        </ul>
        <hr className="tms-shell__divider" />
        <ul>
          {SECONDARY_NAV.map((item) => (
            <NavEntry key={item.label} item={item} />
          ))}
        </ul>
      </nav>
      <main className="tms-shell__main">
        <Outlet />
      </main>
    </div>
  );
}
