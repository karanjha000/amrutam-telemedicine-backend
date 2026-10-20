import { useCallback, useEffect, useState, type ReactNode } from 'react';
import { api, ApiError } from '../lib/api';
import { DailyConsultationsChart } from '../components/DailyConsultationsChart';
import { ErrorNote, fmtMoney, Spinner, SectionCard } from '../components/ui';
import type {
  DailyConsultations,
  Doctor,
  Page,
  RevenueSummary,
  SystemHealth
} from '../types';

function todayIso(): string {
  const d = new Date();
  return isoDate(d);
}

function daysAgoIso(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return isoDate(d);
}

function isoDate(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export default function AdminDashboard(): ReactNode {
  const [date, setDate] = useState<string>(todayIso());
  const [daily, setDaily] = useState<DailyConsultations | null>(null);
  const [dailyError, setDailyError] = useState<string | null>(null);
  const [revenue, setRevenue] = useState<RevenueSummary | null>(null);
  const [revenueError, setRevenueError] = useState<string | null>(null);
  const [health, setHealth] = useState<SystemHealth | null>(null);
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [busy, setBusy] = useState(true);

  const loadDaily = useCallback(async (day: string): Promise<void> => {
    setDailyError(null);
    try {
      const body = await api<DailyConsultations>('/api/v1/admin/analytics/daily-consultations', {
        method: 'POST',
        body: { date: day }
      });
      setDaily(body);
    } catch (err) {
      setDailyError(err instanceof ApiError ? err.message : 'Failed to load daily analytics');
      setDaily(null);
    }
  }, []);

  const loadRevenue = useCallback(async (): Promise<void> => {
    setRevenueError(null);
    try {
      const body = await api<RevenueSummary>('/api/v1/admin/analytics/revenue-summary', {
        method: 'POST',
        body: { from: daysAgoIso(30), to: todayIso() }
      });
      setRevenue(body);
    } catch (err) {
      setRevenueError(err instanceof ApiError ? err.message : 'Failed to load revenue');
      setRevenue(null);
    }
  }, []);

  const loadHealth = useCallback(async (): Promise<void> => {
    try {
      const body = await api<SystemHealth>('/api/v1/admin/analytics/health', {
        method: 'POST',
        body: {}
      });
      setHealth(body);
    } catch {
      setHealth(null);
    }
  }, []);

  const loadDoctors = useCallback(async (): Promise<void> => {
    try {
      const page = await api<Page<Doctor>>('/api/v1/doctors', {
        query: { page: 0, size: 100 }
      });
      setDoctors(page.content);
    } catch {
      setDoctors([]);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    const run = async (): Promise<void> => {
      setBusy(true);
      await Promise.all([loadDaily(date), loadRevenue(), loadHealth(), loadDoctors()]);
      if (!cancelled) {
        setBusy(false);
      }
    };
    void run();
    return () => {
      cancelled = true;
    };
  }, [date, loadDaily, loadRevenue, loadHealth, loadDoctors]);

  const doctorNames = Object.fromEntries(doctors.map((d) => [d.id, d.name]));

  const utilization =
    daily !== null && doctors.length > 0
      ? Math.round((Object.keys(daily.perDoctor).length / doctors.length) * 100)
      : null;

  return (
    <div className="dashboard">
      <h1>Admin dashboard</h1>

      <SectionCard
        title="Daily consultations"
        actions={
          <label className="date-pick">
            <input
              type="date"
              value={date}
              onChange={(ev) => setDate(ev.target.value)}
              aria-label="Select day"
            />
          </label>
        }
      >
        {busy ? (
          <Spinner label="Loading analytics…" />
        ) : dailyError !== null ? (
          <ErrorNote error={dailyError} onRetry={() => void loadDaily(date)} />
        ) : daily !== null ? (
          <DailyConsultationsChart data={daily} doctorNames={doctorNames} />
        ) : null}
      </SectionCard>

      <div className="stat-row">
        <SectionCard title="Revenue (last 30 days)">
          {revenueError !== null ? (
            <ErrorNote error={revenueError} onRetry={() => void loadRevenue()} />
          ) : revenue === null ? (
            <Spinner label="Loading…" />
          ) : (
            <div className="stat-tiles">
              <div className="stat-tile">
                <span className="stat-tile__value">
                  {fmtMoney(revenue.totalCaptured, revenue.currency)}
                </span>
                <span className="stat-tile__label">captured</span>
              </div>
              <div className="stat-tile">
                <span className="stat-tile__value">{revenue.capturedCount}</span>
                <span className="stat-tile__label">payments</span>
              </div>
            </div>
          )}
        </SectionCard>

        <SectionCard title="Doctor utilization">
          {utilization === null ? (
            <p className="muted">No analytics to compare against the doctor roster.</p>
          ) : (
            <div className="utilization">
              <div
                className="utilization__bar"
                role="progressbar"
                aria-valuenow={utilization}
                aria-valuemin={0}
                aria-valuemax={100}
              >
                <div className="utilization__fill" style={{ width: `${utilization}%` }} />
              </div>
              <p>
                {utilization}% of {doctors.length} doctor{doctors.length === 1 ? '' : 's'} had
                consultations on {date}.
              </p>
            </div>
          )}
        </SectionCard>

        <SectionCard title="System health">
          {health === null ? (
            <p className="muted">System health unavailable.</p>
          ) : (
            <ul className="health-list">
              <li>
                <span>Active sessions (24h)</span>
                <strong>{health.activeSessions}</strong>
              </li>
              <li>
                <span>Pending async jobs</span>
                <strong>{health.pendingAsyncJobs}</strong>
              </li>
              <li>
                <span>Dead letters</span>
                <strong>{health.deadLetters}</strong>
              </li>
            </ul>
          )}
        </SectionCard>
      </div>
    </div>
  );
}
