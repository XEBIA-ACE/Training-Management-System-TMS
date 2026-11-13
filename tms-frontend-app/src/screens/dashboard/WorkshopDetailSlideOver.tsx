import { Link } from 'react-router';
import { SlideOverPanel, StatusChip } from '../../components';
import type { MockWorkshop } from '../../mocks/mockData';
import { ROUTES } from '../../routes/routes';

/**
 * SCR-006 Workshop Detail / Requirement Summary Slide-over (F-06 drill-down).
 * TODO(SCR-006): history/audit snippet is [TBD]. Links to full Requirement / Allocation screens
 * must be gated by rights once RBAC exists (SCR-009).
 */
export function WorkshopDetailSlideOver({
  workshop,
  onClose,
}: {
  workshop: MockWorkshop | null;
  onClose: () => void;
}) {
  return (
    <SlideOverPanel
      open={workshop !== null}
      title={workshop?.title ?? ''}
      onClose={onClose}
      headerExtra={workshop && <StatusChip state={workshop.status} label={workshop.statusLabel} />}
    >
      {workshop && (
        <div className="tms-stack">
          <span className="tms-screen-id">SCR-006</span>
          <DetailSection
            title="Customer & SPOC"
            items={[
              ['Customer', workshop.customer],
              ['SPOC', workshop.spoc],
            ]}
          />
          <DetailSection
            title="OEM certification"
            items={[['Certification', workshop.oemCertification]]}
          />
          <DetailSection
            title="Schedule & delivery"
            items={[
              ['Dates / times', workshop.dateTime],
              ['Delivery mode', workshop.deliveryMode],
              ['Location', workshop.location],
              ['Participants', String(workshop.participants)],
            ]}
          />
          <DetailSection
            title="Trainer allocation"
            items={[
              ['Primary trainer', workshop.trainer],
              ['Co-trainer', workshop.coTrainer],
            ]}
          />
          <DetailSection title="Logistics and T&E" items={[['Handling', workshop.logistics]]} />
          <DetailSection title="History" items={[['Audit snippet', '[TBD]']]} />
          <div className="tms-row">
            <Link to={ROUTES.requirementList.path}>Open requirement</Link>
            <Link to={ROUTES.allocationWorkspace.path}>Open allocation</Link>
          </div>
        </div>
      )}
    </SlideOverPanel>
  );
}

function DetailSection({ title, items }: { title: string; items: [string, string][] }) {
  return (
    <section>
      <h3>{title}</h3>
      <dl style={{ margin: 0 }}>
        {items.map(([label, value]) => (
          <div key={label} className="tms-row">
            <dt className="tms-muted">{label}:</dt>
            <dd style={{ margin: 0 }}>{value}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
