import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router';
import {
  EmptyState,
  GlobalErrorBanner,
  Modal,
  PrimaryButton,
  SecondaryButton,
  Table,
  type TableColumn,
} from '../../components';
import {
  createCustomer,
  deleteCustomer,
  errorMessage,
  listCustomers,
  type Customer,
  type CustomerInput,
} from '../../api/customerApi';
import { ROUTES, customerDetailPath } from '../../routes/routes';
import { ScreenHeader } from '../common/ScreenHeader';
import { CustomerForm } from './CustomerForm';

type Notice = { variant: 'error' | 'warning' | 'info'; message: string };

/**
 * SCR-016 Customer List (US-001): lists customers (FR-US-001-002), creates customers
 * (FR-US-001-001) and removes them (FR-US-001-005).
 * GAP(FR-US-001-002): pagination/filtering are not specified; every returned record is rendered.
 */
export function CustomerListScreen() {
  const [customers, setCustomers] = useState<Customer[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [notice, setNotice] = useState<Notice | null>(null);
  const [created, setCreated] = useState<Customer | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<Customer | null>(null);

  const load = useCallback(() => {
    setLoadError(null);
    listCustomers()
      .then(setCustomers)
      .catch((error: unknown) => setLoadError(errorMessage(error)));
  }, []);

  useEffect(load, [load]);

  const handleCreate = (values: CustomerInput) => {
    setSaving(true);
    setCreateError(null);
    createCustomer(values)
      .then((customer) => {
        setCreated(customer);
        setNotice(null);
        setCustomers((current) => (current ? [...current, customer] : [customer]));
        setCreateOpen(false);
      })
      .catch((error: unknown) => setCreateError(errorMessage(error)))
      .finally(() => setSaving(false));
  };

  const handleDelete = (customer: Customer) => {
    setPendingDelete(null);
    deleteCustomer(customer.id)
      .then((outcome) => {
        setCustomers((current) => current?.filter((item) => item.id !== customer.id) ?? null);
        if (created?.id === customer.id) setCreated(null);
        setNotice(
          outcome === 'deleted'
            ? { variant: 'info', message: `Customer ${customer.name} was removed.` }
            : {
                variant: 'warning',
                message: `Customer ${customer.name} was not found. It may already have been removed.`,
              },
        );
      })
      .catch((error: unknown) => setNotice({ variant: 'error', message: errorMessage(error) }));
  };

  const columns: TableColumn<Customer>[] = [
    { key: 'id', header: 'Customer ID' },
    { key: 'name', header: 'Name' },
    { key: 'city', header: 'City' },
    {
      key: 'actions',
      header: 'Actions',
      render: (row) => (
        <span className="tms-row">
          <Link to={customerDetailPath(row.id)} aria-label={`View ${row.name}`}>
            View
          </Link>
          <SecondaryButton aria-label={`Remove ${row.name}`} onClick={() => setPendingDelete(row)}>
            Remove
          </SecondaryButton>
        </span>
      ),
    },
  ];

  return (
    <section className="tms-stack">
      <ScreenHeader
        route={ROUTES.customerList}
        actions={
          <PrimaryButton
            onClick={() => {
              setCreateError(null);
              setCreateOpen(true);
            }}
          >
            Add customer
          </PrimaryButton>
        }
      />
      {created && (
        <div className="tms-card tms-stack" role="status" aria-label="Customer created">
          <strong>Customer created</strong>
          <dl className="tms-grid">
            <div>
              <dt className="tms-muted">Customer ID</dt>
              <dd>{created.id}</dd>
            </div>
            <div>
              <dt className="tms-muted">Name</dt>
              <dd>{created.name}</dd>
            </div>
            <div>
              <dt className="tms-muted">City</dt>
              <dd>{created.city}</dd>
            </div>
          </dl>
        </div>
      )}
      {notice && <GlobalErrorBanner variant={notice.variant} message={notice.message} />}
      {loadError && <GlobalErrorBanner variant="error" message={loadError} onRetry={load} />}
      {customers === null && !loadError && <p className="tms-muted">Loading customers…</p>}
      {customers !== null && (
        <Table
          caption="Customers"
          columns={columns}
          rows={customers}
          getRowId={(row) => row.id}
          emptyState={<EmptyState title="No customers yet" />}
        />
      )}
      <Modal
        open={createOpen}
        title="Add customer"
        onClose={() => setCreateOpen(false)}
        footer={
          <>
            <SecondaryButton onClick={() => setCreateOpen(false)}>Cancel</SecondaryButton>
            <PrimaryButton type="submit" form="customer-create-form" disabled={saving}>
              Create customer
            </PrimaryButton>
          </>
        }
      >
        {createError && <GlobalErrorBanner variant="error" message={createError} />}
        <CustomerForm id="customer-create-form" disabled={saving} onSubmit={handleCreate}>
          {null}
        </CustomerForm>
      </Modal>
      <Modal
        open={pendingDelete !== null}
        title="Remove customer?"
        onClose={() => setPendingDelete(null)}
        footer={
          <>
            <SecondaryButton onClick={() => setPendingDelete(null)}>Cancel</SecondaryButton>
            <PrimaryButton onClick={() => pendingDelete && handleDelete(pendingDelete)}>
              Remove
            </PrimaryButton>
          </>
        }
      >
        <p>{pendingDelete && `${pendingDelete.name} (${pendingDelete.city}) will be removed.`}</p>
      </Modal>
    </section>
  );
}
