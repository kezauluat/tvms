import { useCallback, useEffect, useState } from 'react'
import api, { errorMessage } from '../../api/client'
import { Alert, Button, Card, Input, Select } from '../../components/ui'
import { clean, fmtDate, PHONE_HINT } from '../../utils/format'

const emptyDriver = { fullName: '', licenseNumber: '', dateOfBirth: '', phoneNumber: '', address: '' }
const emptyVehicle = { plateNumber: '', make: '', model: '', year: '', ownerId: '' }
const emptyOfficer = { fullName: '', badgeNumber: '', station: '', phoneNumber: '', email: '', password: '' }

export default function PeopleTab() {
  const [drivers, setDrivers] = useState([])
  const [vehicles, setVehicles] = useState([])
  const [officers, setOfficers] = useState([])
  const [driver, setDriver] = useState(emptyDriver)
  const [vehicle, setVehicle] = useState(emptyVehicle)
  const [officer, setOfficer] = useState(emptyOfficer)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const load = useCallback(async () => {
    try {
      const [d, v, o] = await Promise.all([api.get('/drivers'), api.get('/vehicles'), api.get('/officers')])
      setDrivers(d.data)
      setVehicles(v.data)
      setOfficers(o.data)
    } catch (e) {
      setError(errorMessage(e))
    }
  }, [])
  useEffect(() => { load() }, [load])

  const run = (fn, ok, reset) => async (e) => {
    e.preventDefault()
    try {
      await fn()
      setMessage(ok)
      setError('')
      reset()
      load()
    } catch (err) {
      setError(errorMessage(err))
      setMessage('')
    }
  }

  const remove = async (path, label) => {
    if (!window.confirm(`Delete ${label}?`)) return
    try { await api.delete(path); load() } catch (e) { setError(errorMessage(e)) }
  }

  return (
    <>
      <Alert type="success" onClose={() => setMessage('')}>{message}</Alert>
      <Alert onClose={() => setError('')}>{error}</Alert>
      <div className="mb-6 grid gap-6 lg:grid-cols-3">
        <Card>
          <h2 className="mb-3 font-bold">Register driver</h2>
          <form onSubmit={run(() => api.post('/drivers', clean(driver)), 'Driver registered.', () => setDriver(emptyDriver))} className="space-y-3">
            <Input label="Full name" required value={driver.fullName} onChange={(e) => setDriver({ ...driver, fullName: e.target.value })} />
            <Input label="License number" required value={driver.licenseNumber} onChange={(e) => setDriver({ ...driver, licenseNumber: e.target.value })} />
            <Input label="Date of birth" type="date" required value={driver.dateOfBirth} onChange={(e) => setDriver({ ...driver, dateOfBirth: e.target.value })} />
            <Input label="Phone" required value={driver.phoneNumber} onChange={(e) => setDriver({ ...driver, phoneNumber: e.target.value })} />
            <Input label="Address" value={driver.address} onChange={(e) => setDriver({ ...driver, address: e.target.value })} />
            <Button type="submit">Add driver</Button>
          </form>
        </Card>
        <Card>
          <h2 className="mb-3 font-bold">Register vehicle</h2>
          <form onSubmit={run(() => api.post('/vehicles', { plateNumber: vehicle.plateNumber, make: vehicle.make, model: vehicle.model, year: Number(vehicle.year), owner: { id: vehicle.ownerId } }),
            'Vehicle registered.', () => setVehicle(emptyVehicle))} className="space-y-3">
            <Input label="Plate number" required placeholder="RAB123A" value={vehicle.plateNumber} onChange={(e) => setVehicle({ ...vehicle, plateNumber: e.target.value })} />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Make" required value={vehicle.make} onChange={(e) => setVehicle({ ...vehicle, make: e.target.value })} />
              <Input label="Model" required value={vehicle.model} onChange={(e) => setVehicle({ ...vehicle, model: e.target.value })} />
            </div>
            <Input label="Year" type="number" min="1980" required value={vehicle.year} onChange={(e) => setVehicle({ ...vehicle, year: e.target.value })} />
            <Select label="Owner" required value={vehicle.ownerId} onChange={(e) => setVehicle({ ...vehicle, ownerId: e.target.value })}>
              <option value="">Choose driver</option>
              {drivers.map((d) => <option key={d.id} value={d.id}>{d.fullName} · {d.licenseNumber}</option>)}
            </Select>
            <Button type="submit">Add vehicle</Button>
          </form>
        </Card>
        <Card>
          <h2 className="mb-3 font-bold">Create officer account</h2>
          <form onSubmit={run(() => api.post('/admin/officer-accounts', clean(officer)), 'Officer account created. They can log in now.', () => setOfficer(emptyOfficer))} className="space-y-3">
            <Input label="Full name" required value={officer.fullName} onChange={(e) => setOfficer({ ...officer, fullName: e.target.value })} />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Badge" required placeholder="RNP-1002" value={officer.badgeNumber} onChange={(e) => setOfficer({ ...officer, badgeNumber: e.target.value })} />
              <Input label="Station" value={officer.station} onChange={(e) => setOfficer({ ...officer, station: e.target.value })} />
            </div>
            <Input label="Phone" required value={officer.phoneNumber} onChange={(e) => setOfficer({ ...officer, phoneNumber: e.target.value })} hint={PHONE_HINT} />
            <Input label="Email" type="email" required value={officer.email} onChange={(e) => setOfficer({ ...officer, email: e.target.value })} />
            <Input label="Temporary password" type="password" minLength={8} required value={officer.password} onChange={(e) => setOfficer({ ...officer, password: e.target.value })} />
            <Button type="submit">Create officer</Button>
          </form>
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {[
          ['Drivers', drivers, (d) => [d.fullName, `${d.licenseNumber} · born ${fmtDate(d.dateOfBirth)}`], (d) => `/drivers/${d.id}`],
          ['Vehicles', vehicles, (v) => [`${v.plateNumber} · ${v.make} ${v.model}`, `${v.year} · owner ${v.owner?.fullName}`], (v) => `/vehicles/${v.id}`],
          ['Officers', officers, (o) => [o.fullName, `${o.badgeNumber} · ${o.station || 'No station'}`], (o) => `/officers/${o.id}`],
        ].map(([title, rows, show, path]) => (
          <Card key={title}>
            <h2 className="mb-3 font-bold">{title} ({rows.length})</h2>
            <ul className="max-h-80 divide-y divide-slate-100 overflow-y-auto">
              {rows.map((r) => {
                const [a, b] = show(r)
                return (
                  <li key={r.id} className="flex items-center justify-between gap-2 py-2">
                    <span><span className="block text-sm font-semibold">{a}</span><span className="block text-xs text-slate-500">{b}</span></span>
                    <button className="text-xs text-red-600 hover:underline" onClick={() => remove(path(r), a)}>Delete</button>
                  </li>
                )
              })}
            </ul>
          </Card>
        ))}
      </div>
    </>
  )
}
