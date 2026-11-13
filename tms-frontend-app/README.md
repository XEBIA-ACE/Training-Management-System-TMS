# TMS Frontend App

React SPA scaffold for the ACE_For TMS `frontend-app` service.
Built for story `SCAFFOLD-frontend-app` (see `../specs/SCAFFOLD-frontend-app`).

## Stack

React 19, React Router 7 (declarative `BrowserRouter`), TypeScript, Vite 7, Vitest 4 + Testing
Library (jsdom), ESLint 9 (flat config, typescript-eslint, react-hooks), Prettier.

## Layout

| Path                         | Purpose                                                                     |
| ---------------------------- | --------------------------------------------------------------------------- |
| `src/main.tsx`               | Entrypoint: mounts `<App />` inside `BrowserRouter`                         |
| `src/App.tsx`                | Route shell (T003)                                                          |
| `src/config/env.ts`          | Environment-based configuration (T002)                                      |
| `src/routes/routes.ts`       | Route table keyed by screen IDs (`SCR-0xx`)                                 |
| `src/navigation/navItems.ts` | Primary navigation (NAV-01) from `references/navigation.md`                 |
| `src/layout/GlobalShell.tsx` | SCR-003 Global Shell & Role-Based Navigation Layout                         |
| `src/styles/tokens.css`      | Design tokens (colors, typography, spacing) as CSS variables                |
| `src/components/`            | Reusable components from `references/components.md`                         |
| `src/screens/`               | Screen stubs (SCR-001..SCR-010) and the placeholder for unspecified screens |
| `src/mocks/mockData.ts`      | Static placeholder data. TODO: replace with API data in feature stories     |

## Configuration

All configuration comes from `VITE_TMS_*` environment variables (see `.env.example`); only that
prefix is exposed to the bundle (`envPrefix` in `vite.config.ts`). Nothing is hardcoded. If a
variable is unset, the app falls back to an empty-safe value and still renders.

| Variable                           | Purpose                                          | When unset        |
| ---------------------------------- | ------------------------------------------------ | ----------------- |
| `VITE_TMS_APP_ENV`                 | Environment label shown in the shell header      | Label hidden      |
| `VITE_TMS_API_BASE_URL`            | TMS API base URL (not called yet)                | `''`              |
| `VITE_TMS_FEATURE_FLAGS`           | Comma-separated feature flags                    | `[]`              |
| `VITE_TMS_SESSION_TIMEOUT_MINUTES` | Idle timeout before SCR-002 (AUTH-02, value TBD) | `null` (disabled) |

Vite inlines these values at build/dev time, so a SPA bundle must never contain secrets.

## Run locally

Requires Node.js >= 22.22.

```bash
npm ci
cp .env.example .env.local      # optional; all values may stay empty
npm run dev                     # http://localhost:5173 -> redirects to /login (SCR-001)
```

## Build, lint, and test

```bash
npm run verify        # lint + format:check + test + build (AC-1 evidence)
npm run lint
npm run format        # prettier --write
npm test              # vitest run
npm run build         # tsc --noEmit && vite build -> dist/
```

`npm ci` works as-is. When re-resolving dependencies (`npm install` with no lockfile), use
`npm install --legacy-peer-deps`: npm 10.8 crashes (`Cannot read properties of null (reading
'edgesOut')`) while resolving Vitest 4's optional peers. Every required peer is listed in
`package.json`.

## Routes

| Path                                            | Screen                                                           | Status                           |
| ----------------------------------------------- | ---------------------------------------------------------------- | -------------------------------- |
| `/`                                             | redirects to `/login`                                            |                                  |
| `/login`                                        | SCR-001 Login & Session Management                               | stub                             |
| `/session-expired`                              | SCR-002 Session Expired / Re-authentication                      | stub                             |
| (layout)                                        | SCR-003 Global Shell & Role-Based Navigation Layout              | stub                             |
| `/dashboard`                                    | redirects to `/dashboard/coordinator`                            |                                  |
| `/dashboard/coordinator`                        | SCR-004 Coordinator Landing – Workshop Dashboard                 | stub                             |
| `/dashboard/manager`                            | SCR-005 Manager Landing – Workshop Dashboard & Summary Analytics | stub                             |
| (row click on SCR-004/005)                      | SCR-006 Workshop Detail / Requirement Summary Slide-over         | stub                             |
| `/admin/users`                                  | SCR-007 Admin – User List                                        | stub                             |
| `/admin/users/new`, `/admin/users/:userId/edit` | SCR-008 Admin – User Creation & Edit                             | stub                             |
| `/admin/rights`                                 | SCR-009 Admin – User Rights Management                           | stub                             |
| `/master-data/trainers`                         | SCR-010 Trainer Management – List                                | stub                             |
| `/profile`                                      | My Profile (navigation.md -> SCR-003)                            | placeholder, details TBD         |
| `/sign-out`                                     | Sign Out (SCR-044) as the Sign-out Confirmation modal            | stub; confirm routes to `/login` |
| Other navigation targets                        | SCR-013, 016, 018, 019, 020, 022, 023, 026–038, 040              | placeholder (Empty State)        |

