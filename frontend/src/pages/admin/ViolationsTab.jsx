import { useCallback, useEffect, useState } from 'react'
import api, { errorMessage } from '../../api/client'
import ViolationDetailModal from '../../components/ViolationDetailModal'
import { Alert, AppealBadge, Button, Card, EmptyState, FineBadge, Textarea, inputClass } from '../../components/ui'
import { fmtDateTime, rwf } from '../../utils/format'

function AppealDecision({ violation, onDone }) {
  const [note, setNote] = useState('')
  const [error, setError] = useState('')
  const decide = async (accepted) => {
    try {
      await api.patch(`/violations/${violation.id}/appeal-decision`, { accepted, note })
      onDone(accepted ? 'Appeal accepted: violation dismissed, fine cancelled.' : 'Appeal rejected: the fine stands.')
    } catch (e) {
      setError(errorMessage(e))
    }
  }
  if (violation.appealStatus !== 'PENDING') return null
  return (
    <div className="rounded-lg border border-sky-200 p-3">
      <h3 className="mb-2 text-sm font-bold">Decide this appeal</h3>
      <Alert>{error}</Alert>
      <Textarea label="Decision note (sent to the driver)" value={note} onChange={(e) => setNote(e.target.value)} />
      <div className="mt-3 flex gap-2">
        <Button variant="success" disabled={!note.trim()} onClick={() => decide(true)}>Accept appeal</Button>
        <Button variant="danger" disabled={!note.trim()} onClick={() => decide(false)}>Reject appeal</Button>
      </div>
    </div>
  )
}

export default function ViolationsTab() {
  const [plate, setPlate] = useState('')
  const [appealStatus, setAppealStatus] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [detailId, setDetailId] = useState(null)

  const load = useCallback(async () => {
    try {
      const { data } = await api.get('/violations', { params: { plate: plate || undefined, appealStatus: appealStatus || undefined, page, size: 10 } })
      setData(data)
    } catch (e) {
      setError(errorMessage(e))
    }
  }, [plate, appealStatus, page])

  useEffect(() => {
    const t = setTimeout(load, 300)
    return () => clearTimeout(t)
  }, [load])

  return (
    <Card>
      <Alert type="success" onClose={() => setMessage('')}>{message}</Alert>
      <Alert onClose={() => setError('')}>{error}</Alert>
      <div className="mb-4 grid gap-3 sm:grid-cols-3">
        <input className={`${inputClass} sm:col-span-2`} placeholder="Filter by plate" value={plate} onChange={(e) => { setPlate(e.target.value); setPage(0) }} />
        <select className={inputClass} value={appealStatus} onChange={(e) => { setAppealStatus(e.target.value); setPage(0) }}>
          <option value="">All violations</option>
          <option value="PENDING">Pending appeals</option>
          <option value="ACCEPTED">Accepted appeals</option>
          <option value="REJECTED">Rejected appeals</option>
        </select>
      </div>
      {!data ? null : data.content.length === 0 ? <EmptyState>No violations match.</EmptyState> : (
        <>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead className="border-b border-slate-200 text-xs uppercase text-slate-500">
                <tr><th className="py-2 pr-3">Reference</th><th className="pr-3">Plate</th><th className="pr-3">Offence</th><th className="pr-3">Driver</th><th className="pr-3">Date</th><th className="pr-3">Fine</th><th>Status</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.content.map((v) => (
                  <tr key={v.id} className="cursor-pointer hover:bg-slate-50" onClick={() => setDetailId(v.id)}>
                    <td className="py-2 pr-3 font-mono text-xs">{v.referenceNumber}</td>
                    <td className="pr-3 font-semibold">{v.plateNumber}</td>
                    <td className="pr-3">{v.violationName}</td>
                    <td className="pr-3">{v.driverName}</td>
                    <td className="pr-3 text-slate-500">{fmtDateTime(v.occurredAt)}</td>
                    <td className="pr-3">{rwf(v.totalDue)}</td>
                    <td className="space-x-1"><FineBadge status={v.fineStatus} /><AppealBadge status={v.appealStatus} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="mt-3 flex items-center justify-between text-sm">
            <Button variant="ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
            <span className="text-slate-500">Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} total</span>
            <Button variant="ghost" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</Button>
          </div>
        </>
      )}
      {detailId && (
        <ViolationDetailModal id={detailId} onClose={() => setDetailId(null)}>
          {(v) => <AppealDecision violation={v} onDone={(m) => { setDetailId(null); setMessage(m); load() }} />}
        </ViolationDetailModal>
      )}
    </Card>
  )
}
