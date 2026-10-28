import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import Logo from './Logo.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { label } from '../lib/enums.js'
import { initial } from '../lib/format.js'

const STAFF_NAV = [
  { to: '/', label: 'Inicio', end: true },
  { to: '/clientes', label: 'Clientes' },
  { to: '/mascotas', label: 'Mascotas' },
  { to: '/citas', label: 'Citas' },
  { to: '/pienso', label: 'Pienso' },
  { to: '/ventas', label: 'Ventas' },
  { to: '/adopciones', label: 'Adopciones' },
]

const ADMIN_NAV = { to: '/usuarios', label: 'Usuarios' }

const CLIENT_NAV = [
  { to: '/', label: 'Inicio', end: true },
  { to: '/tienda', label: 'Tienda' },
  { to: '/mis-mascotas', label: 'Mis mascotas' },
  { to: '/mis-citas', label: 'Mis citas' },
  { to: '/mis-compras', label: 'Mis compras' },
  { to: '/mis-adopciones', label: 'Mis adopciones' },
  { to: '/mi-perfil', label: 'Mi perfil' },
]

export default function Layout() {
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const { user, isStaff, isAdmin, logout } = useAuth()

  const nav = !user ? [] : isStaff ? (isAdmin ? [...STAFF_NAV, ADMIN_NAV] : STAFF_NAV) : CLIENT_NAV
  const appointmentLink = !user ? '/login' : isStaff ? '/citas' : '/mis-citas'

  // Cierra el menú móvil al cambiar de ruta.
  useEffect(() => {
    setMenuOpen(false)
  }, [location.pathname])

  const signOut = async () => {
    try {
      await logout()
    } catch {
      // Even if the server is unreachable the local session is already cleared.
    }
    navigate('/login', { replace: true })
  }

  return (
    <div className="site">
      <a className="skip-link" href="#contenido">Saltar al contenido</a>

      <div className="flag-strip" aria-hidden="true">
        <span /><span /><span />
      </div>

      <header className="site-header">
        <div className="container header-inner">
          <button
            type="button"
            className="nav-toggle"
            aria-label="Abrir menú"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((open) => !open)}
          >
            ☰
          </button>

          <Link to="/" className="brand">
            <Logo />
            <span className="brand-text">
              <strong>Galgo Español</strong>
              <small>Clínica Veterinaria</small>
            </span>
          </Link>

          <div className="header-cta">
            <a className="header-phone" href="tel:+34900000000">☎ 900 000 000</a>

            {user ? (
              <>
                <Link className="btn btn-primary header-cta-btn" to={appointmentLink}>Pedir cita</Link>
                <div className="user-menu">
                  <span className="avatar" aria-hidden="true">{initial(user.fullName)}</span>
                  <span className="user-menu-text">
                    <strong>{user.fullName}</strong>
                    <small>{label('role', user.role)}</small>
                  </span>
                  <button type="button" className="btn btn-ghost btn-sm" onClick={signOut}>Salir</button>
                </div>
              </>
            ) : (
              <div className="auth-actions">
                <Link className="btn btn-ghost" to="/login">Iniciar sesión</Link>
                <Link className="btn btn-primary" to="/registro">Registrarse</Link>
              </div>
            )}
          </div>
        </div>
      </header>

      {nav.length > 0 && (
        <nav className="main-nav" aria-label="Principal">
          <div className={`container nav-inner ${menuOpen ? 'open' : ''}`}>
            {nav.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                {item.label}
              </NavLink>
            ))}
          </div>
        </nav>
      )}

      <main id="contenido" className="page">
        <div className="container">
          <Outlet />
        </div>
      </main>

      <footer className="site-footer">
        <div className="container">
          <div className="footer-grid">
            <div className="footer-brand">
              <strong>Galgo Español</strong>
              <p>Clínica veterinaria y refugio. Cuidamos de tus mascotas y buscamos hogar para las que esperan adopción.</p>
              <div className="footer-flags" aria-hidden="true">
                <span /><span /><span />
              </div>
            </div>
            <div>
              <h4>Secciones</h4>
              <ul>
                {isStaff ? (
                  <>
                    <li><Link to="/mascotas">Mascotas</Link></li>
                    <li><Link to="/citas">Citas</Link></li>
                    <li><Link to="/pienso">Tienda de pienso</Link></li>
                    <li><Link to="/adopciones">Adopciones</Link></li>
                  </>
                ) : (
                  <>
                    <li><Link to={user ? '/mis-mascotas' : '/login'}>Mis mascotas</Link></li>
                    <li><Link to={user ? '/mis-citas' : '/login'}>Mis citas</Link></li>
                    <li><Link to={user ? '/mis-compras' : '/login'}>Mis compras</Link></li>
                    <li><Link to="/registro">Crear cuenta</Link></li>
                  </>
                )}
              </ul>
            </div>
            <div>
              <h4>Contacto</h4>
              <ul>
                <li>☎ 900 000 000</li>
                <li>✉ hola@galgoespanol.example</li>
                <li>Av. de la Raza 1, Madrid</li>
                <li>Lun–Sáb · 9:00–20:00</li>
              </ul>
            </div>
          </div>
          <div className="footer-bottom">
            <span>© {new Date().getFullYear()} Clínica Veterinaria Galgo Español</span>
            <span>Acceso seguro con JWT y renovación de sesión</span>
          </div>
        </div>
      </footer>
    </div>
  )
}
