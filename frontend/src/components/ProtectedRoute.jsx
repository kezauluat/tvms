import { Navigate, useLocation } from 'react-router-dom'
import { homeFor, useAuth } from '../auth/AuthContext'
import { Spinner } from './ui'

/** Front-end guard. The real protection is on the server (URL rules + @PreAuthorize). */
export default function ProtectedRoute({ roles, children, allowIncompleteProfile = false }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <Spinner />
  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />
  if (roles && !roles.includes(user.role)) return <Navigate to={homeFor(user)} replace />
  if (user.role === 'DRIVER' && !user.profileComplete && !allowIncompleteProfile) {
    return <Navigate to="/complete-profile" replace />
  }
  return children
}
