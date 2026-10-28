import { useState } from 'react'
import PageHeader from '../components/PageHeader.jsx'
import DataTable from '../components/DataTable.jsx'
import Pagination from '../components/Pagination.jsx'
import Modal from '../components/Modal.jsx'
import Badge from '../components/Badge.jsx'
import { Field, SelectInput, TextArea, TextInput } from '../components/Field.jsx'
import { useApi, useList } from '../hooks/useApi.js'
import { useForm } from '../hooks/useForm.js'
import { appointments, pets } from '../lib/api.js'
import { appointmentStatusTone, label, options, APPOINTMENT_STATUS } from '../lib/enums.js'
import { useConfirm, useToast } from '../context/ui.jsx'
import { dateTime, fromLocalInput, toLocalInput } from '../lib/format.js'

const EMPTY = { petId: '', scheduledAt: '', reason: '', vetName: '', durationMinutes: 30, notes: '' }
const statusOptions = options('appointmentStatus', APPOINTMENT_STATUS)
const statusFilterOptions = options('appointmentStatus', APPOINTMENT_STATUS)

export default function AppointmentsPage() {
  const toast = useToast()
  const confirm = useConfirm()

  const [filters, setFilters] = useState({ petId: '', status: '' })
  const list = useList((params) => appointments.list(params), filters, 10)
  const petsQuery = useApi(() => pets.list({ page: 0, size: 200, sort: 'name,asc' }), [])

  const petOptions = (petsQuery.data?.content ?? []).map((pet) => ({
    value: String(pet.id),
    label: `${pet.name} · ${label('species', pet.species)}`,
  }))

  const { values, setValues, setField, reset } = useForm(EMPTY)
  const [editing, setEditing] = useState(null)
  const [open, setOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [errors, setErrors] = useState({})

  const openCreate = () => {
    setEditing(null)
    reset({ ...EMPTY, scheduledAt: toLocalInput(new Date(Date.now() + 24 * 3600 * 1000)) })
    setErrors({})
    setOpen(true)
  }

  const openEdit = (row) => {
    setEditing(row)
    setValues({
      petId: String(row.petId),
      scheduledAt: toLocalInput(row.scheduledAt),
      reason: row.reason ?? '',
      vetName: row.vetName ?? '',
      durationMinutes: row.durationMinutes ?? 30,
      notes: row.notes ?? '',
    })
    setErrors({})
    setOpen(true)
  }

  const submit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setErrors({})
    const payload = {
      petId: Number(values.petId),
      scheduledAt: fromLocalInput(values.scheduledAt),
      reason: values.reason.trim(),
      vetName: values.vetName.trim() || null,
      durationMinutes: values.durationMinutes === '' ? null : Number(values.durationMinutes),
      notes: values.notes.trim() || null,
    }
    try {
      if (editing) {
        await appointments.update(editing.id, payload)
        toast.success('Cita actualizada')
      } else {
        await appointments.create(payload)
        toast.success('Cita reservada')
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

  const changeStatus = async (row, status) => {
    try {
      await appointments.changeStatus(row.id, status)
      toast.success(`Cita marcada como ${label('appointmentStatus', status).toLowerCase()}`)
      list.reload()
    } catch (error) {
      toast.error(error.message)
    }
  }

  const remove = async (row) => {
    const ok = await confirm({
      title: 'Eliminar cita',
      message: `¿Eliminar la cita de ${row.petName} del ${dateTime(row.scheduledAt)}?`,
      confirmLabel: 'Eliminar',
      danger: true,
    })
    if (!ok) return
    try {
      await appointments.remove(row.id)
      toast.success('Cita eliminada')
      list.reload()
    } catch (error) {
      toast.error(error.message)
    }
  }

  const columns = [
    { key: 'petName', header: 'Mascota', className: 'cell-strong' },
    { key: 'scheduledAt', header: 'Fecha', render: (row) => dateTime(row.scheduledAt) },
    { key: 'duration', header: 'Duración', render: (row) => `${row.durationMinutes} min` },
    { key: 'reason', header: 'Motivo', className: 'cell-muted' },
    { key: 'vetName', header: 'Veterinario', render: (row) => row.vetName ?? '—' },
    { key: 'status', header: 'Estado', render: (row) => <Badge tone={appointmentStatusTone(row.status)}>{label('appointmentStatus', row.status)}</Badge> },
    {
      key: 'actions',
      header: 'Acciones',
      hideLabel: true,
      className: 'cell-actions',
      render: (row) => (
        <div className="btn-row">
          {row.status === 'scheduled' && (
            <>
              <button type="button" className="btn btn-primary btn-sm" onClick={() => changeStatus(row, 'completed')}>Completar</button>
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => changeStatus(row, 'cancelled')}>Cancelar</button>
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => openEdit(row)}>Editar</button>
            </>
          )}
          {row.status === 'cancelled' && (
            <>
              <button type="button" className="btn btn-outline btn-sm" onClick={() => changeStatus(row, 'scheduled')}>Reactivar</button>
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => openEdit(row)}>Editar</button>
            </>
          )}
          {row.status === 'completed' && <span className="cell-muted">Historial clínico</span>}
          {row.status !== 'completed' && (
            <button type="button" className="btn btn-danger btn-sm" onClick={() => remove(row)}>Borrar</button>
          )}
        </div>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        eyebrow="Clínica"
        title="Citas"
        description="Reserva visitas veterinarias y gestiona su ciclo de vida."
        actions={
          <>
            <button type="button" className="btn btn-ghost" onClick={list.reload}>↻ Actualizar</button>
            <button type="button" className="btn btn-primary" onClick={openCreate}>+ Nueva cita</button>
          </>
        }
      />

      <div className="toolbar">
        <Field label="Mascota">
          <SelectInput
            value={filters.petId}
            placeholder="Todas"
            options={petOptions}
            onChange={(event) => setFilters((f) => ({ ...f, petId: event.target.value }))}
          />
        </Field>
        <Field label="Estado">
          <SelectInput
            value={filters.status}
            placeholder="Todos"
            options={statusFilterOptions}
            onChange={(event) => setFilters((f) => ({ ...f, status: event.target.value }))}
          />
        </Field>
        <span className="spacer" />
        <button type="button" className="btn btn-ghost btn-sm" onClick={() => setFilters({ petId: '', status: '' })}>
          Limpiar filtros
        </button>
      </div>

      <DataTable columns={columns} rows={list.rows} loading={list.loading} empty={
        <><div className="empty-icon">📅</div><p>No hay citas que coincidan con los filtros.</p></>
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
        title={editing ? 'Editar cita' : 'Nueva cita'}
        onClose={() => setOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-ghost" onClick={() => setOpen(false)}>Cancelar</button>
            <button type="submit" form="appointment-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Guardando…' : 'Guardar'}
            </button>
          </>
        }
      >
        <form id="appointment-form" onSubmit={submit}>
          <div className="form-grid">
            <Field label="Mascota" required error={errors.petId}>
              <SelectInput
                value={values.petId}
                invalid={Boolean(errors.petId)}
                placeholder="Selecciona una mascota"
                options={petOptions}
                onChange={(event) => setField('petId', event.target.value)}
              />
            </Field>
            <Field label="Fecha y hora" required error={errors.scheduledAt}>
              <TextInput
                type="datetime-local"
                value={values.scheduledAt}
                invalid={Boolean(errors.scheduledAt)}
                onChange={(event) => setField('scheduledAt', event.target.value)}
              />
            </Field>
            <Field label="Motivo" required error={errors.reason} className="full">
              <TextInput
                value={values.reason}
                invalid={Boolean(errors.reason)}
                onChange={(event) => setField('reason', event.target.value)}
                placeholder="Revisión anual y vacunación"
              />
            </Field>
            <Field label="Veterinario" error={errors.vetName}>
              <TextInput
                value={values.vetName}
                invalid={Boolean(errors.vetName)}
                onChange={(event) => setField('vetName', event.target.value)}
                placeholder="Dra. Elena Vázquez"
              />
            </Field>
            <Field label="Duración (min)" error={errors.durationMinutes} hint="Por defecto, 30 minutos.">
              <TextInput
                type="number"
                min="5"
                max="480"
                value={values.durationMinutes}
                invalid={Boolean(errors.durationMinutes)}
                onChange={(event) => setField('durationMinutes', event.target.value)}
              />
            </Field>
            <Field label="Notas" error={errors.notes} className="full">
              <TextArea
                value={values.notes}
                invalid={Boolean(errors.notes)}
                onChange={(event) => setField('notes', event.target.value)}
                placeholder="Traer el cartón de vacunación."
              />
            </Field>
          </div>
        </form>
      </Modal>
    </>
  )
}
