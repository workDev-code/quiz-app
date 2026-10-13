import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button } from '@/components/Button'
import { Field, TextInput } from '@/components/Field'
import { Banner } from '@/components/Banner'
import { loginSchema, type LoginInput } from '@/features/validation'
import { auth } from '@/features/api'
import { useAuth } from '@/lib/auth'
import { ApiCallError } from '@/lib/api'
import { toast } from '@/lib/toast'

export default function LoginPage() {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginInput>({ resolver: zodResolver(loginSchema) })
  const [err, setErr] = useState<string | null>(null)
  const setAuth = useAuth((s) => s.setAuth)
  const nav = useNavigate()
  const loc = useLocation()
  const flash = sessionStorage.getItem('flash')
  if (flash) { sessionStorage.removeItem('flash') }

  const onSubmit = async (data: LoginInput) => {
    setErr(null)
    try {
      const r = await auth.login(data.email.trim(), data.password)
      setAuth(r)
      toast('Đăng nhập thành công', 'good')
      nav(r.user.role === 'TEACHER' ? '/teacher' : '/')
    } catch (e) {
      if (e instanceof ApiCallError) setErr(e.message)
      else setErr('Đăng nhập thất bại, thử lại sau.')
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
              <line x1="4.5" y1="4.5" x2="6.5" y2="6.5" /><line x1="17.5" y1="17.5" x2="19.5" y2="19.5" />
              <line x1="4.5" y1="19.5" x2="6.5" y2="17.5" /><line x1="17.5" y1="6.5" x2="19.5" y2="4.5" />
            </g>
          </svg>
          Ánh Dương Quiz
        </div>
        <h2>Ra đề, chấm điểm và lưu kết quả cho trung tâm nhỏ.</h2>
        <ul className="facts">
          <li><b>Máy chủ chấm điểm</b><span>Điểm được tính phía server, đáp án không lộ cho tới khi nộp.</span></li>
          <li><b>Đáp án đúng chỉ hiện sau khi nộp</b><span>Học sinh chỉ thấy câu hỏi và đáp án; đáp án đúng ẩn.</span></li>
          <li><b>Kết quả được lưu lâu dài</b><span>Bài làm được snapshot, giáo viên có thể xem lại bất cứ lúc nào.</span></li>
        </ul>
      </aside>
      <section className="auth-main">
        <h1>Đăng nhập</h1>
        {flash ? <Banner tone="warn" role="alert">{flash}</Banner> : null}
        {err ? <Banner tone="bad" role="alert">{err}</Banner> : null}
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <Field label="Email" error={errors.email?.message}>
            <TextInput type="email" autoComplete="email" {...register('email')} invalid={!!errors.email} />
          </Field>
          <Field label="Mật khẩu" error={errors.password?.message}>
            <TextInput type="password" autoComplete="current-password" {...register('password')} invalid={!!errors.password} />
          </Field>
          <Button variant="primary" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </Button>
        </form>
        <p className="muted small">Chưa có tài khoản? <Link to="/register" className="link">Đăng ký</Link></p>
        <div className="acct-box small">
          <strong>Tài khoản mẫu (chỉ chạy ở bản dev)</strong>
          <div className="muted">Học sinh: <span className="mono">hocsinh@demo.vn</span></div>
          <div className="muted">Giáo viên: <span className="mono">giaovien@demo.vn</span></div>
          <div className="muted">Mật khẩu: <span className="mono">Demo@1234</span></div>
        </div>
      </section>
    </div>
  )
}
