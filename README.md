# API Clínica Veterinaria

API REST en Spring Boot 3.5 / Java 21 para gestionar una clínica veterinaria: clientes, mascotas, citas,
**venta de pienso** y **adopción de mascotas**.

## Puesta en marcha

```bash
./mvnw spring-boot:run     # http://localhost:8080
```

O bien:

```bash
./mvnw package
java -jar target/vet-clinic-api-0.0.1-SNAPSHOT.jar
```

| Recurso | URL |
| --- | --- |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Consola H2 | http://localhost:8080/h2-console (JDBC `jdbc:h2:mem:vetclinic`, user `sa`, sin clave) |

Al arrancar se carga un conjunto de datos de ejemplo (2 clientes, 4 productos, 4 mascotas, 2 citas, 1 venta,
1 adopción devuelta) y tres cuentas de demostración:

| Rol | Email | Contraseña |
| --- | --- | --- |
| Administrador | `admin@vetclinic.example` | `Admin1234!` |
| Gestor de clínica | `gestor@vetclinic.example` | `Gestor1234!` |
| Cliente | `cliente@vetclinic.example` | `Cliente1234!` |

El cliente de demostración está vinculado a la ficha de Lucía Fernández. El seed se desactiva con
`--app.seed.enabled=false` (en ese caso no hay ninguna cuenta y hay que crear el primer administrador con SQL
o con `app.seed.enabled=true` una vez).

## Autenticación y roles

La API es **stateless**: un JWT de acceso de corta duración (15 min) viaja en `Authorization: Bearer …` y un
**refresh token opaco** permite renovarlo sin volver a pedir la contraseña. Solo se guarda el hash SHA-256 del
refresh token.

| Rol | Qué puede hacer |
| --- | --- |
| `admin` | Todo: módulos de la clínica **y** gestión de usuarios, roles y accesos. |
| `manager` | Operación diaria: clientes, mascotas, citas, pienso, ventas y adopciones. No gestiona usuarios. |
| `client` | Solo sus datos: perfil, sus mascotas, sus citas (reservar/cancelar), sus compras y adopciones. |

Endpoints:

| Método | Ruta | Acceso | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | público | Alta de cliente. Crea la ficha de propietario o, si ya existe con ese email, exige que el teléfono coincida. |
| `POST` | `/api/v1/auth/login` | público | Devuelve `accessToken`, `refreshToken` y el usuario. |
| `POST` | `/api/v1/auth/refresh` | público | Rota el refresh token (cuerpo o cookie httpOnly) y emite un access token nuevo. |
| `POST` | `/api/v1/auth/logout` | público | Revoca el refresh token presentado. Idempotente. |
| `GET` | `/api/v1/auth/me` | autenticado | Cuenta actual a partir del Bearer. |
| `GET/POST` | `/api/v1/users` | `admin` | Listado paginado (filtros `role`, `enabled`, `q`) y alta de cuentas. |
| `GET` | `/api/v1/users/{id}` | `admin` | Detalle de una cuenta. |
| `PATCH` | `/api/v1/users/{id}/role` | `admin` | Cambio de rol. El último administrador activo no puede degradarse. |
| `PATCH` | `/api/v1/users/{id}/enabled` | `admin` | Activa/desactiva. Al desactivar se revocan sus sesiones. |
| `DELETE` | `/api/v1/users/{id}` | `admin` | Borra la cuenta y conserva la ficha de cliente. |
| `GET/PUT` | `/api/v1/me` | `client` | Perfil propio. |
| `POST` | `/api/v1/me/password` | `client` | Cambio de contraseña (cierra el resto de sesiones). |
| `GET` | `/api/v1/me/shop/products`, `/{id}` | `client` | Catálogo activo de la tienda de pienso. |
| `POST` | `/api/v1/me/shop/orders` | `client` | Pedido online: crea la venta a su nombre y descuenta stock. |
| `GET` | `/api/v1/me/pets` | `client` | Solo las mascotas del cliente. |
| `GET/POST` | `/api/v1/me/appointments` | `client` | Sus citas y reserva de visitas para sus mascotas. |
| `PATCH` | `/api/v1/me/appointments/{id}/cancel` | `client` | Cancela una cita propia. |
| `GET` | `/api/v1/me/purchases` | `client` | Tickets de la tienda a su nombre. |
| `GET` | `/api/v1/me/adoptions` | `client` | Adopciones a su nombre. |

