import { useState } from 'react';
import {
  EmptyState,
  FilterChips,
  FilterPanel,
  PrimaryButton,
  StatusChip,
  Table,
  TextInput,
  type TableColumn,
} from '../../components';
import { MOCK_TRAINERS, type MockTrainer } from '../../mocks/mockData';
import { ROUTES } from '../../routes/routes';
import { ScreenHeader } from '../common/ScreenHeader';

const FILTERS = [
  { id: 'active', label: 'Active' },
  { id: 'inactive', label: 'Inactive' },
  { id: 'internal', label: 'Internal' },
  { id: 'external', label: 'External' },
  { id: 'weekend', label: 'Weekend available' },
];

const yesNo = (value: boolean) => (value ? 'Yes' : 'No');

const COLUMNS: TableColumn<MockTrainer>[] = [
  { key: 'name', header: 'Name' },
  { key: 'employeeId', header: 'Employee ID' },
  {
    key: 'type',
    header: 'Internal / External',
    render: (row) => (row.internal ? 'Internal' : 'External'),
  },
  { key: 'locationTimezone', header: 'Location / timezone' },
  { key: 'weekend', header: 'Weekend availability', render: (row) => yesNo(row.weekendAvailable) },
  {
    key: 'incentive',
    header: 'Incentive eligibility',
    render: (row) => yesNo(row.incentiveEligible),
  },
  {
    key: 'status',
    header: 'Status',
    // TODO(SCR-010): mapping of active/inactive onto Status Chip states is not specified.
    render: (row) =>
      row.active ? (
        <StatusChip state="confirmed" label="Active" />
      ) : (
        <StatusChip state="unconfirmed" label="Inactive" />
      ),
  },
];

/**
 * SCR-010 Trainer Management – List (F-01). No delete actions by design.
 * TODO(F-01): trainer creation and detail screens are not part of this scaffold; the add action
 * is disabled and filtering is not applied to data.
 */
export function TrainerListScreen() {
  const [activeFilters, setActiveFilters] = useState<string[]>(['active']);
  const toggle = (id: string) =>
    setActiveFilters((current) =>
      current.includes(id) ? current.filter((item) => item !== id) : [...current, id],
    );
  return (
    <section className="tms-stack">
      <ScreenHeader
        route={ROUTES.trainerList}
        actions={
          <PrimaryButton disabled title="Trainer creation is not scaffolded yet (F-01)">
            Add trainer
          </PrimaryButton>
        }
      />
      <FilterPanel label="Trainer filters">
        <TextInput label="Search trainers" type="search" placeholder="Name or employee ID" />
        <FilterChips
          label="Filters"
          options={FILTERS}
          activeIds={activeFilters}
          onToggle={toggle}
        />
      </FilterPanel>
      <Table
        caption="Trainers"
        columns={COLUMNS}
        rows={MOCK_TRAINERS}
        getRowId={(row) => row.employeeId}
        emptyState={<EmptyState title="No trainers match the current filters" />}
      />
    </section>
  );
}
