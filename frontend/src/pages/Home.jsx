import { useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errorMessage } from '../api/client'
import { Alert, AppealBadge, Button, Card, EmptyState, FineBadge, inputClass } from '../components/ui'
import { fmtDate, fmtDateTime, rwf } from '../utils/format'

export default function Home() {
  const [plate, setPlate] = useState('')
  const [results, setResults] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const search = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const { data } = await api.get('/public/fines', { params: { plate } })
      setResults(data)
    } catch (err) {
      setError(errorMessage(err))
      setResults(null)
    } finally {
      setBusy(false)
    }
  }

  const total = results?.reduce((s, f) => s + Number(f.totalDue), 0) || 0

  return (
    <>
      <section className="mb-8 overflow-hidden rounded-2xl bg-slate-900 px-6 py-10 text-white sm:px-10 sm:py-14">
        <p className="mb-2 text-sm font-semibold uppercase tracking-widest text-amber-300">Rwanda · Traffic fines</p>
        <h1 className="max-w-2xl text-3xl font-bold sm:text-4xl">Check, pay and appeal traffic fines online</h1>
        <p className="mt-3 max-w-xl text-sm text-slate-300 sm:text-base">
          Enter a plate number to see outstanding fines. Drivers can log in to pay with Mobile Money or appeal a ticket.
        </p>
        <form onSubmit={search} className="mt-6 flex max-w-lg flex-col gap-3 sm:flex-row">
          <input
            className={`${inputClass} text-base uppercase tracking-widest`}
            placeholder="RAB 123 A"
            value={plate}
            onChange={(e) => setPlate(e.target.value)}
            aria-label="Plate number"
            required
          />
          <Button variant="accent" type="submit" disabled={busy} className="sm:w-40">{busy ? 'Checking…' : 'Check fines'}</Button>
        </form>
      </section>

      <Alert>{error}</Alert>

      {results && (
        <Card className="mb-8">
          <div className="mb-4 flex flex-col justify-between gap-2 sm:flex-row sm:items-center">
            <h2 className="text-lg font-bold">Outstanding fines for {plate.toUpperCase().replace(/\s+/g, '')}</h2>
            {results.length > 0 && <p className="text-sm">Total due: <strong className="text-red-600">{rwf(total)}</strong></p>}
          </div>
          {results.length === 0 ? (
            <EmptyState>No outstanding fines for this plate. Drive safe!</EmptyState>
          ) : (
            <ul className="divide-y divide-slate-100">
              {results.map((f) => (
                <li key={f.referenceNumber} className="flex flex-col gap-2 py-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="font-semibold">{f.violationName} <span className="text-xs font-normal text-slate-400">{f.referenceNumber}</span></p>
                    <p className="text-sm text-slate-500">{fmtDateTime(f.occurredAt)} · {f.location}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <AppealBadge status={f.appealStatus} />
                    <FineBadge status={f.fineStatus} />
                    <div className="text-right">
                      <p className="font-bold">{rwf(f.totalDue)}</p>
                      <p className="text-xs text-slate-500">Due {fmtDate(f.dueDate)}</p>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          )}
          {results.length > 0 && (
            <p className="mt-4 text-sm text-slate-600">
              Is this your vehicle? <Link to="/login" className="font-semibold text-amber-700 hover:underline">Log in</Link> or{' '}
              <Link to="/register" className="font-semibold text-amber-700 hover:underline">register</Link> to pay or appeal.
            </p>
          )}
        </Card>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        {[
          ['1. Officer records', 'Officers record the offence at the roadside with GPS and evidence. A fine is issued instantly.'],
          ['2. You get notified', 'An SMS and email arrive with the reference, the amount and the due date (30 days).'],
          ['3. Pay or appeal', 'Pay with Mobile Money, or appeal within 14 days. Late fines get a 10% penalty.'],
        ].map(([t, d]) => (
          <Card key={t}><h3 className="font-bold text-slate-900">{t}</h3><p className="mt-1 text-sm text-slate-600">{d}</p></Card>
        ))}
      </div>
    </>
  )
}