Detalles de seguridad:

- **Rotación con detección de reuso.** Cada refresh invalida el token anterior. Si llega un token ya usado se
  interpreta como robo y se revocan **todas** las sesiones de esa cuenta.
- **Cookie httpOnly.** El refresh token también se envía como cookie `httpOnly`, `SameSite=Lax`, `Path=/api/v1/auth`,
  para que la SPA no tenga que guardarlo en JavaScript. El access token vive solo en memoria.
- **Cambios de rol y bajas inmediatas.** El filtro JWT recarga la cuenta en cada petición: desactivar un usuario o
  cambiarle el rol surte efecto en la siguiente llamada, sin esperar a que caduque el access token.
- Los endpoints de cliente comprueban la propiedad del recurso y devuelven **404** (no 403) ante ids ajenos, para
  no filtrar su existencia.

```bash
# login
curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"cliente@vetclinic.example","password":"Cliente1234!"}' | jq

# usar el access token
curl -s localhost:8080/api/v1/me/pets -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# renovar con el refresh token del cuerpo
curl -s -X POST localhost:8080/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"..."}' | jq
```

### Acceso con GitHub (OAuth2)

Además de email/contraseña, los clientes pueden entrar con GitHub. El flujo lo gestiona Spring Security
(`spring-boot-starter-oauth2-client`) y termina en el mismo refresh token httpOnly que el login normal:
ningún token viaja en la URL.

1. Crea una **OAuth App** en <https://github.com/settings/developers>.
2. Callback URL: `http://localhost:8080/login/oauth2/code/github` (en producción, la URL pública equivalente).
3. Arranca la API con las credenciales:

   ```bash
   GITHUB_OAUTH_ENABLED=true \
   GITHUB_CLIENT_ID=tu_client_id \
   GITHUB_CLIENT_SECRET=tu_client_secret \
   ./mvnw spring-boot:run
   ```

Comportamiento:

- `GET /api/v1/auth/providers` indica si GitHub está configurado; la SPA oculta el botón si no lo está.
- Si el email verificado de GitHub ya pertenece a un **cliente**, la cuenta se vincula a esa ficha (y a su
  historial). Si no existe, se crea el cliente y su ficha de propietario al vuelo (sin teléfono, que el
  cliente puede completar en su perfil).
- Si el email pertenece a una cuenta de **personal**, el acceso se rechaza: el personal sigue usando su
  contraseña de la clínica.
- Solo se aceptan emails **verificados** por GitHub (`scope: read:user,user:email`).
- El handshake usa sesión HTTP únicamente en `/oauth2/**` y `/login/oauth2/**`; la API sigue siendo
  stateless. El resto de peticiones se autentican con el JWT.

Sin esas variables el botón no aparece y todo lo demás funciona igual. La SPA arranca el flujo en
`/oauth2/authorization/github`; Vite lo redirige al backend junto con `/api`.

Configuración (`application.yml`, todo sobrescribible por entorno):

