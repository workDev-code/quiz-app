import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '@/lib/auth'
import { Button } from './Button'
import { ThemeSwitch } from './ThemeSwitch'

export function TopBar() {
  const user = useAuth((s) => s.user)
  const clear = useAuth((s) => s.clear)
  const nav = useNavigate()
  if (!user) return null
  const isTeacher = user.role === 'TEACHER'
  return (
    <header className="topbar">
      <button className="brand" onClick={() => nav('/')} aria-label="Trang chủ">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <circle cx="12" cy="12" r="5" fill="currentColor" />
          <g stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <line x1="12" y1="2" x2="12" y2="5" />
            <line x1="12" y1="19" x2="12" y2="22" />
            <line x1="2" y1="12" x2="5" y2="12" />
            <line x1="19" y1="12" x2="22" y2="12" />
            <line x1="4.5" y1="4.5" x2="6.5" y2="6.5" />
            <line x1="17.5" y1="17.5" x2="19.5" y2="19.5" />
            <line x1="4.5" y1="19.5" x2="6.5" y2="17.5" />
            <line x1="17.5" y1="6.5" x2="19.5" y2="4.5" />
          </g>
        </svg>
        Ánh Dương Quiz
      </button>
      <nav className="nav" aria-label="Chính">
        {isTeacher ? (
          <>
            <NavLink to="/teacher">Bảng điều khiển</NavLink>
            <NavLink to="/teacher/question-bank">Ngân hàng câu hỏi</NavLink>
          </>
        ) : (
          <>
            <NavLink to="/" end>Trang chính</NavLink>
            <NavLink to="/quizzes">Bài kiểm tra</NavLink>
            <NavLink to="/history">Lịch sử</NavLink>
          </>
        )}
      </nav>
      <div className="who">
        <ThemeSwitch />
        <span className="rolepill">{isTeacher ? 'Giáo viên' : 'Học sinh'}</span>
        <span>{user.name}</span>
        <Button size="sm" variant="ghost" className="who-btn" onClick={() => { clear(); nav('/login') }}>Đăng xuất</Button>
      </div>
    </header>
  )
}

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="shell">
      <div className="app">
        <TopBar />
        <main className="main">{children}</main>
      </div>
    </div>
  )
}
