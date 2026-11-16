import { config } from '../config/env';

/**
 * Client for the trainer endpoints of tms-core-service (US-003).
 *
 * GAP(artifact-SVC-005-api_spec, artifact-SVC-005-c4): the exported API spec/C4 define no trainer
 * endpoints and the backend `com.tms.trainer` module is an empty placeholder. Paths below follow the
 * technical design's endpoint list (`POST /api/trainers`, `GET /api/trainers`); the `/api/v1` prefix
 * mirrors customerApi. No new backend endpoints are invented: there is no dedicated duplicate-check
 * endpoint, so duplicates are detected from `GET /api/trainers`.
 * FIXME(artifact-SVC-005-api_spec): confirm paths, payload field names, the Active status literal,
 * the `timeZone` field name, and whether the list includes inactive trainers and is paged.
 * GAP(US-003): no token acquisition in the SPA yet (AUTH-02); no Authorization header is sent.
 */
export const TRAINERS_PATH = '/api/v1/trainers';

export type TrainerType = 'internal' | 'external';

/** Status literal treated as the happy path (FR-US-003-008). FIXME(artifact-SVC-005-api_spec). */
export const TRAINER_STATUS_ACTIVE = 'Active';

export interface TrainerInput {
  trainerType: TrainerType;
  name: string;
  employeeId?: string;
  email?: string;
  location: string;
}

export interface Trainer {
  id: string;
  trainerType: TrainerType;
  name: string;
  employeeId?: string;
  email?: string;
  location: string;
  status: string;
  /** Server-derived from `location` (FR-US-003-010). */
  timeZone?: string;
}

export class TrainerApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'TrainerApiError';
  }
}

const url = () => `${config.apiBaseUrl}${TRAINERS_PATH}`;

async function send(method: string, body?: unknown): Promise<Response> {
  try {
    return await fetch(url(), {
      method,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new TrainerApiError(0, 'The trainer service could not be reached.');
  }
}

async function errorFrom(response: Response): Promise<TrainerApiError> {
  let message = `The trainer service responded with HTTP ${response.status}.`;
  try {
    const payload = (await response.json()) as { message?: unknown };
    if (typeof payload.message === 'string' && payload.message) message = payload.message;
  } catch {
    // Non-JSON error body: keep the generic message.
  }
  return new TrainerApiError(response.status, message);
}

export async function listTrainers(): Promise<Trainer[]> {
  const response = await send('GET');
  if (response.status !== 200) throw await errorFrom(response);
  const body = (await response.json()) as Trainer[] | { content: Trainer[] };
  return Array.isArray(body) ? body : body.content;
}

export interface DuplicateReport {
  employeeId: boolean;
  email: boolean;
}

const same = (a?: string, b?: string) =>
  !!a && !!b && a.trim().toLowerCase() === b.trim().toLowerCase();

/**
 * Duplicate check by employee identifier and/or email (FR-US-003-005). Compares against the
 * trainer list client-side. FIXME(artifact-SVC-005-api_spec): replace with the service's
 * duplicate-check endpoint once it is specified; inactive-record coverage is unconfirmed.
 */
export async function checkDuplicateTrainer(input: TrainerInput): Promise<DuplicateReport> {
  const existing = await listTrainers();
  return {
    employeeId: existing.some((t) => same(t.employeeId, input.employeeId)),
    email: existing.some((t) => same(t.email, input.email)),
  };
}

/** POST /trainers; resolves with the created trainer on HTTP 201. */
export async function createTrainer(input: TrainerInput): Promise<Trainer> {
  const response = await send('POST', input);
  if (response.status !== 201 && response.status !== 200) throw await errorFrom(response);
  return (await response.json()) as Trainer;
}
