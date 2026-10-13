import { Button } from './Button'

export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return null
  return (
    <div className="pager">
      <span className="muted small">Trang {page + 1}/{totalPages}</span>
      <Button size="sm" disabled={page === 0} onClick={() => onChange(page - 1)}>Trang trước</Button>
      <Button size="sm" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Trang sau</Button>
    </div>
  )
}
