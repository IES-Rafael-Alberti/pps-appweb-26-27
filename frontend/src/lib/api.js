// Thin wrapper around the REST API. Every response and every error goes through
// here so the UI can rely on a single, predictable shape.
//
// Auth model: the access JWT lives only in memory (never localStorage); the refresh
// token is an httpOnly cookie set by the backend. When a call answers 401 we try a
// single token rotation and replay the request, so the user does not lose their work.

// API base used by the browser. VITE_API_URL can be either the API origin
// (https://api.example.com) or the full base (https://api.example.com/api/v1).
// When it is not set, requests stay relative so the Vite dev proxy / same-origin
// deployment keeps working.
function resolveApiBase() {
  const configured = (import.meta.env.VITE_API_URL || '/api/v1').trim().replace(/\/+$/, '')
  if (!configured) return '/api/v1'
  return configured.endsWith('/api/v1') ? configured : `${configured}/api/v1`
}

export const API_BASE = resolveApiBase()

// The GitHub handshake must go to the API, not to the SPA origin, when both are
// deployed on different hosts. With a relative base the dev proxy handles it.
export const OAUTH_AUTHORIZATION_URL = /^https?:\/\//i.test(API_BASE)
  ? `${API_BASE.replace(/\/api\/v1$/, '')}/oauth2/authorization/github`
  : '/oauth2/authorization/github'

const BASE = API_BASE

