import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
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
import { MOCK_USERS, type MockUser } from '../../mocks/mockData';
import { ROUTES, adminUserEditPath } from '../../routes/routes';
import { ScreenHeader } from '../common/ScreenHeader';

const STATUS_FILTERS = [
  { id: 'active', label: 'Active' },
  { id: 'inactive', label: 'Inactive' },
  { id: 'all', label: 'All' },
];

// TODO(SCR-007): "last login" column is [TBD]; sorting/search behaviour is future work (F-10).
const COLUMNS: TableColumn<MockUser>[] = [
  { key: 'employeeId', header: 'Employee ID' },
  { key: 'name', header: 'Name' },
  { key: 'email', header: 'Email' },
  { key: 'roles', header: 'Role(s)' },
  {
    key: 'status',
    header: 'Status',
    // TODO(SCR-007): mapping of active/inactive onto Status Chip states is not specified.
    render: (row) =>
      row.active ? (
        <StatusChip state="confirmed" label="Active" />
      ) : (
        <StatusChip state="unconfirmed" label="Inactive" />
      ),
  },
  {
    key: 'rights',
    header: 'Rights',
    render: (row) => (row.hasRights ? '—' : <StatusChip state="error" label="Missing rights" />),
  },
  {
    key: 'actions',
    header: 'Actions',
    render: (row) => (
      <span className="tms-row">
        <Link to={adminUserEditPath(row.employeeId)}>Edit</Link>
        <Link to={`${ROUTES.adminUserRights.path}?user=${encodeURIComponent(row.employeeId)}`}>
          Rights
        </Link>
      </span>
    ),
  },
];

/** SCR-007 Admin – User List (AUTH-01, F-10). No delete action by design. */
export function UserListScreen() {
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState('active');
  return (
    <section className="tms-stack">
      <ScreenHeader
        route={ROUTES.adminUserList}
        actions={
          <PrimaryButton onClick={() => navigate(ROUTES.adminUserCreate.path)}>
            Create user
          </PrimaryButton>
        }
      />
      <FilterPanel label="User filters">
        <TextInput label="Search users" type="search" placeholder="Employee ID, name or email" />
        <FilterChips
          label="Status"
          options={STATUS_FILTERS}
          activeIds={[statusFilter]}
          onToggle={setStatusFilter}
        />
      </FilterPanel>
      <Table
        caption="Users"
        columns={COLUMNS}
        rows={MOCK_USERS}
        getRowId={(row) => row.employeeId}
        emptyState={<EmptyState title="No users match the current filters" />}
      />
    </section>
  );
}
