import { useState } from 'react'
import PageHeader from '../components/PageHeader.jsx'
import DataTable from '../components/DataTable.jsx'
import Pagination from '../components/Pagination.jsx'
import Modal from '../components/Modal.jsx'
import { Field, TextInput } from '../components/Field.jsx'
import { useList } from '../hooks/useApi.js'
import { useForm } from '../hooks/useForm.js'
import { owners } from '../lib/api.js'
import { useConfirm, useToast } from '../context/ui.jsx'
import { date } from '../lib/format.js'

const EMPTY = { fullName: '', email: '', phone: '', address: '' }

export default function OwnersPage() {
  const toast = useToast()
  const confirm = useConfirm()
  const list = useList((params) => owners.list({ ...params, sort: 'fullName,asc' }), {}, 10)

  const { values, setValues, setField, reset } = useForm(EMPTY)
  const [editing, setEditing] = useState(null)
  const [open, setOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [errors, setErrors] = useState({})

  const openCreate = () => {
    setEditing(null)
    reset(EMPTY)
    setErrors({})
    setOpen(true)
  }

  const openEdit = (row) => {
    setEditing(row)
    setValues({
      fullName: row.fullName ?? '',
      email: row.email ?? '',
      phone: row.phone ?? '',
      address: row.address ?? '',
    })
    setErrors({})
    setOpen(true)
  }

  const submit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setErrors({})
    const payload = {
      fullName: values.fullName.trim(),
      email: values.email.trim(),
      phone: values.phone.trim(),
      address: values.address.trim() || null,
    }
    try {
      if (editing) {
        await owners.update(editing.id, payload)
        toast.success('Cliente actualizado')
      } else {
        await owners.create(payload)
        toast.success('Cliente registrado')
      }
      setOpen(false)
      list.reload()
    } catch (error) {
      if (error.errors) setErrors(error.errors)
      else toast.error(error.message)
    } finally {
      setSaving(false)
    }
  }

  const remove = async (row) => {
    const ok = await confirm({
      title: 'Eliminar cliente',
      message: `¿Eliminar a ${row.fullName}? Esta acción no se puede deshacer.`,
      confirmLabel: 'Eliminar',
      danger: true,
    })
    if (!ok) return
    try {
      await owners.remove(row.id)
      toast.success('Cliente eliminado')
      list.reload()
    } catch (error) {
      toast.error(error.message)
    }
  }

  const columns = [
    { key: 'fullName', header: 'Nombre', className: 'cell-strong' },
    {
      key: 'email',
      header: 'Email',
      render: (row) => <a href={`mailto:${row.email}`}>{row.email}</a>,
    },
    { key: 'phone', header: 'Teléfono', render: (row) => row.phone ?? '—' },
    { key: 'address', header: 'Dirección', className: 'cell-muted', render: (row) => row.address ?? '—' },
    { key: 'createdAt', header: 'Alta', render: (row) => date(row.createdAt) },
    {
      key: 'actions',
      header: 'Acciones',
      hideLabel: true,
      className: 'cell-actions',
      render: (row) => (
        <div className="btn-row">
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => openEdit(row)}>Editar</button>
          <button type="button" className="btn btn-danger btn-sm" onClick={() => remove(row)}>Borrar</button>
        </div>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        eyebrow="Gestión"
        title="Clientes"
        description="Propietarios de mascotas y adoptantes de la clínica."
        actions={
          <>
            <button type="button" className="btn btn-ghost" onClick={list.reload}>↻ Actualizar</button>
            <button type="button" className="btn btn-primary" onClick={openCreate}>+ Nuevo cliente</button>
          </>
        }
      />

      <DataTable columns={columns} rows={list.rows} loading={list.loading} empty={
        <><div className="empty-icon">👤</div><p>Aún no hay clientes registrados.</p></>
      } />
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
        title={editing ? 'Editar cliente' : 'Nuevo cliente'}
        onClose={() => setOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-ghost" onClick={() => setOpen(false)}>Cancelar</button>
            <button type="submit" form="owner-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Guardando…' : 'Guardar'}
            </button>
          </>
        }
      >
        <form id="owner-form" onSubmit={submit}>
          <div className="form-grid">
            <Field label="Nombre completo" required error={errors.fullName} className="full">
              <TextInput
                value={values.fullName}
                invalid={Boolean(errors.fullName)}
                onChange={(event) => setField('fullName', event.target.value)}
                placeholder="Marta Ruiz"
              />
            </Field>
            <Field label="Email" required error={errors.email}>
              <TextInput
                type="email"
                value={values.email}
                invalid={Boolean(errors.email)}
                onChange={(event) => setField('email', event.target.value)}
                placeholder="marta.ruiz@example.com"
              />
            </Field>
            <Field label="Teléfono" required error={errors.phone}>
              <TextInput
                value={values.phone}
                invalid={Boolean(errors.phone)}
                onChange={(event) => setField('phone', event.target.value)}
                placeholder="+34 600 123 456"
              />
            </Field>
            <Field label="Dirección" error={errors.address} className="full">
              <TextInput
                value={values.address}
                invalid={Boolean(errors.address)}
                onChange={(event) => setField('address', event.target.value)}
                placeholder="Calle Mayor 12, Madrid"
              />
            </Field>
          </div>
        </form>
      </Modal>
    </>
  )
}
