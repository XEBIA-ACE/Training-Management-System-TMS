/**
 * Route table keyed by the approved screen IDs (references/screens.md, references/navigation.md).
 * Screen IDs are used verbatim and must not be repurposed.
 *
 * `scaffolded: true` screens have components (SCAFFOLD-frontend-app stubs or feature stories).
 * Others are referenced by the primary navigation but have no exported screen spec yet; they
 * render a placeholder until a later story defines them.
 */
export interface ScreenRoute {
  screenId: string;
  path: string;
  title: string;
  scaffolded: boolean;
}

export const ROUTES = {
  login: {
    screenId: 'SCR-001',
    path: '/login',
    title: 'Login & Session Management',
    scaffolded: true,
  },
  sessionExpired: {
    screenId: 'SCR-002',
    path: '/session-expired',
    title: 'Session Expired / Re-authentication',
    scaffolded: true,
  },
  profile: { screenId: 'SCR-003', path: '/profile', title: 'My Profile', scaffolded: true },
  coordinatorDashboard: {
    screenId: 'SCR-004',
    path: '/dashboard/coordinator',
    title: 'Coordinator Landing – Workshop Dashboard',
    scaffolded: true,
  },
  managerDashboard: {
    screenId: 'SCR-005',
    path: '/dashboard/manager',
    title: 'Manager Landing – Workshop Dashboard & Summary Analytics',
    scaffolded: true,
  },
  adminUserList: {
    screenId: 'SCR-007',
    path: '/admin/users',
    title: 'Admin – User List',
    scaffolded: true,
  },
  adminUserCreate: {
    screenId: 'SCR-008',
    path: '/admin/users/new',
    title: 'Admin – User Creation & Edit',
    scaffolded: true,
  },
  adminUserEdit: {
    screenId: 'SCR-008',
    path: '/admin/users/:userId/edit',
    title: 'Admin – User Creation & Edit',
    scaffolded: true,
  },
  adminUserRights: {
    screenId: 'SCR-009',
    path: '/admin/rights',
    title: 'Admin – User Rights Management',
    scaffolded: true,
  },
  trainerList: {
    screenId: 'SCR-010',
    path: '/master-data/trainers',
    title: 'Trainer Management – List',
    scaffolded: true,
  },
  oemCertifications: {
    screenId: 'SCR-013',
    path: '/master-data/oem-certifications',
    title: 'OEM Certifications',
    scaffolded: false,
  },
  customerList: {
    screenId: 'SCR-016',
    path: '/customers',
    title: 'Customer List',
    scaffolded: true,
  },
  customerDetail: {
    screenId: 'SCR-018',
    path: '/customers/detail',
    title: 'Customer Detail',
    scaffolded: true,
  },
  requirementList: {
    screenId: 'SCR-019',
    path: '/requirements',
    title: 'Requirement List',
    scaffolded: false,
  },
  newRequirement: {
    screenId: 'SCR-020',
    path: '/requirements/new',
    title: 'New Requirement',
    scaffolded: false,
  },
  allocationSelect: {
    screenId: 'SCR-022',
    path: '/allocation',
    title: 'Select Requirement',
    scaffolded: false,
  },
  allocationWorkspace: {
    screenId: 'SCR-023',
    path: '/allocation/workspace',
    title: 'Allocation Workspace',
    scaffolded: false,
  },
  checklistSessionSelector: {
    screenId: 'SCR-026',
    path: '/checklists',
    title: 'Session Selector',
    scaffolded: false,
  },
  checklistEditor: {
    screenId: 'SCR-027',
    path: '/checklists/editor',
    title: 'Checklist Editor',
    scaffolded: false,
  },
  checklistStatus: {
    screenId: 'SCR-028',
    path: '/checklists/status',
    title: 'Checklist Status',
    scaffolded: false,
  },
  feedbackWorkshopSelector: {
    screenId: 'SCR-029',
    path: '/feedback',
    title: 'Workshop Selector',
    scaffolded: false,
  },
  feedbackSetup: {
    screenId: 'SCR-030',
    path: '/feedback/setup',
    title: 'Feedback Setup',
    scaffolded: false,
  },
  feedbackDistribution: {
    screenId: 'SCR-031',
    path: '/feedback/distribution',
    title: 'Distribution & Reminders',
    scaffolded: false,
  },
  feedbackAnalytics: {
    screenId: 'SCR-032',
    path: '/feedback/analytics',
    title: 'Feedback Analytics',
    scaffolded: false,
  },
  reportsHome: { screenId: 'SCR-033', path: '/reports', title: 'Reports Home', scaffolded: false },
  reportTrainerAllocation: {
    screenId: 'SCR-034',
    path: '/reports/trainer-allocation',
    title: 'Trainer Allocation Report',
    scaffolded: false,
  },
  reportTrainerIncentive: {
    screenId: 'SCR-035',
    path: '/reports/trainer-incentive',
    title: 'Trainer Incentive Report',
    scaffolded: false,
  },
  reportTrainerFeedback: {
    screenId: 'SCR-036',
    path: '/reports/trainer-feedback',
    title: 'Trainer Feedback Report',
    scaffolded: false,
  },
  reportCostPerOem: {
    screenId: 'SCR-037',
    path: '/reports/cost-per-oem',
    title: 'Cost per OEM Report',
    scaffolded: false,
  },
  reportPendingActionItems: {
    screenId: 'SCR-038',
    path: '/reports/pending-action-items',
    title: 'Pending Action Items',
    scaffolded: false,
  },
  inactiveEntities: {
    screenId: 'SCR-040',
    path: '/admin/inactive-entities',
    title: 'Inactive Entities',
    scaffolded: false,
  },
  signOut: { screenId: 'SCR-044', path: '/sign-out', title: 'Sign Out', scaffolded: false },
} as const satisfies Record<string, ScreenRoute>;

export type RouteKey = keyof typeof ROUTES;

export const PLACEHOLDER_ROUTES: readonly ScreenRoute[] = Object.values(ROUTES).filter(
  (route) => !route.scaffolded,
);

export function adminUserEditPath(userId: string): string {
  return ROUTES.adminUserEdit.path.replace(':userId', encodeURIComponent(userId));
}

export function customerDetailPath(customerId: string): string {
  return `${ROUTES.customerDetail.path}?id=${encodeURIComponent(customerId)}`;
}
