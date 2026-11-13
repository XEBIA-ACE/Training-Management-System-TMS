/**
 * Environment-based configuration (SCAFFOLD-frontend-app, T002).
 *
 * All values come from `VITE_TMS_*` environment variables resolved at build/dev time by Vite.
 * Nothing is hardcoded: when a variable is unset the app falls back to an empty-safe value
 * (empty string / empty list / null) and still renders.
 */
export interface AppConfig {
  appEnv: string;
  /** Empty string when unset. No API calls are made by the scaffold. */
  apiBaseUrl: string;
  featureFlags: readonly string[];
  /** TODO(AUTH-02): session timeout value is TBD; null disables the idle timer. */
  sessionTimeoutMinutes: number | null;
}

type EnvSource = Record<string, string | boolean | undefined>;

function readString(env: EnvSource, key: string): string {
  const value = env[key];
  return typeof value === 'string' ? value.trim() : '';
}

function readList(env: EnvSource, key: string): string[] {
  return readString(env, key)
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

function readPositiveNumber(env: EnvSource, key: string): number | null {
  const raw = readString(env, key);
  if (raw === '') return null;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

export function loadConfig(env: EnvSource): AppConfig {
  return Object.freeze({
    appEnv: readString(env, 'VITE_TMS_APP_ENV'),
    apiBaseUrl: readString(env, 'VITE_TMS_API_BASE_URL').replace(/\/+$/, ''),
    featureFlags: Object.freeze(readList(env, 'VITE_TMS_FEATURE_FLAGS')),
    sessionTimeoutMinutes: readPositiveNumber(env, 'VITE_TMS_SESSION_TIMEOUT_MINUTES'),
  });
}

export const config: AppConfig = loadConfig(import.meta.env);

export function isFeatureEnabled(flag: string, source: AppConfig = config): boolean {
  return source.featureFlags.includes(flag);
}
