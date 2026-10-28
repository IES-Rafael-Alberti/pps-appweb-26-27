#!/usr/bin/env node
/**
 * Genera datos sinteticos realistas contra la API en marcha para poder probar
 * la paginacion y los filtros del frontend.
 *
 *   API_URL=http://localhost:8080/api/v1 node scripts/generate-data.mjs
 *   npm run seed
 *
 * Volumen configurable por variables de entorno:
 *   SEED_OWNERS, SEED_PRODUCTS, SEED_PETS, SEED_APPOINTMENTS, SEED_SALES, SEED_ADOPTIONS
 *
 * La API usa H2 en memoria: los datos desaparecen al reiniciar el backend.
 */

const API = (process.env.API_URL || 'http://localhost:8080/api/v1').replace(/\/+$/, '')

const CONF = {
  owners: int(process.env.SEED_OWNERS, 137),
  products: int(process.env.SEED_PRODUCTS, 164),
  pets: int(process.env.SEED_PETS, 186),
  appointments: int(process.env.SEED_APPOINTMENTS, 150),
  sales: int(process.env.SEED_SALES, 140),
  adoptions: int(process.env.SEED_ADOPTIONS, 72),
}

// ----------------------------------------------------------------- pools de datos

const FIRST = ['Lucía', 'Marta', 'Carlos', 'Javier', 'Elena', 'Pablo', 'Sara', 'Álvaro', 'Nuria', 'Iván',
  'Carmen', 'Diego', 'Raquel', 'Sergio', 'Paula', 'Andrés', 'Irene', 'Marcos', 'Claudia', 'Hugo',
  'Nerea', 'Adrián', 'Silvia', 'Rubén', 'Alba', 'Óscar', 'Patricia', 'Guillermo', 'Vega', 'Bruno',
  'Alicia', 'Fernando', 'Rocío', 'Jorge', 'Cristina', 'Alberto', 'Marina', 'Ángel', 'Beatriz', 'Tomás']

const LAST = ['Fernández', 'García', 'Martín', 'Ruiz', 'Gómez', 'Sánchez', 'Pérez', 'Romero', 'Vázquez',
  'Torres', 'Ramírez', 'Navarro', 'Molina', 'Ortega', 'Delgado', 'Castro', 'Rubio', 'Serrano', 'Blanco',
  'Marín', 'Iglesias', 'Medina', 'Cortés', 'Garrido', 'Santos', 'Vidal', 'Reyes', 'Campos', 'Herrera', 'Peña']

const STREETS = ['Calle Mayor', 'Avenida del Mar', 'Calle Sol', 'Paseo de la Castellana', 'Calle Alcalá',
  'Avenida de la Constitución', 'Calle Real', 'Paseo del Prado', 'Calle Nueva', 'Avenida de la Libertad',
  'Calle San Juan', 'Ronda de Toledo', 'Calle del Carmen', 'Avenida de América']

const CITIES = ['Madrid', 'Barcelona', 'Valencia', 'Sevilla', 'Zaragoza', 'Málaga', 'Bilbao', 'Murcia',
  'Alicante', 'Valladolid', 'Vigo', 'Gijón', 'Granada', 'Córdoba', 'Santander', 'Pamplona', 'Salamanca',
  'Toledo', 'Cáceres', 'Logroño']

const PET_NAMES = ['Roco', 'Nina', 'Bruno', 'Luna', 'Max', 'Toby', 'Lola', 'Simba', 'Canela', 'Rocky',
  'Maya', 'Nilo', 'Kira', 'Trufa', 'Duna', 'Coco', 'Milo', 'Chispa', 'Zeus', 'Pepa', 'Beto', 'Nala',
  'Golfo', 'Tara', 'Frida', 'Otto', 'Perla', 'Ragnar', 'Sombra', 'Trueno', 'Kiwi', 'Lucas', 'Mora',
  'Panda', 'Rita', 'Sultán', 'Tina', 'Yago', 'Zoe', 'Bimba', 'Draco', 'Greta', 'Hera', 'Índigo']

const BREEDS = {
  dog: ['Schnauzer', 'Beagle', 'Golden Retriever', 'Labrador', 'Border Collie', 'Pastor Alemán',
    'Bulldog Francés', 'Galgo Español', 'Husky', 'Chihuahua', 'Cocker Spaniel', 'Mastín Español', 'Podenco'],
  cat: ['Europeo común', 'Siamés', 'Persa', 'Maine Coon', 'Azul Ruso', 'Bengalí', 'Angora', 'Romano'],
  bird: ['Periquito', 'Canario', 'Agapornis', 'Cotorra', 'Ninfa', 'Loro gris'],
  rabbit: ['Enano holandés', 'Belier', 'Cabeza de león', 'Angora', 'Rex'],
  reptile: ['Gecko leopardo', 'Tortuga mediterránea', 'Pogona', 'Serpiente del maíz', 'Iguana verde'],
  other: ['Hurón', 'Cobaya', 'Chinchilla', 'Erizo'],
}

