import { useEffect, useState } from 'react'
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import GithubMark from '../components/GithubMark.jsx'
import Logo from '../components/Logo.jsx'
import { Field, TextInput } from '../components/Field.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { auth as authApi, OAUTH_AUTHORIZATION_URL } from '../lib/api.js'

const DEMO = [
  { role: 'Administrador', email: 'admin@vetclinic.example', password: 'Admin1234!' },
  { role: 'Gestor de clínica', email: 'gestor@vetclinic.example', password: 'Gestor1234!' },
  { role: 'Cliente', email: 'cliente@vetclinic.example', password: 'Cliente1234!' },
]

const OAUTH_ERRORS = {
  oauth_staff_account: 'Ese email pertenece a una cuenta del personal. Entra con tu contraseña de la clínica.',
  oauth_disabled: 'Tu cuenta está desactivada. Contacta con la clínica para reactivarla.',
  github_denied: 'Has cancelado el acceso con GitHub. Puedes intentarlo de nuevo o entrar con contraseña.',
  oauth_error: 'No se pudo completar el acceso con GitHub. Inténtalo de nuevo o usa tu contraseña.',
}


export default function LoginPage() {
  const { user, ready, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const [values, setValues] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [githubEnabled, setGithubEnabled] = useState(false)

  const oauthError = searchParams.get('oauthError')

  useEffect(() => {
    let cancelled = false
    authApi
      .providers()
      .then((providers) => {
        if (!cancelled) setGithubEnabled(Boolean(providers?.githubEnabled))
      })
      .catch(() => {
        if (!cancelled) setGithubEnabled(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const destination = location.state?.from && location.state.from !== '/login'
    ? location.state.from
    : '/'

  if (ready && user) return <Navigate to={destination} replace />

  const setField = (field, value) => setValues((current) => ({ ...current, [field]: value }))

  const submit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      const logged = await login(values.email.trim(), values.password)
      navigate(logged.role === 'client' ? '/mis-citas' : destination, { replace: true })
    } catch (cause) {
      setError(cause.message)
    } finally {
      setSaving(false)
    }
  }

  const useDemo = (demo) => {
    setValues({ email: demo.email, password: demo.password })
    setError('')
  }

  return (
    <section className="auth-layout">
      <div className="auth-intro">
        <span className="eyebrow">Área privada</span>
        <h1>Tu clínica, también en línea</h1>
        <p>
          Consulta las fichas de tus mascotas, reserva o cancela citas y revisa tus compras en la
          tienda. El personal de la clínica accede con las mismas credenciales a su panel de gestión.
        </p>
        <ul className="auth-points">
          <li>Acceso con JWT de corta duración y renovación automática.</li>
          <li>Tus datos solo los ves tú y el personal autorizado.</li>
          <li>Si eres cliente de la clínica, regístrate con el mismo email y teléfono.</li>
        </ul>
      </div>

      <div className="auth-card card">
        <div className="auth-brand">
          <Logo />
          <div>
            <strong>Galgo Español</strong>
            <small>Clínica Veterinaria</small>
          </div>
        </div>

        <h2>Iniciar sesión</h2>
        <p className="cell-muted">Introduce tu email y contraseña para continuar.</p>

        {oauthError && (
          <div className="auth-error" role="alert">
            {OAUTH_ERRORS[oauthError] ?? OAUTH_ERRORS.oauth_error}
          </div>
        )}

        {githubEnabled && (
          <>
            <a className="btn btn-dark btn-block github-btn" href={OAUTH_AUTHORIZATION_URL}>
              <GithubMark /> Continuar con GitHub
            </a>
            <div className="auth-divider"><span>o entra con tu email</span></div>
          </>
        )}

        <form onSubmit={submit} noValidate>
          <Field label="Email" required>
            <TextInput
              type="email"
              autoComplete="email"
              value={values.email}
              onChange={(event) => setField('email', event.target.value)}
              placeholder="tu@email.com"
              required
            />
          </Field>
          <Field label="Contraseña" required>
            <TextInput
              type="password"
              autoComplete="current-password"
              value={values.password}
              onChange={(event) => setField('password', event.target.value)}
              placeholder="••••••••"
              required
            />
          </Field>

          {error && <div className="auth-error" role="alert">{error}</div>}

          <button type="submit" className="btn btn-primary btn-block" disabled={saving}>
            {saving ? 'Entrando…' : 'Entrar'}
          </button>
        </form>

        <p className="auth-switch">
          ¿Todavía no tienes cuenta? <Link to="/registro">Regístrate como cliente</Link>
        </p>

        <details className="demo-box">
          <summary>Cuentas de demostración</summary>
          <ul>
            {DEMO.map((demo) => (
              <li key={demo.email}>
                <span>
                  <strong>{demo.role}</strong>
                  <br />
                  <code>{demo.email}</code> · <code>{demo.password}</code>
                </span>
                <button type="button" className="btn btn-outline btn-sm" onClick={() => useDemo(demo)}>
                  Usar
                </button>
              </li>
            ))}
          </ul>
        </details>
      </div>
    </section>
  )
}
