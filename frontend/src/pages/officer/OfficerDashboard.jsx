import { useCallback, useEffect, useState } from 'react'
import api, { errorMessage } from '../../api/client'
import ViolationDetailModal from '../../components/ViolationDetailModal'
import { Alert, Button, Card, EmptyState, FineBadge, Input, PageTitle, Select, Textarea, inputClass } from '../../components/ui'
import { fmtDateTime, nowLocalInput, rwf } from '../../utils/format'

/** Extra measurements asked for depending on the offence (saved as flexible MongoDB fields). */
const EXTRA_FIELDS = {
  SPEEDING: [['recordedSpeedKmh', 'Recorded speed (km/h)'], ['speedLimitKmh', 'Speed limit (km/h)']],
  DRUNK_DRIVING: [['alcoholLevel', 'Alcohol level (g/L)']],
}

const emptyForm = () => ({ violationTypeId: '', location: '', occurredAt: nowLocalInput(), driverLicenseNumber: '', notes: '', latitude: '', longitude: '', photoUrl: '', details: {} })

export default function OfficerDashboard() {
  const [types, setTypes] = useState([])
  const [plate, setPlate] = useState('')
  const [vehicle, setVehicle] = useState(null)
  const [otherDriver, setOtherDriver] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(null)
  const [busy, setBusy] = useState(false)
  const [records, setRecords] = useState(null)
  const [page, setPage] = useState(0)
  const [detailId, setDetailId] = useState(null)

  const loadRecords = useCallback(async (p) => {
    try {
      const { data } = await api.get('/violations/recorded-by-me', { params: { page: p, size: 8 } })
      setRecords(data)
    } catch (e) {
      setError(errorMessage(e))
    }
  }, [])

  useEffect(() => {
    api.get('/violation-types').then((r) => setTypes(r.data)).catch((e) => setError(errorMessage(e)))
  }, [])
  useEffect(() => { loadRecords(page) }, [loadRecords, page])

  const type = types.find((t) => t.id === form.violationTypeId)
  const set = (f) => (e) => setForm({ ...form, [f]: e.target.value })

  const lookup = async () => {
    setError('')
    setVehicle(null)
    try {
      const { data } = await api.get(`/vehicles/plate/${encodeURIComponent(plate.replace(/\s+/g, ''))}`)
      setVehicle(data)
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  const useGps = () => {
    if (!navigator.geolocation) return setError('This device has no GPS / location support')
    navigator.geolocation.getCurrentPosition(
      (pos) => setForm((f) => ({ ...f, latitude: pos.coords.latitude.toFixed(6), longitude: pos.coords.longitude.toFixed(6) })),
      () => setError('Location permission was denied'),
    )
  }

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    setSuccess(null)
    const details = Object.fromEntries(Object.entries(form.details).filter(([, v]) => v !== ''))
    try {
      const { data } = await api.post('/violations', {
        plateNumber: vehicle.plateNumber,
        violationTypeId: form.violationTypeId,
        location: form.location,
        occurredAt: form.occurredAt || null,
        driverLicenseNumber: otherDriver ? form.driverLicenseNumber : null,
        notes: form.notes || null,
        latitude: form.latitude === '' ? null : Number(form.latitude),
        longitude: form.longitude === '' ? null : Number(form.longitude),
        photoUrls: form.photoUrl ? [form.photoUrl] : [],
        details,
      })
      setSuccess(data)
      setForm(emptyForm())
      setVehicle(null)
      setPlate('')
      setOtherDriver(false)
      setPage(0)
      loadRecords(0)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <PageTitle title="Record a violation" subtitle="Look up the vehicle, choose the offence, submit. The driver is notified by SMS and email." />
      {success && (
        <Alert type="success" onClose={() => setSuccess(null)}>
          Recorded {success.referenceNumber}: {success.violationName} for {success.plateNumber}, fine {rwf(success.fineAmount)}.
        </Alert>
      )}
      <Alert onClose={() => setError('')}>{error}</Alert>

      <div className="grid gap-6 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <div className="mb-4 flex gap-2">
            <input className={`${inputClass} uppercase tracking-widest`} placeholder="Plate, e.g. RAB123A" value={plate}
                   onChange={(e) => setPlate(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), lookup())} aria-label="Plate number" />
            <Button type="button" variant="accent" onClick={lookup} disabled={!plate.trim()}>Look up</Button>
          </div>

          {vehicle && (
            <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm">
              <p className="font-bold">{vehicle.plateNumber} · {vehicle.make} {vehicle.model} ({vehicle.year})</p>
              <p className="text-slate-600">Owner: {vehicle.owner?.fullName} · License {vehicle.owner?.licenseNumber}</p>
            </div>
          )}

          <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <Select label="Offence" required value={form.violationTypeId} onChange={(e) => setForm({ ...form, violationTypeId: e.target.value, details: {} })} disabled={!vehicle}>
                <option value="">Choose the offence</option>
                {types.map((t) => <option key={t.id} value={t.id}>{t.name} · {rwf(t.fineAmount)} · {t.demeritPoints} pts</option>)}
              </Select>
            </div>
            {(EXTRA_FIELDS[type?.code] || []).map(([key, label]) => (
              <Input key={key} label={label} type="number" min="0" step="any" value={form.details[key] || ''}
                     onChange={(e) => setForm({ ...form, details: { ...form.details, [key]: e.target.value } })} />
            ))}
            <Input label="Location" required placeholder="e.g. KN 5 Rd, Remera" value={form.location} onChange={set('location')} disabled={!vehicle} />
            <Input label="Date and time" type="datetime-local" value={form.occurredAt} onChange={set('occurredAt')} disabled={!vehicle} hint="Within the last 7 days" />
            <label className="flex items-center gap-2 text-sm sm:col-span-2">
              <input type="checkbox" checked={otherDriver} onChange={(e) => setOtherDriver(e.target.checked)} disabled={!vehicle} />
              The person driving is not the owner
            </label>
            {otherDriver && <Input label="Driver's license number" required value={form.driverLicenseNumber} onChange={set('driverLicenseNumber')} />}
            <div className="sm:col-span-2 grid grid-cols-[1fr_1fr_auto] items-end gap-2">
              <Input label="Latitude" value={form.latitude} onChange={set('latitude')} disabled={!vehicle} />
              <Input label="Longitude" value={form.longitude} onChange={set('longitude')} disabled={!vehicle} />
              <Button type="button" variant="secondary" onClick={useGps} disabled={!vehicle}>GPS</Button>
            </div>
            <div className="sm:col-span-2"><Input label="Photo link (optional)" type="url" value={form.photoUrl} onChange={set('photoUrl')} disabled={!vehicle} /></div>
            <div className="sm:col-span-2"><Textarea label="Notes (optional)" maxLength={1000} value={form.notes} onChange={set('notes')} disabled={!vehicle} /></div>
            <div className="sm:col-span-2">
              <Button type="submit" className="w-full" disabled={!vehicle || !form.violationTypeId || busy}>
                {busy ? 'Saving…' : type ? `Record violation · fine ${rwf(type.fineAmount)}` : 'Record violation'}
              </Button>
            </div>
          </form>
        </Card>

        <Card className="lg:col-span-2">
          <h2 className="mb-3 font-bold">My recent records</h2>
          {!records ? null : records.content.length === 0 ? <EmptyState>Nothing recorded yet.</EmptyState> : (
            <>
              <ul className="divide-y divide-slate-100">
                {records.content.map((v) => (
                  <li key={v.id}>
                    <button className="flex w-full items-center justify-between gap-2 py-2 text-left hover:bg-slate-50" onClick={() => setDetailId(v.id)}>
                      <span>
                        <span className="block text-sm font-semibold">{v.plateNumber} · {v.violationName}</span>
                        <span className="block text-xs text-slate-500">{fmtDateTime(v.occurredAt)} · {v.referenceNumber}</span>
                      </span>
                      <FineBadge status={v.fineStatus} />
                    </button>
                  </li>
                ))}
              </ul>
              <div className="mt-3 flex items-center justify-between text-sm">
                <Button variant="ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
                <span className="text-slate-500">Page {records.page + 1} of {Math.max(records.totalPages, 1)}</span>
                <Button variant="ghost" disabled={page + 1 >= records.totalPages} onClick={() => setPage(page + 1)}>Next</Button>
              </div>
            </>
          )}
        </Card>
      </div>
      {detailId && <ViolationDetailModal id={detailId} onClose={() => setDetailId(null)} />}
    </>
  )
}
