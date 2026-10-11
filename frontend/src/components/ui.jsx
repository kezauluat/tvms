import { useEffect } from 'react'

export const inputClass =
  'w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 placeholder:text-slate-400 focus:border-amber-500 focus:outline-none focus:ring-2 focus:ring-amber-200 disabled:bg-slate-100'

export function Button({ variant = 'primary', className = '', ...props }) {
  const styles = {
    primary: 'bg-slate-900 text-white hover:bg-slate-800',
    accent: 'bg-amber-400 text-slate-900 hover:bg-amber-300',
    secondary: 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50',
    danger: 'bg-red-600 text-white hover:bg-red-700',
    success: 'bg-emerald-600 text-white hover:bg-emerald-700',
    ghost: 'text-slate-700 hover:bg-slate-100',
  }
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50 ${styles[variant]} ${className}`}
      {...props}
    />
  )
}

export function Field({ label, children, hint }) {
  return (
    <label className="block text-sm">
      <span className="mb-1 block font-medium text-slate-700">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-xs text-slate-500">{hint}</span>}
    </label>
  )
}

export function Input({ label, hint, ...props }) {
  return (
    <Field label={label} hint={hint}>
      <input className={inputClass} {...props} />
    </Field>
  )
}

export function Select({ label, hint, children, ...props }) {
  return (
    <Field label={label} hint={hint}>
      <select className={inputClass} {...props}>{children}</select>
    </Field>
  )
}

export function Textarea({ label, hint, ...props }) {
  return (
    <Field label={label} hint={hint}>
      <textarea className={inputClass} rows={3} {...props} />
    </Field>
  )
}

export function Card({ children, className = '' }) {
  return <div className={`rounded-xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5 ${className}`}>{children}</div>
}

export function Alert({ type = 'error', children, onClose }) {
  if (!children) return null
  const styles = {
    error: 'border-red-200 bg-red-50 text-red-700',
    success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
    info: 'border-sky-200 bg-sky-50 text-sky-800',
    warning: 'border-amber-200 bg-amber-50 text-amber-800',
  }
  return (
    <div role="alert" className={`mb-4 flex items-start justify-between gap-3 rounded-lg border px-4 py-3 text-sm ${styles[type]}`}>
      <span>{children}</span>
      {onClose && <button onClick={onClose} className="font-bold opacity-60 hover:opacity-100" aria-label="Close">×</button>}
    </div>
  )
}

const FINE_STYLES = {
  UNPAID: 'bg-amber-100 text-amber-800',
  OVERDUE: 'bg-red-100 text-red-700',
  PAID: 'bg-emerald-100 text-emerald-700',
  CANCELLED: 'bg-slate-200 text-slate-600',
}

export function FineBadge({ status }) {
  if (!status) return null
  return <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${FINE_STYLES[status] || 'bg-slate-100'}`}>{status}</span>
}

const APPEAL_STYLES = {
  PENDING: 'bg-sky-100 text-sky-700',
  ACCEPTED: 'bg-emerald-100 text-emerald-700',
  REJECTED: 'bg-red-100 text-red-700',
}

export function AppealBadge({ status }) {
  if (!status || status === 'NONE') return null
  return <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${APPEAL_STYLES[status]}`}>Appeal {status.toLowerCase()}</span>
}

export function Spinner() {
  return (
    <div className="flex justify-center py-10">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-amber-200 border-t-amber-500" />
    </div>
  )
}

export function PageTitle({ title, subtitle, children }) {
  return (
    <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
      </div>
      {children}
    </div>
  )
}

export function StatCard({ label, value, tone = 'slate', sub }) {
  const tones = {
    slate: 'text-slate-900',
    amber: 'text-amber-600',
    red: 'text-red-600',
    green: 'text-emerald-600',
    sky: 'text-sky-600',
  }
  return (
    <Card>
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${tones[tone]}`}>{value}</p>
      {sub && <p className="mt-1 text-xs text-slate-500">{sub}</p>}
    </Card>
  )
}

export function EmptyState({ children }) {
  return <p className="rounded-lg border border-dashed border-slate-300 px-4 py-8 text-center text-sm text-slate-500">{children}</p>
}

/** Accessible modal: closes with Escape or the backdrop; full-screen sheet on phones. */
export function Modal({ title, onClose, children, wide = false }) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/50 sm:items-center sm:p-4" onClick={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={`max-h-[92vh] w-full overflow-y-auto rounded-t-2xl bg-white p-5 shadow-xl sm:rounded-2xl ${wide ? 'sm:max-w-2xl' : 'sm:max-w-md'}`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-start justify-between gap-4">
          <h2 className="text-lg font-bold text-slate-900">{title}</h2>
          <button onClick={onClose} className="text-2xl leading-none text-slate-400 hover:text-slate-700" aria-label="Close">×</button>
        </div>
        {children}
      </div>
    </div>
  )
}
