import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button } from '@/components/Button'
import { Field, TextInput } from '@/components/Field'
import { Banner } from '@/components/Banner'
import { registerSchema, type RegisterInput } from '@/features/validation'
import { auth } from '@/features/api'
import { useAuth } from '@/lib/auth'
import { ApiCallError } from '@/lib/api'
import { toast } from '@/lib/toast'

export default function RegisterPage() {
  const { control, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<RegisterInput>({
    resolver: zodResolver(registerSchema),
    mode: 'onChange',
    defaultValues: { name: '', email: '', password: '' }
  })
  const [err, setErr] = useState<string | null>(null)
  const setAuth = useAuth((s) => s.setAuth)
  const nav = useNavigate()

  const onSubmit = async (data: RegisterInput) => {
    setErr(null)
    try {
      const r = await auth.register(data.name.trim(), data.email.trim(), data.password)
      setAuth(r)
      reset()
      toast('Tạo tài khoản thành công', 'good')
      nav('/')
    } catch (e) {
      if (e instanceof ApiCallError) setErr(e.message)
      else setErr('Đăng ký thất bại.')
    }
  }

  return (
    <div className="auth">
      <aside className="auth-side">
        <div className="brand">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle cx="12" cy="12" r="5" fill="currentColor" />
            <g stroke="currentColor" strokeWidth="2" strokeLinecap="round">
              <line x1="12" y1="2" x2="12" y2="5" /><line x1="12" y1="19" x2="12" y2="22" />
              <line x1="2" y1="12" x2="5" y2="12" /><line x1="19" y1="12" x2="22" y2="12" />
            </g>
          </svg>
          Ánh Dương Quiz
        </div>
        <h2>Tạo tài khoản học sinh để bắt đầu làm bài.</h2>
        <ul className="facts">
          <li><b>Tài khoản mới là học sinh</b><span>Muốn làm giáo viên, tài khoản phải được tạo qua seed.</span></li>
          <li><b>Mật khẩu mạnh</b><span>Ít nhất 8 ký tự, gồm cả chữ và số.</span></li>
          <li><b>Email là duy nhất</b><span>Mỗi email chỉ đăng ký được một tài khoản.</span></li>
        </ul>
      </aside>
      <section className="auth-main">
        <h1>Đăng ký</h1>
        {err ? <Banner tone="bad" role="alert">{err}</Banner> : null}
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <Field label="Họ tên" error={errors.name?.message}>
            <Controller
              name="name"
              control={control}
              render={({ field }) => (
                <TextInput
                  autoComplete="name"
                  name={field.name}
                  value={field.value}
                  onChange={field.onChange}
                  onBlur={field.onBlur}
                  ref={field.ref}
                  invalid={!!errors.name}
                />
              )}
            />
          </Field>
          <Field label="Email" error={errors.email?.message}>
            <Controller
              name="email"
              control={control}
              render={({ field }) => (
                <TextInput
                  type="email"
                  autoComplete="email"
                  name={field.name}
                  value={field.value}
                  onChange={field.onChange}
                  onBlur={field.onBlur}
                  ref={field.ref}
                  invalid={!!errors.email}
                />
              )}
            />
          </Field>
          <Field label="Mật khẩu" hint="Ít nhất 8 ký tự, gồm cả chữ và số" error={errors.password?.message}>
            <Controller
              name="password"
              control={control}
              render={({ field }) => (
                <TextInput
                  type="password"
                  autoComplete="new-password"
                  name={field.name}
                  value={field.value}
                  onChange={field.onChange}
                  onBlur={field.onBlur}
                  ref={field.ref}
                  invalid={!!errors.password}
                />
              )}
            />
          </Field>
          <Button variant="primary" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Đang tạo...' : 'Tạo tài khoản'}
          </Button>
        </form>
        <p className="muted small">Đã có tài khoản? <Link to="/login" className="link">Đăng nhập</Link></p>
      </section>
    </div>
  )
}