| Propiedad | Por defecto | Uso |
| --- | --- | --- |
| `APP_JWT_SECRET` | secreto de desarrollo | Clave HS256, mínimo 32 bytes. **Obligatorio cambiarla fuera de desarrollo.** |
| `app.security.jwt.access-token-ttl` | `15m` | Vida del access token. |
| `app.security.refresh-token-ttl` | `7d` | Vida del refresh token. |
| `REFRESH_COOKIE_SECURE` | `false` | Ponlo a `true` con HTTPS. |
| `REFRESH_COOKIE_SAME_SITE` | `Lax` | Usa `None` (junto con `REFRESH_COOKIE_SECURE=true`) si la SPA y la API están en sitios distintos, p. ej. dos servicios de Render. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,…` | Orígenes del frontend permitidos por CORS, separados por comas. |
| `GITHUB_OAUTH_ENABLED` | `false` | Activa el login con GitHub. |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | vacíos | Credenciales de la OAuth App de GitHub. |
| `FRONTEND_URL` | `http://localhost:5173` | Destino al que vuelve el navegador tras el login OAuth2. |

El frontend se configura con `VITE_API_URL` en el momento del build (por ejemplo
`VITE_API_URL=https://pps-appweb-26-27-b0q7.onrender.com/api/v1`). Consulta
[`frontend/README.md`](frontend/README.md) para el despliegue en Render.


## Módulos

| Recurso | Endpoints | Notas |
| --- | --- | --- |
| `owners` | `GET/POST /api/v1/owners`, `GET/PUT/DELETE /{id}` | Email único, no se puede borrar con mascotas asociadas |
| `pets` | `GET/POST /api/v1/pets`, `GET/PUT/DELETE /{id}` | Filtros `species`, `status`, `ownerId`, `shelter` |
| `appointments` | `GET/POST`, `GET/PUT/DELETE /{id}`, `PATCH /{id}/status` | Máquina de estados `scheduled → completed\|cancelled` |
| `foods` | `GET/POST`, `GET/PUT/DELETE /{id}`, `PATCH /{id}/stock`, `/low-stock` | Baja lógica en vez de borrado físico |
| `sales` | `GET/POST /api/v1/sales`, `GET /{id}`, `GET /reports/top-products` | Ticket inmutable, descuenta stock |
| `adoptions` | `GET/POST`, `GET/DELETE /{id}`, `POST /{id}/cancel`, `POST /{id}/return` | Solo mascotas `available` |
| `auth` | `POST /register`, `/login`, `/refresh`, `/logout`, `GET /me` | JWT + refresh con rotación |
| `users` | `GET/POST /api/v1/users`, `GET /{id}`, `PATCH /{id}/role`, `PATCH /{id}/enabled`, `DELETE /{id}` | Solo `admin` |
| `me` | `/api/v1/me`, `/pets`, `/appointments`, `/purchases`, `/adoptions` | Solo `client`, siempre datos propios |
| `me/shop` | `GET /products`, `GET /products/{id}`, `POST /orders` | Tienda online para `client`; precios y stock los calcula el servidor |

Todos los listados aceptan `?page=`, `?size=` y `?sort=campo,asc|desc`. Para ordenar por varios campos se
repite el parámetro: `?sort=fullName,asc&sort=email,desc`. Una columna que no exista en la entidad devuelve
`400` indicando qué campo falló, nunca `500`.

## Reglas de negocio

**Venta de pienso.** Los precios y totales los calcula siempre el servidor a partir del catálogo: el cliente
solo envía `productId` y `quantity`. La venta corre en una transacción que bloquea las filas implicadas con
`SELECT ... FOR UPDATE` en orden ascendente de id, de modo que dos cajas cobrando la última bolsa a la vez no
pueden venderla dos veces (una recibe `201`, la otra `409`). Si una sola línea falla, se revierte el ticket
entero. Cada línea guarda una copia del nombre y del precio del momento de la venta, así que cambiar el
catálogo no reescribe el histórico.

**Adopción.** Solo se puede adoptar una mascota en estado `available`. Al adoptar, la mascota pasa a
`adopted` y su propietario pasa a ser el adoptante, lo que la reserva frente a un segundo intento (`409`).
`cancel` y `return` devuelven el animal al refugio: la mascota vuelve a `available` y queda sin propietario,
por lo que puede volver a adoptarse.

