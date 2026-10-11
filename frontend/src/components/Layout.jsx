import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Layout() {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const links = [
    { to: '/', label: 'Check fines', show: true, end: true },
    { to: '/my', label: 'My fines', show: user?.role === 'DRIVER' },
    { to: '/officer', label: 'Record violation', show: user?.role === 'OFFICER' },
    { to: '/admin', label: 'Admin', show: user?.role === 'ADMIN' },
  ].filter((l) => l.show)

  const linkClass = ({ isActive }) =>
    `block rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'bg-white/10 text-amber-300' : 'text-slate-200 hover:bg-white/10'}`

  const handleLogout = async () => {
    await logout()
    setOpen(false)
    navigate('/login')
  }

  return (
    <div className="flex min-h-screen flex-col bg-slate-50 text-slate-800">
      <header className="sticky top-0 z-30 bg-slate-900 text-white shadow">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <Link to="/" className="flex items-center gap-2 font-bold" onClick={() => setOpen(false)}>
            <span className="grid h-9 w-9 place-items-center rounded-lg bg-amber-400 text-slate-900" aria-hidden="true">
              <svg viewBox="0 0 24 24" className="h-5 w-5" fill="currentColor"><path d="M12 2 1 21h22L12 2Zm0 5.5L18.5 19h-13L12 7.5Zm-1 3.5v4h2v-4h-2Zm0 5v2h2v-2h-2Z" /></svg>
            </span>
            <span className="leading-tight">
              Traffic Fines
              <span className="block text-[11px] font-medium text-slate-400">Violation &amp; Fine Management</span>
            </span>
          </Link>

          <nav className="hidden items-center gap-1 md:flex">
            {links.map((l) => <NavLink key={l.to} to={l.to} end={l.end} className={linkClass}>{l.label}</NavLink>)}
          </nav>

          <div className="hidden items-center gap-3 md:flex">
            {user ? (
              <>
                <span className="text-sm text-slate-300">{user.fullName} <span className="rounded bg-white/10 px-1.5 py-0.5 text-[11px] uppercase">{user.role}</span></span>
                <button onClick={handleLogout} className="rounded-lg border border-white/20 px-3 py-1.5 text-sm hover:bg-white/10">Log out</button>
              </>
            ) : (
              <>
                <Link to="/login" className="text-sm font-medium text-slate-200 hover:text-white">Log in</Link>
                <Link to="/register" className="rounded-lg bg-amber-400 px-3 py-1.5 text-sm font-semibold text-slate-900 hover:bg-amber-300">Register</Link>
              </>
            )}
          </div>

          <button className="rounded-md p-2 hover:bg-white/10 md:hidden" onClick={() => setOpen((o) => !o)} aria-label="Toggle menu" aria-expanded={open}>
            <svg className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              {open ? <path d="M6 18L18 6M6 6l12 12" /> : <path d="M4 6h16M4 12h16M4 18h16" />}
            </svg>
          </button>
        </div>

        {open && (
          <div className="border-t border-white/10 px-4 pb-4 pt-2 md:hidden">
            {links.map((l) => <NavLink key={l.to} to={l.to} end={l.end} className={linkClass} onClick={() => setOpen(false)}>{l.label}</NavLink>)}
            <div className="mt-3 border-t border-white/10 pt-3">
              {user ? (
                <>
                  <p className="px-3 pb-2 text-sm text-slate-400">Signed in as {user.fullName}</p>
                  <button onClick={handleLogout} className="w-full rounded-lg border border-white/20 px-3 py-2 text-sm">Log out</button>
                </>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  <Link to="/login" onClick={() => setOpen(false)} className="rounded-lg border border-white/20 px-3 py-2 text-center text-sm">Log in</Link>
                  <Link to="/register" onClick={() => setOpen(false)} className="rounded-lg bg-amber-400 px-3 py-2 text-center text-sm font-semibold text-slate-900">Register</Link>
                </div>
              )}
            </div>
          </div>
        )}
        <div className="road-stripe" />
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:py-8">
        <Outlet />
      </main>

      <footer className="border-t border-slate-200 bg-white py-4 text-center text-xs text-slate-500">
        Traffic Violation Management System · Web Technology project · AUCA
      </footer>
    </div>
  )
}
