import { EmptyState } from '../../components';
import type { ScreenRoute } from '../../routes/routes';
import { ScreenHeader } from './ScreenHeader';

/**
 * Placeholder for screens referenced by the primary navigation (references/navigation.md) whose
 * screen spec is not part of SCAFFOLD-frontend-app. TODO: implement in the owning feature story.
 */
export function ScreenPlaceholder({ route }: { route: ScreenRoute }) {
  return (
    <section>
      <ScreenHeader route={route} />
      <div className="tms-card">
        <EmptyState
          title="Not yet implemented"
          description={`${route.screenId} has no exported screen specification in this scaffold.`}
        />
      </div>
    </section>
  );
}
