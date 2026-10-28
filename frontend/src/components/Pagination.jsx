export default function Pagination({ page = 0, totalPages = 1, totalElements = 0, size = 20, onPage, onSize }) {
  if (totalElements === 0 && totalPages <= 1) return null

  return (
    <div className="pagination">
      <span className="info">
        {totalElements} registro{totalElements === 1 ? '' : 's'} · página {page + 1} de {Math.max(totalPages, 1)}
      </span>
      <span className="spacer" />
      <label className="info">
        Por página&nbsp;
        <select
          className="select"
          style={{ width: 'auto', padding: '.35rem .5rem' }}
          value={size}
          onChange={(event) => onSize?.(Number(event.target.value))}
        >
          {[10, 20, 50, 100].map((option) => (
            <option key={option} value={option}>{option}</option>
          ))}
        </select>
      </label>
      <div className="btn-row">
        <button type="button" className="btn btn-ghost btn-sm" disabled={page <= 0} onClick={() => onPage?.(page - 1)}>
          ‹ Anterior
        </button>
        <button
          type="button"
          className="btn btn-ghost btn-sm"
          disabled={page + 1 >= totalPages}
          onClick={() => onPage?.(page + 1)}
        >
          Siguiente ›
        </button>
      </div>
    </div>
  )
}
