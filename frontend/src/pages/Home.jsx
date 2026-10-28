import { useAuth } from '../context/AuthContext.jsx'
import ClientDashboard from './ClientDashboard.jsx'
import Dashboard from './Dashboard.jsx'

/** Staff land on the clinic panel; clients land on their own portal. */
export default function Home() {
  const { user } = useAuth()
  return user?.role === 'client' ? <ClientDashboard /> : <Dashboard />
}
