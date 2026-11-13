/**
 * Static placeholder data for screen stubs.
 * TODO(F-06, F-01, F-10): replace with real data from the TMS API (config.apiBaseUrl) in the
 * feature stories. No network calls are made by the scaffold.
 */
import type { StatusChipState } from '../components';

export interface MockWorkshop {
  id: string;
  title: string;
  dateTime: string;
  trainer: string;
  location: string;
  participants: number;
  status: StatusChipState;
  statusLabel: string;
  customer: string;
  spoc: string;
  oemCertification: string;
  deliveryMode: string;
  coTrainer: string;
  logistics: string;
}

export const MOCK_WORKSHOPS: readonly MockWorkshop[] = [
  {
    id: 'WS-0001',
    title: 'Placeholder Workshop A',
    dateTime: '— (placeholder)',
    trainer: 'Trainer A',
    location: 'Location A',
    participants: 0,
    status: 'confirmed',
    statusLabel: 'Confirmed',
    customer: 'Customer A',
    spoc: 'SPOC A',
    oemCertification: 'OEM Certification A',
    deliveryMode: 'Delivery mode A',
    coTrainer: '—',
    logistics: '—',
  },
  {
    id: 'WS-0002',
    title: 'Placeholder Workshop B',
    dateTime: '— (placeholder)',
    trainer: 'Trainer B',
    location: 'Location B',
    participants: 0,
    status: 'unconfirmed',
    statusLabel: 'Unconfirmed',
    customer: 'Customer B',
    spoc: 'SPOC B',
    oemCertification: 'OEM Certification B',
    deliveryMode: 'Delivery mode B',
    coTrainer: '—',
    logistics: '—',
  },
  {
    id: 'WS-0003',
    title: 'Placeholder Workshop C',
    dateTime: '— (placeholder)',
    trainer: '—',
    location: 'Location C',
    participants: 0,
    status: 'pending',
    statusLabel: 'Pending',
    customer: 'Customer C',
    spoc: 'SPOC C',
    oemCertification: 'OEM Certification C',
    deliveryMode: 'Delivery mode C',
    coTrainer: '—',
    logistics: '—',
  },
];

export interface MockUser {
  employeeId: string;
  name: string;
  email: string;
  roles: string;
  active: boolean;
  hasRights: boolean;
}

export const MOCK_USERS: readonly MockUser[] = [
  {
    employeeId: 'EMP-0001',
    name: 'Placeholder User A',
    email: 'user.a@example.invalid',
    roles: '—',
    active: true,
    hasRights: true,
  },
  {
    employeeId: 'EMP-0002',
    name: 'Placeholder User B',
    email: 'user.b@example.invalid',
    roles: '—',
    active: false,
    hasRights: false,
  },
];

export interface MockTrainer {
  employeeId: string;
  name: string;
  internal: boolean;
  locationTimezone: string;
  weekendAvailable: boolean;
  incentiveEligible: boolean;
  active: boolean;
}

export const MOCK_TRAINERS: readonly MockTrainer[] = [
  {
    employeeId: 'TR-0001',
    name: 'Placeholder Trainer A',
    internal: true,
    locationTimezone: 'Location A / TZ A',
    weekendAvailable: true,
    incentiveEligible: false,
    active: true,
  },
  {
    employeeId: 'TR-0002',
    name: 'Placeholder Trainer B',
    internal: false,
    locationTimezone: 'Location B / TZ B',
    weekendAvailable: false,
    incentiveEligible: true,
    active: false,
  },
];

/** Modules listed for the rights matrix in references/screens.md (SCR-009). */
export const RIGHTS_MODULES: readonly string[] = [
  'Trainers',
  'Certifications',
  'Customers',
  'Requirements',
  'Allocation',
  'Workshop Dashboard',
  'Reports',
  'Admin',
];
