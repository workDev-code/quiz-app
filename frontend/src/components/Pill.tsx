import type { ReactNode } from 'react'
import clsx from 'clsx'

export function Pill({ tone = 'neutral', children }: { tone?: 'good' | 'warn' | 'bad' | 'info' | 'neutral'; children: ReactNode }) {
  return <span className={clsx('pill', tone !== 'neutral' && tone)}>{children}</span>
}

export function statusPill(s: string) {
  return s === 'PUBLISHED' ? <Pill tone="good">Đã xuất bản</Pill> : <Pill tone="warn">Bản nháp</Pill>
}
export function resultPill(passed: boolean) {
  return passed ? <Pill tone="good">Đạt</Pill> : <Pill tone="bad">Chưa đạt</Pill>
}
export function openPill(open: boolean) {
  return open ? <Pill tone="good">Đang mở</Pill> : <Pill tone="neutral">Đã đóng</Pill>
}
export function attemptsPill(left: number) {
  if (left === 0) return <Pill tone="warn">Hết lượt</Pill>
  return <Pill tone="good">Còn {left} lượt</Pill>
}
