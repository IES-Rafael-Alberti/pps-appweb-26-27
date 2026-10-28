import { useState } from 'react'
import PageHeader from '../components/PageHeader.jsx'
import DataTable from '../components/DataTable.jsx'
import Pagination from '../components/Pagination.jsx'
import Modal from '../components/Modal.jsx'
import Badge from '../components/Badge.jsx'
import { Field, SelectInput, TextArea, TextInput } from '../components/Field.jsx'
import { useApi, useList } from '../hooks/useApi.js'
import { adoptions, owners, pets } from '../lib/api.js'
import { adoptionStatusTone, label, options, ADOPTION_STATUS } from '../lib/enums.js'
import { useConfirm, useToast } from '../context/ui.jsx'
import { dateTime, money } from '../lib/format.js'

const statusOptions = options('adoptionStatus', ADOPTION_STATUS)

export default function AdoptionsPage() {
  const toast = useToast()
  const confirm = useConfirm()

  const [filters, setFilters] = useState({ status: '' })
  const list = useList((params) => adoptions.list(params), filters, 10)

  const availableQuery = useApi(() => pets.list({ page: 0, size: 200, sort: 'name,asc', status: 'available' }), [])
  const ownersQuery = useApi(() => owners.list({ page: 0, size: 100, sort: 'fullName,asc' }), [])

  const availablePets = availableQuery.data?.content ?? []
  const petOptions = availablePets.map((pet) => ({
    value: String(pet.id),
    label: `${pet.name} · ${label('species', pet.species)}`,
  }))
  const ownerOptions = (ownersQuery.data?.content ?? []).map((owner) => ({
    value: String(owner.id),
    label: `${owner.fullName} · ${owner.email}`,
  }))

  const [open, setOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [errors, setErrors] = useState({})
  const [form, setForm] = useState({ petId: '', adopterId: '', fee: '', notes: '' })

  const [action, setAction] = useState(null) // { type: 'cancel' | 'return', row }
  const [reason, setReason] = useState('')

  const setField = (name, value) => setForm((current) => ({ ...current, [name]: value }))

  const openCreate = () => {
    setForm({ petId: '', adopterId: '', fee: '', notes: '' })
    setErrors({})
    setOpen(true)
  }

  const submit = async (event) => {
    event.preventDefault()
    setErrors({})
    if (!form.petId || !form.adopterId) {
      setErrors({
        petId: form.petId ? undefined : 'petId is required',
        adopterId: form.adopterId ? undefined : 'adopterId is required',
      })
      return
    }
    setSaving(true)
    try {
      const adoption = await adoptions.adopt({
        petId: Number(form.petId),
        adopterId: Number(form.adopterId),
        fee: form.fee === '' ? null : Number(form.fee),
        notes: form.notes.trim() || null,
      })
      toast.success(`${adoption.petName} adoptada por ${adoption.adopterName}`)
      setOpen(false)
      list.reload()
      availableQuery.reload()
    } catch (error) {
      if (error.errors) setErrors(error.errors)
      else toast.error(error.message)
    } finally {
      setSaving(false)
    }
  }

  const runAction = async (event) => {
    event.preventDefault()
    try {
      if (action.type === 'cancel') await adoptions.cancel(action.row.id, reason.trim() || null)
      else await adoptions.markReturned(action.row.id, reason.trim() || null)
      toast.success(action.type === 'cancel' ? 'Adopción cancelada' : 'Devolución registrada')
      setAction(null)
      setReason('')
      list.reload()
      availableQuery.reload()
    } catch (error) {
      toast.error(error.message)
    }
  }

  const remove = async (row) => {
    const ok = await confirm({
      title: 'Eliminar registro',
      message: `¿Eliminar la adopción de ${row.petName}? Solo se pueden borrar las cerradas.`,
      confirmLabel: 'Eliminar',
      danger: true,
    })
    if (!ok) return
    try {
      await adoptions.remove(row.id)
      toast.success('Registro eliminado')
      list.reload()
    } catch (error) {
      toast.error(error.message)
    }
  }

  const columns = [
    { key: 'petName', header: 'Mascota', className: 'cell-strong' },
    { key: 'petSpecies', header: 'Especie', render: (row) => label('species', row.petSpecies) },
    { key: 'adopterName', header: 'Adoptante' },
    { key: 'adoptedAt', header: 'Fecha', render: (row) => dateTime(row.adoptedAt) },
    { key: 'status', header: 'Estado', render: (row) => <Badge tone={adoptionStatusTone(row.status)}>{label('adoptionStatus', row.status)}</Badge> },
    { key: 'fee', header: 'Tarifa', render: (row) => money(row.fee) },
    {
      key: 'actions',
      header: 'Acciones',
      hideLabel: true,
      className: 'cell-actions',
      render: (row) => (
        <div className="btn-row">
          {row.status === 'completed' && (
            <>
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => { setAction({ type: 'cancel', row }); setReason('') }}>
                Cancelar
              </button>
              <button type="button" className="btn btn-outline btn-sm" onClick={() => { setAction({ type: 'return', row }); setReason('') }}>
                Devolver
              </button>
            </>
          )}
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
        eyebrow="Refugio"
        title="Adopciones"
        description="Animales que encuentran hogar. Solo se adoptan mascotas disponibles."
        actions={
          <>
            <button type="button" className="btn btn-ghost" onClick={list.reload}>↻ Actualizar</button>
            <button type="button" className="btn btn-primary" onClick={openCreate}>+ Nueva adopción</button>
          </>
        }
      />

      <div className="toolbar">
        <Field label="Estado">
          <SelectInput
            value={filters.status}
            placeholder="Todos"
            options={statusOptions}
            onChange={(event) => setFilters({ status: event.target.value })}
          />
        </Field>
        <span className="spacer" />
        <button type="button" className="btn btn-ghost btn-sm" onClick={() => setFilters({ status: '' })}>
          Limpiar filtros
        </button>
      </div>

      <DataTable columns={columns} rows={list.rows} loading={list.loading} empty={
        <><div className="empty-icon">🏠</div><p>No hay adopciones que coincidan con los filtros.</p></>
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
        title="Nueva adopción"
        onClose={() => setOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-ghost" onClick={() => setOpen(false)}>Cancelar</button>
            <button type="submit" form="adoption-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Adoptando…' : 'Registrar adopción'}
            </button>
          </>
        }
      >
        <form id="adoption-form" onSubmit={submit}>
          <div className="form-grid">
            <Field label="Mascota" required error={errors.petId} hint="Solo aparecen las que están en adopción.">
              <SelectInput
                value={form.petId}
                invalid={Boolean(errors.petId)}
                placeholder={availablePets.length ? 'Selecciona una mascota' : 'No hay mascotas disponibles'}
                options={petOptions}
                onChange={(event) => setField('petId', event.target.value)}
              />
            </Field>
            <Field label="Adoptante" required error={errors.adopterId}>
              <SelectInput
                value={form.adopterId}
                invalid={Boolean(errors.adopterId)}
                placeholder="Selecciona un cliente"
                options={ownerOptions}
                onChange={(event) => setField('adopterId', event.target.value)}
              />
            </Field>
            <Field label="Tarifa (€)" error={errors.fee}>
              <TextInput
                type="number"
                step="0.01"
                min="0"
                value={form.fee}
                invalid={Boolean(errors.fee)}
                onChange={(event) => setField('fee', event.target.value)}
                placeholder="85.00"
              />
            </Field>
            <Field label="Notas" error={errors.notes} className="full">
              <TextArea
                value={form.notes}
                invalid={Boolean(errors.notes)}
                onChange={(event) => setField('notes', event.target.value)}
                placeholder="Recogida el sábado por la mañana."
              />
            </Field>
          </div>
        </form>
      </Modal>

      <Modal
        open={Boolean(action)}
        title={action?.type === 'cancel' ? 'Cancelar adopción' : 'Registrar devolución'}
        size="sm"
        onClose={() => setAction(null)}
        footer={
          <>
            <button type="button" className="btn btn-ghost" onClick={() => setAction(null)}>Cerrar</button>
            <button type="submit" form="adoption-action-form" className="btn btn-primary">Confirmar</button>
          </>
        }
      >
        {action && (
          <form id="adoption-action-form" onSubmit={runAction}>
            <p className="modal-intro">
              <strong>{action.row.petName}</strong> volverá al refugio y quedará disponible para adopción de nuevo.
            </p>
            <Field label="Motivo">
              <TextArea
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                placeholder="Alergias en el nuevo hogar"
              />
            </Field>
          </form>
        )}
      </Modal>
    </>
  )
}
