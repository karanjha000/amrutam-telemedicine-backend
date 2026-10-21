import { useEffect, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../lib/api';

export default function LoginPage(): JSX.Element {
  const { login, verifyMfa, mfaPending, user, token, ready } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/';

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [code, setCode] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // After a successful (non-MFA) login, the context already holds the
  // token; redirect there.
  useEffect(() => {
    if (ready && user !== null && token !== null) {
      navigate(from, { replace: true });
    }
  }, [ready, user, token, navigate, from]);

  const submit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await login({ username: username.trim(), password });
      // If an MFA challenge was returned, mfaPending flips and the form
      // below switches to the code step — no navigation.
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to sign in. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const submitMfa = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    if (mfaPending === null) {
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await verifyMfa({ username: mfaPending.username, code });
      // Context now holds the user + token; the effect above redirects.
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'MFA verification failed.');
    } finally {
      setSubmitting(false);
    }
  };

  if (mfaPending !== null) {
    return (
      <div className="auth-card" data-testid="mfa-form">
        <h1>Verify your identity</h1>
        <p className="auth-card__hint">
          Enter the 6-digit code from your authenticator app.
          {mfaPending.provisioningUri !== null && mfaPending.provisioningUri.length > 0 ? (
            <span className="auth-card__uri">{mfaPending.provisioningUri}</span>
          ) : null}
        </p>
        <form onSubmit={submitMfa} aria-label="MFA verification">
          <label>
            Code
            <input
              type="text"
              inputMode="numeric"
              pattern="\d{6}"
              required
              maxLength={6}
              value={code}
              onChange={(ev) => setCode(ev.target.value)}
              placeholder="000000"
              aria-label="MFA code"
            />
          </label>
          {error !== null ? <p className="form-error" role="alert">{error}</p> : null}
          <button type="submit" className="btn btn--primary" disabled={submitting || code.length !== 6}>
            {submitting ? 'Verifying…' : 'Verify'}
          </button>
        </form>
        <Link className="auth-card__back" to="/login" onClick={() => setError(null)}>
          Back to sign in
        </Link>
      </div>
    );
  }

  return (
    <div className="auth-card">
      <h1>Sign in</h1>
      <p className="auth-card__hint">Amrutam telemedicine</p>
      <form onSubmit={submit} aria-label="Sign in">
        <label>
          Username
          <input
            type="text"
            required
            autoComplete="username"
            value={username}
            onChange={(ev) => setUsername(ev.target.value)}
          />
        </label>
        <label>
          Password
          <input
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(ev) => setPassword(ev.target.value)}
          />
        </label>
        {error !== null ? <p className="form-error" role="alert">{error}</p> : null}
        <button type="submit" className="btn btn--primary" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className="auth-card__hint">
        New patient? <Link to="/register">Create an account</Link>
      </p>
    </div>
  );
}
