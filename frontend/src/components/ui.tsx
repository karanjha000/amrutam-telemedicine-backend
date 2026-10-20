import type { ReactNode } from 'react';

/** A small loading indicator used while a query is in flight. */
export function Spinner({ label = 'Loading…' }: { label?: string }): ReactNode {
  return (
    <div className="spinner" role="status" aria-live="polite">
      <span className="spinner__dot" aria-hidden="true" />
      {label}
    </div>
  );
}

/** Renders an ApiError-style message for a page section. */
export function ErrorNote({ error, onRetry }: { error: unknown; onRetry?: () => void }): ReactNode {
  const message =
    error instanceof Error ? error.message : error === null || error === undefined ? 'Unknown error' : String(error);
  return (
    <div className="error-note" role="alert">
      <p>{message}</p>
      {onRetry !== undefined ? (
        <button type="button" className="btn btn--ghost" onClick={onRetry}>
          Retry
        </button>
      ) : null}
    </div>
  );
}

export function SectionCard({
  title,
  actions,
  children
}: {
  title: string;
  actions?: ReactNode;
  children: ReactNode;
}): ReactNode {
  return (
    <section className="card">
      <div className="card__head">
        <h2 className="card__title">{title}</h2>
        {actions !== undefined ? <div className="card__actions">{actions}</div> : null}
      </div>
      <div className="card__body">{children}</div>
    </section>
  );
}

export function StatusBadge({ status }: { status: string }): ReactNode {
  const cls = 'badge badge--' + status.toLowerCase();
  return <span className={cls}>{status}</span>;
}

export function fmtDateTime(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) {
    return iso;
  }
  return d.toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short'
  });
}

export function fmtMoney(amount: number | null, currency = 'INR'): string {
  if (amount === null) {
    return '—';
  }
  return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(amount);
}
