import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

function Loading() {
  return (
    <div className="auth-loading">
      <div className="spinner" />
      <p className="cell-muted">Comprobando tu sesión…</p>
    </div>
  )
}

/**
 * Layout guard. With no `roles` it only requires a session; with a list it also checks
 * the role. Users without access land on their own home instead of a dead end.
 */
export function RequireAuth({ roles }) {
  const { user, ready } = useAuth()
  const location = useLocation()

  if (!ready) return <Loading />
  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return <Outlet />
}

export function RequireStaff() {
  return <RequireAuth roles={['admin', 'manager']} />
}

export function RequireAdmin() {
  return <RequireAuth roles={['admin']} />
}

export function RequireClient() {
  return <RequireAuth roles={['client']} />
}