export class ApiError extends Error {
  constructor(status, problem, fallback) {
    super(problem?.detail || problem?.title || fallback || `Error ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem || null
    this.errors = problem?.errors || null
  }
}

let accessToken = null
let sessionListener = null
let refreshInFlight = null

/** Called by the AuthProvider to keep the UI in sync when the backend renews or ends a session. */
export function setSessionListener(listener) {
  sessionListener = listener
}

export function setAccessToken(token) {
  accessToken = token
}

export function getAccessToken() {
  return accessToken
}

function buildQuery(params) {
  const usp = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    usp.append(key, value)
  })
  const query = usp.toString()
  return query ? `?${query}` : ''
}

async function parse(response) {
  if (response.status === 204) return null
  const text = await response.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

function toError(response, data) {
  return new ApiError(
    response.status,
    data && typeof data === 'object' ? data : null,
    typeof data === 'string' ? data : null,
  )
}

/**
 * Exchanges the httpOnly refresh cookie for a new access token. Concurrent 401s share
 * the same in-flight request, so a page that fires five calls only rotates once.
 */
export async function refreshSession() {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      let response
      try {
        response = await fetch(`${BASE}/auth/refresh`, {
          method: 'POST',
          credentials: 'include',
          headers: { Accept: 'application/json' },
        })
      } catch {
        return null
      }
      const data = await parse(response)
      if (!response.ok || !data?.accessToken) {
        accessToken = null
        sessionListener?.(null)
        return null
      }
      accessToken = data.accessToken
      sessionListener?.(data.user ?? null)
      return data
    })().finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

async function rawRequest(path, { method = 'GET', body, params, auth = true } = {}) {
  let response
  try {
    response = await fetch(`${BASE}${path}${buildQuery(params)}`, {
      method,
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(auth && accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError(0, null, 'No se pudo contactar con el servidor')
  }

  const data = await parse(response)
  if (!response.ok) throw toError(response, data)
  return data
}

async function request(path, options = {}) {
  const { auth = true } = options
  try {
    return await rawRequest(path, options)
  } catch (error) {
    const canRetry = auth && error.status === 401 && !path.startsWith('/auth/')
    if (!canRetry) throw error
    const renewed = await refreshSession()
    if (!renewed) throw error
    return rawRequest(path, options)
  }
}

const get = (path, params) => request(path, { params })
const post = (path, body, options = {}) => request(path, { method: 'POST', body, ...options })
const put = (path, body) => request(path, { method: 'PUT', body })
const patch = (path, body) => request(path, { method: 'PATCH', body })
const del = (path) => request(path, { method: 'DELETE' })

export const auth = {
  register: (body) => post('/auth/register', body, { auth: false }),
  login: (body) => post('/auth/login', body, { auth: false }),
  providers: () => request('/auth/providers', { auth: false }),
  refresh: refreshSession,
  me: () => get('/auth/me'),
  logout: async () => {
    try {
      await post('/auth/logout', {}, { auth: false })
    } finally {
      accessToken = null
      sessionListener?.(null)
    }
  },
}

export const users = {
  list: (params) => get('/users', params),
  get: (id) => get(`/users/${id}`),
  create: (body) => post('/users', body),
  changeRole: (id, role) => patch(`/users/${id}/role`, { role }),
  setEnabled: (id, enabled) => patch(`/users/${id}/enabled`, { enabled }),
  remove: (id) => del(`/users/${id}`),
}

export const portal = {
  profile: () => get('/me'),
  updateProfile: (body) => put('/me', body),
  changePassword: (body) => post('/me/password', body),
  pets: (params) => get('/me/pets', params),
  appointments: (params) => get('/me/appointments', params),
  bookAppointment: (body) => post('/me/appointments', body),
  cancelAppointment: (id) => patch(`/me/appointments/${id}/cancel`),
  purchases: (params) => get('/me/purchases', params),
  adoptions: (params) => get('/me/adoptions', params),
  shopProducts: (params) => get('/me/shop/products', params),
  shopProduct: (id) => get(`/me/shop/products/${id}`),
  order: (body) => post('/me/shop/orders', body),
}

export const owners = {
  list: (params) => get('/owners', params),
  get: (id) => get(`/owners/${id}`),
  create: (body) => post('/owners', body),
  update: (id, body) => put(`/owners/${id}`, body),
  remove: (id) => del(`/owners/${id}`),
}

export const pets = {
  list: (params) => get('/pets', params),
  get: (id) => get(`/pets/${id}`),
  create: (body) => post('/pets', body),
  update: (id, body) => put(`/pets/${id}`, body),
  remove: (id) => del(`/pets/${id}`),
}

export const appointments = {
  list: (params) => get('/appointments', params),
  get: (id) => get(`/appointments/${id}`),
  create: (body) => post('/appointments', body),
  update: (id, body) => put(`/appointments/${id}`, body),
  changeStatus: (id, status) => patch(`/appointments/${id}/status`, { status }),
  remove: (id) => del(`/appointments/${id}`),
}

export const foods = {
  list: (params) => get('/foods', params),
  lowStock: (threshold = 5) => get('/foods/low-stock', { threshold }),
  get: (id) => get(`/foods/${id}`),
  create: (body) => post('/foods', body),
  update: (id, body) => put(`/foods/${id}`, body),
  adjustStock: (id, delta) => patch(`/foods/${id}/stock`, { delta }),
  setActive: (id, active) => patch(`/foods/${id}/${active ? 'activate' : 'deactivate'}`),
  remove: (id) => del(`/foods/${id}`),
}

export const sales = {
  list: (params) => get('/sales', params),
  get: (id) => get(`/sales/${id}`),
  create: (body) => post('/sales', body),
  topProducts: (limit = 10) => get('/sales/reports/top-products', { limit }),
}

export const adoptions = {
  list: (params) => get('/adoptions', params),
  get: (id) => get(`/adoptions/${id}`),
  adopt: (body) => post('/adoptions', body),
  cancel: (id, reason) => post(`/adoptions/${id}/cancel`, { reason }),
  markReturned: (id, reason) => post(`/adoptions/${id}/return`, { reason }),
  remove: (id) => del(`/adoptions/${id}`),
}

/** Turns an RFC 7807 field map into { field: message } for the forms. */
export function fieldErrors(error) {
  if (!error || !error.errors) return {}
  return error.errors
}
