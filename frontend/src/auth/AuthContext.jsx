import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import api, { tokens } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(Boolean(tokens.access))

  const loadMe = useCallback(async () => {
    if (!tokens.access) {
      setUser(null)
      setLoading(false)
      return null
    }
    try {
      const { data } = await api.get('/auth/me')
      setUser(data)
      return data
    } catch {
      tokens.clear()
      setUser(null)
      return null
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadMe()
    const onLogout = () => setUser(null)
    window.addEventListener('auth:logout', onLogout)
    return () => window.removeEventListener('auth:logout', onLogout)
  }, [loadMe])

  const handleAuth = (data) => {
    tokens.set(data.accessToken, data.refreshToken)
    setUser(data.user)
    return data.user
  }

  const login = async (email, password) => handleAuth((await api.post('/auth/login', { email, password })).data)
  const register = async (form) => handleAuth((await api.post('/auth/register', form)).data)

  const loginWithTokens = async (accessToken, refreshToken) => {
    tokens.set(accessToken, refreshToken)
    return loadMe()
  }

  const completeProfile = async (form) => {
    const { data } = await api.post('/auth/complete-profile', form)
    setUser(data)
    return data
  }

  const logout = async () => {
    const refreshToken = tokens.refresh
    tokens.clear()
    setUser(null)
    if (refreshToken) await api.post('/auth/logout', { refreshToken }).catch(() => {})
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, loginWithTokens, completeProfile, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)

/** Where each role lands after logging in. */
export function homeFor(user) {
  if (!user) return '/'
  if (user.role === 'ADMIN') return '/admin'
  if (user.role === 'OFFICER') return '/officer'
  return user.profileComplete ? '/my' : '/complete-profile'
}
