export const rwf = (n) => (n == null ? '—' : `${Math.round(Number(n)).toLocaleString('en-US')} RWF`)

export const fmtDateTime = (s) =>
  s ? new Date(s).toLocaleString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—'

/** LocalDate strings ("2026-11-10") are formatted without timezone shifts. */
export const fmtDate = (s) => {
  if (!s) return '—'
  const [y, m, d] = s.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
}

/** Value for <input type="datetime-local"> set to the current minute. */
export function nowLocalInput() {
  const d = new Date()
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset())
  return d.toISOString().slice(0, 16)
}

export const daysSince = (s) => (Date.now() - new Date(s).getTime()) / 86400000

/** Empty strings become null so optional backend fields pass validation. */
export const clean = (obj) => Object.fromEntries(Object.entries(obj).map(([k, v]) => [k, v === '' ? null : v]))

export const PHONE_HINT = 'Rwandan number, e.g. 0788123456'
