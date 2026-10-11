import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="py-20 text-center">
      <p className="text-5xl font-bold text-amber-500">404</p>
      <p className="mt-2 text-slate-600">This page does not exist.</p>
      <Link to="/" className="mt-4 inline-block text-sm font-semibold text-amber-700 hover:underline">Go home</Link>
    </div>
  )
}
