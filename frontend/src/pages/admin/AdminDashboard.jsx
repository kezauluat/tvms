import { useState } from 'react'
import { PageTitle } from '../../components/ui'
import Overview from './Overview'
import ViolationsTab from './ViolationsTab'
import TypesTab from './TypesTab'
import PeopleTab from './PeopleTab'
import UsersTab from './UsersTab'

const TABS = [
  ['overview', 'Overview', Overview],
  ['violations', 'Violations & appeals', ViolationsTab],
  ['types', 'Offence types', TypesTab],
  ['people', 'Drivers, vehicles & officers', PeopleTab],
  ['users', 'User accounts', UsersTab],
]

export default function AdminDashboard() {
  const [tab, setTab] = useState('overview')
  const Current = TABS.find(([key]) => key === tab)[2]
  return (
    <>
      <PageTitle title="Administration" subtitle="Monitor fines, decide appeals and manage the registry" />
      <div className="-mx-4 mb-6 overflow-x-auto px-4">
        <div className="flex w-max gap-2">
          {TABS.map(([key, label]) => (
            <button key={key} onClick={() => setTab(key)}
                    className={`whitespace-nowrap rounded-lg px-4 py-2 text-sm font-semibold ${tab === key ? 'bg-slate-900 text-white' : 'border border-slate-300 bg-white text-slate-600 hover:bg-slate-50'}`}>
              {label}
            </button>
          ))}
        </div>
      </div>
      <Current />
    </>
  )
}
