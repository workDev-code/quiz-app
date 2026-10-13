import { useEffect, useState } from 'react'
import { getTheme, setTheme, initTheme } from '@/lib/theme'
import clsx from 'clsx'

export function ThemeSwitch() {
  const [t, setT] = useState(getTheme())
  useEffect(() => { initTheme() }, [])
  return (
    <div className="seg" role="group" aria-label="Giao diện">
      {(['system', 'light', 'dark'] as const).map((v) => (
        <button
          key={v}
          className={clsx('btn sm')}
          aria-pressed={t === v}
          onClick={() => { setTheme(v); setT(v) }}
        >
          {v === 'system' ? 'Theo máy' : v === 'light' ? 'Sáng' : 'Tối'}
        </button>
      ))}
    </div>
  )
}
