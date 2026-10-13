import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { student } from '@/features/api'
import { Skeleton } from '@/components/Skeleton'
import { EmptyState } from '@/components/EmptyState'
import { Button } from '@/components/Button'
import { Stat } from '@/components/Stat'
import { Pill, openPill, attemptsPill } from '@/components/Pill'
import { fmtDateTime } from '@/lib/format'
import { resultPill } from '@/components/Pill'

export default function StudentDashboardPage() {
  const { data, isLoading, isError, refetch } = useQuery({ queryKey: ['dashboard'], queryFn: student.dashboard })
  const me = useAuthUser()
  return (
    <>
      <div className="ph">
        <div>
          <h1>Xin chào, {me?.name?.split(' ').slice(-1)[0] || me?.name || 'bạn'}</h1>
          <p className="muted">Bắt đầu làm bài hoặc xem lại kết quả gần đây.</p>
        </div>
      </div>
      {isLoading ? (
        <div className="stats">
          <Skeleton h={68} /><Skeleton h={68} /><Skeleton h={68} />
        </div>
      ) : isError ? (
        <EmptyState title="Không tải được dữ liệu" desc="Hãy kiểm tra mạng rồi thử lại." action={<Button onClick={() => refetch()}>Thử lại</Button>} />
      ) : !data ? null : (
        <>
          <div className="stats">
            <Stat label="Bài có thể làm" value={data.availableCount} />
            <Stat label="Lượt đã làm" value={data.totalAttempts} />
            <Stat label="Điểm trung bình" value={data.avgPercent ?? '—'} suffix={data.avgPercent == null ? '' : '%'} />
          </div>
          <div style={{ height: 24 }} />
          <div className="sec-h"><h2>Bài có thể làm ngay</h2><Link to="/quizzes" className="link">Xem tất cả</Link></div>
          {data.quizzes.length === 0 ? (
            <EmptyState title="Chưa có bài nào đang mở" desc="Hãy quay lại sau khi giáo viên xuất bản bài mới." />
          ) : (
            <div className="grid">
              {data.quizzes.map((q) => (
                <Link to={`/quizzes/${q.id}`} key={q.id} className="card qcard" style={{ textDecoration: 'none', color: 'inherit' }}>
                  <div className="row">
                    <Pill tone="info">{q.subject}</Pill>
                    {openPill(q.available)}
                  </div>
                  <h3>{q.title}</h3>
                  <p className="clamp small">{q.description || 'Không có mô tả.'}</p>
                  <dl className="meta">
                    <div><dt>Số câu</dt><dd>{q.questionCount}</dd></div>
                    <div><dt>Thời gian</dt><dd>{q.durationMin} phút</dd></div>
                    <div><dt>Điểm đạt</dt><dd>{q.passPct}%</dd></div>
                  </dl>
                  <div className="row between">
                    {attemptsPill(q.attemptsLeft)}
                    <span className="link">Xem chi tiết</span>
                  </div>
                </Link>
              ))}
            </div>
          )}
          <div style={{ height: 24 }} />
          <div className="sec-h"><h2>Kết quả gần đây</h2><Link to="/history" className="link">Lịch sử đầy đủ</Link></div>
          {data.recent.length === 0 ? (
            <EmptyState title="Bạn chưa nộp bài nào" desc="Hãy bắt đầu làm một bài để thấy kết quả ở đây." />
          ) : (
            <div className="list">
              {data.recent.map((r) => (
                <div className="item" key={r.id}>
                  <div className="t">
                    <strong>{r.quizTitle}</strong>
                    <span className="muted small">Lần {r.seq} • {fmtDateTime(r.submittedAt)}</span>
                  </div>
                  <div className="row" style={{ gap: 16 }}>
                    <span className="mono">{r.score}/{r.total} • {r.percent}%</span>
                    {resultPill(r.passed)}
                    <Link to={`/attempts/${r.id}`} className="link">Xem</Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </>
  )
}

import { useAuth } from '@/lib/auth'
function useAuthUser() { return useAuth((s) => s.user) }
