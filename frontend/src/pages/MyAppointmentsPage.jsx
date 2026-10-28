import { useState } from 'react'
import PageHeader from '../components/PageHeader.jsx'
import DataTable from '../components/DataTable.jsx'
import Pagination from '../components/Pagination.jsx'
import Modal from '../components/Modal.jsx'
import Badge from '../components/Badge.jsx'
import { Field, SelectInput, TextArea, TextInput } from '../components/Field.jsx'
import { useApi, useList } from '../hooks/useApi.js'
import { useForm } from '../hooks/useForm.js'
import { portal } from '../lib/api.js'
import { useConfirm, useToast } from '../context/ui.jsx'
import { appointmentStatusTone, label } from '../lib/enums.js'
import { dateTime, fromLocalInput, toLocalInput } from '../lib/format.js'

const EMPTY = { petId: '', scheduledAt: '', reason: '', durationMinutes: 30, notes: '' }

export default function MyAppointmentsPage() {
  const toast = useToast()
  const confirm = useConfirm()

  const list = useList((params) => portal.appointments(params), {}, 10)
  const petsQuery = useApi(() => portal.pets({ page: 0, size: 200, sort: 'name,asc' }), [])
  const pets = petsQuery.data?.content ?? []

  const petOptions = pets.map((pet) => ({
    value: String(pet.id),
    label: `${pet.name} · ${label('species', pet.species)}`,
  }))

  const { values, setField, reset } = useForm(EMPTY)
  const [open, setOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [errors, setErrors] = useState({})

  const openCreate = () => {
    reset({
      ...EMPTY,
      petId: pets.length === 1 ? String(pets[0].id) : '',
      scheduledAt: toLocalInput(new Date(Date.now() + 24 * 3600 * 1000)),
    })
    setErrors({})
    setOpen(true)
  }

  const submit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setErrors({})
    try {
      await portal.bookAppointment({
        petId: Number(values.petId),
        scheduledAt: fromLocalInput(values.scheduledAt),
        reason: values.reason.trim(),
        durationMinutes: values.durationMinutes === '' ? null : Number(values.durationMinutes),
        notes: values.notes.trim() || null,
      })
      toast.success('Cita reservada. Te esperamos en la clínica.')
      setOpen(false)
      list.reload()
    } catch (error) {
      if (error.errors) setErrors(error.errors)
      else toast.error(error.message)
    } finally {
      setSaving(false)
    }
  }

  const cancel = async (row) => {
    const ok = await confirm({
      title: 'Cancelar cita',
      message: `¿Cancelar la cita de ${row.petName} del ${dateTime(row.scheduledAt)}?`,
      confirmLabel: 'Cancelar cita',
      danger: true,
    })
    if (!ok) return
    try {
      await portal.cancelAppointment(row.id)
      toast.success('Cita cancelada')
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
    { key: 'vetName', header: 'Veterinario', render: (row) => row.vetName ?? 'Por asignar' },
    { key: 'status', header: 'Estado', render: (row) => <Badge tone={appointmentStatusTone(row.status)}>{label('appointmentStatus', row.status)}</Badge> },
    {
      key: 'actions',
      header: 'Acciones',
      hideLabel: true,
      className: 'cell-actions',
      render: (row) => (
        row.status === 'scheduled'
          ? <button type="button" className="btn btn-danger btn-sm" onClick={() => cancel(row)}>Cancelar</button>
          : <span className="cell-muted">—</span>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        eyebrow="Mi cuenta"
        title="Mis citas"
        description="Reserva visitas para tus mascotas y cancela las que ya no necesites."
        actions={
          <>
            <button type="button" className="btn btn-ghost" onClick={list.reload}>↻ Actualizar</button>
            <button type="button" className="btn btn-primary" onClick={openCreate} disabled={!pets.length}>
              + Pedir cita
            </button>
          </>
        }
      />

      {!petsQuery.loading && !pets.length && (
        <div className="notice notice-info">
          Todavía no tienes mascotas registradas a tu nombre, así que no puedes reservar cita en línea.
          Llama al <a href="tel:+34900000000">900 000 000</a> y la clínica te ayudará.
        </div>
      )}

      <DataTable
        columns={columns}
        rows={list.rows}
        loading={list.loading}
        empty={<><div className="empty-icon">📅</div><p>No tienes citas registradas.</p></>}
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
        title="Pedir cita"
        onClose={() => setOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-ghost" onClick={() => setOpen(false)}>Cancelar</button>
            <button type="submit" form="client-appointment-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Reservando…' : 'Reservar cita'}
            </button>
          </>
        }
      >
        <form id="client-appointment-form" onSubmit={submit}>
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
                placeholder="Revisión anual, vacunación, cojera…"
              />
            </Field>
            <Field label="Duración (min)" error={errors.durationMinutes} hint="Si no lo indicas, 30 minutos.">
              <TextInput
                type="number"
                min="5"
                max="480"
                value={values.durationMinutes}
                invalid={Boolean(errors.durationMinutes)}
                onChange={(event) => setField('durationMinutes', event.target.value)}
              />
            </Field>
            <Field label="Notas para el veterinario" error={errors.notes} className="full">
              <TextArea
                value={values.notes}
                invalid={Boolean(errors.notes)}
                onChange={(event) => setField('notes', event.target.value)}
                placeholder="Prefiero por la mañana, el animal es nervioso…"
              />
            </Field>
          </div>
        </form>
      </Modal>
    </>
  )
}
