import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import * as api from '../lib/api.js'

const AuthContext = createContext(null)

/**
 * Keeps the logged in user in React state. The tokens themselves never touch
 * localStorage: the access token lives in the api module and the refresh token in
 * an httpOnly cookie, so a XSS bug cannot read the long lived credential.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    api.setSessionListener((nextUser) => {
      setUser(nextUser)
    })

    let cancelled = false
    // On a page reload there is no access token in memory: ask the cookie for a new one.
    api.auth
      .refresh()
      .then((data) => {
        if (!cancelled) setUser(data?.user ?? null)
      })
      .catch(() => {
        if (!cancelled) setUser(null)
      })
      .finally(() => {
        if (!cancelled) setReady(true)
      })

    return () => {
      cancelled = true
    }
  }, [])

  const applySession = useCallback((data) => {
    api.setAccessToken(data.accessToken)
    setUser(data.user)
    return data.user
  }, [])

  const login = useCallback(
    async (email, password) => applySession(await api.auth.login({ email, password })),
    [applySession],
  )

  const register = useCallback(
    async (payload) => applySession(await api.auth.register(payload)),
    [applySession],
  )

  const logout = useCallback(async () => {
    try {
      await api.auth.logout()
    } finally {
      api.setAccessToken(null)
      setUser(null)
    }
  }, [])

  const value = useMemo(
    () => ({
      user,
      ready,
      isAuthenticated: Boolean(user),
      isStaff: user?.role === 'admin' || user?.role === 'manager',
      isAdmin: user?.role === 'admin',
      login,
      register,
      logout,
    }),
    [user, ready, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return context
}
