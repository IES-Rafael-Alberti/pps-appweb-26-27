import { useMemo, useState } from 'react'
import PageHeader from '../components/PageHeader.jsx'
import DataTable from '../components/DataTable.jsx'
import Pagination from '../components/Pagination.jsx'
import Modal from '../components/Modal.jsx'
import Badge from '../components/Badge.jsx'
import { Field, SelectInput, TextArea, TextInput } from '../components/Field.jsx'
import { useApi, useList } from '../hooks/useApi.js'
import { foods, owners, sales } from '../lib/api.js'
import { label, options, PAYMENT_METHODS } from '../lib/enums.js'
import { useToast } from '../context/ui.jsx'
import { dateTime, money, num } from '../lib/format.js'

const paymentOptions = options('payment', PAYMENT_METHODS)

export default function SalesPage() {
  const toast = useToast()

  const [filters, setFilters] = useState({ paymentMethod: '' })
  const list = useList((params) => sales.list(params), filters, 10)

  const productsQuery = useApi(() => foods.list({ page: 0, size: 200, sort: 'name,asc' }), [])
  const ownersQuery = useApi(() => owners.list({ page: 0, size: 100, sort: 'fullName,asc' }), [])
  const topQuery = useApi(() => sales.topProducts(5), [])

  const products = productsQuery.data?.content ?? []
  const productOptions = products.map((product) => ({
    value: String(product.id),
    label: `${product.name} · ${money(product.unitPrice)} (${product.stock} uds.)`,
  }))
  const ownerOptions = (ownersQuery.data?.content ?? []).map((owner) => ({
    value: String(owner.id),
    label: `${owner.fullName} · ${owner.email}`,
  }))

  const [open, setOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [errors, setErrors] = useState({})
  const [customerId, setCustomerId] = useState('')
  const [customerName, setCustomerName] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('card')
  const [notes, setNotes] = useState('')
  const [lines, setLines] = useState([{ productId: '', quantity: 1 }])

  const [detail, setDetail] = useState(null)

  const previewTotal = useMemo(() => {
    return lines.reduce((total, line) => {
      const product = products.find((item) => String(item.id) === String(line.productId))
      if (!product) return total
      return total + Number(product.unitPrice) * (Number(line.quantity) || 0)
    }, 0)
  }, [lines, products])

  const openCreate = () => {
    setCustomerId('')
    setCustomerName('')
    setPaymentMethod('card')
    setNotes('')
    setLines([{ productId: '', quantity: 1 }])
    setErrors({})
    setOpen(true)
  }

  const onCustomerChange = (value) => {
    setCustomerId(value)
    const owner = (ownersQuery.data?.content ?? []).find((item) => String(item.id) === value)
    if (owner) setCustomerName(owner.fullName)
  }

  const setLine = (index, patch) => {
    setLines((current) => current.map((line, i) => (i === index ? { ...line, ...patch } : line)))
  }

  const addLine = () => setLines((current) => [...current, { productId: '', quantity: 1 }])
  const removeLine = (index) => setLines((current) => current.filter((_, i) => i !== index))

  const submit = async (event) => {
    event.preventDefault()
    setErrors({})

    const cleanLines = lines
      .filter((line) => line.productId !== '')
      .map((line) => ({ productId: Number(line.productId), quantity: Number(line.quantity) }))

    if (cleanLines.length === 0) {
      toast.info('Añade al menos una línea con producto y cantidad.')
      return
    }
    if (!customerName.trim()) {
      setErrors({ customerName: 'customerName is required' })
      return
    }

    setSaving(true)
    try {
      const sale = await sales.create({
        customerId: customerId ? Number(customerId) : null,
        customerName: customerName.trim(),
        paymentMethod,
        lines: cleanLines,
        notes: notes.trim() || null,
      })
      toast.success(`Ticket ${sale.saleNumber} emitido por ${money(sale.totalAmount)}`)
      setOpen(false)
      list.reload()
      productsQuery.reload()
      topQuery.reload()
    } catch (error) {
      if (error.errors) setErrors(error.errors)
      else toast.error(error.message)
    } finally {
      setSaving(false)
    }
  }

  const columns = [
    { key: 'saleNumber', header: 'Ticket', className: 'cell-strong' },
    { key: 'soldAt', header: 'Fecha', render: (row) => dateTime(row.soldAt) },
    { key: 'customerName', header: 'Cliente' },
    { key: 'totalItems', header: 'Unidades', render: (row) => num(row.totalItems) },
    { key: 'totalAmount', header: 'Total', render: (row) => <strong>{money(row.totalAmount)}</strong> },
    { key: 'paymentMethod', header: 'Pago', render: (row) => <Badge tone="blue" plain>{label('payment', row.paymentMethod)}</Badge> },
    {
      key: 'actions',
      header: 'Acciones',
      hideLabel: true,
      className: 'cell-actions',
      render: (row) => (
        <button type="button" className="btn btn-ghost btn-sm" onClick={() => setDetail(row)}>Ver ticket</button>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        eyebrow="Tienda"
        title="Ventas"
        description="Tickets emitidos. Los precios y totales los calcula siempre el servidor."
        actions={
          <>
            <button type="button" className="btn btn-ghost" onClick={list.reload}>↻ Actualizar</button>
            <button type="button" className="btn btn-primary" onClick={openCreate}>+ Nueva venta</button>
          </>
        }
      />

      <div className="toolbar">
        <Field label="Forma de pago">
          <SelectInput
            value={filters.paymentMethod}
            placeholder="Todas"
            options={paymentOptions}
            onChange={(event) => setFilters({ paymentMethod: event.target.value })}
          />
        </Field>
        <span className="spacer" />
        <button type="button" className="btn btn-ghost btn-sm" onClick={() => setFilters({ paymentMethod: '' })}>
          Limpiar filtros
        </button>
      </div>

      <DataTable columns={columns} rows={list.rows} loading={list.loading} empty={
        <><div className="empty-icon">🧾</div><p>Todavía no hay tickets registrados.</p></>
      } />
      <Pagination
        page={list.page}
        size={list.size}
        totalPages={list.totalPages}
        totalElements={list.totalElements}
        onPage={list.setPage}
        onSize={list.setSize}
      />

      <div className="section-head">
        <span className="bar" />
        <h2>Más vendidos</h2>
      </div>
      <div className="grid grid-3">
        {(topQuery.data ?? []).map((product) => (
          <div key={product.productId} className="card">
            <h3 className="card-title">{product.productName}</h3>
            <p className="cell-muted" style={{ margin: '0 0 .4rem' }}>{num(product.unitsSold)} unidades vendidas</p>
            <strong style={{ fontFamily: 'var(--font-head)', fontSize: '1.3rem' }}>{money(product.revenue)}</strong>
          </div>
        ))}
        {!topQuery.loading && (topQuery.data ?? []).length === 0 && (
          <p className="cell-muted">Sin datos de ventas todavía.</p>
        )}
      </div>

      {/* Nueva venta */}
      <Modal
        open={open}
        title="Nueva venta"
        size="lg"
        onClose={() => setOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-ghost" onClick={() => setOpen(false)}>Cancelar</button>
            <button type="submit" form="sale-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Emitiendo…' : 'Emitir ticket'}
            </button>
          </>
        }
      >
        <form id="sale-form" onSubmit={submit}>
          <div className="form-grid">
            <Field label="Cliente registrado" error={errors.customerId} hint="Opcional: para ventas de mostrador deja el nombre a mano.">
              <SelectInput
                value={customerId}
                placeholder="Sin registrar"
                options={ownerOptions}
                onChange={(event) => onCustomerChange(event.target.value)}
              />
            </Field>
            <Field label="Nombre en el ticket" required error={errors.customerName}>
              <TextInput
                value={customerName}
                invalid={Boolean(errors.customerName)}
                onChange={(event) => setCustomerName(event.target.value)}
                placeholder="Lucía Fernández"
              />
            </Field>
            <Field label="Forma de pago" required error={errors.paymentMethod}>
              <SelectInput
                value={paymentMethod}
                options={paymentOptions}
                onChange={(event) => setPaymentMethod(event.target.value)}
              />
            </Field>
            <Field label="Notas" error={errors.notes}>
              <TextInput
                value={notes}
                invalid={Boolean(errors.notes)}
                onChange={(event) => setNotes(event.target.value)}
                placeholder="Descuento de socio"
              />
            </Field>
          </div>

          <Field label="Líneas del ticket" required error={errors['lines'] || errors.lines}>
            <div className="sale-lines">
              {lines.map((line, index) => {
                const product = products.find((item) => String(item.id) === String(line.productId))
                const lineTotal = product ? Number(product.unitPrice) * (Number(line.quantity) || 0) : 0
                return (
                  <div className="sale-line" key={index}>
                    <Field label={`Producto ${index + 1}`}>
                      <SelectInput
                        value={line.productId}
                        placeholder="Selecciona un producto"
                        options={productOptions}
                        onChange={(event) => setLine(index, { productId: event.target.value })}
                      />
                    </Field>
                    <Field label="Cantidad">
                      <TextInput
                        type="number"
                        min="1"
                        value={line.quantity}
                        onChange={(event) => setLine(index, { quantity: event.target.value })}
                      />
                    </Field>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '.5rem', paddingBottom: '.9rem' }}>
                      <strong style={{ minWidth: '72px', textAlign: 'right' }}>{money(lineTotal)}</strong>
                      <button
                        type="button"
                        className="icon-btn"
                        onClick={() => removeLine(index)}
                        disabled={lines.length === 1}
                        aria-label="Quitar línea"
                      >
                        ×
                      </button>
                    </div>
                  </div>
                )
              })}
            </div>
          </Field>

          <div className="btn-row">
            <button type="button" className="btn btn-outline btn-sm" onClick={addLine}>+ Añadir línea</button>
          </div>

          <div className="sale-summary">
            <span>Total estimado</span>
            <strong>{money(previewTotal)}</strong>
          </div>
          <p className="help-text">El importe definitivo lo confirma el servidor al emitir el ticket.</p>
        </form>
      </Modal>

      {/* Detalle de ticket */}
      <Modal
        open={Boolean(detail)}
        title={detail ? `Ticket ${detail.saleNumber}` : ''}
        size="lg"
        onClose={() => setDetail(null)}
        footer={<button type="button" className="btn btn-ghost" onClick={() => setDetail(null)}>Cerrar</button>}
      >
        {detail && (
          <>
            <dl className="kv">
              <dt>Fecha</dt><dd>{dateTime(detail.soldAt)}</dd>
              <dt>Cliente</dt><dd>{detail.customerName}</dd>
              <dt>Pago</dt><dd>{label('payment', detail.paymentMethod)}</dd>
              <dt>Notas</dt><dd>{detail.notes ?? '—'}</dd>
            </dl>
            <div className="table-wrap" style={{ marginTop: '1rem' }}>
              <table className="data">
                <thead>
                  <tr><th>Producto</th><th>Cantidad</th><th>Precio</th><th>Importe</th></tr>
                </thead>
                <tbody>
                  {detail.lines.map((line) => (
                    <tr key={line.productId}>
                      <td data-label="Producto">
                        <span className="cell-strong">{line.productName}</span>
                        <br />
                        <span className="cell-muted">{line.productBrand}</span>
                      </td>
                      <td data-label="Cantidad">{line.quantity}</td>
                      <td data-label="Precio">{money(line.unitPrice)}</td>
                      <td data-label="Importe">{money(line.lineTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="sale-summary">
              <span>{detail.totalItems} unidades</span>
              <strong>{money(detail.totalAmount)}</strong>
            </div>
          </>
        )}
      </Modal>
    </>
  )
}
