import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode
} from 'react';
import { api, gql, ApiError } from '../lib/api-internal';
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
export default function DoctorDashboard(): ReactNode {
  const { user } = useAuth();
  const [doctor, setDoctor] = useState<Doctor | null>(null);
  const [doctorError, setDoctorError] = useState<string | null>(null);
  const [consultations, setConsultations] = useState<Consultation[] | null>(null);
  const [consultError, setConsultError] = useState<string | null>(null);
  const [completing, setCompleting] = useState<string | null>(null);
  const [completeErrors, setCompleteErrors] = useState<Record<string, string | null>>({});
  const [rxOpen, setRxOpen] = useState<string | null>(null);
  const [rx, setRx] = useState<Record<string, Prescription | null>>({});

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
      // The REST list endpoint is patient-scoped; there is no
      // doctor-scoped listing in this scope. Query with the doctor's linked
      // userId so we at least surface consultations this account can see,
      // and show a hint when the result is empty.
      const page = await api<Page<Consultation>>('/api/v1/consultations', {
        query: { page: 0, size: 50, doctorId: doctor.userId }
      });
      setConsultations(page.content);
      if (page.content.length === 0) {
        setConsultError(
          'No consultations found for your linked profile in this scope. Doctor-scoped consultation listing is a future backend step; your slot and completion actions remain available once a booking references you.'
        );
      }
    } catch (err) {
      setConsultations([]);
      setConsultError(
        err instanceof ApiError
          ? `Consultation listing is not available in this scope (${err.code}): ${err.message}`
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
    if (rx[id] === undefined) {
      let value: Prescription | null = null;
      try {
        value = await api<Prescription>(`/api/v1/consultations/${id}/prescription`);
      } catch {
        value = null; // no prescription exists yet
      }
      setRx((prev) => ({ ...prev, [id]: value }));
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
        {consultations === null ? (
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
                  <button
                    type="button"
                    className="btn btn--primary"
                    disabled={completing === c.id}
                    onClick={() => void completeConsultation(c.id)}
                  >
                    {completing === c.id ? 'Completing…' : 'Complete'}
                  </button>
                  <button
                    type="button"
                    className="btn btn--ghost"
                    onClick={() => void showPrescription(c.id)}
                  >
                    {rxOpen === c.id ? 'Hide prescription' : 'Prescription'}
                  </button>
                  {completeErrors[c.id] !== undefined && completeErrors[c.id] !== null ? (
                    <span className="form-error" role="alert">
                      {completeErrors[c.id]}
                    </span>
                  ) : null}
                </div>
                {rxOpen === c.id ? (
                  <div className="prescription-box">
                    {rx[c.id] === undefined ? (
                      <Spinner label="Loading…" />
                    ) : rx[c.id] === null ? (
                      <p className="muted">
                        No prescription recorded for this consultation yet.
                        Prescriptions are issued after the consultation is
                        completed (write path is a future backend step).
                      </p>
                    ) : (
                      <>
                        {rx[c.id]?.medications ? <p>{rx[c.id]?.medications}</p> : null}
                        {rx[c.id]?.dosageNotes ? <p>{rx[c.id]?.dosageNotes}</p> : null}
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
