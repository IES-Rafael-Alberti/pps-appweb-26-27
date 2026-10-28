import { useCallback, useContext, useMemo, useState } from 'react'
import { createContext } from 'react'
import Modal from '../components/Modal.jsx'

const ToastContext = createContext(null)

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const push = useCallback(
    (message, type) => {
      const id = `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
      setToasts((current) => [...current, { id, message, type }])
      setTimeout(() => dismiss(id), 4200)
    },
    [dismiss],
  )

  const value = useMemo(
    () => ({
      success: (message) => push(message, 'success'),
      error: (message) => push(message, 'error'),
      info: (message) => push(message, 'info'),
    }),
    [push],
  )

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-stack" aria-live="polite">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast toast-${toast.type}`} role="status">
            <strong>{toast.type === 'error' ? 'Error' : toast.type === 'info' ? 'Aviso' : 'Hecho'}</strong>
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) throw new Error('useToast debe usarse dentro de <ToastProvider>')
  return context
}

const ConfirmContext = createContext(null)

export function ConfirmProvider({ children }) {
  const [request, setRequest] = useState(null)

  const confirm = useCallback(
    (options) =>
      new Promise((resolve) => {
        setRequest({
          title: 'Confirmar',
          message: '¿Seguro que quieres continuar?',
          confirmLabel: 'Confirmar',
          danger: false,
          ...options,
          resolve,
        })
      }),
    [],
  )

  const close = (result) => {
    request?.resolve(result)
    setRequest(null)
  }

  const value = useMemo(() => ({ confirm }), [confirm])

  return (
    <ConfirmContext.Provider value={value}>
      {children}
      <Modal
        open={Boolean(request)}
        title={request?.title}
        size="sm"
        onClose={() => close(false)}
        footer={
          <>
            <button type="button" className="btn btn-ghost" onClick={() => close(false)}>
              Cancelar
            </button>
            <button
              type="button"
              className={`btn ${request?.danger ? 'btn-danger' : 'btn-primary'}`}
              onClick={() => close(true)}
            >
              {request?.confirmLabel}
            </button>
          </>
        }
      >
        <p className="modal-intro" style={{ margin: 0 }}>{request?.message}</p>
      </Modal>
    </ConfirmContext.Provider>
  )
}

export function useConfirm() {
  const context = useContext(ConfirmContext)
  if (!context) throw new Error('useConfirm debe usarse dentro de <ConfirmProvider>')
  return context.confirm
}