**Citas.** No se puede reservar en pasado (`@Future`). `durationMinutes` es opcional: si se omite se aplica
una duración de 30 minutos. Una visita completada es parte del historial clínico y no se puede editar ni
borrar; una cancelada sí se puede reactivar, siempre que la fecha siga siendo futura.

**Eliminación.** `DELETE` sobre un recurso con historial (mascota con citas o adopciones, cliente con
mascotas, producto vendido) devuelve `409` en lugar de dejar datos colgando.

## Errores

Todas las respuestas de error siguen [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807)
(`application/problem+json`). La validación reporta **todos** los campos fallidos a la vez:

```json
{
  "type": "https://vetclinic.example.com/problems/validation-error",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/api/v1/pets",
  "errors": { "name": "name is required", "birthDate": "birthDate cannot be in the future" }
}
```

| Código | Cuándo |
| --- | --- |
| `400` | JSON mal formado, parámetro o enum inválido, regla de coherencia, columna de ordenación inexistente |
| `401` | Falta el token, está caducado o el refresh token no es válido |
| `403` | El rol autenticado no puede realizar la acción |
| `404` | Recurso o referencia inexistente, o ninguna ruta coincide con la petición |
| `405` | Verbo HTTP no soportado por la ruta (la respuesta incluye la cabecera `Allow`) |
| `406` | La cabecera `Accept` pide un tipo que la API no produce |
| `409` | Conflicto de estado: stock insuficiente, adopción duplicada, borrado con historial |
| `415` | `Content-Type` no soportado en el cuerpo de la petición |
| `500` | Error inesperado (queda registrado en el log con su traza) |

Los enums aceptan minúsculas tanto en el cuerpo JSON como en los query params, y un valor desconocido
devuelve la lista de valores válidos (`'dragon' is not a valid Species. Accepted values: dog, cat, ...`).

## Ejemplos

Vender pienso:

```bash
curl -X POST localhost:8080/api/v1/sales \
  -H 'Content-Type: application/json' \
  -d '{
        "customerId": 1,
        "customerName": "Lucia Fernandez",
        "paymentMethod": "card",
        "lines": [
          {"productId": 1, "quantity": 2},
          {"productId": 2, "quantity": 1}
        ]
      }'
```

Registrar una mascota del refugio (sin `ownerId` queda `available`):

```bash
curl -X POST localhost:8080/api/v1/pets \
  -H 'Content-Type: application/json' \
  -d '{"name": "Nina", "species": "cat", "breed": "European Shorthair",
       "sex": "female", "birthDate": "2024-05-01",
       "notes": "Looking for a calm home"}'
```

Adoptarla:

```bash
curl -X POST localhost:8080/api/v1/adoptions \
  -H 'Content-Type: application/json' \
  -d '{"petId": 2, "adopterId": 2, "fee": 85.00, "notes": "Picked up Saturday"}'

# devolverla al refugio
curl -X POST localhost:8080/api/v1/adoptions/2/return \
  -H 'Content-Type: application/json' -d '{"reason": "Allergies in the new home"}'
```

Reservar una cita y completarla:

```bash
curl -X POST localhost:8080/api/v1/appointments \
  -H 'Content-Type: application/json' \
  -d '{"petId": 2, "scheduledAt": "2026-10-03T10:30:00",
       "reason": "Vaccination", "vetName": "Dra. Elena Vazquez", "durationMinutes": 30}'

curl -X PATCH localhost:8080/api/v1/appointments/3/status \
  -H 'Content-Type: application/json' -d '{"status": "completed"}'
```

Ajustar stock y consultar productos que se acaban:

```bash
curl -X PATCH localhost:8080/api/v1/foods/1/stock \
  -H 'Content-Type: application/json' -d '{"delta": 20}'   # negativo para retirar

curl "localhost:8080/api/v1/foods/low-stock?threshold=10"
```

## Frontend

