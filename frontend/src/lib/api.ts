/**
 * Minimal REST client for the Amrutam backend.
 *
 * - All paths are relative to the API base (default: same-origin via the
 *   Vite dev proxy in dev, or the nginx proxy in the Docker build).
 * - JWT is persisted in localStorage and attached to every request.
 * - A 401 triggers one automatic refresh via POST /api/v1/auth/refresh, then
 *   the original request is retried. The "refreshing" promise is memoized so
 *   concurrent 401s produce exactly one refresh call.
 * - POST/PUT/PATCH/DELETE writes carry an X-Idempotency-Key (random UUID)
 *   unless the caller supplies their own.
 * - Every response is unwrapped from the { success, data, error, traceId }
 *   envelope; failures throw ApiError carrying the backend error code.
 */

import type { Envelope } from '../types';

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly traceId: string | null;

  constructor(status: number, code: string, message: string, traceId: string | null = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.traceId = traceId;
  }

  /** True for network-level failures (no response received). */
  get isNetworkError(): boolean {
    return this.status === 0;
  }
}

// Note on refresh semantics: the backend's POST /auth/refresh accepts the
// still-valid *access* token and re-issues a new one ({ token } -> string).
// It does not issue a separate long-lived refresh token at login. We store
// the access token under both keys so "refreshToken" can drive the 401 retry:
// the refresh attempt works while the token is still valid (or within clock
// skew), and silently fails (surfacing a 401 to the caller) once it has
// truly expired.
const ACCESS_KEY = 'amrutam.accessToken';
const REFRESH_KEY = 'amrutam.refreshToken';

