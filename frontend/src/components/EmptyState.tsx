import type { ReactNode } from 'react'

export function EmptyState({ title, desc, action }: { title: string; desc: string; action?: ReactNode }) {
  return (
    <div className="empty">
      <h3>{title}</h3>
      <p>{desc}</p>
      {action}
    </div>
  )
}
