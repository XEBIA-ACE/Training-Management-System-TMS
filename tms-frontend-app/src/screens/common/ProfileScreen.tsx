import { EmptyState } from '../../components';
import { ROUTES } from '../../routes/routes';
import { ScreenHeader } from './ScreenHeader';

/** My Profile (navigation.md maps it to SCR-003). TODO(SCR-003): profile details are [TBD]. */
export function ProfileScreen() {
  return (
    <section>
      <ScreenHeader route={ROUTES.profile} />
      <div className="tms-card">
        <EmptyState
          title="Profile details [TBD]"
          description="Defined by a later story (SCR-003)."
        />
      </div>
    </section>
  );
}
