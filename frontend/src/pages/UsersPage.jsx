import { useState } from 'react'
import PageHeader from '../components/PageHeader.jsx'
import DataTable from '../components/DataTable.jsx'
import Pagination from '../components/Pagination.jsx'
import Modal from '../components/Modal.jsx'
import Badge from '../components/Badge.jsx'
import { Field, SelectInput, TextInput } from '../components/Field.jsx'
import { useApi, useList } from '../hooks/useApi.js'
import { useForm } from '../hooks/useForm.js'
import { owners, users } from '../lib/api.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useConfirm, useToast } from '../context/ui.jsx'
import { dateTime } from '../lib/format.js'
import { label, options, ROLES, roleTone } from '../lib/enums.js'

const EMPTY = { fullName: '', email: '', password: '', role: 'client', ownerId: '' }
const roleOptions = options('role', ROLES)

export default function UsersPage() {
  const toast = useToast()
  const confirm = useConfirm()
  const { user: currentUser } = useAuth()

  const [filters, setFilters] = useState({ role: '', enabled: '', q: '' })
  const [search, setSearch] = useState('')
  const list = useList((params) => users.list(params), filters, 10)
  const ownersQuery = useApi(() => owners.list({ page: 0, size: 200, sort: 'fullName,asc' }), [])

  const ownerOptions = (ownersQuery.data?.content ?? []).map((owner) => ({
    value: String(owner.id),
    label: `${owner.fullName} · ${owner.email}`,
  }))

  const { values, setField, reset } = useForm(EMPTY)
  const [open, setOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [errors, setErrors] = useState({})

  const applySearch = (event) => {
    event.preventDefault()
    setFilters((current) => ({ ...current, q: search.trim() }))
  }

  const openCreate = () => {
    reset(EMPTY)
    setErrors({})
    setOpen(true)
  }

  const submit = async (event) => {
    event.preventDefault()
    setErrors({})
    if (values.role === 'client' && !values.ownerId) {
      setErrors({ ownerId: 'Selecciona la ficha de cliente' })
      return
    }
    setSaving(true)
    try {
      await users.create({
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        password: values.password,
        role: values.role,
        ownerId: values.role === 'client' ? Number(values.ownerId) : null,
      })
      toast.success('Usuario creado')
      setOpen(false)
      list.reload()
    } catch (error) {
      if (error.errors) setErrors(error.errors)
      else toast.error(error.message)
    } finally {
      setSaving(false)
    }
  }

  const changeRole = async (row, role) => {
    if (role === row.role) return
    try {
      await users.changeRole(row.id, role)
      toast.success(`Rol de ${row.fullName} actualizado`)
      list.reload()
    } catch (error) {
      toast.error(error.message)
    }
  }

  const toggleEnabled = async (row) => {
    const enable = !row.enabled
    const ok = await confirm({
      title: enable ? 'Activar cuenta' : 'Desactivar cuenta',
      message: enable
        ? `¿Permitir de nuevo el acceso a ${row.fullName}?`
        : `¿Cerrar el acceso de ${row.fullName}? Sus sesiones se revocan al instante.`,
      confirmLabel: enable ? 'Activar' : 'Desactivar',
      danger: !enable,
    })
    if (!ok) return
    try {
      await users.setEnabled(row.id, enable)
      toast.success(enable ? 'Cuenta activada' : 'Cuenta desactivada')
      list.reload()
    } catch (error) {
      toast.error(error.message)
    }
  }

  const remove = async (row) => {
    const ok = await confirm({
      title: 'Eliminar usuario',
      message: `¿Eliminar la cuenta de ${row.fullName}? La ficha de cliente se conserva.`,
      confirmLabel: 'Eliminar',
      danger: true,
    })
    if (!ok) return
    try {
      await users.remove(row.id)
      toast.success('Usuario eliminado')
      list.reload()
    } catch (error) {
      toast.error(error.message)
    }
  }

  const columns = [
    {
      key: 'fullName',
      header: 'Usuario',
      render: (row) => (
        <span>
          <span className="cell-strong">{row.fullName}</span>
          <br />
          <span className="cell-muted">{row.email}</span>
        </span>
      ),
    },
    {
      key: 'role',
      header: 'Rol',
      render: (row) => (
        <div className="role-cell">
          <Badge tone={roleTone(row.role)}>{label('role', row.role)}</Badge>
          <SelectInput
            value={row.role}
            options={roleOptions}
            className="select-sm"
            aria-label={`Rol de ${row.fullName}`}
            disabled={row.id === currentUser?.id}
            onChange={(event) => changeRole(row, event.target.value)}
          />
        </div>
      ),
    },
    {
      key: 'enabled',
      header: 'Estado',
      render: (row) => (
        <Badge tone={row.enabled ? 'green' : 'red'}>{row.enabled ? 'Activa' : 'Desactivada'}</Badge>
      ),
    },
    { key: 'ownerId', header: 'Ficha cliente', render: (row) => row.ownerId ?? '—' },
    { key: 'lastLoginAt', header: 'Último acceso', render: (row) => dateTime(row.lastLoginAt) },
    {
      key: 'actions',
      header: 'Acciones',
      hideLabel: true,
      className: 'cell-actions',
      render: (row) => (
        <div className="btn-row">
          <button type="button" className="btn btn-outline btn-sm" onClick={() => toggleEnabled(row)}>
            {row.enabled ? 'Desactivar' : 'Activar'}
          </button>
          <button
            type="button"
            className="btn btn-danger btn-sm"
            disabled={row.id === currentUser?.id}
            onClick={() => remove(row)}
          >
            Eliminar
          </button>
        </div>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        eyebrow="Administración"
        title="Usuarios"
        description="Cuentas de acceso y roles. Solo el administrador puede gestionarlas."
        actions={
          <>
            <button type="button" className="btn btn-ghost" onClick={list.reload}>↻ Actualizar</button>
            <button type="button" className="btn btn-primary" onClick={openCreate}>+ Nuevo usuario</button>
          </>
        }
      />

      <div className="toolbar">
        <form className="toolbar-search" onSubmit={applySearch}>
          <Field label="Buscar">
            <TextInput
              value={search}
              placeholder="Nombre o email"
              onChange={(event) => setSearch(event.target.value)}
            />
          </Field>
          <button type="submit" className="btn btn-outline">Buscar</button>
        </form>
        <Field label="Rol">
          <SelectInput
            value={filters.role}
            placeholder="Todos"
            options={roleOptions}
            onChange={(event) => setFilters((current) => ({ ...current, role: event.target.value }))}
          />
        </Field>
        <Field label="Estado">
          <SelectInput
            value={filters.enabled}
            placeholder="Todos"
            options={[{ value: 'true', label: 'Activas' }, { value: 'false', label: 'Desactivadas' }]}
            onChange={(event) => setFilters((current) => ({ ...current, enabled: event.target.value }))}
          />
        </Field>
        <span className="spacer" />
        <button
          type="button"
          className="btn btn-ghost btn-sm"
          onClick={() => {
            setSearch('')
            setFilters({ role: '', enabled: '', q: '' })
          }}
        >
          Limpiar filtros
        </button>
      </div>

      <DataTable
        columns={columns}
        rows={list.rows}
        loading={list.loading}
        empty={<><div className="empty-icon">👥</div><p>No hay usuarios que coincidan con los filtros.</p></>}
      />
      <Pagination
        page={list.page}
        size={list.size}
        totalPages={list.totalPages}
        totalElements={list.totalElements}
        onPage={list.setPage}
        onSize={list.setSize}
      />

      <Modal
        open={open}
        title="Nuevo usuario"
        onClose={() => setOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-ghost" onClick={() => setOpen(false)}>Cancelar</button>
            <button type="submit" form="user-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Creando…' : 'Crear usuario'}
            </button>
          </>
        }
      >
        <form id="user-form" onSubmit={submit}>
          <div className="form-grid">
            <Field label="Nombre y apellidos" required error={errors.fullName}>
              <TextInput
                value={values.fullName}
                invalid={Boolean(errors.fullName)}
                onChange={(event) => setField('fullName', event.target.value)}
              />
            </Field>
            <Field label="Email" required error={errors.email}>
              <TextInput
                type="email"
                value={values.email}
                invalid={Boolean(errors.email)}
                onChange={(event) => setField('email', event.target.value)}
              />
            </Field>
            <Field label="Contraseña temporal" required error={errors.password}
              hint="Mínimo 8 caracteres, una letra y un número.">
              <TextInput
                type="password"
                autoComplete="new-password"
                value={values.password}
                invalid={Boolean(errors.password)}
                onChange={(event) => setField('password', event.target.value)}
              />
            </Field>
            <Field label="Rol" required error={errors.role}>
              <SelectInput
                value={values.role}
                options={roleOptions}
                onChange={(event) => setField('role', event.target.value)}
              />
            </Field>
            {values.role === 'client' && (
              <Field label="Ficha de cliente" required error={errors.ownerId} className="full"
                hint="La cuenta se vincula a un cliente ya registrado en la clínica.">
                <SelectInput
                  value={values.ownerId}
                  placeholder="Selecciona un cliente"
                  options={ownerOptions}
                  invalid={Boolean(errors.ownerId)}
                  onChange={(event) => setField('ownerId', event.target.value)}
                />
              </Field>
            )}
          </div>
        </form>
      </Modal>
    </>
  )
}
