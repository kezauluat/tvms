import { Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import Home from './pages/Home'
import Login from './pages/Login'
import Register from './pages/Register'
import OAuthCallback from './pages/OAuthCallback'
import CompleteProfile from './pages/CompleteProfile'
import NotFound from './pages/NotFound'
import DriverDashboard from './pages/driver/DriverDashboard'
import OfficerDashboard from './pages/officer/OfficerDashboard'
import AdminDashboard from './pages/admin/AdminDashboard'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/oauth2/callback" element={<OAuthCallback />} />
        <Route path="/complete-profile" element={<ProtectedRoute roles={['DRIVER']} allowIncompleteProfile><CompleteProfile /></ProtectedRoute>} />
        <Route path="/my" element={<ProtectedRoute roles={['DRIVER']}><DriverDashboard /></ProtectedRoute>} />
        <Route path="/officer" element={<ProtectedRoute roles={['OFFICER']}><OfficerDashboard /></ProtectedRoute>} />
        <Route path="/admin" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboard /></ProtectedRoute>} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}
