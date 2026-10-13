import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'

export function Modal({ open, onClose, title, children, footer, busy = false }: {
  open: boolean
  onClose: () => void
  title?: string
  children: ReactNode
  footer?: ReactNode
  busy?: boolean
}) {
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape' && !busy) onClose() }
    document.addEventListener('keydown', onKey)
    const previouslyFocused = document.activeElement as HTMLElement | null
    ref.current?.focus()
    return () => {
      document.removeEventListener('keydown', onKey)
      previouslyFocused?.focus()
    }
  }, [open, onClose, busy])

  if (!open) return null
  return (
    <div className="modal-back" onClick={(e) => { if (e.target === e.currentTarget && !busy) onClose() }}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title} tabIndex={-1} ref={ref}>
        {title ? <h2>{title}</h2> : null}
        {children}
        {footer ? <div className="row end">{footer}</div> : null}
      </div>
    </div>
  )
}
