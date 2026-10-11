import { useCallback, useEffect, useState } from 'react'
import api, { errorMessage } from '../../api/client'
import { Alert, Button, Card, Input } from '../../components/ui'
import { rwf } from '../../utils/format'

const empty = { id: null, code: '', name: '', description: '', fineAmount: '', demeritPoints: '' }

export default function TypesTab() {
  const [types, setTypes] = useState([])
  const [form, setForm] = useState(empty)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const load = useCallback(async () => {
    try { setTypes((await api.get('/violation-types/all')).data) } catch (e) { setError(errorMessage(e)) }
  }, [])
  useEffect(() => { load() }, [load])

  const set = (f) => (e) => setForm({ ...form, [f]: e.target.value })

  const save = async (e) => {
    e.preventDefault()
    const body = { code: form.code, name: form.name, description: form.description || null, fineAmount: Number(form.fineAmount), demeritPoints: Number(form.demeritPoints), active: true }
    try {
      if (form.id) await api.put(`/violation-types/${form.id}`, body)
      else await api.post('/violation-types', body)
      setMessage(form.id ? 'Offence type updated. Existing fines keep their original amount.' : 'Offence type created.')
      setForm(empty)
      setError('')
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  const toggle = async (t) => {
    try {
      if (t.active) await api.delete(`/violation-types/${t.id}`)
      else await api.put(`/violation-types/${t.id}`, { ...t, active: true })
      load()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <Card className="lg:col-span-2">
        <h2 className="mb-3 font-bold">Offence catalogue</h2>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[560px] text-left text-sm">
            <thead className="border-b border-slate-200 text-xs uppercase text-slate-500">
              <tr><th className="py-2 pr-3">Code</th><th className="pr-3">Name</th><th className="pr-3">Fine</th><th className="pr-3">Points</th><th /></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {types.map((t) => (
                <tr key={t.id} className={t.active ? '' : 'text-slate-400'}>
                  <td className="py-2 pr-3 font-mono text-xs">{t.code}</td>
                  <td className="pr-3">{t.name}</td>
                  <td className="pr-3">{rwf(t.fineAmount)}</td>
                  <td className="pr-3">{t.demeritPoints}</td>
                  <td className="whitespace-nowrap text-right">
                    <Button variant="ghost" className="px-2 py-1" onClick={() => setForm({ ...t, description: t.description || '' })}>Edit</Button>
                    <Button variant="ghost" className="px-2 py-1" onClick={() => toggle(t)}>{t.active ? 'Deactivate' : 'Activate'}</Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
      <Card>
        <h2 className="mb-3 font-bold">{form.id ? 'Edit offence type' : 'New offence type'}</h2>
        <Alert type="success" onClose={() => setMessage('')}>{message}</Alert>
        <Alert onClose={() => setError('')}>{error}</Alert>
        <form onSubmit={save} className="space-y-3">
          <Input label="Code" required placeholder="SPEEDING" value={form.code} onChange={set('code')} />
          <Input label="Name" required value={form.name} onChange={set('name')} />
          <Input label="Description" value={form.description} onChange={set('description')} />
          <div className="grid grid-cols-2 gap-3">
            <Input label="Fine (RWF)" type="number" min="1000" step="500" required value={form.fineAmount} onChange={set('fineAmount')} />
            <Input label="Demerit points" type="number" min="0" max="12" required value={form.demeritPoints} onChange={set('demeritPoints')} />
          </div>
          <div className="flex gap-2">
            <Button type="submit">{form.id ? 'Save changes' : 'Create'}</Button>
            {form.id && <Button type="button" variant="secondary" onClick={() => setForm(empty)}>Cancel</Button>}
          </div>
        </form>
      </Card>
    </div>
  )
}
