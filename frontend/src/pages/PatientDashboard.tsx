import { useCallback, useEffect, useState } from 'react';
import { api, ApiError } from '../lib/api';
import type { Consultation, Page, Prescription } from '../types';
import { BookingFlow } from '../components/BookingFlow';
import {
  ErrorNote,
  fmtDateTime,
  Spinner,
  StatusBadge,
  SectionCard
} from '../components/ui';

/** undefined = not loaded yet, null = none exists, 'error' = the request itself failed. */
type RxState = Prescription | null | 'error' | undefined;

/**
 * Fetches the prescription for a consultation.
 * Returns null when none exists (the backend maps any miss to an API error / 404);
 * anything that is not an ApiError (network down, etc.) is rethrown so the UI can say so.
 */
async function fetchPrescription(consultationId: string): Promise<Prescription | null> {
  try {
    return await api<Prescription>(`/api/v1/consultations/${consultationId}/prescription`);
  } catch (err) {
    if (err instanceof ApiError) {
      return null;
    }
    throw err;
  }
}

export default function PatientDashboard(): JSX.Element {
  const [consultations, setConsultations] = useState<Consultation[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [prescriptions, setPrescriptions] = useState<Record<string, RxState>>({});
  const [showPrescription, setShowPrescription] = useState<string | null>(null);

  const load = useCallback(async (): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const page = await api<Page<Consultation>>('/api/v1/consultations', {
        query: { page: 0, size: 50 }
      });
      setConsultations(page.content);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load consultations');
      setConsultations((prev) => prev ?? []);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const togglePrescription = async (consultationId: string): Promise<void> => {
    if (showPrescription === consultationId) {
      setShowPrescription(null);
      return;
    }
    setShowPrescription(consultationId);
    const current = prescriptions[consultationId];
    if (current === undefined || current === 'error') {
      try {
        const rx = await fetchPrescription(consultationId);
        setPrescriptions((prev) => ({ ...prev, [consultationId]: rx }));
      } catch {
        setPrescriptions((prev) => ({ ...prev, [consultationId]: 'error' }));
      }
    }
  };

  // Only show the full-page spinner on the first load; refreshes keep the list on screen.
  const firstLoad = loading && consultations === null;

  return (
    <div className="dashboard">
      <h1>Patient dashboard</h1>

      <SectionCard title="Book a doctor">
        {/* The new consultation appears in the list after the refresh. */}
        <BookingFlow onBooked={() => void load()} />
      </SectionCard>

      <SectionCard title="My consultations">
        {error !== null ? <ErrorNote error={error} onRetry={() => void load()} /> : null}
        {firstLoad ? (
          <Spinner label="Loading consultations…" />
        ) : (consultations ?? []).length === 0 ? (
          <p className="muted">No consultations yet — book one above.</p>
        ) : (
          <ul className="consultation-list">
            {(consultations ?? []).map((c) => (
              <li key={c.id} className="consultation-list__item">
                <div className="consultation-list__top">
                  <span className="consultation-list__when">{fmtDateTime(c.scheduledAt)}</span>
                  <StatusBadge status={c.status} />
                </div>
                <code className="consultation-list__id">{c.id}</code>
                <div className="consultation-list__actions">
                  <button
                    type="button"
                    className="btn btn--ghost"
                    aria-expanded={showPrescription === c.id}
                    onClick={() => void togglePrescription(c.id)}
                  >
                    {showPrescription === c.id ? 'Hide prescription' : 'Prescription'}
                  </button>
                  {c.status !== 'CANCELLED' && c.status !== 'COMPLETED' ? (
                    <CancelButton consultationId={c.id} onDone={() => void load()} />
                  ) : null}
                </div>
                {showPrescription === c.id ? (
                  <div className="prescription-box">
                    <PrescriptionBox state={prescriptions[c.id]} />
                  </div>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </SectionCard>
    </div>
  );
}

function PrescriptionBox({ state }: { state: RxState }): JSX.Element {
  if (state === undefined) {
    return <Spinner label="Loading…" />;
  }
  if (state === 'error') {
    return (
      <p className="form-error" role="alert">
        Could not load the prescription. Hide it and try again.
      </p>
    );
  }
  if (state === null) {
    return <p className="muted">No prescription recorded for this consultation yet.</p>;
  }
  return (
    <>
      {state.medications ? (
        <p>
          <strong>Medications:</strong> {state.medications}
        </p>
      ) : null}
      {state.dosageNotes ? (
        <p>
          <strong>Notes:</strong> {state.dosageNotes}
        </p>
      ) : null}
      {state.createdAt ? <p className="muted">Issued {fmtDateTime(state.createdAt)}</p> : null}
    </>
  );
}

function CancelButton({ consultationId, onDone }: { consultationId: string; onDone: () => void }) {
  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);

  const cancel = async (): Promise<void> => {
    if (!window.confirm('Cancel this consultation? This cannot be undone.')) {
      return;
    }
    setCancelling(true);
    setCancelError(null);
    try {
      await api<null>(`/api/v1/consultations/${consultationId}/cancel`, { method: 'POST' });
      onDone();
    } catch (err) {
      setCancelError(err instanceof ApiError ? err.message : 'Cancellation failed');
    } finally {
      setCancelling(false);
    }
  };

  return (
    <span>
      <button
        type="button"
        className="btn btn--danger"
        disabled={cancelling}
        onClick={() => void cancel()}
      >
        {cancelling ? 'Cancelling…' : 'Cancel'}
      </button>
      {cancelError !== null ? (
        <span className="form-error" role="alert">
          {' '}
          {cancelError}
        </span>
      ) : null}
    </span>
  );
}