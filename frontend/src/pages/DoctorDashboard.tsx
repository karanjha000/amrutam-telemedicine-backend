import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, ApiError } from '../lib/api';
import { gql } from '../lib/graphql';
import { SlotsManager } from '../components/SlotsManager';
import {
  ErrorNote,
  fmtDateTime,
  Spinner,
  StatusBadge,
  SectionCard
} from '../components/ui';
import { useAuth } from '../context/AuthContext';
import type { Consultation, Doctor, Page, Prescription } from '../types';

interface GqlDoctorRow {
  id: string;
  userId: string;
  name: string | null;
  specialization: string;
  consultationFee: number | null;
  yearsExperience: number;
  bio: string | null;
}

/** undefined = not loaded yet, null = none exists, 'error' = the request itself failed. */
type RxState = Prescription | null | 'error' | undefined;

const MY_DOCTOR_QUERY = `
  query MyDoctor {
    doctors {
      id
      userId
      name
      specialization
      consultationFee
      yearsExperience
      bio
    }
  }
`;

/**
 * Doctor-facing dashboard:
 *  - resolve the linked doctor row via GraphQL (my userId -> doctor)
 *  - manage availability slots
 *  - list consultations and complete them
 *
 * Prescription creation is NOT exposed by the backend REST surface in this
 * scope (only GET /consultations/{id}/prescription; the backend flags the
 * write path as a future step). The doctor flow therefore stops at "mark
 * complete" and shows the prescription read-only when one exists.
 */