const CATEGORY_LINES = {
  dog_food: ['Adult Medium', 'Adult Mini', 'Senior', 'Light', 'Grain Free', 'Salmón', 'Cordero', 'Digestive'],
  cat_food: ['Adult Sterilized', 'Indoor', 'Urinary', 'Hairball', 'Salmón', 'Senior'],
  puppy_food: ['Puppy Mini', 'Puppy Medium', 'Junior'],
  kitten_food: ['Kitten', 'Baby Cat'],
  bird_food: ['Mix Periquitos', 'Mix Canarios', 'Premium Cotorras'],
  rabbit_food: ['Pellets Conejo', 'Mix Conejo'],
  treats: ['Snack Dental', 'Premios Naturales', 'Hueso Prensado', 'Golosinas blandas'],
  supplements: ['Condroitina', 'Omega 3', 'Vitaminas', 'Probiótico'],
  medicine: ['Antiparasitario', 'Pipeta', 'Desparasitante', 'Colirio'],
}

const CATEGORY_PRICE = {
  dog_food: [18, 65], cat_food: [16, 58], puppy_food: [15, 55], kitten_food: [14, 50],
  bird_food: [6, 20], rabbit_food: [7, 22], treats: [3, 14], supplements: [9, 32], medicine: [8, 40],
}

const SIZES = ['1 kg', '2 kg', '3 kg', '4 kg', '7,5 kg', '12 kg', '15 kg', 'x14 uds.', '250 ml', '500 ml']
const BRANDS = ['Royal Canin', "Hill's", 'Purina', 'Acana', 'Orijen', 'Eukanuba', 'Advance', 'Specific',
  'Barking Heads', 'Nath', 'Lobito', 'Naku', 'Ownat', 'Gosbi', 'Dibaq', 'Ultima', 'Affinity', 'Brekkies',
  'Vetnova', 'Trovet']

const REASONS = ['Revisión anual', 'Vacunación', 'Desparasitación', 'Consulta dermatológica', 'Limpieza dental',
  'Corte de uñas', 'Ecografía', 'Analítica de sangre', 'Revisión postoperatoria', 'Microchip y pasaporte',
  'Consulta general', 'Castración', 'Esterilización', 'Rayos X']

const VETS = ['Dra. Elena Vázquez', 'Dr. Marcos Serrano', 'Dra. Nuria Iglesias', 'Dr. Pablo Cortés',
  'Dra. Sara Medina', 'Dr. Iván Romero']

const PET_NOTES = ['Se lleva bien con niños.', 'Vacunado y microchipado.', 'Tímido al principio, muy cariñoso.',
  'Necesita dieta especial.', 'Le encanta jugar con la pelota.', 'Rescatado de la calle.', 'Sociable con otros perros.',
  'Requiere revisiones periódicas.', 'Muy tranquilo en casa.', 'Le asustan los ruidos fuertes.']

// ----------------------------------------------------------------- utilidades

function int(value, fallback) {
  const parsed = Number.parseInt(value ?? '', 10)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback
}

function pick(list) {
  return list[Math.floor(Math.random() * list.length)]
}

function randInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

function chance(probability) {
  return Math.random() < probability
}

function money(value) {
  return Math.round(value * 100) / 100
}

function slug(text) {
  return text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
    .replace(/[^a-z0-9]+/g, '.').replace(/^\.|\.$/g, '')
}

function pad(n) {
  return String(n).padStart(2, '0')
}

function localIso(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T`
    + `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function isoDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function shuffle(list) {
  const copy = [...list]
  for (let i = copy.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[copy[i], copy[j]] = [copy[j], copy[i]]
  }
  return copy
}

async function mapLimit(items, limit, worker) {
  const results = new Array(items.length)
  let next = 0
  const runners = Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (next < items.length) {
      const index = next
      next += 1
      results[index] = await worker(items[index], index)
    }
  })
  await Promise.all(runners)
  return results
}

