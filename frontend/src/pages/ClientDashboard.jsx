import { Link } from 'react-router-dom'
import { useApi } from '../hooks/useApi.js'
import { portal } from '../lib/api.js'
import { dateTime, num, titleCase } from '../lib/format.js'

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

export default function ClientDashboard() {
  const { data, loading, error } = useApi(async () => {
    const [profile, pets, appointments, purchases] = await Promise.all([
      portal.profile(),
      portal.pets({ page: 0, size: 1 }),
      portal.appointments({ page: 0, size: 50 }),
      portal.purchases({ page: 0, size: 1 }),
    ])
    return { profile, pets, appointments, purchases }
  }, [])

  const upcoming = (data?.appointments?.content ?? [])
    .filter((appointment) => appointment.status === 'scheduled'
      && new Date(appointment.scheduledAt).getTime() > Date.now())
    .sort((a, b) => new Date(a.scheduledAt) - new Date(b.scheduledAt))
    .slice(0, 3)

  const firstName = data?.profile?.fullName?.split(' ')[0]

  return (
    <>
      <section className="hero hero-client">
        <div className="hero-flags" aria-hidden="true"><span /><span /><span /></div>
        <h2>{loading ? 'Hola' : `Hola, ${firstName || 'de nuevo'}`}</h2>
        <p>
          Este es tu espacio en la Clínica Galgo Español. Consulta tus mascotas, reserva una visita y
          revisa tus compras sin llamar por teléfono.
        </p>
        <div className="page-actions">
          <Link className="btn btn-dark" to="/mis-citas">Pedir cita</Link>
          <Link className="btn btn-ghost" to="/tienda">Tienda de pienso</Link>
        </div>
      </section>

      {error && <div className="card">No se pudo cargar tu resumen: {error.message}</div>}

      <div className="grid grid-4">
        <Stat icon="🐾" value={loading ? '…' : num(data?.pets?.totalElements)} label="Mis mascotas" to="/mis-mascotas" />
        <Stat icon="📅" value={loading ? '…' : num(upcoming.length)} label="Próximas citas" to="/mis-citas" />
        <Stat icon="🧾" value={loading ? '…' : num(data?.purchases?.totalElements)} label="Compras" to="/mis-compras" />
        <Stat icon="👤" value={loading ? '…' : titleCase(data?.profile?.role)} label="Mi perfil" to="/mi-perfil" />
      </div>

      <div className="grid grid-2" style={{ marginTop: '1.6rem' }}>
        <section>
          <div className="section-head">
            <span className="bar" />
            <h2>Próximas citas</h2>
            <Link className="link link-more" to="/mis-citas">Ver todas</Link>
          </div>
          <div className="card">
            {loading ? (
              <div className="spinner" />
            ) : upcoming.length ? (
              <div className="sale-lines">
                {upcoming.map((appointment) => (
                  <div key={appointment.id} className="client-appointment">
                    <div>
                      <strong>{appointment.petName}</strong>
                      <div className="cell-muted">{appointment.reason}</div>
                    </div>
                    <div className="client-appointment-date">
                      <strong>{dateTime(appointment.scheduledAt)}</strong>
                      <div className="cell-muted">{appointment.vetName || 'Veterinario por asignar'}</div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="cell-muted" style={{ margin: 0 }}>
                No tienes citas próximas. <Link to="/mis-citas">Reserva una visita</Link>.
              </p>
            )}
          </div>
        </section>

        <section>
          <div className="section-head">
            <span className="bar" />
            <h2>Mi ficha</h2>
            <Link className="link link-more" to="/mi-perfil">Editar</Link>
          </div>
          <div className="card">
            {loading ? (
              <div className="spinner" />
            ) : (
              <dl className="detail-list">
                <dt>Nombre</dt><dd>{data?.profile?.fullName}</dd>
                <dt>Email</dt><dd>{data?.profile?.email}</dd>
                <dt>Teléfono</dt><dd>{data?.profile?.phone || '—'}</dd>
                <dt>Dirección</dt><dd>{data?.profile?.address || '—'}</dd>
              </dl>
            )}
          </div>
        </section>
      </div>
    </>
  )
}
