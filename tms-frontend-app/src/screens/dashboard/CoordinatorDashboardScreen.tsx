import { Select } from '../../components';
import { ROUTES } from '../../routes/routes';
import { ScreenHeader } from '../common/ScreenHeader';
import { STATUS_OPTIONS } from './statusOptions';
import { SummaryTiles, WorkshopDashboard } from './WorkshopDashboard';

/** SCR-004 Coordinator Landing – Workshop Dashboard (NAV-01, F-06). */
export function CoordinatorDashboardScreen() {
  return (
    <section>
      <ScreenHeader route={ROUTES.coordinatorDashboard} />
      <WorkshopDashboard
        summary={
          <SummaryTiles
            label="Summary analytics"
            metrics={[
              { label: 'Total workshops', value: '—' },
              { label: 'Confirmation breakdown', value: '—' },
              { label: 'Next 7 days', value: '—' },
              { label: 'Total participants', value: '—' },
            ]}
          />
        }
        filters={
          <>
            <Select label="Status" options={STATUS_OPTIONS} placeholder="All statuses" />
            <Select label="Date range" options={[]} placeholder="All dates" />
            <Select label="Customer" options={[]} placeholder="All customers" />
            {/* TODO(SCR-004): trainer filter is [TBD]. */}
          </>
        }
      />
    </section>
  );
}
