import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import {
  GlobalErrorBanner,
  PrimaryButton,
  SecondaryButton,
  Select,
  SliderToggle,
} from '../../components';
import { MOCK_USERS, RIGHTS_MODULES } from '../../mocks/mockData';
import { ROUTES } from '../../routes/routes';
import { ScreenHeader } from '../common/ScreenHeader';

type RightsState = Record<string, { view: boolean; edit: boolean }>;

/**
 * SCR-009 Admin – User Rights Management (F-10).
 * TODO(SCR-009, NAV-01): rights granularity is [TBD]; view/edit toggles per module are
 * structural only and are not persisted or enforced.
 */
export function UserRightsScreen() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [userId, setUserId] = useState(searchParams.get('user') ?? '');
  const [rights, setRights] = useState<RightsState>({});
  const user = MOCK_USERS.find((candidate) => candidate.employeeId === userId);

  const setRight = (module: string, kind: 'view' | 'edit', value: boolean) =>
    setRights((current) => ({
      ...current,
      [module]: { ...(current[module] ?? { view: false, edit: false }), [kind]: value },
    }));

  return (
    <section className="tms-stack">
      <ScreenHeader route={ROUTES.adminUserRights} />
      <GlobalErrorBanner
        variant="info"
        message="Rights control navigation visibility and functional access. Activation timing is [TBD]."
      />
      <div className="tms-card tms-stack">
        <Select
          label="User"
          placeholder="Select a user"
          value={userId}
          onChange={(event) => setUserId(event.target.value)}
          options={MOCK_USERS.map((candidate) => ({
            value: candidate.employeeId,
            label: `${candidate.name} (${candidate.employeeId})`,
          }))}
        />
        {user && (
          <div className="tms-muted">
            {user.name} · {user.email} · {user.active ? 'Active' : 'Inactive'}
          </div>
        )}
      </div>
      <div className="tms-card tms-stack" aria-label="Module rights">
        {RIGHTS_MODULES.map((module) => (
          <div key={module} className="tms-row" style={{ justifyContent: 'space-between' }}>
            <strong>{module}</strong>
            <div className="tms-row">
              <SliderToggle
                label={`${module} view`}
                checked={rights[module]?.view ?? false}
                disabled={!user}
                onChange={(value) => setRight(module, 'view', value)}
              />
              <SliderToggle
                label={`${module} edit`}
                checked={rights[module]?.edit ?? false}
                disabled={!user}
                onChange={(value) => setRight(module, 'edit', value)}
              />
            </div>
          </div>
        ))}
      </div>
      <div className="tms-row">
        {/* TODO(F-10): persist rights via admin API. */}
        <PrimaryButton disabled={!user}>Save</PrimaryButton>
        <SecondaryButton onClick={() => navigate(ROUTES.adminUserList.path)}>
          Cancel
        </SecondaryButton>
      </div>
    </section>
  );
}
