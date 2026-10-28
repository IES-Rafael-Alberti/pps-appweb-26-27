import { useCallback, useEffect, useRef } from 'react'
import { useState } from 'react'

/** Runs an async loader and exposes { data, loading, error, reload }. */
export function useApi(loader, deps = []) {
  const loaderRef = useRef(loader)
  loaderRef.current = loader

  const [state, setState] = useState({ data: null, loading: true, error: null })

  const reload = useCallback(async () => {
    setState((current) => ({ ...current, loading: true, error: null }))
    try {
      const data = await loaderRef.current()
      setState({ data, loading: false, error: null })
    } catch (error) {
      setState({ data: null, loading: false, error })
    }
  }, [])

  useEffect(() => {
    reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  return { ...state, reload, setData: (data) => setState((s) => ({ ...s, data })) }
}

/** Paginated list bound to a loader that receives { page, size, ...filters }. */
export function useList(loader, filters = {}, initialSize = 20) {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(initialSize)
  const filterKey = JSON.stringify(filters)

  const query = useApi(() => loader({ ...filters, page, size }), [filterKey, page, size])

  // Whenever a filter changes we jump back to the first page.
  useEffect(() => {
    setPage(0)
  }, [filterKey])

  return {
    ...query,
    page,
    size,
    setPage,
    setSize: (next) => {
      setSize(next)
      setPage(0)
    },
    rows: query.data?.content ?? [],
    totalPages: query.data?.totalPages ?? 1,
    totalElements: query.data?.totalElements ?? 0,
  }
}
