import { forwardRef, type InputHTMLAttributes, type ReactNode, type TextareaHTMLAttributes, type SelectHTMLAttributes } from 'react'
import clsx from 'clsx'

export function Field({ label, error, hint, children }: { label: string; error?: string; hint?: ReactNode; children: ReactNode }) {
  return (
    <div className="field">
      <label>{label}</label>
      {children}
      {error ? <div className="err" role="alert">{error}</div> : hint ? <div className="hint">{hint}</div> : null}
    </div>
  )
}

type TextInputProps = InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean }

export const TextInput = forwardRef<HTMLInputElement, TextInputProps>(function TextInput(
  { invalid, className, ...rest },
  ref
) {
  return <input ref={ref} className={clsx('input', className)} aria-invalid={invalid ? 'true' : undefined} {...rest} />
})

type TextAreaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & { invalid?: boolean }

export const TextArea = forwardRef<HTMLTextAreaElement, TextAreaProps>(function TextArea(
  { invalid, className, ...rest },
  ref
) {
  return <textarea ref={ref} className={clsx('input', className)} aria-invalid={invalid ? 'true' : undefined} {...rest} />
})

type SelectProps = SelectHTMLAttributes<HTMLSelectElement> & { invalid?: boolean }

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { invalid, className, children, ...rest },
  ref
) {
  return <select ref={ref} className={clsx('input', className)} aria-invalid={invalid ? 'true' : undefined} {...rest}>{children}</select>
})
