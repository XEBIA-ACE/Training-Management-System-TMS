import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, type Mock } from 'vitest';
import { CUSTOMERS_PATH, CUSTOMER_PAGE_SIZE, type Customer } from '../../api/customerApi';
import { ROUTES, customerDetailPath } from '../../routes/routes';
import { renderApp } from '../../test/renderApp';

/**
 * US-001 acceptance tests (FR-US-001-AC1..FR-US-001-AC5) for the customer screens SCR-016 / SCR-018.
 * Each test asserts the HTTP interaction (verb, path, status handling) and the resulting UI.
 * Test obligations: test:acceptance:US-001-ac-1..ac-5.
 */

const ACME: Customer = {
  id: '11111111-1111-1111-1111-111111111111',
  name: 'Acme Corp',
  city: 'Pune',
};
const BETA: Customer = {
  id: '22222222-2222-2222-2222-222222222222',
  name: 'Beta Ltd',
  city: 'Delhi',
};

type Handler = (url: string, init: RequestInit) => Response;

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function page(content: Customer[], number = 0, totalPages = 1): unknown {
  return {
    content,
    page: number,
    size: CUSTOMER_PAGE_SIZE,
    totalElements: content.length,
    totalPages,
  };
}

const LIST_URL = `${CUSTOMERS_PATH}?page=0&size=${CUSTOMER_PAGE_SIZE}`;

function mockFetch(handler: Handler): Mock {
  const fn = vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) =>
    handler(String(input), init),
  );
  vi.stubGlobal('fetch', fn);
  return fn;
}

function calls(fn: Mock): { method: string; url: string; body?: unknown }[] {
  return (fn.mock.calls as [string, RequestInit][]).map(([url, init]) => ({
    method: init.method ?? 'GET',
    url: String(url),
    body: typeof init.body === 'string' ? JSON.parse(init.body) : undefined,
  }));
}

afterEach(() => vi.unstubAllGlobals());