async function api(method, path, body) {
  const response = await fetch(`${API}${path}`, {
    method,
    headers: body !== undefined ? { 'Content-Type': 'application/json', Accept: 'application/json' } : { Accept: 'application/json' },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  if (response.status === 204) return null
  const text = await response.text()
  let data = null
  if (text) {
    try { data = JSON.parse(text) } catch { data = text }
  }
  if (!response.ok) {
    const detail = data && typeof data === 'object' ? (data.detail || data.title) : data
    throw new Error(`${method} ${path} -> ${response.status}: ${detail ?? ''}`)
  }
  return data
}

const progress = (label, done, total) => {
  if (done % 25 === 0 || done === total) {
    process.stdout.write(`  ${label}: ${done}/${total}\r`)
    if (done === total) process.stdout.write('\n')
  }
}

// ----------------------------------------------------------------- generacion

async function seedOwners() {
  process.stdout.write(`Clientes (${CONF.owners})…\n`)
  const payloads = Array.from({ length: CONF.owners }, (_, index) => {
    const first = pick(FIRST)
    const last = pick(LAST)
    return {
      fullName: `${first} ${last}`,
      email: `${slug(first)}.${slug(last)}${index + 1}@example.com`,
      phone: `+34 6${randInt(10000000, 99999999)}`,
      address: `${pick(STREETS)} ${randInt(1, 180)}, ${pick(CITIES)}`,
    }
  })
  let done = 0
  const owners = await mapLimit(payloads, 8, async (payload) => {
    const created = await api('POST', '/owners', payload)
    done += 1
    progress('Clientes', done, payloads.length)
    return created
  })
  return owners
}

async function seedProducts() {
  process.stdout.write(`Productos (${CONF.products})…\n`)
  const categories = Object.keys(CATEGORY_LINES)
  const payloads = Array.from({ length: CONF.products }, (_, index) => {
    const category = pick(categories)
    const line = pick(CATEGORY_LINES[category])
    const size = pick(SIZES)
    const [minPrice, maxPrice] = CATEGORY_PRICE[category]
    // ~15% de productos entran con stock bajo para que el panel tenga alertas.
    const stock = chance(0.15) ? randInt(0, 4) : randInt(15, 400)
    return {
      name: `${line} ${size}`,
      brand: pick(BRANDS),
      category,
      description: `Lote sintético #${index + 1}. Producto de demostración para pruebas de catálogo.`,
      unitPrice: money(minPrice + Math.random() * (maxPrice - minPrice)),
      stock,
      weightKg: money(randInt(100, 15000) / 1000),
    }
  })
  let done = 0
  const products = await mapLimit(payloads, 8, async (payload) => {
    const created = await api('POST', '/foods', payload)
    done += 1
    progress('Productos', done, payloads.length)
    return created
  })
  return products
}

async function seedPets(owners) {
  process.stdout.write(`Mascotas (${CONF.pets})…\n`)
  const speciesPool = ['dog', 'dog', 'dog', 'cat', 'cat', 'cat', 'bird', 'rabbit', 'reptile', 'other']
  const payloads = Array.from({ length: CONF.pets }, () => {
    const species = pick(speciesPool)
    const owned = owners.length > 0 && chance(0.55)
    const owner = owned ? pick(owners) : null
    const birth = new Date()
    birth.setFullYear(birth.getFullYear() - randInt(1, 15))
    birth.setMonth(randInt(0, 11))
    birth.setDate(randInt(1, 28))
    return {
      name: pick(PET_NAMES),
      species,
      breed: pick(BREEDS[species]),
      birthDate: isoDate(birth),
      sex: pick(['male', 'female', 'female', 'unknown']),
      status: owner ? 'owned' : 'available',
      ownerId: owner ? owner.id : null,
      notes: pick(PET_NOTES),
    }
  })
  let done = 0
  const pets = await mapLimit(payloads, 8, async (payload) => {
    const created = await api('POST', '/pets', payload)
    done += 1
    progress('Mascotas', done, payloads.length)
    return created
  })
  return pets
}

async function seedAppointments(pets) {
  if (pets.length === 0) return []
  process.stdout.write(`Citas (${CONF.appointments})…\n`)
  const payloads = Array.from({ length: CONF.appointments }, () => {
    const pet = pick(pets)
    const date = new Date()
    date.setDate(date.getDate() + randInt(1, 120))
    date.setHours(randInt(9, 19), pick([0, 15, 30, 45]), 0, 0)
    return {
      petId: pet.id,
      scheduledAt: localIso(date),
      reason: pick(REASONS),
      vetName: chance(0.8) ? pick(VETS) : null,
      durationMinutes: randInt(1, 12) * 5,
      notes: chance(0.3) ? pick(PET_NOTES) : null,
    }
  })
  let done = 0
  const appointments = await mapLimit(payloads, 6, async (payload) => {
    const created = await api('POST', '/appointments', payload)
    done += 1
    progress('Citas', done, payloads.length)
    return created
  })
  return appointments
}

async function seedSales(owners, products) {
  process.stdout.write(`Ventas (${CONF.sales})…\n`)
  const stock = new Map(products.map((product) => [product.id, product.stock]))
  const sellable = products.filter((product) => product.stock > 0)
  const created = []

  for (let index = 0; index < CONF.sales; index += 1) {
    const lineCount = randInt(1, 4)
    const chosen = shuffle(sellable).slice(0, lineCount)
    const lines = []
    for (const product of chosen) {
      const available = stock.get(product.id) ?? 0
      const quantity = Math.max(1, Math.min(available, randInt(1, 5)))
      if (available <= 0) continue
      stock.set(product.id, available - quantity)
      lines.push({ productId: product.id, quantity })
    }
    if (lines.length === 0) continue

    const registered = owners.length > 0 && chance(0.7)
    const owner = registered ? pick(owners) : null
    const payload = {
      customerId: owner ? owner.id : null,
      customerName: owner ? owner.fullName : pick(FIRST) + ' ' + pick(LAST),
      paymentMethod: pick(['cash', 'card', 'transfer']),
      lines,
      notes: chance(0.2) ? 'Venta de demostración' : null,
    }
    try {
      created.push(await api('POST', '/sales', payload))
    } catch (error) {
      // 409 por stock: seguimos, es solo data de prueba.
      if (!String(error.message).includes('409')) throw error
    }
    progress('Ventas', index + 1, CONF.sales)
  }
  return created
}

async function seedAdoptions(owners, pets) {
  if (owners.length === 0) return []
  const available = shuffle(pets.filter((pet) => pet.status === 'available'))
  const total = Math.min(CONF.adoptions, available.length)
  process.stdout.write(`Adopciones (${total})…\n`)

  const created = []
  for (let index = 0; index < total; index += 1) {
    const pet = available[index]
    const adopter = pick(owners)
    const payload = {
      petId: pet.id,
      adopterId: adopter.id,
      fee: money(randInt(0, 12000) / 100),
      notes: chance(0.4) ? 'Adopción de demostración' : null,
    }
    try {
      created.push(await api('POST', '/adoptions', payload))
    } catch (error) {
      if (!String(error.message).includes('409')) throw error
    }
    progress('Adopciones', index + 1, total)
  }

  // Un cuarto de las adopciones se cierran (cancelada/devuelta) para poblar los filtros.
  const toClose = shuffle(created).slice(0, Math.floor(created.length / 4))
  for (const adoption of toClose) {
    const reason = pick(['Motivo familiar', 'Alergias en el nuevo hogar', 'Cambio de circunstancias', 'Documentación incompleta'])
    try {
      if (chance(0.5)) await api('POST', `/adoptions/${adoption.id}/cancel`, { reason })
      else await api('POST', `/adoptions/${adoption.id}/return`, { reason })
    } catch {
      // sin importancia para datos de prueba
    }
  }
  return created
}

// ----------------------------------------------------------------- principal

async function main() {
  const started = Date.now()
  process.stdout.write(`\nGenerando datos sintéticos contra ${API}\n`)
  process.stdout.write('──────────────────────────────────────────────\n')

  await api('GET', '/owners?size=1') // comprobacion de que la API responde

  const owners = await seedOwners()
  const products = await seedProducts()
  const pets = await seedPets(owners)
  const appointments = await seedAppointments(pets)
  const sales = await seedSales(owners, products)
  const adoptions = await seedAdoptions(owners, pets)

  process.stdout.write('──────────────────────────────────────────────\n')
  process.stdout.write('Resumen:\n')
  process.stdout.write(`  clientes ....... ${owners.length}\n`)
  process.stdout.write(`  productos ...... ${products.length}\n`)
  process.stdout.write(`  mascotas ....... ${pets.length}\n`)
  process.stdout.write(`  citas .......... ${appointments.length}\n`)
  process.stdout.write(`  ventas ......... ${sales.length}\n`)
  process.stdout.write(`  adopciones ..... ${adoptions.length}\n`)
  process.stdout.write(`\nListo en ${((Date.now() - started) / 1000).toFixed(1)} s. Recarga el frontend para ver la paginación.\n\n`)
}

main().catch((error) => {
  process.stderr.write(`\nError generando datos: ${error.message}\n`)
  process.stderr.write('¿Está la API en marcha en ' + API + '?\n')
  process.exit(1)
})
