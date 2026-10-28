/**
 * Responsive table. Pass `columns` as { key, header, render(row), className, hideLabel }.
 * On small screens each row collapses into a card and the header of every cell is
 * injected through the `data-label` attribute (see styles.css).
 */
export default function DataTable({ columns, rows, keyField = 'id', loading, empty }) {
  if (loading) {
    return (
      <div className="table-wrap">
        <div className="spinner" />
        <p className="empty" style={{ paddingTop: 0 }}>Cargando…</p>
      </div>
    )
  }

  if (!rows || rows.length === 0) {
    return <div className="empty">{empty || <><div className="empty-icon">🐾</div>No hay resultados.</>}</div>
  }

  return (
    <div className="table-wrap">
      <table className="data">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key}>{column.header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={row[keyField] ?? index}>
              {columns.map((column) => (
                <td key={column.key} data-label={column.hideLabel ? '' : column.header} className={column.className}>
                  {column.render ? column.render(row) : row[column.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
