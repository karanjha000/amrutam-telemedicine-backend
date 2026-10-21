import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, ApiError } from '../lib/api';
import type { Doctor, Page } from '../types';
import { useAuth } from '../context/AuthContext';
import { ErrorNote, fmtMoney, Spinner, SectionCard } from '../components/ui';

export default function DoctorsPage(): JSX.Element {
  const { user } = useAuth();
  const [query, setQuery] = useState('');
  const [doctors, setDoctors] = useState<Doctor[] | null>(null);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Each request gets an id; only the latest one may update state, so a slow
  // response for an old search can never overwrite the results of a newer one.
  const latestRequest = useRef(0);

  const load = useCallback(async (q: string): Promise<void> => {
    const requestId = ++latestRequest.current;
    setLoading(true);
    setError(null);
    try {
      const page = await api<Page<Doctor>>('/api/v1/doctors', {
        query: { page: 0, size: 50, ...(q.trim() !== '' ? { q: q.trim() } : {}) }
      });
      if (requestId !== latestRequest.current) {
        return;
      }
      setDoctors(page.content);
      setTotal(page.totalElements);
    } catch (err) {
      if (requestId !== latestRequest.current) {
        return;
      }
      setError(err instanceof ApiError ? err.message : 'Failed to load doctors');
      setDoctors((prev) => prev ?? []);
    } finally {
      if (requestId === latestRequest.current) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    const t = setTimeout(() => void load(query), 300);
    return () => clearTimeout(t);
  }, [query, load]);

  const isPatient = user !== null && user.role === 'PATIENT';
  // Full-page spinner only before the first results; later searches keep the list visible.
  const firstLoad = loading && doctors === null;

  return (
    <div className="dashboard">
      <h1>Doctors</h1>
      <SectionCard
        title={`Results (${total})`}
        actions={
          <input
            type="search"
            className="search-input"
            placeholder="Search by name or specialty…"
            value={query}
            onChange={(ev) => setQuery(ev.target.value)}
            aria-label="Search doctors"
          />
        }
      >
        {error !== null ? <ErrorNote error={error} onRetry={() => void load(query)} /> : null}
        {firstLoad ? (
          <Spinner label="Searching…" />
        ) : (doctors ?? []).length === 0 ? (
          <p className="muted">No doctors match your search.</p>
        ) : (
          <>
            {loading ? <Spinner label="Updating results…" /> : null}
            <ul className="doctor-cards" aria-busy={loading}>
              {(doctors ?? []).map((d) => (
                <li key={d.id} className="doctor-card">
                  <div className="doctor-card__head">
                    <span className="doctor-card__name">{d.name}</span>
                    <span className="muted">
                      {d.totalRatings > 0 ? `★ ${d.ratings.toFixed(1)} (${d.totalRatings})` : 'New'}
                    </span>
                  </div>
                  <p className="doctor-card__meta">
                    {d.specialization} · {d.yearsExperience}y experience ·{' '}
                    {fmtMoney(d.consultationFee)} / consultation
                  </p>
                  {d.bio !== null && d.bio !== undefined && d.bio.length > 0 ? (
                    <p className="doctor-card__bio">{d.bio}</p>
                  ) : null}
                  <div className="doctor-card__actions">
                    {isPatient ? (
                      <Link className="btn btn--primary" to="/patient">
                        Book with {d.name}
                      </Link>
                    ) : user === null ? (
                      <span className="muted">
                        <Link to="/login">Sign in</Link> as a patient to book.
                      </span>
                    ) : (
                      <span className="muted">Only patient accounts can book.</span>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          </>
        )}
      </SectionCard>
    </div>
  );
}