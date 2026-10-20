import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { api, ApiError } from '../lib/api';
import type { Doctor, Page, Slot, SlotCreateInput } from '../types';
import { ErrorNote, fmtDateTime, Spinner, StatusBadge } from './ui';

/**
 * Doctor-side availability management. The REST surface exposes only slot
 * CREATION (POST /doctors/{id}/availability, with a 409 guard on overlapping
 * windows) and listing; the backend has no slot DELETE/CANCEL endpoint in
 * this scope, so the UI supports adding slots and shows the full window.
 */
export function SlotsManager({ doctor }: { doctor: Doctor }): ReactNode {
  const [slots, setSlots] = useState<Slot[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [createdCount, setCreatedCount] = useState(0);

  const windowParams = useCallback((): { from: string; to: string } => {
    const from = new Date();
    const to = new Date();
    to.setDate(to.getDate() + 30);
    return { from: from.toISOString(), to: to.toISOString() };
  }, []);

  const load = useCallback(async (): Promise<void> => {
    setError(null);
    try {
      const { from, to } = windowParams();
      const page = await api<Page<Slot>>(`/api/v1/doctors/${doctor.id}/availability`, {
        query: { from, to, page: 0, size: 100 }
      });
      setSlots(page.content);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load slots');
      setSlots([]);
    }
  }, [doctor.id, windowParams]);

  useEffect(() => {
    void load();
  }, [load]);

  const addSlot = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    if (start === '' || end === '') {
      return;
    }
    setCreating(true);
    setError(null);
    try {
      const body: SlotCreateInput = { startAt: start, endAt: end };
      await api<Slot>(`/api/v1/doctors/${doctor.id}/availability`, {
        method: 'POST',
        body
      });
      setStart('');
      setEnd('');
      setCreatedCount((n) => n + 1);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to create slot');
    } finally {
      setCreating(false);
    }
  };

  const futureSlots = (slots ?? [])
    .filter((s) => new Date(s.startAt).getTime() >= Date.now() - 60_000)
    .sort((a, b) => a.startAt.localeCompare(b.startAt));

  return (
    <div className="slots-manager">
      {createdCount > 0 ? (
        <p className="form-ok" role="status">
          {createdCount} slot{createdCount === 1 ? '' : 's'} added.
        </p>
      ) : null}
      {error !== null ? <ErrorNote error={error} onRetry={() => void load()} /> : null}

      <form className="slots-add" onSubmit={addSlot} aria-label="Add availability slot">
        <label>
          Start
          <input
            type="datetime-local"
            required
            value={start}
            onChange={(ev) => setStart(ev.target.value)}
          />
        </label>
        <label>
          End
          <input
            type="datetime-local"
            required
            value={end}
            onChange={(ev) => setEnd(ev.target.value)}
          />
        </label>
        <button type="submit" className="btn btn--primary" disabled={creating}>
          {creating ? 'Adding…' : 'Add slot'}
        </button>
      </form>

      {slots === null ? (
        <Spinner label="Loading slots…" />
      ) : futureSlots.length === 0 ? (
        <p className="muted">No upcoming slots. Add one above.</p>
      ) : (
        <ul className="slot-table" aria-label="Upcoming slots">
          {futureSlots.map((s) => (
            <li key={s.id} className="slot-table__row">
              <span>
                {fmtDateTime(s.startAt)} – {fmtDateTime(s.endAt)}
              </span>
              <StatusBadge status={s.status} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
