import { isFeatureEnabled, loadConfig } from './env';

describe('loadConfig (T002)', () => {
  it('falls back to empty-safe values when variables are unset', () => {
    const config = loadConfig({});
    expect(config).toEqual({
      appEnv: '',
      apiBaseUrl: '',
      featureFlags: [],
      sessionTimeoutMinutes: null,
    });
  });

  it('reads values from environment variables', () => {
    const config = loadConfig({
      VITE_TMS_APP_ENV: ' qa ',
      VITE_TMS_API_BASE_URL: 'https://api.example.test/',
      VITE_TMS_FEATURE_FLAGS: 'flagA, flagB,,',
      VITE_TMS_SESSION_TIMEOUT_MINUTES: '30',
    });
    expect(config.appEnv).toBe('qa');
    expect(config.apiBaseUrl).toBe('https://api.example.test');
    expect(config.featureFlags).toEqual(['flagA', 'flagB']);
    expect(config.sessionTimeoutMinutes).toBe(30);
    expect(isFeatureEnabled('flagB', config)).toBe(true);
    expect(isFeatureEnabled('flagC', config)).toBe(false);
  });

  it('ignores invalid session timeout values', () => {
    expect(
      loadConfig({ VITE_TMS_SESSION_TIMEOUT_MINUTES: 'abc' }).sessionTimeoutMinutes,
    ).toBeNull();
    expect(loadConfig({ VITE_TMS_SESSION_TIMEOUT_MINUTES: '-5' }).sessionTimeoutMinutes).toBeNull();
  });
});
