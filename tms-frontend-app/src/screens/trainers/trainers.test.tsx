import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, type Mock } from 'vitest';
import { TRAINERS_PATH, type Trainer } from '../../api/trainerApi';
import { getTrainers, resetTrainerStore } from '../../state/trainerStore';
import { renderApp } from '../../test/renderApp';

/**
 * US-003 acceptance tests for Create Trainer Profile (/trainers/new).
 * Obligations: d91b94da-057d-4059-9efb-97a7e7a20ddf:test:acceptance:US-003-ac-1..ac-6.
 */

const EXISTING: Trainer = {
  id: 't-0',
  trainerType: 'internal',
  name: 'Existing',
  employeeId: 'EMP-9',
  email: 'taken@example.com',
  location: 'Pune',
  status: 'Inactive',
};
const CREATED: Trainer = {
  id: 't-1',
  trainerType: 'internal',
  name: 'New Trainer',
  employeeId: 'EMP-1',
  location: 'Pune',
  status: 'Active',
  timeZone: 'Asia/Kolkata',
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function mockFetch(created: Trainer = CREATED): Mock {
  const fn = vi.fn(async (_url: RequestInfo | URL, init: RequestInit = {}) =>
    init.method === 'POST' ? json(created, 201) : json([EXISTING]),
  );
  vi.stubGlobal('fetch', fn);
  return fn;
}

const methods = (fn: Mock) => (fn.mock.calls as [string, RequestInit][]).map(([, i]) => i.method);

async function fill(user: ReturnType<typeof userEvent.setup>, name: string, employeeId: string) {
  await user.type(screen.getByLabelText(/^Name/), name);
  await user.type(screen.getByLabelText(/^Employee ID/), employeeId);
  await user.selectOptions(screen.getByLabelText(/^Location/), 'Pune');
}

afterEach(() => {
  vi.unstubAllGlobals();
  resetTrainerStore();
});

describe('[US-003-ac-1] Create Trainer Profile screen at /trainers/new', () => {
  it('[US-003-ac-1] renders an editable form with type, identity, employee ID, email and location', () => {
    renderApp('/trainers/new');
    expect(screen.getByRole('heading', { name: 'Create Trainer Profile' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Internal' })).toBeChecked();
    expect(screen.getByRole('radio', { name: 'External' })).toBeInTheDocument();
    expect(screen.getByLabelText(/^Name/)).toBeEnabled();
    expect(screen.getByLabelText(/^Employee ID/)).toBeRequired();
    expect(screen.getByLabelText(/^Email/)).not.toBeRequired();
    expect(screen.getByLabelText(/^Location/)).toBeEnabled();
  });
});

describe('[US-003-ac-2] client-side validation', () => {
  it('[US-003-ac-2] blocks submit, shows associated inline errors, and clears them on correction', async () => {
    const fetchMock = mockFetch();
    const user = userEvent.setup();
    renderApp('/trainers/new');
    await user.click(screen.getByRole('button', { name: 'Create trainer' }));
    const idInput = screen.getByLabelText(/^Employee ID/);
    expect(screen.getByText('Name is required.')).toBeInTheDocument();
    expect(idInput).toHaveAccessibleDescription(/Employee ID is required/);
    expect(fetchMock).not.toHaveBeenCalled();
    await user.type(idInput, 'EMP-1');
    expect(screen.queryByText(/Employee ID is required/)).not.toBeInTheDocument();
  });

  it('[US-003-ac-2] requires and format-checks email for external trainers', async () => {
    const fetchMock = mockFetch();
    const user = userEvent.setup();
    renderApp('/trainers/new');
    await user.click(screen.getByRole('radio', { name: 'External' }));
    await user.type(screen.getByLabelText(/^Name/), 'Ext');
    await user.selectOptions(screen.getByLabelText(/^Location/), 'Pune');
    await user.click(screen.getByRole('button', { name: 'Create trainer' }));
    expect(screen.getByText('Email is required for external trainers.')).toBeInTheDocument();
    await user.type(screen.getByLabelText(/^Email/), 'not-an-email');
    await user.click(screen.getByRole('button', { name: 'Create trainer' }));
    expect(screen.getByText('Enter a valid email address.')).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

describe('[US-003-ac-3] duplicate check before create', () => {
  it('[US-003-ac-3] shows field-level errors, skips create, stays on /trainers/new, keeps data', async () => {
    const fetchMock = mockFetch();
    const user = userEvent.setup();
    renderApp('/trainers/new');
    await fill(user, 'Dup Person', 'emp-9');
    await user.click(screen.getByRole('button', { name: 'Create trainer' }));
    expect(await screen.findByText(/employee ID already exists/)).toBeInTheDocument();
    expect(methods(fetchMock)).not.toContain('POST');
    expect(screen.getByRole('heading', { name: 'Create Trainer Profile' })).toBeInTheDocument();
    expect(screen.getByLabelText(/^Name/)).toHaveValue('Dup Person');
  });

  it('[US-003-ac-3] checks duplicates first, then creates once without extra confirmation', async () => {
    const fetchMock = mockFetch();
    const user = userEvent.setup();
    renderApp('/trainers/new');
    await fill(user, 'New Trainer', 'EMP-1');
    await user.click(screen.getByRole('button', { name: 'Create trainer' }));
    await waitFor(() => expect(methods(fetchMock)).toEqual(['GET', 'POST']));
    expect(String(fetchMock.mock.calls[0][0])).toContain(TRAINERS_PATH);
  });
});

describe('[US-003-ac-4] Active status updates client state', () => {
  it('[US-003-ac-4] adds the Active trainer to the store and the trainer list', async () => {
    mockFetch();
    const user = userEvent.setup();
    renderApp('/trainers/new');
    await fill(user, 'New Trainer', 'EMP-1');
    await user.click(screen.getByRole('button', { name: 'Create trainer' }));
    await waitFor(() => expect(getTrainers().map((t) => t.id)).toEqual(['t-1']));
  });

  it('[US-003-ac-4] does not add a non-Active trainer; reports safely', async () => {
    mockFetch({ ...CREATED, status: 'Pending' });
    const user = userEvent.setup();
    renderApp('/trainers/new');
    await fill(user, 'New Trainer', 'EMP-1');
    await user.click(screen.getByRole('button', { name: 'Create trainer' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/not Active/);
    expect(getTrainers()).toHaveLength(0);
  });
});

describe('[US-003-ac-5] server-derived time zone', () => {
  it('[US-003-ac-5] stores the derived time zone with the trainer', async () => {
    mockFetch();
    const user = userEvent.setup();
    renderApp('/trainers/new');
    await fill(user, 'New Trainer', 'EMP-1');
    await user.click(screen.getByRole('button', { name: 'Create trainer' }));
    await waitFor(() => expect(getTrainers()[0]?.timeZone).toBe('Asia/Kolkata'));
  });
});

describe('[US-003-ac-6] audit navigation', () => {
  it('[US-003-ac-6] navigates to /trainers/:trainerId/audit for the new trainer', async () => {
    mockFetch();
    const user = userEvent.setup();
    renderApp('/trainers/new');
    await fill(user, 'New Trainer', 'EMP-1');
    await user.click(screen.getByRole('button', { name: 'Create trainer' }));
    expect(await screen.findByRole('heading', { name: 'Trainer Audit' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Create Trainer Profile' })).toBeNull();
  });
});
