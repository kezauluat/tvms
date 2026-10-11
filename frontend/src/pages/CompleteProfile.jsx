import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { errorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Alert, Button, Card, Input } from '../components/ui'
import { clean, PHONE_HINT } from '../utils/format'

/** Google sign-ups link their driving license once before using the portal. */
export default function CompleteProfile() {
  const { completeProfile, user } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ licenseNumber: '', dateOfBirth: '', phoneNumber: '', address: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (f) => (e) => setForm({ ...form, [f]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await completeProfile(clean(form))
      navigate('/my', { replace: true })
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mx-auto max-w-lg">
      <Card>
        <h1 className="mb-1 text-xl font-bold">Welcome, {user?.fullName}</h1>
        <p className="mb-5 text-sm text-slate-500">Add your driving license to see and pay your fines.</p>
        <Alert>{error}</Alert>
        <form onSubmit={submit} className="space-y-4">
          <Input label="Driving license number" required value={form.licenseNumber} onChange={set('licenseNumber')} />
          <Input label="Date of birth" type="date" required value={form.dateOfBirth} onChange={set('dateOfBirth')} />
          <Input label="Phone" required value={form.phoneNumber} onChange={set('phoneNumber')} hint={PHONE_HINT} />
          <Input label="Address (optional)" value={form.address} onChange={set('address')} />
          <Button type="submit" className="w-full" disabled={busy}>{busy ? 'Saving…' : 'Continue'}</Button>
        </form>
      </Card>
    </div>
  )
}