Se incluye una SPA en React + Vite (`frontend/`) que cubre el CRUD completo de todos los módulos,
con un diseño responsivo inspirado en un portal institucional. Incluye login con email/contraseña o
GitHub, registro de clientes, renovación silenciosa de la sesión y vistas distintas por rol: panel de
clínica para `admin`/`manager`, portal de cliente con tienda online y pantalla de usuarios solo para
`admin`. Arranca la API y luego:

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173 (proxy /api -> :8080)
npm run seed       # genera datos sinteticos para probar la paginacion
```

Detalles en [`frontend/README.md`](frontend/README.md).

## Tests

```bash
./mvnw test
```

66 tests de integración contra H2 real que cubren el ciclo de vida completo de cada módulo, los errores de
validación y de negocio, el caso de concurrencia de dos ventas simultáneas de la última unidad, que exige
que exactamente una gane, y el contrato de errores: ninguna petición inválida puede terminar en `500`.
Los tests de autenticación añaden 15 casos: registro, login, rotación y reuso del refresh token, cookie
httpOnly, logout, baja inmediata de una cuenta, los límites de cada rol y la vinculación de cuentas
GitHub. La tienda de clientes suma 3 casos: visibilidad del catálogo, precio calculado en servidor y
stock insuficiente. Un test adicional comprueba el cableado de OAuth2 (repositorio de registros, storage
del cliente y redirect a GitHub con `state`). En total, **85 tests**.

## Estructura

```
src/main/java/com/vetclinic/
├── common/          # excepciones, handler RFC 7807, PageResponse, parseo de enums
├── config/          # OpenAPI, datos de ejemplo, conversores de query params
├── security/        # filtro JWT, configuración de Spring Security, principal
├── auth/            # cuentas, roles, refresh tokens, login y gestión de usuarios
├── portal/          # endpoints /me del cliente
├── owner/           # clientes
├── pet/             # mascotas
├── appointment/     # citas
├── food/            # catálogo de pienso y stock
├── sale/            # tickets de venta
└── adoption/        # adopciones
```

Cada módulo sigue la misma estructura `entity / repository / service / controller / dto`, con las entidades
separadas de los DTOs para no exponer nunca la capa de persistencia.

## Cambiar a PostgreSQL o MySQL

Sustituye el bloque `spring.datasource` y añade el driver. El esquema lo genera JPA, así que no hay SQL que
migrar a mano:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/vetclinic
    username: vetclinic
    password: secret
  jpa:
    hibernate:
      ddl-auto: validate      # usa flyway/liquibase en producción
```

## Notas

- El login con GitHub depende de que la OAuth App tenga registrada exactamente la callback URL del backend
  (`http://localhost:8080/login/oauth2/code/github` en desarrollo). Si cambias de host o de puerto, actualízala.
- La tienda de clientes es una compra simulada: crea una venta real y descuenta stock, pero no hay pasarela de
  pago. El pedido queda para recoger en la clínica.
- Antes de exponer la API habría que añadir verificación de email en el registro con contraseña (en GitHub ya
  viene verificado), un flujo de recuperación de contraseña y *rate limiting* en `/auth/login` y
  `/auth/register`.
- El refresh token viaja en cookie `SameSite=Lax` y el CSRF está desactivado porque la API solo acepta JSON y
  no hay formularios cross-site. Con HTTPS pon `app.security.refresh-cookie-secure=true`.
- El access token no se puede revocar individualmente: dura 15 minutos. Desactivar la cuenta o cambiarla de rol
  sí surte efecto inmediato porque la cuenta se recarga en cada petición.
- Sin verificación de email, alguien que conozca email y teléfono de un cliente podría registrarse en su nombre.
  En producción conviene enviar un enlace de confirmación antes de vincular la cuenta.
- `ddl-auto: update` es adecuado para desarrollo. En producción usa `validate` con Flyway o Liquibase.
- Las citas usan `LocalDateTime` (hora local de la clínica). Si habrá varias sedes en zonas distintas,
  conviene pasar a `OffsetDateTime` o `ZonedDateTime`.
