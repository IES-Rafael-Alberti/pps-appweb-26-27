# Galgo Español · Frontend

SPA en **React 19 + Vite** para la API REST de la clínica veterinaria. Cubre el CRUD completo
de todos los módulos y está pensada para parecerse al portal de referencia: banda de bandera en la
cabecera, verde corporativo, navegación en mayúsculas, rejilla de tarjetas y botón tipo «Ver todas».
Es totalmente responsiva (las tablas se convierten en tarjetas en móvil).

## Requisitos

- Node.js 20+ (probado con Node 24). En este equipo está instalado de forma local en
  `~/.local/node`:

  ```bash
  export PATH="$HOME/.local/node/bin:$PATH"
  ```

- La API de Spring Boot en marcha (`./mvnw spring-boot:run` en la raíz del proyecto), escuchando en
  `http://localhost:8080`.

## Puesta en marcha

```bash
cd frontend
export PATH="$HOME/.local/node/bin:$PATH"   # solo si Node no está en el PATH
npm install
npm run dev                                 # http://localhost:5173
```

Vite hace de *proxy*: las peticiones a `/api/...`, `/oauth2/authorization/...` y `/login/oauth2/...` se
redirigen a `http://localhost:8080`, así que el navegador no se topa con CORS y el código usa rutas
relativas. Solo se proxea `/oauth2/authorization` (el endpoint que inicia el flujo): si se proxeara todo
`/oauth2`, la ruta `/oauth2/callback` de la SPA acabaría en el backend y devolvería `404`.

> **Login.** La SPA abre en `/login`. Hay tres cuentas de demostración (se muestran en la propia pantalla):
> `admin@vetclinic.example` / `Admin1234!`, `gestor@vetclinic.example` / `Gestor1234!` y
> `cliente@vetclinic.example` / `Cliente1234!`. Los clientes nuevos pueden darse de alta en `/registro`.
>
> **GitHub (opcional).** Si la API arranca con `GITHUB_OAUTH_ENABLED=true`, `GITHUB_CLIENT_ID` y
> `GITHUB_CLIENT_SECRET`, aparece el botón «Continuar con GitHub». La callback que hay que registrar en la
> OAuth App es `http://localhost:8080/login/oauth2/code/github`. Vite redirige `/oauth2/authorization` y
> `/login/oauth2` al backend, y este devuelve al navegador a `/oauth2/callback` de la SPA.

## Scripts

| Comando | Descripción |
| --- | --- |
| `npm run dev` | Servidor de desarrollo con recarga en caliente. |
| `npm run build` | Compila la SPA en `dist/`. |
| `npm run preview` | Sirve localmente el resultado de `dist/`. |
| `npm run seed` | Genera datos sintéticos contra la API en marcha (ver abajo). |

## Datos de prueba (paginación)

Con la API en marcha, `npm run seed` crea por REST un juego de datos realista (clientes,
productos, mascotas, citas, ventas y adopciones coherentes entre sí) para poder probar la
paginación, los filtros y los contadores del panel:

```bash
npm run seed
# equivalente a: API_URL=http://localhost:8080/api/v1 node scripts/generate-data.mjs
```

Por defecto genera 137 clientes, 164 productos, 186 mascotas, 150 citas, 140 ventas y 72
adopciones (con ~25 % canceladas o devueltas). Los volúmenes se ajustan por variables de entorno:

```bash
SEED_OWNERS=500 SEED_PETS=800 SEED_SALES=300 npm run seed
```

Notas:

- La API usa H2 **en memoria**: los datos desaparecen al reiniciar el backend; vuelve a lanzar
  `npm run seed` después de reiniciar.
- Cada ejecución **añade** datos sobre los existentes (no borra nada).
- El script respeta las reglas de negocio (emails únicos, stock disponible, solo mascotas
  `available` para adopción, fechas de cita futuras).

## Configuración

Copia `.env.example` a `.env` si necesitas apuntar a otra API:

