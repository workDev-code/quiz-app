import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { student } from '@/features/api'
import { Skeleton } from '@/components/Skeleton'
import { EmptyState } from '@/components/EmptyState'
import { Button } from '@/components/Button'
import { Pill, openPill, attemptsPill } from '@/components/Pill'
import { fmtDate } from '@/lib/format'
import clsx from 'clsx'

type Filter = 'all' | 'open' | 'closed'

export default function QuizListPage() {
  const [filter, setFilter] = useState<Filter>('all')
  const { data, isLoading, isError, refetch } = useQuery({ queryKey: ['quizzes'], queryFn: student.quizzes })
  const filtered = (data ?? []).filter((q) => filter === 'all' ? true : filter === 'open' ? q.available : !q.available)

  return (
    <>
      <div className="ph">
        <h1>Bài kiểm tra</h1>
        <div className="chips" role="tablist">
          {(['all', 'open', 'closed'] as const).map((f) => (
            <button key={f} className="chip" aria-pressed={filter === f} onClick={() => setFilter(f)}>
              {f === 'all' ? 'Tất cả' : f === 'open' ? 'Có thể làm' : 'Đã đóng'}
            </button>
          ))}
        </div>
      </div>
      {isLoading ? (
        <div className="grid"><Skeleton h={180} /><Skeleton h={180} /><Skeleton h={180} /></div>
      ) : isError ? (
        <EmptyState title="Không tải được danh sách" desc="Vui lòng thử lại." action={<Button onClick={() => refetch()}>Thử lại</Button>} />
      ) : filtered.length === 0 ? (
        <EmptyState title="Chưa có bài nào" desc="Hãy quay lại sau." />
      ) : (
        <div className="grid">
          {filtered.map((q) => (
            <Link key={q.id} to={`/quizzes/${q.id}`} className="card qcard" style={{ textDecoration: 'none', color: 'inherit' }}>
              <div className="row">
                <Pill tone="info">{q.subject}</Pill>
                {openPill(q.available)}
              </div>
              <h3>{q.title}</h3>
              <p className={clsx('clamp small')}>{q.description || 'Không có mô tả.'}</p>
              <dl className="meta">
                <div><dt>Số câu</dt><dd>{q.questionCount}</dd></div>
                <div><dt>Thời gian</dt><dd>{q.durationMin} phút</dd></div>
                <div><dt>Hạn</dt><dd>{fmtDate(q.closesAt)}</dd></div>
              </dl>
              <div className="row between">
                <span className="muted small">Đã làm {q.attemptsUsed}/{q.maxAttempts} lần</span>
                {attemptsPill(q.attemptsLeft)}
                <span className="link">Xem chi tiết</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </>
  )
}
