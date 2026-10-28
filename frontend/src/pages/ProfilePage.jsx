import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../components/PageHeader.jsx'
import { Field, TextInput } from '../components/Field.jsx'
import { useApi } from '../hooks/useApi.js'
import { portal } from '../lib/api.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useToast } from '../context/ui.jsx'
import { dateTime } from '../lib/format.js'

const EMPTY_PROFILE = { fullName: '', phone: '', address: '' }
const EMPTY_PASSWORD = { currentPassword: '', newPassword: '', repeat: '' }

export default function ProfilePage() {
  const toast = useToast()
  const navigate = useNavigate()
  const { logout } = useAuth()

  const profile = useApi(() => portal.profile(), [])
  const [values, setValues] = useState(EMPTY_PROFILE)
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const [passwords, setPasswords] = useState(EMPTY_PASSWORD)
  const [passwordErrors, setPasswordErrors] = useState({})
  const [savingPassword, setSavingPassword] = useState(false)

  useEffect(() => {
    if (!profile.data) return
    setValues({
      fullName: profile.data.fullName ?? '',
      phone: profile.data.phone ?? '',
      address: profile.data.address ?? '',
    })
  }, [profile.data])

  const setField = (field, value) => {
    setValues((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: undefined }))
  }

  const setPasswordField = (field, value) => {
    setPasswords((current) => ({ ...current, [field]: value }))
    setPasswordErrors((current) => ({ ...current, [field]: undefined }))
  }

  const saveProfile = async (event) => {
    event.preventDefault()
    setSaving(true)
    setErrors({})
    try {
      const updated = await portal.updateProfile({
        fullName: values.fullName.trim(),
        phone: values.phone.trim(),
        address: values.address.trim() || null,
      })
      profile.setData(updated)
      toast.success('Datos actualizados')
    } catch (error) {
      if (error.errors) setErrors(error.errors)
      else toast.error(error.message)
    } finally {
      setSaving(false)
    }
  }

  const savePassword = async (event) => {
    event.preventDefault()
    setPasswordErrors({})
    if (passwords.newPassword !== passwords.repeat) {
      setPasswordErrors({ repeat: 'Las contraseñas no coinciden' })
      return
    }
    setSavingPassword(true)
    try {
      await portal.changePassword({
        currentPassword: passwords.currentPassword,
        newPassword: passwords.newPassword,
      })
      toast.success('Contraseña cambiada. Vuelve a entrar con la nueva.')
      try {
        await logout()
      } catch {
        // La sesión local ya está limpia aunque el servidor no responda.
      } finally {
        navigate('/login', { replace: true })
      }
    } catch (error) {
      if (error.errors) setPasswordErrors(error.errors)
      else toast.error(error.message)
    } finally {
      setSavingPassword(false)
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Mi cuenta"
        title="Mi perfil"
        description="Mantén al día tus datos de contacto. El email de acceso no se puede cambiar desde aquí."
      />

      {profile.error && <div className="card">No se pudo cargar tu perfil: {profile.error.message}</div>}

      <div className="grid grid-2">
        <section className="card">
          <h2 className="card-title">Datos personales</h2>
          <form onSubmit={saveProfile}>
            <Field label="Nombre y apellidos" required error={errors.fullName}>
              <TextInput
                value={values.fullName}
                invalid={Boolean(errors.fullName)}
                onChange={(event) => setField('fullName', event.target.value)}
              />
            </Field>
            <Field label="Email">
              <TextInput value={profile.data?.email ?? ''} disabled />
            </Field>
            <Field label="Teléfono" required error={errors.phone}>
              <TextInput
                type="tel"
                value={values.phone}
                invalid={Boolean(errors.phone)}
                onChange={(event) => setField('phone', event.target.value)}
              />
            </Field>
            <Field label="Dirección" error={errors.address}>
              <TextInput
                value={values.address}
                invalid={Boolean(errors.address)}
                onChange={(event) => setField('address', event.target.value)}
              />
            </Field>
            <button type="submit" className="btn btn-primary" disabled={saving || profile.loading}>
              {saving ? 'Guardando…' : 'Guardar cambios'}
            </button>
          </form>
          {profile.data && (
            <p className="cell-muted" style={{ marginBottom: 0 }}>
              Último acceso: {dateTime(profile.data.lastLoginAt)}
            </p>
          )}
        </section>

        <section className="card">
          <h2 className="card-title">Seguridad</h2>
          <p className="cell-muted">
            Al cambiar la contraseña se cierran las sesiones del resto de dispositivos.
          </p>
          <form onSubmit={savePassword}>
            <Field label="Contraseña actual" required error={passwordErrors.currentPassword}>
              <TextInput
                type="password"
                autoComplete="current-password"
                value={passwords.currentPassword}
                invalid={Boolean(passwordErrors.currentPassword)}
                onChange={(event) => setPasswordField('currentPassword', event.target.value)}
              />
            </Field>
            <Field label="Nueva contraseña" required error={passwordErrors.newPassword}
              hint="Mínimo 8 caracteres, una letra y un número.">
              <TextInput
                type="password"
                autoComplete="new-password"
                value={passwords.newPassword}
                invalid={Boolean(passwordErrors.newPassword)}
                onChange={(event) => setPasswordField('newPassword', event.target.value)}
              />
            </Field>
            <Field label="Repite la nueva contraseña" required error={passwordErrors.repeat}>
              <TextInput
                type="password"
                autoComplete="new-password"
                value={passwords.repeat}
                invalid={Boolean(passwordErrors.repeat)}
                onChange={(event) => setPasswordField('repeat', event.target.value)}
              />
            </Field>
            <button type="submit" className="btn btn-dark" disabled={savingPassword}>
              {savingPassword ? 'Cambiando…' : 'Cambiar contraseña'}
            </button>
          </form>
        </section>
      </div>
    </>
  )
}
