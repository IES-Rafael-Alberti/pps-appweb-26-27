import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

/**
 * Landing page after the GitHub handshake. The backend has already set the httpOnly refresh cookie;
 * the AuthProvider is exchanging it for an access token while this screen is shown.
 */
export default function OAuth2CallbackPage() {
  const { user, ready } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (!ready) return
    navigate(user ? '/' : '/login?oauthError=oauth_error', { replace: true })
  }, [ready, user, navigate])

  return (
    <div className="auth-loading">
      <div className="spinner" />
      <p className="cell-muted">Conectando con GitHub…</p>
    </div>
  )
}
