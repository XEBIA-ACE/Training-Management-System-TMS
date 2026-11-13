import { Link } from 'react-router';
import { Select } from '../../components';
import { ROUTES } from '../../routes/routes';
import { ScreenHeader } from '../common/ScreenHeader';
import { STATUS_OPTIONS } from './statusOptions';
import { SummaryTiles, WorkshopDashboard } from './WorkshopDashboard';

/**
 * SCR-005 Manager Landing – Workshop Dashboard & Summary Analytics (NAV-01, F-06).
 * TODO(SCR-005): role-restricted visibility banner depends on RBAC (SCR-009); not shown yet.
 */
export function ManagerDashboardScreen() {
  return (
    <section>
      <ScreenHeader
        route={ROUTES.managerDashboard}
        actions={
          <nav className="tms-row" aria-label="Key reports">
            <Link to={ROUTES.reportTrainerAllocation.path}>Trainer Allocation View</Link>
            <Link to={ROUTES.reportTrainerIncentive.path}>Incentive</Link>
            <Link to={ROUTES.feedbackAnalytics.path}>Feedback Analytics</Link>
          </nav>
        }
      />
      <WorkshopDashboard
        summary={
          <SummaryTiles
            label="KPI tiles"
            metrics={[
              { label: 'Total workshops', value: '—' },
              { label: 'Confirmed vs unconfirmed', value: '—' },
              { label: 'Next 7 days', value: '—' },
              { label: 'Participants', value: '—' },
            ]}
          />
        }
        filters={
          <>
            <Select label="Date range" options={[]} placeholder="All dates" />
            <Select label="Client" options={[]} placeholder="All clients" />
            <Select label="Trainer" options={[]} placeholder="All trainers" />
            <Select label="Status" options={STATUS_OPTIONS} placeholder="All statuses" />
          </>
        }
      />
    </section>
  );
}
