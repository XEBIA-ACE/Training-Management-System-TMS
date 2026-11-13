/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_TMS_APP_ENV?: string;
  readonly VITE_TMS_API_BASE_URL?: string;
  readonly VITE_TMS_FEATURE_FLAGS?: string;
  readonly VITE_TMS_SESSION_TIMEOUT_MINUTES?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
