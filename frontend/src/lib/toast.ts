type ToastKind = 'good' | 'bad' | 'info'
let id = 0
export function toast(message: string, kind: ToastKind = 'info') {
  const root = document.getElementById('toast-root')
  if (!root) return
  const el = document.createElement('div')
  el.className = `toast ${kind === 'info' ? '' : kind}`
  el.textContent = message
  root.appendChild(el)
  setTimeout(() => el.remove(), 4000)
}
