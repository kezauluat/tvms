import axios from 'axios'

export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export const tokens = {
  get access() { return localStorage.getItem('accessToken') },
  get refresh() { return localStorage.getItem('refreshToken') },
  set(access, refresh) {
    localStorage.setItem('accessToken', access)
    localStorage.setItem('refreshToken', refresh)
  },
  clear() {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  },
}

const api = axios.create({ baseURL: `${API_URL}/api` })

api.interceptors.request.use((config) => {
  if (tokens.access) config.headers.Authorization = `Bearer ${tokens.access}`
  return config
})

// When the 15-minute access token expires, get a new pair with the refresh token and retry once.
let refreshing = null
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    const isAuthCall = original?.url?.startsWith('/auth/') && !original.url.startsWith('/auth/me')
    if (error.response?.status === 401 && original && !original._retry && !isAuthCall && tokens.refresh) {
      original._retry = true
      try {
        refreshing = refreshing || axios
          .post(`${API_URL}/api/auth/refresh`, { refreshToken: tokens.refresh })
          .finally(() => { refreshing = null })
        const { data } = await refreshing
        tokens.set(data.accessToken, data.refreshToken)
        original.headers.Authorization = `Bearer ${data.accessToken}`
        return api(original)
      } catch {
        tokens.clear()
        window.dispatchEvent(new Event('auth:logout'))
      }
    }
    return Promise.reject(error)
  },
)

/** Turns backend error JSON ({message} or {errors: {field: msg}}) into one readable line. */
export function errorMessage(err) {
  const data = err?.response?.data
  if (data?.errors && typeof data.errors === 'object') {
    return Object.entries(data.errors).map(([field, msg]) => `${field}: ${msg}`).join(' · ')
  }
  if (data?.message) return data.message
  if (err?.code === 'ERR_NETWORK') return 'Cannot reach the server. Is the backend running?'
  return err?.message || 'Something went wrong'
}

export default api
