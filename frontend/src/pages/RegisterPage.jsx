import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import GithubMark from '../components/GithubMark.jsx'
import Logo from '../components/Logo.jsx'
import { Field, TextInput } from '../components/Field.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { auth as authApi, OAUTH_AUTHORIZATION_URL } from '../lib/api.js'

const EMPTY = { fullName: '', email: '', phone: '', password: '', repeat: '' }

export default function RegisterPage() {
  const { user, ready, register } = useAuth()
  const navigate = useNavigate()
  const [values, setValues] = useState(EMPTY)
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [githubEnabled, setGithubEnabled] = useState(false)

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

  if (ready && user) return <Navigate to="/" replace />

  const setField = (field, value) => {
    setValues((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: undefined }))
  }

  const submit = async (event) => {
    event.preventDefault()
    setError('')

    if (values.password !== values.repeat) {
      setErrors({ repeat: 'Las contraseñas no coinciden' })
      return
    }

    setSaving(true)
    try {
      await register({
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        phone: values.phone.trim(),
        password: values.password,
      })
      navigate('/mis-citas', { replace: true })
    } catch (cause) {
      if (cause.errors) setErrors(cause.errors)
      else setError(cause.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="auth-layout">
      <div className="auth-intro">
        <span className="eyebrow">Nuevos clientes</span>
        <h1>Crea tu cuenta en un minuto</h1>
        <p>
          Con tu cuenta podrás consultar las fichas de tus mascotas, pedir cita y revisar tu historial
          de compras. No necesitas venir a la clínica para registrarte.
        </p>
        <ul className="auth-points">
          <li>Usa el mismo email y teléfono que diste en la clínica para vincular tu historial.</li>
          <li>La contraseña debe tener al menos 8 caracteres, una letra y un número.</li>
          <li>Los datos de tus mascotas solo los ve el personal autorizado.</li>
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

        <h2>Registro de cliente</h2>
        <p className="cell-muted">Crea tu acceso al portal de la clínica.</p>

        {githubEnabled && (
          <>
            <a className="btn btn-dark btn-block github-btn" href={OAUTH_AUTHORIZATION_URL}>
              <GithubMark /> Registrarme con GitHub
            </a>
            <div className="auth-divider"><span>o crea una cuenta con tu email</span></div>
          </>
        )}

        <form onSubmit={submit} noValidate>
          <Field label="Nombre y apellidos" required error={errors.fullName}>
            <TextInput
              autoComplete="name"
              value={values.fullName}
              invalid={Boolean(errors.fullName)}
              onChange={(event) => setField('fullName', event.target.value)}
              placeholder="Lucía Fernández"
              required
            />
          </Field>
          <Field label="Email" required error={errors.email}>
            <TextInput
              type="email"
              autoComplete="email"
              value={values.email}
              invalid={Boolean(errors.email)}
              onChange={(event) => setField('email', event.target.value)}
              placeholder="tu@email.com"
              required
            />
          </Field>
          <Field label="Teléfono" required error={errors.phone} hint="Debe coincidir con el que tenemos en la clínica.">
            <TextInput
              type="tel"
              autoComplete="tel"
              value={values.phone}
              invalid={Boolean(errors.phone)}
              onChange={(event) => setField('phone', event.target.value)}
              placeholder="+34 600 123 456"
              required
            />
          </Field>
          <div className="form-grid">
            <Field label="Contraseña" required error={errors.password}>
              <TextInput
                type="password"
                autoComplete="new-password"
                value={values.password}
                invalid={Boolean(errors.password)}
                onChange={(event) => setField('password', event.target.value)}
                placeholder="Mínimo 8 caracteres"
                required
              />
            </Field>
            <Field label="Repite la contraseña" required error={errors.repeat}>
              <TextInput
                type="password"
                autoComplete="new-password"
                value={values.repeat}
                invalid={Boolean(errors.repeat)}
                onChange={(event) => setField('repeat', event.target.value)}
                placeholder="Repite la contraseña"
                required
              />
            </Field>
          </div>

          {error && <div className="auth-error" role="alert">{error}</div>}

          <button type="submit" className="btn btn-primary btn-block" disabled={saving}>
            {saving ? 'Creando cuenta…' : 'Crear cuenta'}
          </button>
        </form>

        <p className="auth-switch">
          ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
        </p>
      </div>
    </section>
  )
}
