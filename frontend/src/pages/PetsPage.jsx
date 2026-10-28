import { useState } from 'react'
import PageHeader from '../components/PageHeader.jsx'
import DataTable from '../components/DataTable.jsx'
import Pagination from '../components/Pagination.jsx'
import Modal from '../components/Modal.jsx'
import Badge from '../components/Badge.jsx'
import { Field, SelectInput, TextArea, TextInput } from '../components/Field.jsx'
import { useApi, useList } from '../hooks/useApi.js'
import { useForm } from '../hooks/useForm.js'
import { owners, pets } from '../lib/api.js'
import { label, options, petStatusTone, PET_SEX, PET_STATUS, SPECIES } from '../lib/enums.js'
import { useConfirm, useToast } from '../context/ui.jsx'
import { date } from '../lib/format.js'

const EMPTY = {
  name: '', species: 'dog', breed: '', birthDate: '', sex: 'unknown', status: 'available', ownerId: '', notes: '',
}

const speciesOptions = options('species', SPECIES)
const sexOptions = options('sex', PET_SEX)
const statusOptions = options('petStatus', PET_STATUS.filter((status) => status !== 'adopted'))
const adoptedOption = { value: 'adopted', label: label('petStatus', 'adopted') }

export default function PetsPage() {
  const toast = useToast()
  const confirm = useConfirm()

  const [filters, setFilters] = useState({ species: '', status: '', shelter: '', ownerId: '' })
  const list = useList((params) => pets.list(params), filters, 10)
  const ownersQuery = useApi(() => owners.list({ page: 0, size: 100, sort: 'fullName,asc' }), [])

  const ownerOptions = (ownersQuery.data?.content ?? []).map((owner) => ({
    value: String(owner.id),
    label: `${owner.fullName} · ${owner.email}`,
  }))

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
      name: row.name ?? '',
      species: row.species ?? 'dog',
      breed: row.breed ?? '',
      birthDate: row.birthDate ?? '',
      sex: row.sex ?? 'unknown',
      status: row.status === 'adopted' ? 'adopted' : row.status ?? 'available',
      ownerId: row.ownerId ? String(row.ownerId) : '',
      notes: row.notes ?? '',
    })
    setErrors({})
    setOpen(true)
  }

  const onOwnerChange = (value) => {
    setValues((current) => ({
      ...current,
      ownerId: value,
      status: value ? 'owned' : 'available',
    }))
  }

  const submit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setErrors({})
    const payload = {
      name: values.name.trim(),
      species: values.species,
      breed: values.breed.trim() || null,
      birthDate: values.birthDate || null,
      sex: values.sex,
      status: values.status,
      ownerId: values.ownerId ? Number(values.ownerId) : null,
      notes: values.notes.trim() || null,
    }
    try {
      if (editing) {
        await pets.update(editing.id, payload)
        toast.success('Mascota actualizada')
      } else {
        await pets.create(payload)
        toast.success('Mascota registrada')
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
      title: 'Eliminar mascota',
      message: `¿Eliminar a ${row.name}? Si tiene citas o adopciones la API lo impedirá.`,
      confirmLabel: 'Eliminar',
      danger: true,
    })
    if (!ok) return
    try {
      await pets.remove(row.id)
      toast.success('Mascota eliminada')
      list.reload()
    } catch (error) {
      toast.error(error.message)
    }
  }

  const columns = [
    { key: 'name', header: 'Nombre', className: 'cell-strong' },
    { key: 'species', header: 'Especie', render: (row) => <Badge tone="gray" plain>{label('species', row.species)}</Badge> },
    { key: 'breed', header: 'Raza', render: (row) => row.breed ?? '—' },
    { key: 'sex', header: 'Sexo', render: (row) => label('sex', row.sex) },
    { key: 'status', header: 'Estado', render: (row) => <Badge tone={petStatusTone(row.status)}>{label('petStatus', row.status)}</Badge> },
    { key: 'owner', header: 'Dueño', render: (row) => row.ownerName ?? <span className="cell-muted">Refugio</span> },
    { key: 'birthDate', header: 'Nacimiento', render: (row) => date(row.birthDate) },
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
        title="Mascotas"
        description="Fichas de los animales de la clínica y del refugio."
        actions={
          <>
            <button type="button" className="btn btn-ghost" onClick={list.reload}>↻ Actualizar</button>
            <button type="button" className="btn btn-primary" onClick={openCreate}>+ Nueva mascota</button>
          </>
        }
      />

      <div className="toolbar">
        <Field label="Especie">
          <SelectInput
            value={filters.species}
            placeholder="Todas"
            options={speciesOptions}
            onChange={(event) => setFilters((f) => ({ ...f, species: event.target.value }))}
          />
        </Field>
        <Field label="Estado">
          <SelectInput
            value={filters.status}
            placeholder="Todos"
            options={statusOptions}
            onChange={(event) => setFilters((f) => ({ ...f, status: event.target.value }))}
          />
        </Field>
        <Field label="Dueño">
          <SelectInput
            value={filters.ownerId}
            placeholder="Cualquiera"
            options={ownerOptions}
            onChange={(event) => setFilters((f) => ({ ...f, ownerId: event.target.value }))}
          />
        </Field>
        <Field label="Refugio">
          <SelectInput
            value={filters.shelter}
            placeholder="Todos"
            options={[{ value: 'true', label: 'Solo en adopción' }, { value: 'false', label: 'Solo con dueño' }]}
            onChange={(event) => setFilters((f) => ({ ...f, shelter: event.target.value }))}
          />
        </Field>
        <span className="spacer" />
        <button
          type="button"
          className="btn btn-ghost btn-sm"
          onClick={() => setFilters({ species: '', status: '', shelter: '', ownerId: '' })}
        >
          Limpiar filtros
        </button>
      </div>

      <DataTable columns={columns} rows={list.rows} loading={list.loading} empty={
        <><div className="empty-icon">🐾</div><p>No hay mascotas que coincidan con los filtros.</p></>
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
        title={editing ? 'Editar mascota' : 'Nueva mascota'}
        onClose={() => setOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-ghost" onClick={() => setOpen(false)}>Cancelar</button>
            <button type="submit" form="pet-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Guardando…' : 'Guardar'}
            </button>
          </>
        }
      >
        <form id="pet-form" onSubmit={submit}>
          <div className="form-grid">
            <Field label="Nombre" required error={errors.name} className="full">
              <TextInput
                value={values.name}
                invalid={Boolean(errors.name)}
                onChange={(event) => setField('name', event.target.value)}
                placeholder="Roco"
              />
            </Field>
            <Field label="Especie" required error={errors.species}>
              <SelectInput
                value={values.species}
                invalid={Boolean(errors.species)}
                options={speciesOptions}
                onChange={(event) => setField('species', event.target.value)}
              />
            </Field>
            <Field label="Sexo" required error={errors.sex}>
              <SelectInput
                value={values.sex}
                invalid={Boolean(errors.sex)}
                options={sexOptions}
                onChange={(event) => setField('sex', event.target.value)}
              />
            </Field>
            <Field label="Raza" error={errors.breed}>
              <TextInput
                value={values.breed}
                invalid={Boolean(errors.breed)}
                onChange={(event) => setField('breed', event.target.value)}
                placeholder="Schnauzer"
              />
            </Field>
            <Field label="Fecha de nacimiento" error={errors.birthDate}>
              <TextInput
                type="date"
                value={values.birthDate}
                invalid={Boolean(errors.birthDate)}
                onChange={(event) => setField('birthDate', event.target.value)}
              />
            </Field>
            <Field
              label="Dueño"
              error={errors.ownerId}
              hint="Déjalo vacío para que quede en el refugio, disponible para adopción."
            >
              <SelectInput
                value={values.ownerId}
                placeholder="Sin dueño (refugio)"
                options={ownerOptions}
                onChange={(event) => onOwnerChange(event.target.value)}
              />
            </Field>
            <Field label="Estado" error={errors.status}>
              <SelectInput
                value={values.status}
                disabled={values.status === 'adopted'}
                options={values.status === 'adopted' ? [adoptedOption, ...statusOptions] : statusOptions}
                onChange={(event) => setField('status', event.target.value)}
              />
            </Field>
            <Field label="Notas" error={errors.notes} className="full">
              <TextArea
                value={values.notes}
                invalid={Boolean(errors.notes)}
                onChange={(event) => setField('notes', event.target.value)}
                placeholder="Vacunado y microchipado. Se lleva bien con niños."
              />
            </Field>
          </div>
        </form>
      </Modal>
    </>
  )
}
