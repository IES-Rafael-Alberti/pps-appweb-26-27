import { BrowserRouter, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import { RequireAdmin, RequireAuth, RequireClient, RequireStaff } from './components/RequireAuth.jsx'
import Home from './pages/Home.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import OAuth2CallbackPage from './pages/OAuth2CallbackPage.jsx'
import OwnersPage from './pages/OwnersPage.jsx'
import PetsPage from './pages/PetsPage.jsx'
import AppointmentsPage from './pages/AppointmentsPage.jsx'
import FoodsPage from './pages/FoodsPage.jsx'
import SalesPage from './pages/SalesPage.jsx'
import AdoptionsPage from './pages/AdoptionsPage.jsx'
import UsersPage from './pages/UsersPage.jsx'
import MyPetsPage from './pages/MyPetsPage.jsx'
import MyAppointmentsPage from './pages/MyAppointmentsPage.jsx'
import MyPurchasesPage from './pages/MyPurchasesPage.jsx'
import MyAdoptionsPage from './pages/MyAdoptionsPage.jsx'
import ProfilePage from './pages/ProfilePage.jsx'
import ShopPage from './pages/ShopPage.jsx'
import NotFound from './pages/NotFound.jsx'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="login" element={<LoginPage />} />
          <Route path="registro" element={<RegisterPage />} />
          <Route path="oauth2/callback" element={<OAuth2CallbackPage />} />

          <Route element={<RequireAuth />}>
            <Route index element={<Home />} />

            <Route element={<RequireStaff />}>
              <Route path="clientes" element={<OwnersPage />} />
              <Route path="mascotas" element={<PetsPage />} />
              <Route path="citas" element={<AppointmentsPage />} />
              <Route path="pienso" element={<FoodsPage />} />
              <Route path="ventas" element={<SalesPage />} />
              <Route path="adopciones" element={<AdoptionsPage />} />
            </Route>

            <Route element={<RequireAdmin />}>
              <Route path="usuarios" element={<UsersPage />} />
            </Route>

            <Route element={<RequireClient />}>
              <Route path="tienda" element={<ShopPage />} />
              <Route path="mis-mascotas" element={<MyPetsPage />} />
              <Route path="mis-citas" element={<MyAppointmentsPage />} />
              <Route path="mis-compras" element={<MyPurchasesPage />} />
              <Route path="mis-adopciones" element={<MyAdoptionsPage />} />
              <Route path="mi-perfil" element={<ProfilePage />} />
            </Route>
          </Route>

          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
