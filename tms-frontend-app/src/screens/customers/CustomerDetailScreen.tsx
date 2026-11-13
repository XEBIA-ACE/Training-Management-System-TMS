import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router';
import {
  EmptyState,
  GlobalErrorBanner,
  Modal,
  PrimaryButton,
  SecondaryButton,
  TextInput,
} from '../../components';
import {
  deleteCustomer,
  errorMessage,
  getCustomer,
  updateCustomer,
  type Customer,
  type CustomerInput,
} from '../../api/customerApi';
import { ROUTES } from '../../routes/routes';
import { ScreenHeader } from '../common/ScreenHeader';
import { CustomerForm } from './CustomerForm';

type DetailState =
  | { kind: 'idle' }
  | { kind: 'loading' }
  | { kind: 'loaded'; customer: Customer }
  | { kind: 'not_found' }
  | { kind: 'removed'; customer: Customer }
  | { kind: 'error'; message: string };

/**
 * SCR-018 Customer Detail (US-001): views a customer by ID (FR-US-001-003), updates name and/or
 * city (FR-US-001-004) and removes the customer (FR-US-001-005). The customer ID is carried in the
 * `id` query parameter of the existing `/customers/detail` route.
 */
export function CustomerDetailScreen() {
  const [searchParams, setSearchParams] = useSearchParams();
  const customerId = searchParams.get('id')?.trim() ?? '';
  const [lookupId, setLookupId] = useState(customerId);
  const [state, setState] = useState<DetailState>({ kind: 'idle' });
  const [notice, setNotice] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const load = useCallback(() => {
    if (!customerId) {
      setState({ kind: 'idle' });
      return;
    }
    setState({ kind: 'loading' });
    getCustomer(customerId)
      .then((customer) => setState(customer ? { kind: 'loaded', customer } : { kind: 'not_found' }))
      .catch((error: unknown) => setState({ kind: 'error', message: errorMessage(error) }));
  }, [customerId]);

  useEffect(() => {
    setNotice(null);
    setActionError(null);
    setLookupId(customerId);
    load();
  }, [customerId, load]);

  const handleLookup = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const id = lookupId.trim();
    if (id) setSearchParams({ id });
  };

  const handleUpdate = (current: Customer, values: CustomerInput) => {
    const update = {
      ...(values.name !== current.name ? { name: values.name } : {}),
      ...(values.city !== current.city ? { city: values.city } : {}),
    };
    if (!('name' in update) && !('city' in update)) {
      setNotice('No changes to save.');
      return;
    }
    setSaving(true);
    setActionError(null);
    updateCustomer(current.id, update)
      .then((customer) => {
        if (customer) {
          setState({ kind: 'loaded', customer });
          setNotice('Customer updated.');
        } else {
          setState({ kind: 'not_found' });
        }
      })
      .catch((error: unknown) => setActionError(errorMessage(error)))
      .finally(() => setSaving(false));
  };

  const handleDelete = (current: Customer) => {
    setConfirmDelete(false);
    setActionError(null);
    deleteCustomer(current.id)
      .then((outcome) =>
        setState(
          outcome === 'deleted' ? { kind: 'removed', customer: current } : { kind: 'not_found' },
        ),
      )
      .catch((error: unknown) => setActionError(errorMessage(error)));
  };

  const backToList = <Link to={ROUTES.customerList.path}>Back to customer list</Link>;

  return (
    <section className="tms-stack">
      <ScreenHeader route={ROUTES.customerDetail} />
      <form className="tms-card tms-row" onSubmit={handleLookup} aria-label="Find customer">
        <TextInput
          label="Customer ID"
          value={lookupId}
          onChange={(event) => setLookupId(event.target.value)}
        />
        <SecondaryButton type="submit">View customer</SecondaryButton>
      </form>
      {state.kind === 'idle' && (
        <div className="tms-card">
          <EmptyState title="Enter a customer ID to view its details" action={backToList} />
        </div>
      )}
      {state.kind === 'loading' && <p className="tms-muted">Loading customer…</p>}
      {state.kind === 'error' && (
        <GlobalErrorBanner variant="error" message={state.message} onRetry={load} />
      )}
      {state.kind === 'not_found' && (
        <div className="tms-card" role="status">
          <EmptyState
            title="Customer not found"
            description={`No customer exists with ID ${customerId}.`}
            action={backToList}
          />
        </div>
      )}
      {state.kind === 'removed' && (
        <div className="tms-card" role="status">
          <EmptyState
            title="Customer removed"
            description={`${state.customer.name} (${state.customer.id}) was removed.`}
            action={backToList}
          />
        </div>
      )}
      {state.kind === 'loaded' && (
        <>
          {notice && <GlobalErrorBanner variant="info" message={notice} />}
          {actionError && <GlobalErrorBanner variant="error" message={actionError} />}
          <div className="tms-card tms-stack" aria-label="Customer details" role="region">
            <h2>{state.customer.name}</h2>
            <dl className="tms-grid">
              <div>
                <dt className="tms-muted">Customer ID</dt>
                <dd>{state.customer.id}</dd>
              </div>
              <div>
                <dt className="tms-muted">Name</dt>
                <dd>{state.customer.name}</dd>
              </div>
              <div>
                <dt className="tms-muted">City</dt>
                <dd>{state.customer.city}</dd>
              </div>
            </dl>
          </div>
          <div className="tms-card tms-stack" aria-label="Edit customer" role="region">
            <h2>Edit customer</h2>
            <CustomerForm
              key={`${state.customer.id}:${state.customer.name}:${state.customer.city}`}
              id="customer-edit-form"
              initial={state.customer}
              disabled={saving}
              onSubmit={(values) => handleUpdate(state.customer, values)}
            >
              <div className="tms-row">
                <PrimaryButton type="submit" disabled={saving}>
                  Save changes
                </PrimaryButton>
                <SecondaryButton onClick={() => setConfirmDelete(true)}>
                  Remove customer
                </SecondaryButton>
              </div>
            </CustomerForm>
          </div>
          <Modal
            open={confirmDelete}
            title="Remove customer?"
            onClose={() => setConfirmDelete(false)}
            footer={
              <>
                <SecondaryButton onClick={() => setConfirmDelete(false)}>Cancel</SecondaryButton>
                <PrimaryButton onClick={() => handleDelete(state.customer)}>Remove</PrimaryButton>
              </>
            }
          >
            <p>{`${state.customer.name} (${state.customer.city}) will be removed.`}</p>
          </Modal>
        </>
      )}
    </section>
  );
}
