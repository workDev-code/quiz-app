import { useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import StudentDashboardPage from './pages/StudentDashboardPage'
import QuizListPage from './pages/QuizListPage'
import QuizDetailPage from './pages/QuizDetailPage'
import QuizTakePage from './pages/QuizTakePage'
import AttemptDetailPage from './pages/AttemptDetailPage'
import HistoryPage from './pages/HistoryPage'
import TeacherDashboardPage from './pages/TeacherDashboardPage'
import QuizEditPage from './pages/QuizEditPage'
import QuestionBankPage from './pages/QuestionBankPage'
import TeacherResultsPage from './pages/TeacherResultsPage'
import ForbiddenPage from './pages/ForbiddenPage'
import UiKitPage from './pages/UiKitPage'
import { AppShell } from './components/TopBar'
import { useAuth, bootstrapAuth, type Role } from './lib/auth'
import { initTheme } from './lib/theme'

const qc = new QueryClient({ defaultOptions: { queries: { retry: 0, refetchOnWindowFocus: false, staleTime: 30_000 } } })

function RequireAuth({ roles, children }: { roles?: Role[]; children: React.ReactNode }) {
  const user = useAuth((s) => s.user)
  const loc = useLocation()
  useEffect(() => { initTheme() }, [])
  if (!user) return <Navigate to="/login" state={{ from: loc.pathname }} replace />
  if (roles && !roles.includes(user.role)) return <ForbiddenPage />
  return <AppShell>{children}</AppShell>
}

export default function App() {
  useEffect(() => { bootstrapAuth() }, [])
  return (
    <QueryClientProvider client={qc}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/" element={<RequireAuth roles={['STUDENT']}><StudentDashboardPage /></RequireAuth>} />
          <Route path="/quizzes" element={<RequireAuth roles={['STUDENT']}><QuizListPage /></RequireAuth>} />
          <Route path="/quizzes/:id" element={<RequireAuth roles={['STUDENT']}><QuizDetailPage /></RequireAuth>} />
          <Route path="/quizzes/:id/take" element={<RequireAuth roles={['STUDENT']}><QuizTakePage /></RequireAuth>} />
          <Route path="/attempts/:id" element={<RequireAuth roles={['STUDENT']}><AttemptDetailPage /></RequireAuth>} />
          <Route path="/history" element={<RequireAuth roles={['STUDENT']}><HistoryPage /></RequireAuth>} />
          <Route path="/teacher" element={<RequireAuth roles={['TEACHER']}><TeacherDashboardPage /></RequireAuth>} />
          <Route path="/teacher/question-bank" element={<RequireAuth roles={['TEACHER']}><QuestionBankPage /></RequireAuth>} />
          <Route path="/teacher/quizzes/new" element={<RequireAuth roles={['TEACHER']}><QuizEditPage /></RequireAuth>} />
          <Route path="/teacher/quizzes/:id" element={<RequireAuth roles={['TEACHER']}><QuizEditPage /></RequireAuth>} />
          <Route path="/teacher/quizzes/:id/results" element={<RequireAuth roles={['TEACHER']}><TeacherResultsPage /></RequireAuth>} />
          <Route path="/dev/ui-kit" element={<RequireAuth><UiKitPage /></RequireAuth>} />
          <Route path="/403" element={<AppShell><ForbiddenPage /></AppShell>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
