// Formatting helpers. The API talks ISO-8601; the UI speaks Spanish.

const currency = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' })
const dateFmt = new Intl.DateTimeFormat('es-ES', { day: '2-digit', month: 'short', year: 'numeric' })
const dateTimeFmt = new Intl.DateTimeFormat('es-ES', {
  day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
})
const numberFmt = new Intl.NumberFormat('es-ES')

export const money = (value) =>
  value === null || value === undefined || value === '' ? '—' : currency.format(Number(value))

export const num = (value) =>
  value === null || value === undefined || value === '' ? '—' : numberFmt.format(Number(value))

const parse = (value) => (value ? new Date(value) : null)

export const date = (value) => {
  const parsed = parse(value)
  return parsed && !Number.isNaN(parsed.getTime()) ? dateFmt.format(parsed) : '—'
}

export const dateTime = (value) => {
  const parsed = parse(value)
  return parsed && !Number.isNaN(parsed.getTime()) ? dateTimeFmt.format(parsed) : '—'
}

/** ISO instant -> value for an <input type="datetime-local">. */
export function toLocalInput(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** value from a <input type="datetime-local"> -> ISO local date-time. */
export function fromLocalInput(value) {
  return value ? `${value}:00` : null
}

export const initial = (name) => (name ? name.trim().charAt(0).toUpperCase() : '?')

export const titleCase = (value) =>
  value ? value.charAt(0).toUpperCase() + value.slice(1) : value
