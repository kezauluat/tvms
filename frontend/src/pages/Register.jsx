import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { errorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Alert, Button, Card, Input } from '../components/ui'
import { clean, PHONE_HINT } from '../utils/format'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', password: '', phoneNumber: '', licenseNumber: '', dateOfBirth: '', address: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (f) => (e) => setForm({ ...form, [f]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await register(clean(form))
      navigate('/my', { replace: true })
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <Card>
        <h1 className="mb-1 text-xl font-bold">Create a driver account</h1>
        <p className="mb-5 text-sm text-slate-500">If the police already registered your license, use the same date of birth and we link your record.</p>
        <Alert>{error}</Alert>
        <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2"><Input label="Full name" required value={form.fullName} onChange={set('fullName')} /></div>
          <Input label="Email" type="email" required value={form.email} onChange={set('email')} />
          <Input label="Phone" required placeholder="0788123456" value={form.phoneNumber} onChange={set('phoneNumber')} hint={PHONE_HINT} />
          <Input label="Driving license number" required value={form.licenseNumber} onChange={set('licenseNumber')} />
          <Input label="Date of birth" type="date" required value={form.dateOfBirth} onChange={set('dateOfBirth')} hint="You must be at least 18" />
          <Input label="Password" type="password" required minLength={8} value={form.password} onChange={set('password')} hint="8+ characters with letters and numbers" />
          <Input label="Address (optional)" value={form.address} onChange={set('address')} />
          <div className="sm:col-span-2"><Button type="submit" className="w-full" disabled={busy}>{busy ? 'Creating account…' : 'Create account'}</Button></div>
        </form>
        <p className="mt-5 text-center text-sm text-slate-500">Already registered? <Link to="/login" className="font-semibold text-amber-700 hover:underline">Log in</Link></p>
      </Card>
    </div>
  )
}
