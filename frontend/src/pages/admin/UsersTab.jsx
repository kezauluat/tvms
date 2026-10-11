import { useCallback, useEffect, useState } from 'react'
import api, { errorMessage } from '../../api/client'
import { Alert, Button, Card } from '../../components/ui'

export default function UsersTab() {
  const [users, setUsers] = useState([])
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try { setUsers((await api.get('/admin/users')).data) } catch (e) { setError(errorMessage(e)) }
  }, [])
  useEffect(() => { load() }, [load])

  const toggle = async (u) => {
    try {
      await api.patch(`/admin/users/${u.id}/enabled`, null, { params: { value: !u.enabled } })
      load()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Card>
      <Alert onClose={() => setError('')}>{error}</Alert>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[600px] text-left text-sm">
          <thead className="border-b border-slate-200 text-xs uppercase text-slate-500">
            <tr><th className="py-2 pr-3">Name</th><th className="pr-3">Email</th><th className="pr-3">Role</th><th className="pr-3">Login</th><th className="pr-3">Status</th><th /></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {users.map((u) => (
              <tr key={u.id}>
                <td className="py-2 pr-3 font-medium">{u.fullName}</td>
                <td className="pr-3 text-slate-600">{u.email}</td>
                <td className="pr-3">{u.role}</td>
                <td className="pr-3">{u.provider}</td>
                <td className="pr-3"><span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${u.enabled ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>{u.enabled ? 'Active' : 'Disabled'}</span></td>
                <td className="text-right">{u.role !== 'ADMIN' && <Button variant="ghost" className="px-2 py-1" onClick={() => toggle(u)}>{u.enabled ? 'Disable' : 'Enable'}</Button>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  )
}
