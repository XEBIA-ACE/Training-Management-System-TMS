import { useState, type ReactNode } from 'react';
import {
  EmptyState,
  FilterPanel,
  GlobalErrorBanner,
  RadioGroup,
  StatusChip,
  Table,
  type TableColumn,
} from '../../components';
import { MOCK_WORKSHOPS, type MockWorkshop } from '../../mocks/mockData';
import { STATUS_OPTIONS } from './statusOptions';
import { WorkshopDetailSlideOver } from './WorkshopDetailSlideOver';

const TIMELINE_OPTIONS = [
  { value: 'daily', label: 'Daily' },
  { value: 'weekly', label: 'Weekly' },
  { value: 'monthly', label: 'Monthly' },
  { value: 'quarterly', label: 'Quarterly' },
];

const COLUMNS: TableColumn<MockWorkshop>[] = [
  { key: 'title', header: 'Title' },
  { key: 'dateTime', header: 'Date / time' },
  { key: 'trainer', header: 'Trainer' },
  { key: 'location', header: 'Location' },
  { key: 'participants', header: 'Participants' },
  {
    key: 'status',
    header: 'Status',
    render: (row) => <StatusChip state={row.status} label={row.statusLabel} />,
  },
];

export interface SummaryMetric {
  label: string;
  value: string;
}

/**
 * Shared Workshop Dashboard core reused by SCR-004 (coordinator) and SCR-005 (manager) (F-06).
 * TODO(F-06): timeline switching, filtering, pagination / infinite scroll and loading states are
 * structural only; data fetching from config.apiBaseUrl is future work.
 */
export function WorkshopDashboard({
  filters,
  summary,
}: {
  filters: ReactNode;
  summary: ReactNode;
}) {
  const [timeline, setTimeline] = useState('weekly');
  const [selected, setSelected] = useState<MockWorkshop | null>(null);

  return (
    <div className="tms-stack">
      <GlobalErrorBanner
        variant="info"
        message="Showing placeholder data. Dashboard data integration is pending (F-06)."
      />
      {summary}
      <FilterPanel label="Dashboard filters">
        <RadioGroup
          label="Timeline view"
          options={TIMELINE_OPTIONS}
          value={timeline}
          onChange={setTimeline}
        />
        {filters}
      </FilterPanel>
      <div className="tms-row" aria-label="Status legend">
        <span className="tms-muted">Legend:</span>
        {STATUS_OPTIONS.map((option) => (
          <StatusChip
            key={option.value}
            state={option.value as MockWorkshop['status']}
            label={option.label}
          />
        ))}
      </div>
      <Table
        caption="Workshop sessions"
        columns={COLUMNS}
        rows={MOCK_WORKSHOPS}
        getRowId={(row) => row.id}
        onRowClick={setSelected}
        selectedRowId={selected?.id ?? null}
        emptyState={
          <EmptyState
            title="No workshops in the selected range"
            description="Adjust the filters or timeline view."
          />
        }
      />
      <WorkshopDetailSlideOver workshop={selected} onClose={() => setSelected(null)} />
    </div>
  );
}

export function SummaryTiles({ label, metrics }: { label: string; metrics: SummaryMetric[] }) {
  return (
    <section className="tms-grid" aria-label={label}>
      {metrics.map((metric) => (
        <div key={metric.label} className="tms-card">
          <div className="tms-muted">{metric.label}</div>
          <div
            style={{ fontSize: 'var(--font-size-xl)', fontWeight: 'var(--font-weight-semibold)' }}
          >
            {metric.value}
          </div>
        </div>
      ))}
    </section>
  );
}
