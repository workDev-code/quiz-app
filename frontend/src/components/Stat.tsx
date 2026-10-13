import type { ReactNode } from 'react'

export function Stat({ label, value, suffix }: { label: string; value: ReactNode; suffix?: ReactNode }) {
  return (
    <div className="stat">
      <div className="v mono">{value}{suffix}</div>
      <div className="l">{label}</div>
    </div>
  )
}
