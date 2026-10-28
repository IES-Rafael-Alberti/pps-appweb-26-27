// Shared vocabulary: enum values accepted by the API plus their Spanish labels.

export const SPECIES = ['dog', 'cat', 'bird', 'rabbit', 'reptile', 'other']
export const PET_SEX = ['male', 'female', 'unknown']
export const PET_STATUS = ['available', 'owned', 'adopted']
export const APPOINTMENT_STATUS = ['scheduled', 'completed', 'cancelled']
export const FOOD_CATEGORIES = [
  'dog_food',
  'cat_food',
  'puppy_food',
  'kitten_food',
  'bird_food',
  'rabbit_food',
  'treats',
  'supplements',
  'medicine',
]
export const PAYMENT_METHODS = ['cash', 'card', 'transfer']
export const ADOPTION_STATUS = ['completed', 'canceled', 'returned']
export const ROLES = ['admin', 'manager', 'client']

const LABELS = {
  species: {
    dog: 'Perro', cat: 'Gato', bird: 'Ave', rabbit: 'Conejo', reptile: 'Reptil', other: 'Otro',
  },
  sex: { male: 'Macho', female: 'Hembra', unknown: 'Sin determinar' },
  petStatus: { available: 'En adopción', owned: 'Con dueño', adopted: 'Adoptado' },
  appointmentStatus: { scheduled: 'Programada', completed: 'Completada', cancelled: 'Cancelada' },
  category: {
    dog_food: 'Pienso de perro',
    cat_food: 'Pienso de gato',
    puppy_food: 'Pienso de cachorro',
    kitten_food: 'Pienso de gatito',
    bird_food: 'Pienso de ave',
    rabbit_food: 'Pienso de conejo',
    treats: 'Premios',
    supplements: 'Complementos',
    medicine: 'Medicamentos',
  },
  payment: { cash: 'Efectivo', card: 'Tarjeta', transfer: 'Transferencia' },
  adoptionStatus: { completed: 'Completada', canceled: 'Cancelada', returned: 'Devuelta' },
  role: { admin: 'Administrador', manager: 'Gestor de clínica', client: 'Cliente' },
}

export function label(dictionary, value) {
  if (value === null || value === undefined) return '—'
  return LABELS[dictionary]?.[value] ?? value
}

/** { value, label } list ready to feed a <Select>. */
export function options(dictionary, values) {
  return values.map((value) => ({ value, label: label(dictionary, value) }))
}

export function petStatusTone(status) {
  return { available: 'green', owned: 'blue', adopted: 'gray' }[status] ?? 'gray'
}

export function appointmentStatusTone(status) {
  return { scheduled: 'blue', completed: 'green', cancelled: 'red' }[status] ?? 'gray'
}

export function adoptionStatusTone(status) {
  return { completed: 'green', canceled: 'red', returned: 'amber' }[status] ?? 'gray'
}

export function roleTone(role) {
  return { admin: 'red', manager: 'blue', client: 'green' }[role] ?? 'gray'
}
