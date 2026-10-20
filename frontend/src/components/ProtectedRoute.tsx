import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth, hasRole } from '../context/AuthContext';
import type { Role } from '../types';

interface ProtectedRouteProps {
  /** When set, the signed-in user must hold one of these roles. */
  allow?: readonly Role[];
  children: ReactNode;
}

/**
 * Gate for authenticated routes. When no token exists (or the session is
 * still restoring), the visitor is sent to /login with a `from` state so
 * the post-login redirect lands back on the protected page.
 */
export function ProtectedRoute({ allow, children }: ProtectedRouteProps): ReactNode {
  const { user, ready } = useAuth();
  const location = useLocation();

  if (!ready) {
    return <div className="page-status">Restoring session…</div>;
  }

  if (user === null) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (allow !== undefined && !hasRole(user, allow)) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}
