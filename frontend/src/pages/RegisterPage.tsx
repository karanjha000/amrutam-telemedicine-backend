import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../lib/api';
import type { PatientDto } from '../types';

export default function RegisterPage(): JSX.Element {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [phone, setPhone] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [created, setCreated] = useState<PatientDto | null>(null);

  const submit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const patient = await register({
        username: username.trim(),
        password,
        email: email.trim(),
        displayName: displayName.trim(),
        phone: phone.trim() === '' ? undefined : phone.trim()
      });
      setCreated(patient);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to create the account.');
    } finally {
      setSubmitting(false);
    }
  };

  if (created !== null) {
    return (
      <div className="auth-card" data-testid="register-success">
        <h1>Account created</h1>
        <p className="auth-card__hint">
          Welcome, {created.displayName}. You are signed up as {created.username}.
        </p>
        <button
          type="button"
          className="btn btn--primary"
          onClick={() => navigate('/login', { state: { from: '/patient' } })}
        >
          Sign in
        </button>
      </div>
    );
  }

  return (
    <div className="auth-card" data-testid="register-form">
      <h1>Create a patient account</h1>
      <p className="auth-card__hint">Self-service registration opens a patient account.</p>
      <form onSubmit={submit} aria-label="Register">
        <label>
          Username
          <input
            type="text"
            required
            minLength={3}
            maxLength={64}
            autoComplete="username"
            value={username}
            onChange={(ev) => setUsername(ev.target.value)}
          />
        </label>
        <label>
          Display name
          <input
            type="text"
            required
            maxLength={128}
            value={displayName}
            onChange={(ev) => setDisplayName(ev.target.value)}
          />
        </label>
        <label>
          Email
          <input
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(ev) => setEmail(ev.target.value)}
          />
        </label>
        <label>
          Phone (optional)
          <input
            type="tel"
            maxLength={32}
            value={phone}
            onChange={(ev) => setPhone(ev.target.value)}
          />
        </label>
        <label>
          Password
          <input
            type="password"
            required
            minLength={8}
            maxLength={128}
            autoComplete="new-password"
            value={password}
            onChange={(ev) => setPassword(ev.target.value)}
          />
        </label>
        {error !== null ? <p className="form-error" role="alert">{error}</p> : null}
        <button type="submit" className="btn btn--primary" disabled={submitting}>
          {submitting ? 'Creating…' : 'Create account'}
        </button>
      </form>
      <p className="auth-card__hint">
        Already registered? <Link to="/login">Sign in</Link>
      </p>
    </div>
  );
}
