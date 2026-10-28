import { useState } from 'react'
import PageHeader from '../components/PageHeader.jsx'
import DataTable from '../components/DataTable.jsx'
import Pagination from '../components/Pagination.jsx'
import Modal from '../components/Modal.jsx'
import Badge from '../components/Badge.jsx'
import { Field, SelectInput, TextArea, TextInput } from '../components/Field.jsx'
import { useList } from '../hooks/useApi.js'
import { useForm } from '../hooks/useForm.js'
import { foods } from '../lib/api.js'
import { FOOD_CATEGORIES, label, options } from '../lib/enums.js'
import { useConfirm, useToast } from '../context/ui.jsx'
import { money } from '../lib/format.js'

const EMPTY = {
  name: '', brand: '', category: 'dog_food', description: '', unitPrice: '', stock: 0, weightKg: '',
}
const categoryOptions = options('category', FOOD_CATEGORIES)

export default function FoodsPage() {
  const toast = useToast()
  const confirm = useConfirm()

  const [filters, setFilters] = useState({ category: '', brand: '', includeInactive: '', maxStock: '' })
  const list = useList((params) => foods.list(params), filters, 10)

  const { values, setValues, setField, reset } = useForm(EMPTY)
  const [editing, setEditing] = useState(null)
  const [open, setOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [errors, setErrors] = useState({})

  const [stockTarget, setStockTarget] = useState(null)
  const [delta, setDelta] = useState('')

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
      brand: row.brand ?? '',
      category: row.category ?? 'dog_food',
      description: row.description ?? '',
      unitPrice: row.unitPrice ?? '',
      stock: row.stock ?? 0,
      weightKg: row.weightKg ?? '',
    })
    setErrors({})
    setOpen(true)
  }

  const submit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setErrors({})
    const payload = {
      name: values.name.trim(),
      brand: values.brand.trim(),
      category: values.category,
      description: values.description.trim() || null,
      unitPrice: values.unitPrice === '' ? null : Number(values.unitPrice),
      stock: values.stock === '' ? 0 : Number(values.stock),
      weightKg: values.weightKg === '' ? null : Number(values.weightKg),
    }
    try {
      if (editing) {
        await foods.update(editing.id, payload)
        toast.success('Producto actualizado')
      } else {
        await foods.create(payload)
        toast.success('Producto creado')
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

  const applyStock = async (event) => {
    event.preventDefault()
    if (delta === '' || Number(delta) === 0) {
      toast.info('Indica un ajuste distinto de cero.')
      return
    }
    try {
      await foods.adjustStock(stockTarget.id, Number(delta))
      toast.success('Stock actualizado')
      setStockTarget(null)
      setDelta('')
      list.reload()
    } catch (error) {
      toast.error(error.message)
    }
  }

  const toggleActive = async (row) => {
    try {
      await foods.setActive(row.id, !row.active)
      toast.success(row.active ? 'Producto retirado del catálogo' : 'Producto puesto a la venta')
      list.reload()
    } catch (error) {
      toast.error(error.message)
    }
  }

  const remove = async (row) => {
    const ok = await confirm({
      title: 'Eliminar producto',
      message: `¿Eliminar «${row.name}»? Si aparece en ventas pasadas, la API solo lo retirará del catálogo.`,
      confirmLabel: 'Eliminar',
      danger: true,
    })
    if (!ok) return
    try {
      await foods.remove(row.id)
      toast.success('Producto eliminado')
      list.reload()
    } catch (error) {
      toast.error(error.message)
    }
  }

  const stockBadge = (stock) => {
    if (stock === 0) return <Badge tone="red">{stock}</Badge>
    if (stock <= 5) return <Badge tone="amber">{stock}</Badge>
    return <span>{stock}</span>
  }

  const columns = [
    { key: 'name', header: 'Producto', className: 'cell-strong' },
    { key: 'brand', header: 'Marca' },
    { key: 'category', header: 'Categoría', render: (row) => label('category', row.category) },
    { key: 'unitPrice', header: 'Precio', render: (row) => money(row.unitPrice) },
    { key: 'stock', header: 'Stock', render: (row) => stockBadge(row.stock) },
    { key: 'weightKg', header: 'Peso', render: (row) => (row.weightKg ? `${Number(row.weightKg)} kg` : '—') },
    {
      key: 'active',
      header: 'Catálogo',
      render: (row) => (row.active ? <Badge tone="green">A la venta</Badge> : <Badge tone="gray">Retirado</Badge>),
    },
    {
      key: 'actions',
      header: 'Acciones',
      hideLabel: true,
      className: 'cell-actions',
      render: (row) => (
        <div className="btn-row">
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => { setStockTarget(row); setDelta('') }}>Stock</button>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => openEdit(row)}>Editar</button>
          <button type="button" className="btn btn-outline btn-sm" onClick={() => toggleActive(row)}>
            {row.active ? 'Retirar' : 'Activar'}
          </button>
          <button type="button" className="btn btn-danger btn-sm" onClick={() => remove(row)}>Borrar</button>
        </div>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        eyebrow="Tienda"
        title="Pienso y productos"
        description="Catálogo de la tienda y control de existencias."
        actions={
          <>
            <button type="button" className="btn btn-ghost" onClick={list.reload}>↻ Actualizar</button>
            <button type="button" className="btn btn-primary" onClick={openCreate}>+ Nuevo producto</button>
          </>
        }
      />

      <div className="toolbar">
        <Field label="Categoría">
          <SelectInput
            value={filters.category}
            placeholder="Todas"
            options={categoryOptions}
            onChange={(event) => setFilters((f) => ({ ...f, category: event.target.value }))}
          />
        </Field>
        <Field label="Marca">
          <TextInput
            value={filters.brand}
            onChange={(event) => setFilters((f) => ({ ...f, brand: event.target.value }))}
            placeholder="Royal Canin"
          />
        </Field>
        <Field label="Stock máx.">
          <TextInput
            type="number"
            min="0"
            value={filters.maxStock}
            onChange={(event) => setFilters((f) => ({ ...f, maxStock: event.target.value }))}
            placeholder="5"
          />
        </Field>
        <Field label="Catálogo">
          <SelectInput
            value={filters.includeInactive}
            placeholder="Solo activos"
            options={[{ value: 'true', label: 'Incluir retirados' }]}
            onChange={(event) => setFilters((f) => ({ ...f, includeInactive: event.target.value }))}
          />
        </Field>
        <span className="spacer" />
        <button
          type="button"
          className="btn btn-ghost btn-sm"
          onClick={() => setFilters({ category: '', brand: '', includeInactive: '', maxStock: '' })}
        >
          Limpiar filtros
        </button>
      </div>

      <DataTable columns={columns} rows={list.rows} loading={list.loading} empty={
        <><div className="empty-icon">🛒</div><p>No hay productos que coincidan con los filtros.</p></>
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
        title={editing ? 'Editar producto' : 'Nuevo producto'}
        onClose={() => setOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-ghost" onClick={() => setOpen(false)}>Cancelar</button>
            <button type="submit" form="food-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Guardando…' : 'Guardar'}
            </button>
          </>
        }
      >
        <form id="food-form" onSubmit={submit}>
          <div className="form-grid">
            <Field label="Nombre" required error={errors.name}>
              <TextInput
                value={values.name}
                invalid={Boolean(errors.name)}
                onChange={(event) => setField('name', event.target.value)}
                placeholder="Crocodile Adult Mini"
              />
            </Field>
            <Field label="Marca" required error={errors.brand}>
              <TextInput
                value={values.brand}
                invalid={Boolean(errors.brand)}
                onChange={(event) => setField('brand', event.target.value)}
                placeholder="Royal Canin"
              />
            </Field>
            <Field label="Categoría" required error={errors.category}>
              <SelectInput
                value={values.category}
                invalid={Boolean(errors.category)}
                options={categoryOptions}
                onChange={(event) => setField('category', event.target.value)}
              />
            </Field>
            <Field label="Precio (€)" required error={errors.unitPrice}>
              <TextInput
                type="number"
                step="0.01"
                min="0.01"
                value={values.unitPrice}
                invalid={Boolean(errors.unitPrice)}
                onChange={(event) => setField('unitPrice', event.target.value)}
                placeholder="24.50"
              />
            </Field>
            <Field
              label="Stock"
              required
              error={errors.stock}
              hint={editing ? 'El stock se ajusta con el botón «Stock».' : undefined}
            >
              <TextInput
                type="number"
                min="0"
                value={values.stock}
                disabled={Boolean(editing)}
                invalid={Boolean(errors.stock)}
                onChange={(event) => setField('stock', event.target.value)}
              />
            </Field>
            <Field label="Peso (kg)" error={errors.weightKg}>
              <TextInput
                type="number"
                step="0.001"
                min="0.001"
                value={values.weightKg}
                invalid={Boolean(errors.weightKg)}
                onChange={(event) => setField('weightKg', event.target.value)}
                placeholder="3.000"
              />
            </Field>
            <Field label="Descripción" error={errors.description} className="full">
              <TextArea
                value={values.description}
                invalid={Boolean(errors.description)}
                onChange={(event) => setField('description', event.target.value)}
                placeholder="Fórmula para perros adultos pequeños, sabor pollo, saco de 3 kg."
              />
            </Field>
          </div>
        </form>
      </Modal>

      <Modal
        open={Boolean(stockTarget)}
        title="Ajustar stock"
        size="sm"
        onClose={() => setStockTarget(null)}
        footer={
          <>
            <button type="button" className="btn btn-ghost" onClick={() => setStockTarget(null)}>Cancelar</button>
            <button type="submit" form="stock-form" className="btn btn-primary">Aplicar</button>
          </>
        }
      >
        {stockTarget && (
          <form id="stock-form" onSubmit={applyStock}>
            <p className="modal-intro">
              <strong>{stockTarget.name}</strong> · stock actual: {stockTarget.stock} unidades.
            </p>
            <Field label="Movimiento" hint="Positivo para reponer, negativo para retirar unidades.">
              <TextInput
                type="number"
                value={delta}
                onChange={(event) => setDelta(event.target.value)}
                placeholder="20"
                autoFocus
              />
            </Field>
          </form>
        )}
      </Modal>
    </>
  )
}
