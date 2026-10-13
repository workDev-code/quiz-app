export const fmtDate = (iso: string | null | undefined) =>
  iso ? new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—'

export const fmtDateTime = (iso: string | null | undefined) =>
  iso ? new Date(iso).toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—'

export const mmss = (s: number) =>
  `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`

export const letter = (i: number) => String.fromCharCode(65 + i)

export const fmtClosesAt = (iso: string | null | undefined) => (iso ? fmtDate(iso) : 'Không giới hạn')

export function genClientKey(): string {
  const a = (crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`).replace(/-/g, '')
  return `ck-${a.slice(0, 24)}`
}
