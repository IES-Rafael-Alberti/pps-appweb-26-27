import PageHeader from '../components/PageHeader.jsx'
import DataTable from '../components/DataTable.jsx'
import Pagination from '../components/Pagination.jsx'
import Badge from '../components/Badge.jsx'
import { useList } from '../hooks/useApi.js'
import { portal } from '../lib/api.js'
import { date } from '../lib/format.js'
import { label, petStatusTone } from '../lib/enums.js'

export default function MyPetsPage() {
  const list = useList((params) => portal.pets(params), {}, 10)

  const columns = [
    { key: 'name', header: 'Nombre', className: 'cell-strong' },
    { key: 'species', header: 'Especie', render: (row) => label('species', row.species) },
    { key: 'breed', header: 'Raza', render: (row) => row.breed || '—' },
    { key: 'birthDate', header: 'Nacimiento', render: (row) => date(row.birthDate) },
    { key: 'sex', header: 'Sexo', render: (row) => label('sex', row.sex) },
    { key: 'status', header: 'Estado', render: (row) => <Badge tone={petStatusTone(row.status)}>{label('petStatus', row.status)}</Badge> },
    { key: 'notes', header: 'Notas', className: 'cell-muted', render: (row) => row.notes || '—' },
  ]

  return (
    <>
      <PageHeader
        eyebrow="Mi cuenta"
        title="Mis mascotas"
        description="Las mascotas registradas a tu nombre. Para cambiar una ficha, contacta con la clínica."
        actions={<button type="button" className="btn btn-ghost" onClick={list.reload}>↻ Actualizar</button>}
      />

      <DataTable
        columns={columns}
        rows={list.rows}
        loading={list.loading}
        empty={<><div className="empty-icon">🐾</div><p>Todavía no hay mascotas a tu nombre. La clínica puede registrarlas por ti.</p></>}
      />
      <Pagination
        page={list.page}
        size={list.size}
        totalPages={list.totalPages}
        totalElements={list.totalElements}
        onPage={list.setPage}
        onSize={list.setSize}
      />
    </>
  )
}
