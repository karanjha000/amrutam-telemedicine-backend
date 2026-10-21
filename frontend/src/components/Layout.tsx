import { Link, useLocation, useNavigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from '../context/AuthContext';
import type { Role } from '../types';

/** Role-aware dashboard path for the "Dashboard" nav link. */
function dashboardPathFor(role: Role): string {
  switch (role) {
    case 'PATIENT':
      return '/patient';
    case 'DOCTOR':
      return '/doctor';
    case 'ADMIN':
      return '/admin';
  }
}

export function Layout({ children }: { children: ReactNode }): JSX.Element {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const dashPath = user !== null ? dashboardPathFor(user.role) : '/';

  const onLogout = (): void => {
    logout();
    navigate('/login', { replace: true });
  };

  const links: Array<{ to: string; label: string; className?: string }> = [
    { to: '/doctors', label: 'Find a doctor' },
    ...(user !== null ? [{ to: dashPath, label: 'Dashboard' }] : []),
    ...(user?.role === 'PATIENT' ? [{ to: '/patient', label: 'Book' }] : [])
  ];

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header__inner">
          <Link to="/" className="brand" aria-label="Amrutam home">
            Amrutam<span className="brand__dot">.</span>
          </Link>
          <nav className="app-nav" aria-label="Primary">
            {links.map((l) => (
              <Link
                key={l.to + l.label}
                to={l.to}
                className={
                  'app-nav__link' +
                  (location.pathname === l.to ? ' app-nav__link--active' : '')
                }
              >
                {l.label}
              </Link>
            ))}
          </nav>
          <div className="app-header__user">
            {user !== null ? (
              <>
                <span className="user-chip">
                  {user.displayName} <span className="user-chip__role">{user.role.toLowerCase()}</span>
                </span>
                <button type="button" className="btn btn--ghost" onClick={onLogout}>
                  Sign out
                </button>
              </>
            ) : (
              <>
                <Link className="btn btn--ghost" to="/login">
                  Sign in
                </Link>
                <Link className="btn btn--primary" to="/register">
                  Create account
                </Link>
              </>
            )}
          </div>
        </div>
      </header>
      <main className="app-main">{children}</main>
      <footer className="app-footer">
        Amrutam — telemedicine platform · docs: /swagger-ui
      </footer>
    </div>
  );
}
