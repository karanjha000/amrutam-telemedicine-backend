import { useCallback, useEffect, useState } from 'react';
import { api, ApiError } from '../lib/api';
import type { BookingResult, Consultation, Page, Prescription } from '../types';
import { BookingFlow } from '../components/BookingFlow';
import {
  ErrorNote,
  fmtDateTime,
  Spinner,
  StatusBadge,
  SectionCard
} from '../components/ui';

/** Fetches the prescription for a consultation, or null when none exists. */
async function fetchPrescription(consultationId: string): Promise<Prescription | null> {
  try {
    return await api<Prescription>(`/api/v1/consultations/${consultationId}/prescription`);
  } catch {
    // 404 = no prescription yet; the backend also maps any miss to 404.
    return null;
  }
}

export default function PatientDashboard(): JSX.Element {
  const [consultations, setConsultations] = useState<Consultation[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [prescriptions, setPrescriptions] = useState<Record<string, Prescription | null>>({});
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
      setConsultations([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const onBooked = (result: BookingResult): void => {
    // The new consultation shows up in the list on refresh.
    void load();
    void result; // acknowledged: the BookingFlow already shows the result
  };

  const togglePrescription = async (consultationId: string): Promise<void> => {
    if (showPrescription === consultationId) {
      setShowPrescription(null);
      return;
    }
    setShowPrescription(consultationId);
    if (prescriptions[consultationId] === undefined) {
      const rx = await fetchPrescription(consultationId);
      setPrescriptions((prev) => ({ ...prev, [consultationId]: rx }));
    }
  };

  return (
    <div className="dashboard">
      <h1>Patient dashboard</h1>

      <SectionCard title="Book a doctor">
        <BookingFlow onBooked={onBooked} />
      </SectionCard>

      <SectionCard title="My consultations">
        {error !== null ? <ErrorNote error={error} onRetry={() => void load()} /> : null}
        {loading ? (
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
                    {prescriptions[c.id] === undefined ? (
                      <Spinner label="Loading…" />
                    ) : prescriptions[c.id] === null ? (
                      <p className="muted">No prescription recorded for this consultation yet.</p>
                    ) : (
                      <>
                        {prescriptions[c.id]?.medications !== null &&
                        prescriptions[c.id]?.medications !== undefined ? (
                          <p>
                            <strong>Medications:</strong> {prescriptions[c.id]?.medications}
                          </p>
                        ) : null}
                        {prescriptions[c.id]?.dosageNotes !== null &&
                        prescriptions[c.id]?.dosageNotes !== undefined ? (
                          <p>
                            <strong>Notes:</strong> {prescriptions[c.id]?.dosageNotes}
                          </p>
                        ) : null}
                        <p className="muted">
                          Issued {fmtDateTime(prescriptions[c.id]?.createdAt ?? new Date().toISOString())}
                        </p>
                      </>
                    )}
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

function CancelButton({ consultationId, onDone }: { consultationId: string; onDone: () => void }) {
  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);

  const cancel = async (): Promise<void> => {
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
        className="btn btn--ghost"
        disabled={cancelling}
        onClick={() => void cancel()}
      >
        {cancelling ? 'Cancelling…' : 'Cancel'}
      </button>
      {cancelError !== null ? (
        <span className="form-error" role="alert">
          {cancelError}
        </span>
      ) : null}
    </span>
  );
}
