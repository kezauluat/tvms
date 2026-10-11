import { useCallback, useEffect, useState } from 'react'
import api, { errorMessage } from '../../api/client'
import { Alert, Button, Card, Spinner, StatCard } from '../../components/ui'
import { rwf } from '../../utils/format'

export default function Overview() {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const load = useCallback(async () => {
    try {
      setStats((await api.get('/admin/stats')).data)
    } catch (e) {
      setError(errorMessage(e))
    }
  }, [])
  useEffect(() => { load() }, [load])

  const runChecks = async () => {
    try {
      const { data } = await api.post('/admin/jobs/run-fine-checks')
      setMessage(`Fine checks done: ${data.markedOverdue} marked overdue, ${data.remindersQueued} reminders queued.`)
      load()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  if (!stats) return error ? <Alert>{error}</Alert> : <Spinner />
  const max = Math.max(1, ...stats.violationsByType.map((t) => t.count))

  return (
    <>
      <Alert type="success" onClose={() => setMessage('')}>{message}</Alert>
      <Alert onClose={() => setError('')}>{error}</Alert>
      <div className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Violations" value={stats.totalViolations} />
        <StatCard label="Pending appeals" value={stats.pendingAppeals} tone={stats.pendingAppeals ? 'sky' : 'slate'} />
        <StatCard label="Unpaid / overdue" value={`${stats.unpaidFines} / ${stats.overdueFines}`} tone="amber" />
        <StatCard label="Outstanding" value={rwf(stats.outstandingAmount)} tone="red" />
        <StatCard label="Collected" value={rwf(stats.collectedAmount)} tone="green" />
        <StatCard label="Drivers" value={stats.drivers} />
        <StatCard label="Vehicles" value={stats.vehicles} />
        <StatCard label="Officers" value={stats.officers} />
      </div>
      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <h2 className="mb-4 font-bold">Active violations by offence</h2>
          {stats.violationsByType.length === 0 ? <p className="text-sm text-slate-500">No data yet.</p> : (
            <ul className="space-y-3">
              {stats.violationsByType.map((t) => (
                <li key={t.name}>
                  <div className="mb-1 flex justify-between text-sm"><span>{t.name}</span><span className="font-semibold">{t.count}</span></div>
                  <div className="h-2.5 rounded-full bg-slate-100"><div className="h-2.5 rounded-full bg-amber-400" style={{ width: `${(t.count / max) * 100}%` }} /></div>
                </li>
              ))}
            </ul>
          )}
        </Card>
        <Card>
          <h2 className="mb-2 font-bold">Nightly jobs</h2>
          <p className="mb-4 text-sm text-slate-600">Every night unpaid fines past their due date become overdue (+10%), and drivers get a reminder 3 days before the due date. Run them now for a demo.</p>
          <Button variant="accent" onClick={runChecks}>Run fine checks now</Button>
        </Card>
      </div>
    </>
  )
}