## Spec coverage (SCAFFOLD-frontend-app)

| Item                                           | Status           | Where                                                                                                                                                                                                                                                                  |
| ---------------------------------------------- | ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| T001 project skeleton, entrypoint, lint/format | Done             | `package.json`, `src/main.tsx`, `eslint.config.js`, `.prettierrc.json`, `.editorconfig`                                                                                                                                                                                |
| T002 environment-based configuration           | Done             | `src/config/env.ts`, `.env.example`, `src/config/env.test.ts`                                                                                                                                                                                                          |
| T003 page/route shell renders                  | Done             | `src/App.tsx`, `src/layout/GlobalShell.tsx`, `src/screens/**`                                                                                                                                                                                                          |
| T004 verify AC-1                               | Done (automated) | `src/App.test.tsx`: root route renders, shell and all nav items render, every route renders                                                                                                                                                                            |
| T005 store AC-1 evidence                       | Partial          | `npm run verify` output and the PR description. Uploading to an external QA/evidence store is not configured in this repo                                                                                                                                              |
| plan.md design tokens and typography           | Done             | `src/styles/tokens.css`. Only the spec colors are used; hover/active use `filter: brightness()`, disabled uses opacity, focus uses an `info` ring                                                                                                                      |
| plan.md / components.md reusable components    | Done (basic)     | `src/components/`: Primary Button, Secondary Button, Text Input, Dropdown / Select, Radio Group, Slider Toggle (Yes/No), Table / Data Grid, Status Chip / Tag, Modal / Dialog, Slide-over Panel, Filter Panel / Chip Filters, Global Error Banner, Empty State Pattern |
| navigation.md primary navigation               | Done             | `src/navigation/navItems.ts` (labels, screen IDs, and icon names kept verbatim)                                                                                                                                                                                        |
| screens.md screens SCR-001..SCR-010            | Stubs            | Static placeholder content only                                                                                                                                                                                                                                        |

### Not implemented (out of scope for this story)

- Real authentication, session handling, and the idle timeout (AUTH-02). Login only checks that
  fields are non-empty and then routes to SCR-004.
- RBAC and role-based navigation filtering (NAV-01, SCR-009, F-10). Every nav item is shown.
- Data fetching, persistence, and cross-service integration (F-01, F-06, F-10). Screens use
  `src/mocks/mockData.ts`.
- Table sorting, pagination/infinite scroll, loading states, and applying filters to data.
- Chip-based multi-select variant of Dropdown / Select.
- Icons: icon names from navigation.md are carried as `data-icon` attributes; no icon set is
  bundled.

### Gaps and deviations

- AC-1 in the export reads "Scaffold the Store Admin Portal (service)". This repo is ACE_For TMS
  and the story is `SCAFFOLD-frontend-app`, so the scaffold follows the TMS UX artifacts on file
  (`references/*.md`). The AC wording looks like a template carry-over.
- No component/MFE decomposition is on file for `frontend-app`, so it is a single SPA.
- Navigation targets SCR-013, 016, 018, 019, 020, 022, 023, 026–038, 040, and 044 have no
  exported screen spec. They render a placeholder, except SCR-044, which uses the Sign-out
  Confirmation modal named in components.md.
- `[TBD]` items are left as `TODO(<ID>)` comments rather than guessed: SCR-001 password help,
  SCR-002 last attempted action, SCR-003 profile menu and breadcrumbs, SCR-004 trainer filter,
  SCR-006 history/audit, SCR-007 last login, SCR-008 optional fields and uniqueness check,
  SCR-009 rights granularity and activation timing, SCR-005 role-restricted banner.
- Status Chip states are limited to `confirmed | unconfirmed | pending | error` (components.md).
  The spec does not define how active/inactive maps onto them. The stubs use
  confirmed = Active and unconfirmed = Inactive, always with a text label, and mark this with a
  TODO.
- The modal backdrop uses `color-mix()` on `--color-secondary-500` for translucency instead of a
  new color.
- `npm audit` reports a moderate advisory (minimatch, GHSA-g3hv-75wg-m249) in the dev-only
  `typescript-eslint` toolchain. The production dependency audit (`npm audit --omit=dev`) is
  clean.
