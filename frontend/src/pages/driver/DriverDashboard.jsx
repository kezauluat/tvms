import { useCallback, useEffect, useState } from 'react'
import api, { errorMessage } from '../../api/client'
import ViolationDetailModal from '../../components/ViolationDetailModal'
import { Alert, AppealBadge, Button, Card, EmptyState, FineBadge, Input, Modal, PageTitle, Spinner, StatCard, Textarea } from '../../components/ui'
import { daysSince, fmtDate, fmtDateTime, PHONE_HINT, rwf } from '../../utils/format'

const payable = (v) => (v.fineStatus === 'UNPAID' || v.fineStatus === 'OVERDUE') && v.appealStatus !== 'PENDING'
const appealable = (v) => v.appealStatus === 'NONE' && v.status === 'ACTIVE'
  && (v.fineStatus === 'UNPAID' || v.fineStatus === 'OVERDUE') && daysSince(v.occurredAt) <= 14

function PayModal({ violation, defaultPhone, onClose, onPaid }) {
  const [phone, setPhone] = useState(defaultPhone || '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [receipt, setReceipt] = useState(null)

  const pay = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const { data } = await api.post(`/fines/${violation.fineId}/pay`, { phoneNumber: phone })
      setReceipt(data)
      onPaid()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal title={receipt ? 'Payment received' : 'Pay with Mobile Money'} onClose={onClose}>
      {receipt ? (
        <div className="space-y-2 text-sm">
          <p className="rounded-lg bg-emerald-50 p-3 text-emerald-800">Paid {rwf(receipt.amountPaid)} for {receipt.violationReference}.</p>
          <p>Payment reference: <strong>{receipt.paymentReference}</strong></p>
          <p className="text-slate-500">A confirmation was sent by SMS and email.</p>
          <Button className="mt-2 w-full" onClick={onClose}>Done</Button>
        </div>
      ) : (
        <form onSubmit={pay} className="space-y-4">
          <div className="rounded-lg bg-slate-50 p-3 text-sm">
            <p className="font-semibold">{violation.violationName} · {violation.plateNumber}</p>
            <p className="text-slate-500">{violation.referenceNumber}</p>
            <p className="mt-2 text-lg font-bold">{rwf(violation.totalDue)}</p>
          </div>
          <Alert>{error}</Alert>
          <Input label="MoMo phone number" required value={phone} onChange={(e) => setPhone(e.target.value)} hint={PHONE_HINT} />
          <p className="text-xs text-slate-500">Demo: payment is simulated, no money is charged.</p>
          <Button type="submit" variant="success" className="w-full" disabled={busy}>{busy ? 'Processing…' : `Pay ${rwf(violation.totalDue)}`}</Button>
        </form>
      )}
    </Modal>
  )
}

function AppealModal({ violation, onClose, onDone }) {
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await api.post(`/violations/${violation.id}/appeal`, { reason })
      onDone('Appeal submitted. You will be notified of the decision.')
    } catch (err) {
      setError(errorMessage(err))
      setBusy(false)
    }
  }

  return (
    <Modal title={`Appeal ${violation.referenceNumber}`} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        <p className="text-sm text-slate-600">Explain why this fine is wrong. While the appeal is reviewed, the fine cannot become overdue.</p>
        <Alert>{error}</Alert>
        <Textarea label="Reason" required minLength={20} maxLength={1000} rows={5} value={reason} onChange={(e) => setReason(e.target.value)} hint={`${reason.length}/1000 (at least 20 characters)`} />
        <Button type="submit" className="w-full" disabled={busy || reason.trim().length < 20}>{busy ? 'Submitting…' : 'Submit appeal'}</Button>
      </form>
    </Modal>
  )
}

