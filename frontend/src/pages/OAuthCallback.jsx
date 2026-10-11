import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { homeFor, useAuth } from '../auth/AuthContext'
import { Spinner } from '../components/ui'

/** Google login returns here with our tokens in the URL fragment. */
export default function OAuthCallback() {
  const { loginWithTokens } = useAuth()
  const navigate = useNavigate()
  const done = useRef(false)

  useEffect(() => {
    if (done.current) return
    done.current = true
    const params = new URLSearchParams(window.location.hash.slice(1))
    const access = params.get('accessToken')
    const refresh = params.get('refreshToken')
    if (!access || !refresh) {
      navigate('/login?error=oauth', { replace: true })
      return
    }
    window.history.replaceState(null, '', window.location.pathname)
    loginWithTokens(access, refresh).then((user) => navigate(user ? homeFor(user) : '/login?error=oauth', { replace: true }))
  }, [loginWithTokens, navigate])

  return <Spinner />
}