export default function DoctorDashboard(): JSX.Element {
  const { user } = useAuth();
  const [doctor, setDoctor] = useState<Doctor | null>(null);
  const [doctorError, setDoctorError] = useState<string | null>(null);
  const [consultations, setConsultations] = useState<Consultation[] | null>(null);
  const [consultError, setConsultError] = useState<string | null>(null);
  const [completing, setCompleting] = useState<string | null>(null);
  const [completeErrors, setCompleteErrors] = useState<Record<string, string | null>>({});
  const [rxOpen, setRxOpen] = useState<string | null>(null);
  const [rx, setRx] = useState<Record<string, RxState>>({});

  const resolveDoctor = useCallback(async (): Promise<void> => {
    if (user === null) {
      return;
    }
    setDoctorError(null);
    try {
      const res = await gql<{ doctors: GqlDoctorRow[] }>(MY_DOCTOR_QUERY);
      const match = (res.data?.doctors ?? []).find((d) => d.userId === user.id);
      if (match === undefined) {
        setDoctorError('No doctor profile is linked to your account.');
        setDoctor(null);
      } else {
        const full = await api<Doctor>(`/api/v1/doctors/${match.id}`);
        setDoctor(full);
      }
    } catch (err) {
      setDoctorError(
        err instanceof ApiError ? err.message : 'Failed to resolve your doctor profile'
      );
      setDoctor(null);
    }
  }, [user]);

  useEffect(() => {
    void resolveDoctor();
  }, [resolveDoctor]);

  const loadConsultations = useCallback(async (): Promise<void> => {
    if (doctor === null) {
      return;
    }
    setConsultError(null);
    try {
      // The REST list endpoint is patient-scoped; there is no doctor-scoped
      // listing in this scope, so we query with the doctor's linked userId.
      const page = await api<Page<Consultation>>('/api/v1/consultations', {
        query: { page: 0, size: 50, doctorId: doctor.userId }
      });
      setConsultations(page.content);
    } catch (err) {
      setConsultations((prev) => prev ?? []);
      setConsultError(
        err instanceof ApiError
          ? `Could not load consultations (${err.code}): ${err.message}`
          : 'Failed to load consultations'
      );
    }
  }, [doctor]);

  useEffect(() => {
    void loadConsultations();
  }, [loadConsultations]);

  const upcoming = useMemo(
    () =>
      (consultations ?? [])
        .filter((c) => c.status === 'PAID' || c.status === 'PENDING_PAYMENT')
        .sort((a, b) => a.scheduledAt.localeCompare(b.scheduledAt)),
    [consultations]
  );

  const completeConsultation = async (id: string): Promise<void> => {
    if (!window.confirm('Mark this consultation as completed? This cannot be undone.')) {
      return;
    }
    setCompleting(id);
    setCompleteErrors((prev) => ({ ...prev, [id]: null }));
    try {
      await api<null>(`/api/v1/consultations/${id}/complete`, { method: 'POST' });
      await loadConsultations();
    } catch (err) {
      setCompleteErrors((prev) => ({
        ...prev,
        [id]: err instanceof ApiError ? err.message : 'Could not complete consultation'
      }));
    } finally {
      setCompleting(null);
    }
  };

  const showPrescription = async (id: string): Promise<void> => {
    if (rxOpen === id) {
      setRxOpen(null);
      return;
    }
    setRxOpen(id);
    const current = rx[id];
    if (current === undefined || current === 'error') {
      try {
        const value = await api<Prescription>(`/api/v1/consultations/${id}/prescription`);
        setRx((prev) => ({ ...prev, [id]: value }));
      } catch (err) {
        // ApiError = no prescription exists yet; anything else = the request failed.
        setRx((prev) => ({ ...prev, [id]: err instanceof ApiError ? null : 'error' }));
      }
    }
  };

  return (
    <div className="dashboard">
      <h1>Doctor dashboard</h1>

      {doctorError !== null ? (
        <ErrorNote error={doctorError} onRetry={() => void resolveDoctor()} />
      ) : null}
      {doctor !== null ? (
        <SectionCard title={`Availability — ${doctor.name}`}>
          <SlotsManager doctor={doctor} />
        </SectionCard>
      ) : null}

      <SectionCard title="Consultations to complete">
        {consultError !== null ? <p className="muted">{consultError}</p> : null}
        {doctor === null ? (
          // No linked profile (or still resolving): nothing to list, and no endless spinner.
          doctorError !== null ? (
            <p className="muted">Consultations will appear once your doctor profile is linked.</p>
          ) : (
            <Spinner label="Loading…" />
          )
        ) : consultations === null ? (
          <Spinner label="Loading…" />
        ) : upcoming.length === 0 ? (
          <p className="muted">No upcoming consultations.</p>
        ) : (
          <ul className="consultation-list">
            {upcoming.map((c) => (
              <li key={c.id} className="consultation-list__item">
                <div className="consultation-list__top">
                  <span>{fmtDateTime(c.scheduledAt)}</span>
                  <StatusBadge status={c.status} />
                </div>
                <code>{c.id}</code>
                <div className="consultation-list__actions">
                  {c.status === 'PAID' ? (
                    <button
                      type="button"
                      className="btn btn--primary"
                      disabled={completing === c.id}
                      onClick={() => void completeConsultation(c.id)}
                    >
                      {completing === c.id ? 'Completing…' : 'Complete'}
                    </button>
                  ) : (
                    <span className="muted">Awaiting payment</span>
                  )}
                  <button
                    type="button"
                    className="btn btn--ghost"
                    aria-expanded={rxOpen === c.id}
                    onClick={() => void showPrescription(c.id)}
                  >
                    {rxOpen === c.id ? 'Hide prescription' : 'Prescription'}
                  </button>
                  {completeErrors[c.id] ? (
                    <span className="form-error" role="alert">
                      {completeErrors[c.id]}
                    </span>
                  ) : null}
                </div>
                {rxOpen === c.id ? (
                  <div className="prescription-box">
                    <PrescriptionBox state={rx[c.id]} />
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
    return (
      <p className="muted">
        No prescription recorded for this consultation yet. Writing prescriptions is not
        available from this dashboard yet.
      </p>
    );
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
    </>
  );
}