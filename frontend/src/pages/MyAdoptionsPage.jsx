import PageHeader from '../components/PageHeader.jsx'
import DataTable from '../components/DataTable.jsx'
import Pagination from '../components/Pagination.jsx'
import Badge from '../components/Badge.jsx'
import { useList } from '../hooks/useApi.js'
import { portal } from '../lib/api.js'
import { dateTime, money } from '../lib/format.js'
import { adoptionStatusTone, label } from '../lib/enums.js'

export default function MyAdoptionsPage() {
  const list = useList((params) => portal.adoptions(params), {}, 10)

  const columns = [
    { key: 'petName', header: 'Mascota', className: 'cell-strong' },
    { key: 'petSpecies', header: 'Especie', render: (row) => label('species', row.petSpecies) },
    { key: 'adoptedAt', header: 'Fecha', render: (row) => dateTime(row.adoptedAt) },
    { key: 'fee', header: 'Tasa', render: (row) => money(row.fee) },
    { key: 'status', header: 'Estado', render: (row) => <Badge tone={adoptionStatusTone(row.status)}>{label('adoptionStatus', row.status)}</Badge> },
    { key: 'notes', header: 'Notas', className: 'cell-muted', render: (row) => row.notes || '—' },
  ]

  return (
    <>
      <PageHeader
        eyebrow="Mi cuenta"
        title="Mis adopciones"
        description="Animales del refugio adoptados a tu nombre y su estado actual."
        actions={<button type="button" className="btn btn-ghost" onClick={list.reload}>↻ Actualizar</button>}
      />

      <DataTable
        columns={columns}
        rows={list.rows}
        loading={list.loading}
        empty={<><div className="empty-icon">🏠</div><p>Todavía no has adoptado ningún animal del refugio.</p></>}
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
