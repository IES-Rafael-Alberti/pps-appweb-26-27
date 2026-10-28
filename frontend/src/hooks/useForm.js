import { useState } from 'react'

/** Tiny form-state helper shared by every create/edit dialog. */
export function useForm(initial) {
  const [values, setValues] = useState(initial)

  const setField = (name, value) => setValues((current) => ({ ...current, [name]: value }))

  return { values, setValues, setField, reset: (next = initial) => setValues(next) }
}
