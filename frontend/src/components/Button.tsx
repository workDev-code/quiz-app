import type { ButtonHTMLAttributes, ReactNode } from 'react'
import clsx from 'clsx'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
  size?: 'sm' | 'md'
  children: ReactNode
}

export function Button({ variant = 'secondary', size = 'md', className, children, ...rest }: ButtonProps) {
  return (
    <button
      className={clsx('btn', variant === 'primary' && 'primary', variant === 'danger' && 'danger',
        variant === 'ghost' && 'ghost', size === 'sm' && 'sm', className)}
      {...rest}
    >
      {children}
    </button>
  )
}
