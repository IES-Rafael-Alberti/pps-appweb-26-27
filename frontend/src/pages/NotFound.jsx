import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="empty" style={{ paddingTop: '4rem' }}>
      <div className="empty-icon" style={{ fontSize: '3.4rem' }}>🐕</div>
      <h1 className="head-font" style={{ fontSize: '2rem' }}>Página no encontrada</h1>
      <p>El galgo se ha perdido por estos lares. Vuelve al panel principal.</p>
      <Link className="btn btn-primary" to="/">Volver al inicio</Link>
    </div>
  )
}
