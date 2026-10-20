import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '../../context/AuthContext';
import { ApiError, api } from '../../lib/api';
import type { LoginResult, User } from '../../types';
import LoginPage from '../LoginPage';

// Mock the network layer; AuthProvider + LoginPage call `api` from here.
vi.mock('../../lib/api', async () => {
  // Spreading the real module (real ApiError class + real jwtStore object
  // with real getters backed by the real localStorage) and then replacing
  // `api` with a controllable mock.
  const actual = await vi.importActual('../../lib/api');
  return {
    ...actual,
    api: vi.fn()
  };
});

const JOHN: User = {
  id: 'u1',
  username: 'patient.john',
  displayName: 'John Doe',
  email: 'john@example.com',
  role: 'PATIENT',
  phone: null,
  preferredLanguage: null
};

function renderPage() {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/login']}>
        <LoginPage />
      </MemoryRouter>
    </AuthProvider>
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.mocked(api).mockReset();
    window.localStorage.clear();
  });

  it('renders the sign-in form with username, password and a submit button', () => {
    renderPage();
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('submits credentials and surfaces an ApiError message on failure', async () => {
    vi.mocked(api).mockRejectedValueOnce(
      new ApiError(401, 'INVALID_CREDENTIALS', 'Invalid credentials')
    );
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('Username'), 'patient.john');
    await user.type(screen.getByLabelText('Password'), 'secret123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(api).toHaveBeenCalledWith('/api/v1/auth/login', {
      method: 'POST',
      body: { username: 'patient.john', password: 'secret123' }
    });
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid credentials');
  });

  it('persists the token on a successful non-MFA login', async () => {
    vi.mocked(api).mockResolvedValueOnce({
      token: 'jwt-token-abc',
      mfaChallenge: null,
      user: JOHN
    } as LoginResult);
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('Username'), 'patient.john');
    await user.type(screen.getByLabelText('Password'), 'secret123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(window.localStorage.getItem('amrutam.accessToken')).toBe('jwt-token-abc');
  });

  it('switches to the MFA step when login returns a challenge', async () => {
    vi.mocked(api)
      .mockResolvedValueOnce({
        token: null,
        mfaChallenge: { secret: null, provisioningUri: 'otpauth://totp/amrutam:john?secret=ABC' },
        user: JOHN
      } as LoginResult)
      .mockResolvedValueOnce('mfa-token-xyz')
      .mockResolvedValueOnce(JOHN);
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('Username'), 'patient.john');
    await user.type(screen.getByLabelText('Password'), 'secret123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    const mfa = await screen.findByTestId('mfa-form');
    expect(mfa).toBeInTheDocument();

    const codeInput = screen.getByLabelText('MFA code');
    const verify = screen.getByRole('button', { name: /verify/i });
    expect(verify).toBeDisabled();

    await user.type(codeInput, '123456');
    expect(verify).toBeEnabled();
    await user.click(verify);

    expect(api).toHaveBeenCalledWith('/api/v1/auth/mfa/verify', {
      method: 'POST',
      body: { username: 'patient.john', code: '123456' }
    });
    expect(window.localStorage.getItem('amrutam.accessToken')).toBe('mfa-token-xyz');
  });

  it('links to the register page', () => {
    renderPage();
    expect(screen.getByRole('link', { name: /create an account/i })).toHaveAttribute(
      'href',
      '/register'
    );
  });
});
