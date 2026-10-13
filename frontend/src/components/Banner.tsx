import type { ReactNode } from 'react'
import clsx from 'clsx'

export function Banner({ tone = 'info', children, role }: { tone?: 'info' | 'good' | 'warn' | 'bad'; children: ReactNode; role?: 'alert' | 'status' }) {
  return (
    <div
      className={clsx('banner', tone)}
      role={role ?? (tone === 'bad' ? 'alert' : 'status')}
    >
      {children}
    </div>
  )
}
