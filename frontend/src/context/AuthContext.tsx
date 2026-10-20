/**
 * Authentication context: login / logout / register, role-aware protected
 * routes, and token persistence.
 *
 * The backend hands back a bare `role` name (PATIENT / DOCTOR / ADMIN);
 * ROLE_* authority strings are used by @PreAuthorize server-side. This
 * module centralizes that mapping so the UI never compares against the
 * raw authority strings.
 */

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode
} from 'react';
import { api, ApiError, jwtStore } from '../lib/api';
import { ROLE_PREFIX } from '../types';
import type {
  LoginInput,
  LoginResult,
  MfaVerifyInput,
  PatientDto,
  RegisterPatientInput,
  Role,
  User
} from '../types';

export const roleAuthority: Record<Role, string> = ROLE_PREFIX;

interface MfaPending {
  username: string;
  secret: string | null;
  provisioningUri: string | null;
}

interface AuthContextValue {
  user: User | null;
  token: string | null;
  /** True once the initial session restore (or its failure) has resolved. */
  ready: boolean;
  /** Present when login() returned an MFA challenge; cleared on verifyMfa/logout. */
  mfaPending: MfaPending | null;
  login(input: LoginInput): Promise<void>;
  /** Completes an MFA-challenged login with the 6-digit code. */
  verifyMfa(input: MfaVerifyInput): Promise<void>;
  /**
   * Registers a new patient (self-service). The backend returns the created
   * patient but does NOT issue a token; the UI then directs the user to log
   * in (or auto-login is up to the caller).
   */
  register(input: RegisterPatientInput): Promise<PatientDto>;
  logout(): void;
  /** Re-reads /api/v1/users/me to refresh the user object (e.g. after a profile update). */
  refreshUser(): Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [ready, setReady] = useState<boolean>(false);
  const [mfaPending, setMfaPending] = useState<MfaPending | null>(null);

  useEffect(() => {
    const token = jwtStore.accessToken;
    if (token === null) {
      setReady(true);
      return;
    }
    let cancelled = false;
    api<User>('/api/v1/users/me')
      .then((me) => {
        if (!cancelled) setUser(me);
      })
      .catch(() => {
        // A stale / expired token: clear it so the app starts signed out.
        jwtStore.clear();
        if (!cancelled) setUser(null);
      })
      .finally(() => {
        if (!cancelled) setReady(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (input: LoginInput): Promise<void> => {
    const result = await api<LoginResult>('/api/v1/auth/login', {
      method: 'POST',
      body: input
    });
    if (result.token === null && result.mfaChallenge !== null) {
      // TOTP enabled: hold the flow and ask the UI for the code.
      setMfaPending({
        username: input.username,
        secret: result.mfaChallenge.secret,
        provisioningUri: result.mfaChallenge.provisioningUri
      });
      return;
    }
    const token = result.token;
    if (token === null) {
      throw new ApiError(0, 'UNEXPECTED', 'Login did not return a token');
    }
    jwtStore.set(token);
    setUser(result.user);
  }, []);

  const verifyMfa = useCallback(async (input: MfaVerifyInput): Promise<void> => {
    const token = await api<string>('/api/v1/auth/mfa/verify', {
      method: 'POST',
      body: input
    });
    jwtStore.set(token);
    setMfaPending(null);
    // The MFA verify endpoint returns only the token; fetch the user view.
    const me = await api<User>('/api/v1/users/me');
    setUser(me);
  }, []);

  const register = useCallback(async (input: RegisterPatientInput): Promise<PatientDto> => {
    return api<PatientDto>('/api/v1/auth/register', { method: 'POST', body: input });
  }, []);

  const logout = useCallback((): void => {
    jwtStore.clear();
    setUser(null);
    setMfaPending(null);
  }, []);

  const refreshUser = useCallback(async (): Promise<void> => {
    const me = await api<User>('/api/v1/users/me');
    setUser(me);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token: jwtStore.accessToken,
      ready,
      mfaPending,
      login,
      verifyMfa,
      register,
      logout,
      refreshUser
    }),
    [user, ready, mfaPending, login, verifyMfa, register, logout, refreshUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (ctx === null) {
    throw new Error('useAuth must be used within an <AuthProvider>');
  }
  return ctx;
}

/**
 * True when the given user holds one of the allowed roles. A null user is
 * never allowed. Used with <RequireRole> / the <ProtectedRoute> helper.
 */
export function hasRole(user: User | null, allowed: readonly Role[]): boolean {
  if (user === null) {
    return false;
  }
  return allowed.some((r) => user.role === r);
}