export default function DriverDashboard() {
  const [summary, setSummary] = useState(null)
  const [vehicles, setVehicles] = useState([])
  const [violations, setViolations] = useState(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [paying, setPaying] = useState(null)
  const [appealing, setAppealing] = useState(null)
  const [detailId, setDetailId] = useState(null)

  const load = useCallback(async () => {
    try {
      const [s, veh, vio] = await Promise.all([api.get('/me/summary'), api.get('/me/vehicles'), api.get('/me/violations')])
      setSummary(s.data)
      setVehicles(veh.data)
      setViolations(vio.data)
    } catch (e) {
      setError(errorMessage(e))
      setViolations([])
    }
  }, [])

  useEffect(() => { load() }, [load])

  if (!violations) return <Spinner />
  const pct = summary ? Math.min(100, Math.round((summary.demeritPoints / summary.demeritPointLimit) * 100)) : 0

  return (
    <>
      <PageTitle title="My fines" subtitle={summary ? `${summary.fullName} · License ${summary.licenseNumber}` : ''} />
      <Alert type="success" onClose={() => setMessage('')}>{message}</Alert>
      <Alert onClose={() => setError('')}>{error}</Alert>

      {summary && (
        <div className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
          <StatCard label="Outstanding" value={rwf(summary.outstandingAmount)} tone={summary.unpaidFineCount ? 'red' : 'green'} />
          <StatCard label="Unpaid fines" value={summary.unpaidFineCount} tone={summary.unpaidFineCount ? 'amber' : 'green'} />
          <Card>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Demerit points (12 months)</p>
            <p className={`mt-1 text-2xl font-bold ${summary.licenseAtRisk ? 'text-red-600' : 'text-slate-900'}`}>{summary.demeritPoints} / {summary.demeritPointLimit}</p>
            <div className="mt-2 h-2 rounded-full bg-slate-100"><div className={`h-2 rounded-full ${pct >= 75 ? 'bg-red-500' : 'bg-amber-400'}`} style={{ width: `${pct}%` }} /></div>
            {summary.licenseAtRisk && <p className="mt-1 text-xs font-semibold text-red-600">License at risk of suspension</p>}
          </Card>
          <StatCard label="My vehicles" value={summary.vehicleCount} sub={vehicles.map((v) => v.plateNumber).join(' · ') || 'None registered'} />
        </div>
      )}

      <h2 className="mb-3 text-lg font-bold">Violations</h2>
      {violations.length === 0 ? (
        <EmptyState>No violations on record. Keep it up!</EmptyState>
      ) : (
        <div className="space-y-3">
          {violations.map((v) => (
            <Card key={v.id}>
              <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div>
                  <div className="mb-1 flex flex-wrap items-center gap-2">
                    <FineBadge status={v.fineStatus} />
                    <AppealBadge status={v.appealStatus} />
                    <span className="text-xs text-slate-400">{v.referenceNumber}</span>
                  </div>
                  <p className="font-semibold">{v.violationName} · {v.plateNumber}</p>
                  <p className="text-sm text-slate-500">{fmtDateTime(v.occurredAt)} · {v.location}</p>
                  {v.appealDecisionNote && <p className="mt-1 text-sm text-slate-600">Decision: {v.appealDecisionNote}</p>}
                </div>
                <div className="flex flex-wrap items-center gap-2 md:justify-end">
                  <div className="mr-2 text-right">
                    <p className="font-bold">{rwf(v.totalDue)}</p>
                    <p className="text-xs text-slate-500">{v.fineStatus === 'PAID' ? `Paid ${fmtDate(v.paidAt?.slice(0, 10))}` : `Due ${fmtDate(v.dueDate)}`}</p>
                  </div>
                  {payable(v) && <Button variant="success" onClick={() => setPaying(v)}>Pay</Button>}
                  {appealable(v) && <Button variant="secondary" onClick={() => setAppealing(v)}>Appeal</Button>}
                  <Button variant="ghost" onClick={() => setDetailId(v.id)}>Details</Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {paying && <PayModal violation={paying} defaultPhone={summary?.phoneNumber} onClose={() => setPaying(null)} onPaid={load} />}
      {appealing && <AppealModal violation={appealing} onClose={() => setAppealing(null)} onDone={(m) => { setAppealing(null); setMessage(m); load() }} />}
      {detailId && <ViolationDetailModal id={detailId} onClose={() => setDetailId(null)} />}
    </>
  )
}