describe('[US-001-ac-1] create customer via POST /customers', () => {
  it('[US-001-ac-1] sends POST and shows the created customer ID, name and city on HTTP 201', async () => {
    const created: Customer = {
      id: '33333333-3333-3333-3333-333333333333',
      name: 'Gamma',
      city: 'Goa',
    };
    const fetchMock = mockFetch((_url, init) =>
      init.method === 'POST' ? json(created, 201) : json(page([ACME]), 200),
    );
    const user = userEvent.setup();
    renderApp(ROUTES.customerList.path);
    await screen.findByText('Acme Corp');

    await user.click(screen.getByRole('button', { name: 'Add customer' }));
    const dialog = screen.getByRole('dialog', { name: 'Add customer' });
    await user.type(within(dialog).getByLabelText(/Name/), 'Gamma');
    await user.type(within(dialog).getByLabelText(/City/), 'Goa');
    await user.click(within(dialog).getByRole('button', { name: 'Create customer' }));

    const confirmation = await screen.findByRole('status', { name: 'Customer created' });
    expect(within(confirmation).getByText(created.id)).toBeInTheDocument();
    expect(within(confirmation).getByText('Gamma')).toBeInTheDocument();
    expect(within(confirmation).getByText('Goa')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(calls(fetchMock)).toContainEqual({
      method: 'POST',
      url: CUSTOMERS_PATH,
      body: { name: 'Gamma', city: 'Goa' },
    });
  });

  it('[US-001-ac-1] keeps the form open with an error when the response is not 201', async () => {
    mockFetch((_url, init) =>
      init.method === 'POST'
        ? json({ message: 'Request validation failed.' }, 400)
        : json(page([]), 200),
    );
    const user = userEvent.setup();
    renderApp(ROUTES.customerList.path);
    await screen.findByText('No customers yet');
    await user.click(screen.getByRole('button', { name: 'Add customer' }));
    const dialog = screen.getByRole('dialog', { name: 'Add customer' });
    await user.type(within(dialog).getByLabelText(/Name/), 'Gamma');
    await user.type(within(dialog).getByLabelText(/City/), 'Goa');
    await user.click(within(dialog).getByRole('button', { name: 'Create customer' }));

    expect(await within(dialog).findByText('Request validation failed.')).toBeInTheDocument();
    expect(screen.queryByRole('status', { name: 'Customer created' })).not.toBeInTheDocument();
  });
});

describe('[US-001-ac-2] list customers via GET /customers', () => {
  it('[US-001-ac-2] sends GET and renders every customer returned with HTTP 200', async () => {
    const fetchMock = mockFetch(() => json(page([ACME, BETA]), 200));
    renderApp(ROUTES.customerList.path);

    const table = await screen.findByRole('table', { name: 'Customers' });
    for (const customer of [ACME, BETA]) {
      const row = within(table).getByText(customer.id).closest('tr') as HTMLElement;
      expect(within(row).getByText(customer.name)).toBeInTheDocument();
      expect(within(row).getByText(customer.city)).toBeInTheDocument();
    }
    expect(within(table).getAllByRole('row')).toHaveLength(3);
    expect(calls(fetchMock)).toEqual([{ method: 'GET', url: LIST_URL, body: undefined }]);
  });

  it('[US-001-ac-2] walks every page of GET /api/v1/customers and renders all customers', async () => {
    const fetchMock = mockFetch((url) =>
      url.includes('page=1') ? json(page([BETA], 1, 2), 200) : json(page([ACME], 0, 2), 200),
    );
    renderApp(ROUTES.customerList.path);

    const table = await screen.findByRole('table', { name: 'Customers' });
    await waitFor(() => expect(within(table).getAllByRole('row')).toHaveLength(3));
    expect(within(table).getByText(ACME.name)).toBeInTheDocument();
    expect(within(table).getByText(BETA.name)).toBeInTheDocument();
    expect(calls(fetchMock).map((call) => call.url)).toEqual([
      LIST_URL,
      `${CUSTOMERS_PATH}?page=1&size=${CUSTOMER_PAGE_SIZE}`,
    ]);
  });

  it('[US-001-ac-2] shows an error banner when the list request fails', async () => {
    mockFetch(() => json({}, 500));
    renderApp(ROUTES.customerList.path);
    expect(await screen.findByRole('alert')).toHaveTextContent('HTTP 500');
  });
});

describe('[US-001-ac-3] view customer via GET /customers/:id', () => {
  it('[US-001-ac-3] sends GET /customers/:id and displays the customer on HTTP 200', async () => {
    const fetchMock = mockFetch(() => json(ACME, 200));
    renderApp(customerDetailPath(ACME.id));

    const details = await screen.findByRole('region', { name: 'Customer details' });
    expect(within(details).getByText(ACME.id)).toBeInTheDocument();
    expect(within(details).getAllByText('Acme Corp').length).toBeGreaterThan(0);
    expect(within(details).getByText('Pune')).toBeInTheDocument();
    expect(calls(fetchMock)).toEqual([
      { method: 'GET', url: `${CUSTOMERS_PATH}/${ACME.id}`, body: undefined },
    ]);
  });

  it('[US-001-ac-3] shows a not-found state on HTTP 404', async () => {
    mockFetch(() => json({ code: 'CUSTOMER_NOT_FOUND' }, 404));
    renderApp(customerDetailPath('missing-id'));

    expect(await screen.findByText('Customer not found')).toBeInTheDocument();
    expect(screen.getByText('No customer exists with ID missing-id.')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Customer details' })).not.toBeInTheDocument();
  });

  it('[US-001-ac-3] looks up a customer by the ID entered on SCR-018', async () => {
    const fetchMock = mockFetch(() => json(ACME, 200));
    const user = userEvent.setup();
    renderApp(ROUTES.customerDetail.path);
    expect(fetchMock).not.toHaveBeenCalled();

    await user.type(screen.getByLabelText('Customer ID'), ACME.id);
    await user.click(screen.getByRole('button', { name: 'View customer' }));

    expect(await screen.findByRole('region', { name: 'Customer details' })).toBeInTheDocument();
    expect(calls(fetchMock)[0]).toMatchObject({
      method: 'GET',
      url: `${CUSTOMERS_PATH}/${ACME.id}`,
    });
  });

  it('[US-001-ac-3] navigates from the list to the detail view', async () => {
    mockFetch((url) => (url.endsWith(ACME.id) ? json(ACME, 200) : json(page([ACME]), 200)));
    const user = userEvent.setup();
    renderApp(ROUTES.customerList.path);
    await user.click(await screen.findByRole('link', { name: 'View Acme Corp' }));
    expect(await screen.findByRole('region', { name: 'Customer details' })).toBeInTheDocument();
  });
});

describe('[US-001-ac-4] update customer via PUT /customers/:id', () => {
  it('[US-001-ac-4] sends PUT with the changed name/city and reflects the update on HTTP 200', async () => {
    const fetchMock = mockFetch((_url, init) =>
      init.method === 'PUT'
        ? json({ ...ACME, ...(JSON.parse(String(init.body)) as Partial<Customer>) }, 200)
        : json(ACME, 200),
    );
    const user = userEvent.setup();
    renderApp(customerDetailPath(ACME.id));
    const editor = await screen.findByRole('region', { name: 'Edit customer' });

    const city = within(editor).getByLabelText(/City/);
    await user.clear(city);
    await user.type(city, 'Bengaluru');
    await user.click(within(editor).getByRole('button', { name: 'Save changes' }));

    const details = screen.getByRole('region', { name: 'Customer details' });
    await waitFor(() => expect(within(details).getByText('Bengaluru')).toBeInTheDocument());
    expect(within(details).queryByText('Pune')).not.toBeInTheDocument();
    expect(screen.getByText('Customer updated.')).toBeInTheDocument();
    expect(calls(fetchMock)).toContainEqual({
      method: 'PUT',
      url: `${CUSTOMERS_PATH}/${ACME.id}`,
      body: { city: 'Bengaluru' },
    });

    const name = within(screen.getByRole('region', { name: 'Edit customer' })).getByLabelText(
      /Name/,
    );
    await user.clear(name);
    await user.type(name, 'Acme Global');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() =>
      expect(within(details).getAllByText('Acme Global').length).toBeGreaterThan(0),
    );
    expect(calls(fetchMock).at(-1)).toEqual({
      method: 'PUT',
      url: `${CUSTOMERS_PATH}/${ACME.id}`,
      body: { name: 'Acme Global' },
    });
  });

  it('[US-001-ac-4] shows the not-found state when the update returns HTTP 404', async () => {
    mockFetch((_url, init) => (init.method === 'PUT' ? json({}, 404) : json(ACME, 200)));
    const user = userEvent.setup();
    renderApp(customerDetailPath(ACME.id));
    const editor = await screen.findByRole('region', { name: 'Edit customer' });
    const name = within(editor).getByLabelText(/Name/);
    await user.clear(name);
    await user.type(name, 'Renamed');
    await user.click(within(editor).getByRole('button', { name: 'Save changes' }));
    expect(await screen.findByText('Customer not found')).toBeInTheDocument();
  });
});

describe('[US-001-ac-5] delete customer via DELETE /customers/:id', () => {
  it.each([200, 204])(
    '[US-001-ac-5] removes the customer from the list on HTTP %i',
    async (status) => {
      const fetchMock = mockFetch((_url, init) =>
        init.method === 'DELETE' ? new Response(null, { status }) : json(page([ACME, BETA]), 200),
      );
      const user = userEvent.setup();
      renderApp(ROUTES.customerList.path);
      await user.click(await screen.findByRole('button', { name: 'Remove Acme Corp' }));
      const dialog = screen.getByRole('dialog', { name: 'Remove customer?' });
      await user.click(within(dialog).getByRole('button', { name: 'Remove' }));

      expect(await screen.findByText('Customer Acme Corp was removed.')).toBeInTheDocument();
      expect(screen.queryByText(ACME.id)).not.toBeInTheDocument();
      expect(screen.getByText(BETA.id)).toBeInTheDocument();
      expect(calls(fetchMock)).toContainEqual({
        method: 'DELETE',
        url: `${CUSTOMERS_PATH}/${ACME.id}`,
        body: undefined,
      });
    },
  );

  it('[US-001-ac-5] indicates not found on HTTP 404 from the list', async () => {
    mockFetch((_url, init) => (init.method === 'DELETE' ? json({}, 404) : json(page([ACME]), 200)));
    const user = userEvent.setup();
    renderApp(ROUTES.customerList.path);
    await user.click(await screen.findByRole('button', { name: 'Remove Acme Corp' }));
    await user.click(
      within(screen.getByRole('dialog', { name: 'Remove customer?' })).getByRole('button', {
        name: 'Remove',
      }),
    );
    expect(
      await screen.findByText(
        'Customer Acme Corp was not found. It may already have been removed.',
      ),
    ).toBeInTheDocument();
  });

  it('[US-001-ac-5] shows the removed state on SCR-018 after HTTP 204', async () => {
    const fetchMock = mockFetch((_url, init) =>
      init.method === 'DELETE' ? new Response(null, { status: 204 }) : json(ACME, 200),
    );
    const user = userEvent.setup();
    renderApp(customerDetailPath(ACME.id));
    await user.click(await screen.findByRole('button', { name: 'Remove customer' }));
    await user.click(
      within(screen.getByRole('dialog', { name: 'Remove customer?' })).getByRole('button', {
        name: 'Remove',
      }),
    );
    expect(await screen.findByText('Customer removed')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Customer details' })).not.toBeInTheDocument();
    expect(calls(fetchMock).at(-1)).toMatchObject({
      method: 'DELETE',
      url: `${CUSTOMERS_PATH}/${ACME.id}`,
    });
  });

  it('[US-001-ac-5] shows the not-found state on SCR-018 after HTTP 404', async () => {
    mockFetch((_url, init) => (init.method === 'DELETE' ? json({}, 404) : json(ACME, 200)));
    const user = userEvent.setup();
    renderApp(customerDetailPath(ACME.id));
    await user.click(await screen.findByRole('button', { name: 'Remove customer' }));
    await user.click(
      within(screen.getByRole('dialog', { name: 'Remove customer?' })).getByRole('button', {
        name: 'Remove',
      }),
    );
    expect(await screen.findByText('Customer not found')).toBeInTheDocument();
  });
});
