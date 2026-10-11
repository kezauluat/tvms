import { useEffect, useState } from 'react'
import api, { errorMessage } from '../api/client'
import { fmtDate, fmtDateTime, rwf } from '../utils/format'
import { Alert, AppealBadge, FineBadge, Modal, Spinner } from './ui'

const LABELS = {
  recordedSpeedKmh: 'Recorded speed (km/h)',
  speedLimitKmh: 'Speed limit (km/h)',
  alcoholLevel: 'Alcohol level (g/L)',
  device: 'Device',
}

function Row({ label, children }) {
  return (
    <div className="flex justify-between gap-4 border-b border-slate-100 py-2 text-sm last:border-0">
      <span className="text-slate-500">{label}</span>
      <span className="text-right font-medium text-slate-800">{children}</span>
    </div>
  )
}

/** Full record of one violation, including the roadside evidence stored in MongoDB. */
export default function ViolationDetailModal({ id, onClose, children }) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let alive = true
    api.get(`/violations/${id}`)
      .then((r) => alive && setData(r.data))
      .catch((e) => alive && setError(errorMessage(e)))
    return () => { alive = false }
  }, [id])

  const v = data?.violation
  const ev = data?.evidence

  return (
    <Modal title={v ? `Violation ${v.referenceNumber}` : 'Violation'} onClose={onClose} wide>
      <Alert>{error}</Alert>
      {!v && !error && <Spinner />}
      {v && (
        <div className="grid gap-5 md:grid-cols-2">
          <div>
            <div className="mb-2 flex flex-wrap gap-2"><FineBadge status={v.fineStatus} /><AppealBadge status={v.appealStatus} /></div>
            <Row label="Offence">{v.violationName}</Row>
            <Row label="Vehicle">{v.plateNumber} · {v.vehicleDescription}</Row>
            <Row label="Driver">{v.driverName} ({v.driverLicenseNumber})</Row>
            <Row label="When">{fmtDateTime(v.occurredAt)}</Row>
            <Row label="Where">{v.location}</Row>
            <Row label="Officer">{v.officerName} · {v.officerBadge}</Row>
            <Row label="Demerit points">{v.demeritPoints}</Row>
          </div>
          <div>
            <h3 className="mb-1 text-sm font-semibold text-slate-700">Fine</h3>
            <Row label="Amount">{rwf(v.fineAmount)}</Row>
            {Number(v.penaltyAmount) > 0 && <Row label="Late penalty">{rwf(v.penaltyAmount)}</Row>}
            <Row label="Total">{rwf(v.totalDue)}</Row>
            <Row label="Due date">{fmtDate(v.dueDate)}</Row>
            {v.paymentReference && <Row label="Paid">{fmtDateTime(v.paidAt)} · {v.paymentReference}</Row>}
            {v.appealReason && (
              <div className="mt-3 rounded-lg bg-sky-50 p-3 text-sm">
                <p className="font-semibold text-sky-800">Appeal</p>
                <p className="mt-1 text-sky-900">{v.appealReason}</p>
                {v.appealDecisionNote && <p className="mt-2 text-sky-800"><strong>Decision:</strong> {v.appealDecisionNote}</p>}
              </div>
            )}
          </div>
          <div className="md:col-span-2">
            <h3 className="mb-1 text-sm font-semibold text-slate-700">Roadside evidence</h3>
            {!ev ? (
              <p className="text-sm text-slate-500">No extra evidence was recorded.</p>
            ) : (
              <div className="rounded-lg bg-slate-50 p-3">
                {ev.notes && <p className="mb-2 text-sm text-slate-700">{ev.notes}</p>}
                {Object.entries(ev.details || {}).map(([k, val]) => <Row key={k} label={LABELS[k] || k}>{val}</Row>)}
                {ev.latitude != null && (
                  <Row label="GPS">
                    <a className="text-amber-700 underline" target="_blank" rel="noreferrer"
                       href={`https://www.openstreetmap.org/?mlat=${ev.latitude}&mlon=${ev.longitude}#map=17/${ev.latitude}/${ev.longitude}`}>
                      {ev.latitude.toFixed(5)}, {ev.longitude.toFixed(5)}
                    </a>
                  </Row>
                )}
                {ev.photoUrls?.length > 0 && (
                  <div className="mt-2 flex flex-wrap gap-2">
                    {ev.photoUrls.map((u) => <a key={u} href={u} target="_blank" rel="noreferrer" className="text-sm text-amber-700 underline">Photo</a>)}
                  </div>
                )}
              </div>
            )}
          </div>
          {children && <div className="md:col-span-2">{children(v)}</div>}
        </div>
      )}
    </Modal>
  )
}
