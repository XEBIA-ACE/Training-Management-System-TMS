import { config } from '../config/env';

/**
 * Client for the customer REST API served by tms-core-service (US-001, FR-US-001-AC1..AC5).
 *
 * Requests go to `${VITE_TMS_API_BASE_URL}/api/v1/customers[/:id]` (FR-US-001-AC1..AC5).
 * GAP(US-001): the SPA has no token acquisition yet (AUTH-02), so no Authorization header is sent;
 * the backend requires a bearer JWT for these routes.
 */
export const CUSTOMERS_PATH = '/api/v1/customers';

/** Page size used when walking `GET /api/v1/customers` pages (backend maximum). */
export const CUSTOMER_PAGE_SIZE = 100;

export interface Customer {
  id: string;
  name: string;
  city: string;
}

export interface CustomerInput {
  name: string;
  city: string;
}

/** One page of `GET /api/v1/customers` (FR-US-001-AC2); `page` is zero-based. */
export interface CustomerPage {
  content: Customer[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Name and/or city; omitted fields are left unchanged (FR-US-001-AC4). */
export type CustomerUpdate = Partial<CustomerInput>;

/** Unexpected HTTP status or network failure (`status` 0) from the customer API. */
export class CustomerApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'CustomerApiError';
  }
}

function customersUrl(id?: string): string {
  const base = `${config.apiBaseUrl}${CUSTOMERS_PATH}`;
  return id === undefined ? base : `${base}/${encodeURIComponent(id)}`;
}

async function send(method: string, url: string, body?: unknown): Promise<Response> {
  try {
    return await fetch(url, {
      method,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new CustomerApiError(0, 'The customer service could not be reached.');
  }
}

async function errorFrom(response: Response): Promise<CustomerApiError> {
  let message = `The customer service responded with HTTP ${response.status}.`;
  try {
    const payload = (await response.json()) as { message?: unknown };
    if (typeof payload.message === 'string' && payload.message) message = payload.message;
  } catch {
    // Non-JSON error body: keep the generic message.
  }
  return new CustomerApiError(response.status, message);
}

/** POST /customers; resolves with the created customer on HTTP 201 (FR-US-001-AC1). */
export async function createCustomer(input: CustomerInput): Promise<Customer> {
  const response = await send('POST', customersUrl(), input);
  if (response.status !== 201) throw await errorFrom(response);
  return (await response.json()) as Customer;
}

/**
 * GET /customers; walks every page (HTTP 200) and resolves with all customers (FR-US-001-AC2).
 * GAP(FR-US-001-AC2): the list screen has no paging UI specified, so all pages are loaded.
 */
export async function listCustomers(): Promise<Customer[]> {
  const customers: Customer[] = [];
  for (let page = 0; ; page++) {
    const url = `${customersUrl()}?page=${page}&size=${CUSTOMER_PAGE_SIZE}`;
    const response = await send('GET', url);
    if (response.status !== 200) throw await errorFrom(response);
    const body = (await response.json()) as CustomerPage;
    customers.push(...body.content);
    if (page + 1 >= body.totalPages) return customers;
  }
}

/** GET /customers/:id; `null` on HTTP 404 (FR-US-001-AC3). */
export async function getCustomer(id: string): Promise<Customer | null> {
  const response = await send('GET', customersUrl(id));
  if (response.status === 404) return null;
  if (response.status !== 200) throw await errorFrom(response);
  return (await response.json()) as Customer;
}

/** PUT /customers/:id; updated customer on HTTP 200, `null` on 404 (FR-US-001-AC4). */
export async function updateCustomer(id: string, update: CustomerUpdate): Promise<Customer | null> {
  const response = await send('PUT', customersUrl(id), update);
  if (response.status === 404) return null;
  if (response.status !== 200) throw await errorFrom(response);
  return (await response.json()) as Customer;
}

export type DeleteCustomerOutcome = 'deleted' | 'not_found';

/** DELETE /customers/:id; `deleted` on HTTP 200/204, `not_found` on 404 (FR-US-001-AC5). */
export async function deleteCustomer(id: string): Promise<DeleteCustomerOutcome> {
  const response = await send('DELETE', customersUrl(id));
  if (response.status === 200 || response.status === 204) return 'deleted';
  if (response.status === 404) return 'not_found';
  throw await errorFrom(response);
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Unexpected error.';
}
