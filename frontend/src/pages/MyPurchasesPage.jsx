import { useState } from 'react'
import PageHeader from '../components/PageHeader.jsx'
import DataTable from '../components/DataTable.jsx'
import Pagination from '../components/Pagination.jsx'
import Modal from '../components/Modal.jsx'
import { useList } from '../hooks/useApi.js'
import { portal } from '../lib/api.js'
import { dateTime, money, num } from '../lib/format.js'
import { label } from '../lib/enums.js'

export default function MyPurchasesPage() {
  const list = useList((params) => portal.purchases(params), {}, 10)
  const [detail, setDetail] = useState(null)

  const columns = [
    { key: 'saleNumber', header: 'Ticket', className: 'cell-strong' },
    { key: 'soldAt', header: 'Fecha', render: (row) => dateTime(row.soldAt) },
    { key: 'paymentMethod', header: 'Pago', render: (row) => label('payment', row.paymentMethod) },
    { key: 'totalItems', header: 'Artículos', render: (row) => num(row.totalItems) },
    { key: 'totalAmount', header: 'Total', render: (row) => <strong>{money(row.totalAmount)}</strong> },
    {
      key: 'actions',
      header: 'Detalle',
      hideLabel: true,
      className: 'cell-actions',
      render: (row) => (
        <button type="button" className="btn btn-outline btn-sm" onClick={() => setDetail(row)}>Ver líneas</button>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        eyebrow="Mi cuenta"
        title="Mis compras"
        description="Tickets de la tienda de pienso asociados a tu ficha de cliente."
        actions={<button type="button" className="btn btn-ghost" onClick={list.reload}>↻ Actualizar</button>}
      />

      <DataTable
        columns={columns}
        rows={list.rows}
        loading={list.loading}
        empty={<><div className="empty-icon">🧾</div><p>Todavía no has comprado en la tienda de la clínica.</p></>}
      />
      <Pagination
        page={list.page}
        size={list.size}
        totalPages={list.totalPages}
        totalElements={list.totalElements}
        onPage={list.setPage}
        onSize={list.setSize}
      />

      <Modal
        open={Boolean(detail)}
        title={detail ? `Ticket ${detail.saleNumber}` : 'Ticket'}
        onClose={() => setDetail(null)}
        footer={<button type="button" className="btn btn-primary" onClick={() => setDetail(null)}>Cerrar</button>}
      >
        {detail && (
          <>
            <p className="cell-muted" style={{ marginTop: 0 }}>
              {dateTime(detail.soldAt)} · {label('payment', detail.paymentMethod)}
            </p>
            <div className="sale-lines">
              {detail.lines.map((line) => (
                <div key={line.productId} className="sale-line">
                  <span>
                    <strong>{line.productName}</strong>
                    <span className="cell-muted"> · {line.productBrand}</span>
                    <br />
                    <span className="cell-muted">{num(line.quantity)} × {money(line.unitPrice)}</span>
                  </span>
                  <strong>{money(line.lineTotal)}</strong>
                </div>
              ))}
            </div>
            <div className="sale-total">
              <span>Total</span>
              <strong>{money(detail.totalAmount)}</strong>
            </div>
          </>
        )}
      </Modal>
    </>
  )
}
