import type { ReactNode } from 'react';
import type { ScreenRoute } from '../../routes/routes';

export function ScreenHeader({ route, actions }: { route: ScreenRoute; actions?: ReactNode }) {
  return (
    <div className="tms-page-header">
      <div>
        <h1>{route.title}</h1>
        <span className="tms-screen-id">{route.screenId}</span>
      </div>
      {actions && <div className="tms-row">{actions}</div>}
    </div>
  );
}
