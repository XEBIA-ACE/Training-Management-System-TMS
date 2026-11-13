import { Navigate, Route, Routes } from 'react-router';
import { GlobalShell } from './layout/GlobalShell';
import { PLACEHOLDER_ROUTES, ROUTES } from './routes/routes';
import { UserFormScreen } from './screens/admin/UserFormScreen';
import { UserListScreen } from './screens/admin/UserListScreen';
import { UserRightsScreen } from './screens/admin/UserRightsScreen';
import { LoginScreen } from './screens/auth/LoginScreen';
import { SessionExpiredScreen } from './screens/auth/SessionExpiredScreen';
import { ProfileScreen } from './screens/common/ProfileScreen';
import { ScreenPlaceholder } from './screens/common/ScreenPlaceholder';
import { SignOutScreen } from './screens/common/SignOutScreen';
import { CoordinatorDashboardScreen } from './screens/dashboard/CoordinatorDashboardScreen';
import { ManagerDashboardScreen } from './screens/dashboard/ManagerDashboardScreen';
import { TrainerListScreen } from './screens/masterdata/TrainerListScreen';

/**
 * Route shell (T003). Unauthenticated screens render standalone; everything else renders inside
 * SCR-003 Global Shell.
 * TODO(AUTH-02, NAV-01): add an auth guard that redirects to SCR-001 / SCR-002 and role-based
 * landing once authentication is implemented.
 */
export function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to={ROUTES.login.path} replace />} />
      <Route path={ROUTES.login.path} element={<LoginScreen />} />
      <Route path={ROUTES.sessionExpired.path} element={<SessionExpiredScreen />} />
      <Route element={<GlobalShell />}>
        <Route
          path="/dashboard"
          element={<Navigate to={ROUTES.coordinatorDashboard.path} replace />}
        />
        <Route path={ROUTES.coordinatorDashboard.path} element={<CoordinatorDashboardScreen />} />
        <Route path={ROUTES.managerDashboard.path} element={<ManagerDashboardScreen />} />
        <Route path={ROUTES.adminUserList.path} element={<UserListScreen />} />
        <Route path={ROUTES.adminUserCreate.path} element={<UserFormScreen />} />
        <Route path={ROUTES.adminUserEdit.path} element={<UserFormScreen />} />
        <Route path={ROUTES.adminUserRights.path} element={<UserRightsScreen />} />
        <Route path={ROUTES.trainerList.path} element={<TrainerListScreen />} />
        <Route path={ROUTES.profile.path} element={<ProfileScreen />} />
        <Route path={ROUTES.signOut.path} element={<SignOutScreen />} />
        {PLACEHOLDER_ROUTES.filter((route) => route !== ROUTES.signOut).map((route) => (
          <Route key={route.path} path={route.path} element={<ScreenPlaceholder route={route} />} />
        ))}
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
