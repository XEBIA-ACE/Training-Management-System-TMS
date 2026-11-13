import type { ReactNode } from 'react';

export interface TableColumn<Row> {
  key: string;
  header: string;
  render?: (row: Row) => ReactNode;
}

export interface TableProps<Row> {
  caption: string;
  columns: readonly TableColumn<Row>[];
  rows: readonly Row[];
  getRowId: (row: Row) => string;
  onRowClick?: (row: Row) => void;
  selectedRowId?: string | null;
  emptyState?: ReactNode;
}

/**
 * Table / Data Grid. States: default, row_hover, row_selected, header.
 * TODO: sorting, pagination / infinite scroll are listed in references/components.md but their
 * behavior is not specified yet; wire them when list screens are implemented.
 */
export function Table<Row>({
  caption,
  columns,
  rows,
  getRowId,
  onRowClick,
  selectedRowId,
  emptyState,
}: TableProps<Row>) {
  if (rows.length === 0 && emptyState) {
    return <div className="tms-table-wrap">{emptyState}</div>;
  }
  return (
    <div className="tms-table-wrap">
      <table className="tms-table">
        <caption className="tms-muted" style={{ textAlign: 'left', padding: 'var(--space-2)' }}>
          {caption}
        </caption>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} scope="col">
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const rowId = getRowId(row);
            const selected = selectedRowId === rowId;
            const className = [
              onRowClick ? 'tms-table__row--clickable' : '',
              selected ? 'tms-table__row--selected' : '',
            ]
              .filter(Boolean)
              .join(' ');
            return (
              <tr
                key={rowId}
                className={className || undefined}
                aria-selected={onRowClick ? selected : undefined}
                tabIndex={onRowClick ? 0 : undefined}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
                onKeyDown={
                  onRowClick
                    ? (event) => {
                        if (event.key === 'Enter' || event.key === ' ') {
                          event.preventDefault();
                          onRowClick(row);
                        }
                      }
                    : undefined
                }
              >
                {columns.map((column) => (
                  <td key={column.key}>
                    {column.render
                      ? column.render(row)
                      : String((row as Record<string, unknown>)[column.key] ?? '')}
                  </td>
                ))}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
