import { useState } from 'react'
import { Link } from 'react-router-dom'
import PageHeader from '../components/PageHeader.jsx'
import Pagination from '../components/Pagination.jsx'
import Modal from '../components/Modal.jsx'
import Badge from '../components/Badge.jsx'
import { Field, SelectInput, TextArea, TextInput } from '../components/Field.jsx'
import { useList } from '../hooks/useApi.js'
import { portal } from '../lib/api.js'
import { useToast } from '../context/ui.jsx'
import { FOOD_CATEGORIES, PAYMENT_METHODS, label, options } from '../lib/enums.js'
import { money, num } from '../lib/format.js'

const categoryOptions = options('category', FOOD_CATEGORIES)
const paymentOptions = options('payment', PAYMENT_METHODS)

function stockTone(stock) {
  if (stock === 0) return 'red'
  if (stock <= 5) return 'amber'
  return 'green'
}

export default function ShopPage() {
  const toast = useToast()
  const [filters, setFilters] = useState({ category: '', brand: '' })
  const [brandInput, setBrandInput] = useState('')
  const list = useList((params) => portal.shopProducts(params), filters, 12)

  const [cart, setCart] = useState({})
  const [open, setOpen] = useState(false)
  const [paymentMethod, setPaymentMethod] = useState('card')
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const [lastOrder, setLastOrder] = useState(null)

  const entries = Object.values(cart)
  const totalItems = entries.reduce((sum, entry) => sum + entry.quantity, 0)
  const total = entries.reduce(
    (sum, entry) => sum + entry.quantity * Number(entry.product.unitPrice),
    0,
  )

  const add = (product) => {
    if (product.stock === 0) return
    setCart((current) => {
      const quantity = current[product.id]?.quantity ?? 0
      if (quantity >= product.stock) return current
      return { ...current, [product.id]: { product, quantity: quantity + 1 } }
    })
  }

  const changeQuantity = (entry, delta) => {
    setCart((current) => {
      const next = entry.quantity + delta
      if (next <= 0) {
        const copy = { ...current }
        delete copy[entry.product.id]
        return copy
      }
      if (next > entry.product.stock) return current
      return { ...current, [entry.product.id]: { ...entry, quantity: next } }
    })
  }

  const applyBrand = (event) => {
    event.preventDefault()
    setFilters((current) => ({ ...current, brand: brandInput.trim() }))
  }

  const placeOrder = async (event) => {
    event.preventDefault()
    setSaving(true)
    try {
      const sale = await portal.order({
        paymentMethod,
        notes: notes.trim() || null,
        lines: entries.map((entry) => ({
          productId: entry.product.id,
          quantity: entry.quantity,
        })),
      })
      setLastOrder(sale)
      setCart({})
      setNotes('')
      setPaymentMethod('card')
      setOpen(false)
      toast.success(`Pedido ${sale.saleNumber} confirmado`)
      list.reload()
    } catch (error) {
      toast.error(error.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Tienda"
        title="Pienso y cuidado"
        description="Compra en la tienda de la clínica y recoge tu pedido cuando vengas. El stock se reserva al confirmar."
        actions={<button type="button" className="btn btn-ghost" onClick={list.reload}>↻ Actualizar</button>}
      />

      {lastOrder && (
        <div className="notice notice-success">
          Pedido <strong>{lastOrder.saleNumber}</strong> confirmado por <strong>{money(lastOrder.totalAmount)}</strong>.
          Puedes verlo en <Link to="/mis-compras">Mis compras</Link>.
          <button type="button" className="icon-btn" aria-label="Cerrar aviso" onClick={() => setLastOrder(null)}>×</button>
        </div>
      )}

      <div className="toolbar">
        <Field label="Categoría">
          <SelectInput
            value={filters.category}
            placeholder="Todas"
            options={categoryOptions}
            onChange={(event) => setFilters((current) => ({ ...current, category: event.target.value }))}
          />
        </Field>
        <form className="toolbar-search" onSubmit={applyBrand}>
          <Field label="Marca">
            <TextInput
              value={brandInput}
              placeholder="Royal Canin, Purina…"
              onChange={(event) => setBrandInput(event.target.value)}
            />
          </Field>
          <button type="submit" className="btn btn-outline">Buscar</button>
        </form>
        <span className="spacer" />
        <button
          type="button"
          className="btn btn-ghost btn-sm"
          onClick={() => {
            setBrandInput('')
            setFilters({ category: '', brand: '' })
          }}
        >
          Limpiar filtros
        </button>
      </div>

      <div className="shop-layout">
        <div>
          {list.loading ? (
            <div className="spinner" />
          ) : list.rows.length === 0 ? (
            <div className="empty">
              <div className="empty-icon">🛒</div>
              <p>No hay productos que coincidan con los filtros.</p>
            </div>
          ) : (
            <div className="shop-grid">
              {list.rows.map((product) => {
                const inCart = cart[product.id]?.quantity ?? 0
                return (
                  <article key={product.id} className="card shop-card">
                    <div className="shop-card-top">
                      <Badge tone="blue">{label('category', product.category)}</Badge>
                      <Badge tone={stockTone(product.stock)}>
                        {product.stock === 0 ? 'Agotado' : `${num(product.stock)} uds.`}
                      </Badge>
                    </div>
                    <h3 className="card-title">{product.name}</h3>
                    <p className="shop-brand">{product.brand}</p>
                    {product.description && <p className="cell-muted shop-desc">{product.description}</p>}
                    <div className="shop-card-foot">
                      <div>
                        <span className="shop-price">{money(product.unitPrice)}</span>
                        {product.weightKg && (
                          <span className="cell-muted"> · {num(product.weightKg)} kg</span>
                        )}
                      </div>
                      {product.stock === 0 ? (
                        <button type="button" className="btn btn-ghost btn-sm" disabled>Sin stock</button>
                      ) : inCart === 0 ? (
                        <button type="button" className="btn btn-primary btn-sm" onClick={() => add(product)}>
                          Añadir
                        </button>
                      ) : (
                        <div className="qty-controls">
                          <button type="button" className="qty-btn" aria-label="Quitar una unidad"
                            onClick={() => changeQuantity(cart[product.id], -1)}>−</button>
                          <span>{inCart}</span>
                          <button type="button" className="qty-btn" aria-label="Añadir una unidad"
                            disabled={inCart >= product.stock}
                            onClick={() => add(product)}>+</button>
                        </div>
                      )}
                    </div>
                  </article>
                )
              })}
            </div>
          )}

          <Pagination
            page={list.page}
            size={list.size}
            totalPages={list.totalPages}
            totalElements={list.totalElements}
            onPage={list.setPage}
            onSize={list.setSize}
          />
        </div>

        <aside className="card shop-cart">
          <h2 className="card-title">Mi cesta</h2>
          {entries.length === 0 ? (
            <p className="cell-muted">Todavía no has añadido ningún producto.</p>
          ) : (
            <>
              <div className="cart-lines">
                {entries.map((entry) => (
                  <div key={entry.product.id} className="cart-line">
                    <div>
                      <strong>{entry.product.name}</strong>
                      <div className="cell-muted">
                        {entry.quantity} × {money(entry.product.unitPrice)}
                      </div>
                    </div>
                    <strong>{money(entry.quantity * Number(entry.product.unitPrice))}</strong>
                  </div>
                ))}
              </div>
              <div className="cart-total">
                <span>{totalItems} artículo{totalItems === 1 ? '' : 's'}</span>
                <strong>{money(total)}</strong>
              </div>
              <button type="button" className="btn btn-primary btn-block" onClick={() => setOpen(true)}>
                Confirmar pedido
              </button>
              <button type="button" className="btn btn-ghost btn-block" onClick={() => setCart({})}>
                Vaciar cesta
              </button>
            </>
          )}
        </aside>
      </div>

      <Modal
        open={open}
        title="Confirmar pedido"
        onClose={() => setOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-ghost" onClick={() => setOpen(false)}>Seguir comprando</button>
            <button type="submit" form="shop-order-form" className="btn btn-primary" disabled={saving || !entries.length}>
              {saving ? 'Confirmando…' : `Confirmar · ${money(total)}`}
            </button>
          </>
        }
      >
        <form id="shop-order-form" onSubmit={placeOrder}>
          <div className="cart-lines">
            {entries.map((entry) => (
              <div key={entry.product.id} className="cart-line">
                <div>
                  <strong>{entry.product.name}</strong>
                  <div className="cell-muted">{entry.quantity} × {money(entry.product.unitPrice)}</div>
                </div>
                <strong>{money(entry.quantity * Number(entry.product.unitPrice))}</strong>
              </div>
            ))}
          </div>
          <div className="cart-total">
            <span>Total</span>
            <strong>{money(total)}</strong>
          </div>
          <Field label="Forma de pago" required>
            <SelectInput
              value={paymentMethod}
              options={paymentOptions}
              onChange={(event) => setPaymentMethod(event.target.value)}
            />
          </Field>
          <Field label="Notas" hint="Por ejemplo, cuándo pasarás a recogerlo.">
            <TextArea
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              placeholder="Recojo el sábado por la mañana."
            />
          </Field>
          <p className="cell-muted" style={{ margin: 0 }}>
            El importe final y el stock se calculan en el servidor al confirmar.
          </p>
        </form>
      </Modal>
    </>
  )
}