| Variable | Por defecto | Uso |
| --- | --- | --- |
| `VITE_API_URL` | `/api/v1` | Base de la API que usa el cliente (en el navegador). |
| `VITE_PROXY_TARGET` | `http://localhost:8080` | Destino del proxy del servidor de desarrollo. |

## Módulos cubiertos

**Personal (`admin` y `manager`)**

- **Panel** — resumen con contadores, más vendidos y productos a reponer.
- **Clientes** — alta, edición y borrado (la API bloquea el borrado si tiene mascotas).
- **Mascotas** — filtros por especie, estado, dueño y refugio; alta/edición/baja.
- **Citas** — reserva, edición, cambio de estado (`completar`, `cancelar`, `reactivar`) y borrado.
- **Pienso** — catálogo, edición, ajuste de stock (delta), retirar/activar y borrado.
- **Ventas** — emisor de tickets con líneas y total estimado, listado y detalle, más vendidos.
- **Adopciones** — adoptar mascotas disponibles, cancelar, devolver y borrar registros cerrados.
- **Usuarios** (solo `admin`) — listado con filtros, alta de cuentas, cambio de rol, activar/desactivar y borrar.

**Clientes (`client`)**

- **Inicio** — saludo, próximas citas y resumen de su ficha.
- **Tienda** — catálogo activo, cesta, stock disponible y pedido online con la venta creada a su nombre.
- **Mis mascotas** — solo los animales a su nombre.
- **Mis citas** — reservar para sus mascotas y cancelar visitas.
- **Mis compras** — tickets de la tienda con desglose de líneas.
- **Mis adopciones** — historial de adopciones.
- **Mi perfil** — datos de contacto y cambio de contraseña.

## Sesión y seguridad en el cliente

- El **access token JWT** vive solo en memoria (módulo `lib/api.js`); nunca en `localStorage`.
- El **refresh token** es una cookie `httpOnly` que el navegador no puede leer desde JavaScript.
- Ante un `401`, `lib/api.js` renueva la sesión una sola vez (las peticiones concurrentes comparten la misma
  renovación) y repite la petición original. Si la renovación falla, el `AuthProvider` limpia la sesión y el
  guard de rutas redirige a `/login`.
- El botón de GitHub no aparece si `/api/v1/auth/providers` indica que el backend no tiene OAuth2 configurado.
  Tras el handshake, la SPA vuelve a `/oauth2/callback`, pide un access token con la cookie httpOnly y entra
  en el portal; los errores (`oauth_staff_account`, `oauth_disabled`, `github_denied`…) se explican en `/login`.
- `RequireAuth`/`RequireStaff`/`RequireAdmin`/`RequireClient` (`components/RequireAuth.jsx`) ocultan y protegen
  las rutas por rol; la autorización real siempre la impone el backend.

Todos los errores que devuelve la API (RFC 7807) se muestran como avisos, y los de validación se
asocian al campo correspondiente del formulario.

## Estructura

```
frontend/
├── index.html
├── vite.config.js          # plugin de React + proxy a :8080
└── src/
    ├── main.jsx            # providers (toasts, confirmación) y montaje
    ├── App.jsx             # rutas
    ├── styles.css          # sistema de diseño (responsivo)
    ├── lib/                # cliente de API (tokens y refresh), enums y formateo
    ├── hooks/              # useApi / useList / useForm
    ├── context/            # sesión (AuthContext), toasts y diálogo de confirmación
    ├── components/         # layout, guards de rol, tabla, modal, paginación, campos…
    └── pages/              # una página por módulo del personal y del portal del cliente
```

## Servir la SPA desde Spring Boot (opcional)

Si prefieres un único proceso, compila con `npm run build` y copia `dist/` dentro de
`src/main/resources/static/` del backend. Al usar rutas relativas (`/api/v1`) la SPA funcionará
servida por el propio Spring Boot.
