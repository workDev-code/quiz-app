export function Skeleton({ w = '100%', h = 16, className = '' }: { w?: number | string; h?: number | string; className?: string }) {
  return <div className={`sk ${className}`} style={{ width: w, height: typeof h === 'number' ? `${h}px` : h }} aria-busy="true" />
}
