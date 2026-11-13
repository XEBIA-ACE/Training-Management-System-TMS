import { ROUTES, type RouteKey } from '../routes/routes';

/**
 * Primary navigation (NAV-01) exactly as documented in references/navigation.md.
 * Icon names are carried as-is from the spec; no icon set is bundled yet.
 *
 * TODO(NAV-01, SCR-009): role-based visibility. RBAC granularity is [TBD]; every item is shown
 * until the rights model is defined. `requiredRight` is intentionally left unset.
 */
export interface NavItem {
  label: string;
  route: RouteKey;
  icon: string;
  children?: readonly NavItem[];
  requiredRight?: string;
}

export const PRODUCT_NAME = 'ACE_For TMS';

export const PRIMARY_NAV: readonly NavItem[] = [
  {
    label: 'Dashboard',
    route: 'coordinatorDashboard',
    icon: 'dashboard',
    children: [
      { label: 'Coordinator Dashboard', route: 'coordinatorDashboard', icon: 'grid' },
      { label: 'Manager Dashboard', route: 'managerDashboard', icon: 'bar-chart' },
    ],
  },
  {
    label: 'Customers',
    route: 'customerList',
    icon: 'users',
    children: [
      { label: 'Customer List', route: 'customerList', icon: 'list' },
      { label: 'Customer Detail', route: 'customerDetail', icon: 'file-text' },
    ],
  },
  {
    label: 'Requirements',
    route: 'requirementList',
    icon: 'clipboard',
    children: [
      { label: 'Requirement List', route: 'requirementList', icon: 'list' },
      { label: 'New Requirement', route: 'newRequirement', icon: 'plus-circle' },
    ],
  },
  {
    label: 'Allocation',
    route: 'allocationSelect',
    icon: 'user-check',
    children: [
      { label: 'Select Requirement', route: 'allocationSelect', icon: 'search' },
      { label: 'Allocation Workspace', route: 'allocationWorkspace', icon: 'sliders' },
    ],
  },
  {
    label: 'Feedback',
    route: 'feedbackWorkshopSelector',
    icon: 'message-square',
    children: [
      { label: 'Workshop Selector', route: 'feedbackWorkshopSelector', icon: 'calendar' },
      { label: 'Feedback Setup', route: 'feedbackSetup', icon: 'settings' },
      { label: 'Distribution & Reminders', route: 'feedbackDistribution', icon: 'send' },
      { label: 'Feedback Analytics', route: 'feedbackAnalytics', icon: 'pie-chart' },
    ],
  },
  {
    label: 'Checklists',
    route: 'checklistSessionSelector',
    icon: 'check-square',
    children: [
      { label: 'Session Selector', route: 'checklistSessionSelector', icon: 'search' },
      { label: 'Checklist Editor', route: 'checklistEditor', icon: 'edit' },
      { label: 'Checklist Status', route: 'checklistStatus', icon: 'activity' },
    ],
  },
  {
    label: 'Reports',
    route: 'reportsHome',
    icon: 'bar-chart-2',
    children: [
      { label: 'Reports Home', route: 'reportsHome', icon: 'home' },
      { label: 'Trainer Allocation Report', route: 'reportTrainerAllocation', icon: 'user-check' },
      { label: 'Trainer Incentive Report', route: 'reportTrainerIncentive', icon: 'dollar-sign' },
      { label: 'Trainer Feedback Report', route: 'reportTrainerFeedback', icon: 'message-circle' },
      { label: 'Cost per OEM Report', route: 'reportCostPerOem', icon: 'trending-up' },
      { label: 'Pending Action Items', route: 'reportPendingActionItems', icon: 'alert-circle' },
    ],
  },
  {
    label: 'Master Data',
    route: 'trainerList',
    icon: 'database',
    children: [
      { label: 'Trainers', route: 'trainerList', icon: 'user' },
      { label: 'OEM Certifications', route: 'oemCertifications', icon: 'award' },
    ],
  },
  {
    label: 'Admin',
    route: 'adminUserList',
    icon: 'shield',
    children: [
      { label: 'User Management', route: 'adminUserList', icon: 'users' },
      { label: 'User Rights', route: 'adminUserRights', icon: 'key' },
      { label: 'Inactive Entities', route: 'inactiveEntities', icon: 'archive' },
    ],
  },
];

/** Items rendered after the divider in the navigation. */
export const SECONDARY_NAV: readonly NavItem[] = [
  { label: 'My Profile', route: 'profile', icon: 'user-circle' },
  { label: 'Sign Out', route: 'signOut', icon: 'log-out' },
];

export function pathFor(route: RouteKey): string {
  return ROUTES[route].path;
}