function read(key: string): string | null {
  try {
    return window.localStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key: string, value: string | null): void {
  try {
    if (value === null) {
      window.localStorage.removeItem(key);
    } else {
      window.localStorage.setItem(key, value);
    }
  } catch {
    // localStorage unavailable (SSR/private mode) — the app still works
    // per-request, it just cannot persist the session.
  }
}

type JwtStore = {
  accessToken: string | null;
  refreshToken: string | null;
  set(accessToken: string): void;
  clear(): void;
};

export const jwtStore: JwtStore = {
  get accessToken(): string | null {
    return read(ACCESS_KEY);
  },
  get refreshToken(): string | null {
    return read(REFRESH_KEY);
  },
  set(accessToken: string): void {
    write(ACCESS_KEY, accessToken);
    write(REFRESH_KEY, accessToken);
  },
  clear(): void {
    write(ACCESS_KEY, null);
    write(REFRESH_KEY, null);
  }
};

function newIdempotencyKey(): string {
  const c = globalThis.crypto;
  if (c?.randomUUID) {
    return c.randomUUID();
  }
  // Fallback for contexts without crypto.randomUUID: v4-shaped UUID.
  const bytes = new Uint8Array(16);
  c?.getRandomValues?.(bytes);
  for (let i = 0; i < 16; i += 1) {
    if (bytes[i] === undefined) bytes[i] = Math.floor(Math.random() * 256);
  }
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-4${hex.slice(9, 12)}-8${hex.slice(13, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

export interface RequestOptions {
  method?: string;
  body?: unknown;
  headers?: Record<string, string>;
  /** Override the generated idempotency key (e.g. to force an exact replay). */
  idempotencyKey?: string;
  query?: Record<string, string | number | undefined | null>;
  signal?: AbortSignal;
}

const WRITE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

function resolveBase(): string {
  const base = import.meta.env.VITE_API_BASE_URL ?? '/';
  // A relative base (empty or '/') means "same origin" — the dev proxy or
  // the nginx proxy takes over. A full URL bypasses the proxy.
  return base === '' ? '/' : base;
}

export function buildUrl(path: string, query?: RequestOptions['query']): string {
  const base = resolveBase();
  const normalizedBase = base.endsWith('/') ? base.slice(0, -1) : base;
  let url = `${normalizedBase}${path.startsWith('/') ? path : `/${path}`}`;
  if (query) {
    const params = new URLSearchParams();
    for (const [k, v] of Object.entries(query)) {
      if (v !== undefined && v !== null) {
        params.set(k, String(v));
      }
    }
    const qs = params.toString();
    if (qs.length > 0) {
      url += `?${qs}`;
    }
  }
  return url;
}

function unwrap<T>(env: Envelope<T>): T {
  if (env.success && env.data !== null && env.data !== undefined) {
    return env.data;
  }
  const err = env.error;
  throw new ApiError(
    0,
    err?.code ?? 'UNKNOWN',
    err?.message ?? 'Request failed: the API returned success=false without a usable payload',
    env.traceId
  );
}

let refreshing: Promise<string> | null = null;

/**
 * Refreshes the access token using the stored token. Returns the new access
 * token. Memoized: concurrent callers await the same promise, and the memo
 * is cleared afterwards so the next failure re-refreshes.
 */
export async function refreshAccessToken(): Promise<string> {
  if (refreshing === null) {
    refreshing = (async () => {
      try {
        const token = jwtStore.refreshToken;
        if (token === null) {
          throw new ApiError(401, 'UNAUTHORIZED', 'No refresh token available');
        }
        const res = await fetch(buildUrl('/api/v1/auth/refresh'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ token })
        });
        const env = (await res.json().catch(() => null)) as Envelope<string> | null;
        if (!res.ok || env === null) {
          const errBody = env?.error;
          throw new ApiError(
            res.status,
            errBody?.code ?? 'UNAUTHORIZED',
            errBody?.message ?? 'Token refresh failed',
            env?.traceId ?? null
          );
        }
        const newToken = unwrap(env);
        jwtStore.set(newToken);
        return newToken;
      } finally {
        refreshing = null;
      }
    })();
  }
  return refreshing;
}

async function doFetch<T>(url: string, init: RequestInit, retry: boolean): Promise<T> {
  const headers: Record<string, string> = { ...(init.headers as Record<string, string> | undefined) };

  const access = jwtStore.accessToken;
  if (access !== null) {
    headers.Authorization = `Bearer ${access}`;
  }

  let res: Response;
  try {
    res = await fetch(url, { ...init, headers });
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Failed to reach the Amrutam API', null);
  }

  if (res.status === 401 && !retry && jwtStore.refreshToken !== null) {
    try {
      await refreshAccessToken();
    } catch {
      // Refresh failed — fall through: surface the original 401.
    }
    if (jwtStore.accessToken !== null) {
      // Retry exactly once with the fresh token.
      return doFetch<T>(url, init, true);
    }
  }

  // Parse the body even for error statuses — the envelope carries the
  // machine-readable error code on failure.
  const text = await res.text();
  let env: Envelope<T> | null = null;
  if (text.length > 0) {
    try {
      env = JSON.parse(text) as Envelope<T>;
    } catch {
      env = null;
    }
  }

  if (!res.ok || env === null || env.success === false) {
    const errBody = env?.error ?? null;
    throw new ApiError(
      res.status,
      errBody?.code ?? 'HTTP_' + res.status,
      errBody?.message ?? res.statusText,
      env?.traceId ?? null
    );
  }

  return unwrap<T>(env as Envelope<T>);
}

/** Performs an authenticated REST request and unwraps the envelope. */
export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, headers, idempotencyKey, query, signal } = options;
  const init: RequestInit = { method, signal, headers: { ...headers } };
  if (body !== undefined) {
    (init.headers as Record<string, string>)['Content-Type'] =
      (init.headers as Record<string, string>)['Content-Type'] ?? 'application/json';
    init.body = typeof body === 'string' ? body : JSON.stringify(body);
  }
  // Auto-generate the idempotency key on first execution. Retries after a
  // 401-while-refreshing reuse the same key: a replay of the same intent is
  // exactly what the backend's idempotency store is designed for.
  if (WRITE_METHODS.has(method.toUpperCase()) && idempotencyKey === undefined) {
    (init.headers as Record<string, string>)['X-Idempotency-Key'] = newIdempotencyKey();
  } else if (WRITE_METHODS.has(method.toUpperCase()) && idempotencyKey !== undefined) {
    (init.headers as Record<string, string>)['X-Idempotency-Key'] = idempotencyKey;
  }

  return doFetch<T>(buildUrl(path, query), init, false);
}
