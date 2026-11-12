# Screens

## Login & Session Management
Secure authentication screen used by coordinators, managers, admins, and other staff to access the TMS, enforcing RBAC at entry and handling invalid credential flows.

- Category: Authentication
- Key elements: Email/username input field, Password input field, Login/Submit button, Inline validation and generic error messaging area, Link or help text for password support [TBD exact mechanism], Branding/logo header, Session timeout re-login view variant
- Associated flows: User authentication (AUTH-02), Session expiry and re-authentication, Routing to role-based landing pages

## Session Expired / Re-authentication
Interstitial screen shown when sessions expire, requiring users to authenticate again before accessing protected areas.

- Category: Authentication
- Key elements: Message indicating session timeout/expiry, Summary of last attempted action [TBD], Email/username and password fields (or CTA to Login Screen), Login button, Link to support/password help [TBD]
- Associated flows: Session timeout handling (AUTH-02), Re-authentication to continue work

## Global Shell & Role-Based Navigation Layout
Layout frame used by all authenticated screens, implementing RBAC-driven navigation and providing the structural container for module content.

- Category: Navigation
- Key elements: Persistent header with product name/logo, Left-side navigation menu with role-based menu items (Customers, Requirements, Allocation, Dashboard, Feedback, Reports, Admin, Masters, etc.), User profile/access menu (view profile, sign out) [TBD details], Main content area placeholder for module screens, Context-preserving back navigation and breadcrumbs [TBD pattern]
- Associated flows: Role-based navigation (NAV-01), Access to all feature modules (F-01 to F-10, AUTH-01, F-09, etc.), Sign-out and profile access [TBD]

## Coordinator Landing – Workshop Dashboard
Coordinator-focused dashboard presenting workshops in a configurable timeline table with supporting analytics and drill-down capabilities.

- Category: Dashboard
- Key elements: Timeline view switcher (daily/weekly/monthly/quarterly), Workshop sessions table/list with columns: title, date/time, trainer, location, participant count, status, Status indicators with legend (confirmed, unconfirmed, pending), Filters (status, date range shortcut, customer, trainer [TBD]), Summary analytics panel (total workshops, confirmation breakdown, next 7 days, total participants), Pagination or infinite scroll controls and loading states, Empty-state messaging and partial-data warning banner
- Associated flows: Coordinator landing view (NAV-01, F-06), Monitoring planned/potential workshops, Filtering by status and time horizon, Drill-down to workshop/requirement detail

## Manager Landing – Workshop Dashboard & Summary Analytics
Manager-oriented variant of the Workshop Dashboard emphasizing KPI analytics and access to reporting tools.

- Category: Dashboard
- Key elements: Same core workshop list/timeline as coordinator dashboard (reused layout), Prominent KPI tiles (total workshops, confirmed vs unconfirmed, next 7 days, participants), Filters suitable for managers (date range, client, trainer, status), Access links to key reports (Trainer Allocation View, Incentive, Feedback Analytics), Role-restricted visibility banner for limited access, if applicable
- Associated flows: Manager landing view (NAV-01, F-06), Monitoring pipeline health, Navigating to reporting module

## Workshop Detail / Requirement Summary Slide-over
Contextual detail view presented as a slide-over or modal from dashboards and reports, summarizing all key workshop attributes.

- Category: Dashboard
- Key elements: Header with workshop/requirement title and status tag, Sections for: customer & SPOC, OEM certification, dates/times, delivery mode, location, participants, Trainer allocation summary (primary, co-trainer, statuses), Logistics and T&E handling details, History/audit snippet [TBD], including key status changes, Links/actions to open full requirement or allocation screens (based on rights)
- Associated flows: Drill-down from Workshop Dashboard (F-06), Quick review of workshop context, Navigation to full Requirement or Allocation modules

## Admin – User List
Super-admin-only listing of user accounts with controls to add new users and manage existing ones while respecting non-deletion policies.

- Category: Admin
- Key elements: Searchable, sortable table of users (employee ID, name, email, role(s), status, last login [TBD]), Filters for active/inactive/all users, Button to create new user, Row actions to open User Detail / Rights Assignment, Indicator for users missing rights/access
- Associated flows: Viewing all users (AUTH-01, F-10), Accessing user creation form, Accessing user rights management

## Admin – User Creation & Edit
Form used by super-admins to add new TMS users or edit existing ones while enforcing immutable and unique identifiers.

- Category: Admin
- Key elements: Form fields: employee ID, name, email, optional fields [TBD], Status indicator (active/inactive) for edit mode, Validation messages for format and uniqueness across active/inactive accounts, Save/Cancel buttons, Non-editable employee ID field after creation
- Associated flows: Admin-driven onboarding (AUTH-01, F-10), Editing user profile data

## Admin – User Rights Management
RBAC configuration screen where super-admins control module and feature permissions per user, affecting navigation visibility and functional access.

- Category: Admin
- Key elements: User selector or entry from User List, Summary of selected user’s details, Matrix or checklist of modules/features (Trainers, Certifications, Customers, Requirements, Allocation, Workshop Dashboard, Reports, Admin, etc.) with view/edit rights toggles [TBD granularity], Save/Cancel controls, Info banner explaining RBAC effect and activation timing
- Associated flows: Assigning user rights after creation (F-10), Updating rights as responsibilities change, Revoking access to modules

## Trainer Management – List
Central listing of trainer master records enabling users to find, inspect, and manage trainers while respecting activation/inactivation patterns.

- Category: Master Data
- Key elements: Searchable, filterable table of trainers (name, employee ID, internal/external, location/timezone, weekend availability, incentive eligibility, status), Filters for active/inactive/internal/external/weekend-available, Buttons/links to add new trainer and open trainer detail, Status pill for active/inactive (with absence of delete actions)
- Associated flows: Viewing trainer master data (F-01), Accessing trainer creation and edit forms, Filtering trainers for planning/reference
