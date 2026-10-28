import { Link } from 'react-router-dom'
import { useApi } from '../hooks/useApi.js'
import { appointments, foods, owners, pets, sales } from '../lib/api.js'
import { money, num } from '../lib/format.js'

const MODULES = [
  { to: '/clientes', icon: '👤', title: 'Clientes', text: 'Altas, edición y bajas de propietarios y adoptantes.' },
  { to: '/mascotas', icon: '🐾', title: 'Mascotas', text: 'Ficha de cada animal, con dueño o en el refugio.' },
  { to: '/citas', icon: '📅', title: 'Citas', text: 'Reserva visitas y gestiona su estado.' },
  { to: '/pienso', icon: '🛒', title: 'Pienso', text: 'Catálogo de la tienda y control de stock.' },
  { to: '/ventas', icon: '🧾', title: 'Ventas', text: 'Registra tickets y consulta los más vendidos.' },
  { to: '/adopciones', icon: '🏠', title: 'Adopciones', text: 'Adopta, cancela o registra devoluciones.' },
]

function Stat({ icon, value, label, to }) {
  const content = (
    <div className="card card-hover stat">
      <span className="stat-icon">{icon}</span>
      <span className="stat-value">{value}</span>
      <span className="stat-label">{label}</span>
    </div>
  )
  return to ? <Link to={to} style={{ color: 'inherit' }}>{content}</Link> : content
}

export default function Dashboard() {
  const { data, loading, error } = useApi(async () => {
    const [ownersRes, petsRes, availableRes, apptRes, lowStock, salesRes, top] = await Promise.all([
      owners.list({ page: 0, size: 1 }),
      pets.list({ page: 0, size: 1 }),
      pets.list({ page: 0, size: 1, status: 'available' }),
      appointments.list({ page: 0, size: 1, status: 'scheduled' }),
      foods.lowStock(5),
      sales.list({ page: 0, size: 1 }),
      sales.topProducts(5),
    ])
    return {
      owners: ownersRes.totalElements,
      pets: petsRes.totalElements,
      available: availableRes.totalElements,
      appointments: apptRes.totalElements,
      lowStock,
      sales: salesRes.totalElements,
      top,
    }
  }, [])

  return (
    <>
      <section className="hero">
        <div className="hero-flags" aria-hidden="true"><span /><span /><span /></div>
        <h2>Tu clínica veterinaria, de un vistazo</h2>
        <p>
          Clientes, mascotas, citas, tienda de pienso y adopciones en un mismo panel. Todo lo que
          ocurre en Galgo Español, actualizado al instante.
        </p>
        <div className="page-actions">
          <Link className="btn btn-dark" to="/citas">Reservar una cita</Link>
          <Link className="btn btn-ghost" to="/mascotas">Ver mascotas del refugio</Link>
        </div>
      </section>

      {error && <div className="card">No se pudo cargar el resumen: {error.message}</div>}

      <div className="grid grid-4">
        <Stat icon="👤" value={loading ? '…' : num(data?.owners)} label="Clientes" to="/clientes" />
        <Stat icon="🐾" value={loading ? '…' : num(data?.pets)} label="Mascotas" to="/mascotas" />
        <Stat icon="🏠" value={loading ? '…' : num(data?.available)} label="En adopción" to="/adopciones" />
        <Stat icon="📅" value={loading ? '…' : num(data?.appointments)} label="Citas programadas" to="/citas" />
        <Stat icon="🛒" value={loading ? '…' : num(data?.lowStock?.length ?? 0)} label="Productos bajo stock" to="/pienso" />
        <Stat icon="🧾" value={loading ? '…' : num(data?.sales)} label="Tickets emitidos" to="/ventas" />
      </div>

      <div className="section-head">
        <span className="bar" />
        <h2>Módulos</h2>
      </div>
      <div className="grid grid-3">
        {MODULES.map((module) => (
          <Link key={module.to} to={module.to} style={{ color: 'inherit', textDecoration: 'none' }}>
            <div className="card card-hover">
              <div className="stat">
                <span className="stat-icon">{module.icon}</span>
                <h3 className="card-title">{module.title}</h3>
                <p className="cell-muted" style={{ margin: 0 }}>{module.text}</p>
              </div>
            </div>
          </Link>
        ))}
      </div>

      <div className="grid grid-2" style={{ marginTop: '1.6rem' }}>
        <section>
          <div className="section-head">
            <span className="bar" />
            <h2>Más vendidos</h2>
            <Link className="link link-more" to="/ventas">Ver ventas</Link>
          </div>
          <div className="card">
            {loading ? (
              <div className="spinner" />
            ) : data?.top?.length ? (
              <div className="sale-lines">
                {data.top.map((product, index) => (
                  <div key={product.productId} style={{ display: 'flex', alignItems: 'center', gap: '.7rem' }}>
                    <span className="badge badge-green">{index + 1}</span>
                    <span style={{ flex: 1 }} className="cell-strong">{product.productName}</span>
                    <span className="cell-muted">{num(product.unitsSold)} u.</span>
                    <strong>{money(product.revenue)}</strong>
                  </div>
                ))}
              </div>
            ) : (
              <p className="cell-muted" style={{ margin: 0 }}>Todavía no hay ventas registradas.</p>
            )}
          </div>
        </section>

        <section>
          <div className="section-head">
            <span className="bar" />
            <h2>Reponer stock</h2>
            <Link className="link link-more" to="/pienso">Ver catálogo</Link>
          </div>
          <div className="card">
            {loading ? (
              <div className="spinner" />
            ) : data?.lowStock?.length ? (
              <div className="sale-lines">
                {data.lowStock.map((product) => (
                  <div key={product.id} style={{ display: 'flex', alignItems: 'center', gap: '.7rem' }}>
                    <span style={{ flex: 1 }}>
                      <span className="cell-strong">{product.name}</span>
                      <br />
                      <span className="cell-muted">{product.brand}</span>
                    </span>
                    <span className={`badge ${product.stock === 0 ? 'badge-red' : 'badge-amber'}`}>
                      {product.stock} uds.
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="cell-muted" style={{ margin: 0 }}>Sin productos bajo stock. Todo en orden. ✅</p>
            )}
          </div>
        </section>
      </div>
    </>
  )
}
