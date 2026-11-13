import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach, beforeEach, vi } from 'vitest';

// Tests never reach the network: screens that call the API get an empty 200 response unless a
// test stubs `fetch` itself.
beforeEach(() => {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () => new Response('[]', { status: 200 })),
  );
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});
