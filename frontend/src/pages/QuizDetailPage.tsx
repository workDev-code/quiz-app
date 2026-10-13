import { useQuery } from '@tanstack/react-query'
import { useNavigate, useParams, Link } from 'react-router-dom'
import { student } from '@/features/api'
import { Skeleton } from '@/components/Skeleton'
import { Button } from '@/components/Button'
import { Banner } from '@/components/Banner'
import { Pill, openPill, attemptsPill } from '@/components/Pill'
import { fmtDate } from '@/lib/format'

export default function QuizDetailPage() {
  const { id } = useParams<{ id: string }>()
  const quizId = Number(id)
  const nav = useNavigate()
  const { data, isLoading, isError, refetch } = useQuery({ queryKey: ['quiz', quizId], queryFn: () => student.quiz(quizId) })

  if (isLoading) {
    return <div className="ph"><Skeleton h={32} w={300} /></div>
  }
  if (isError || !data) {
    return <Banner tone="bad" role="alert">Không tải được bài kiểm tra. <button className="link" onClick={() => refetch()}>Thử lại</button></Banner>
  }
  const q = data.summary
  const start = () => {
    if (data.canStart) nav(`/quizzes/${quizId}/take`)
  }
  return (
    <>
      <Link to="/quizzes" className="link">← Quay lại danh sách</Link>
      <div className="ph">
        <div>
          <h1>{q.title}</h1>
          <div className="row" style={{ marginTop: 8 }}>
            <Pill tone="info">{q.subject}</Pill>
            {openPill(q.available)}
            {attemptsPill(q.attemptsLeft)}
          </div>
        </div>
      </div>
      <div className="split">
        <div>
          <div className="card">
            <h3>Mô tả</h3>
            <p className="muted" style={{ marginTop: 8 }}>{q.description || 'Không có mô tả.'}</p>
          </div>
          <div style={{ height: 16 }} />
          <div className="card">
            <h3>Hướng dẫn làm bài</h3>
            <ol className="rules" style={{ marginTop: 8 }}>
              <li>Đọc câu hỏi, chọn một đáp án đúng nhất.</li>
              <li>Bấm phím "Câu tiếp" hoặc bấm vào số câu bên phải để chuyển nhanh.</li>
              <li>Bộ đếm giờ sẽ tự động nộp khi về 0. Bạn có thể nộp sớm bất cứ lúc nào.</li>
              <li>Đáp án đúng chỉ hiện sau khi bạn nộp bài.</li>
              <li>Đã dùng hết số lượt sẽ không thể làm lại.</li>
            </ol>
          </div>
          <div style={{ height: 16 }} />
          <div className="card">
            <h3>Các lần làm trước</h3>
            <p className="muted small" style={{ marginTop: 8 }}>Đã làm {q.attemptsUsed} lần. {q.bestPercent != null ? `Điểm cao nhất: ${q.bestPercent}%` : ''}</p>
            <div style={{ marginTop: 8 }}>
              <Link to="/history" className="link">Xem lịch sử chi tiết</Link>
            </div>
          </div>
        </div>
        <aside className="card" style={{ position: 'sticky', top: 16 }}>
          <dl className="facts-dl">
            <div><dt>Số câu</dt><dd>{q.questionCount}</dd></div>
            <div><dt>Thời gian</dt><dd>{q.durationMin} phút</dd></div>
            <div><dt>Điểm đạt</dt><dd>{q.passPct}%</dd></div>
            <div><dt>Hạn làm bài</dt><dd>{fmtDate(q.closesAt)}</dd></div>
            <div><dt>Lượt làm</dt><dd>{q.attemptsUsed}/{q.maxAttempts}</dd></div>
          </dl>
          {data.unavailableReason ? (
            <>
              <Banner tone="warn" role="alert">{data.unavailableReason}</Banner>
              <Button disabled style={{ marginTop: 8 }}>Bắt đầu làm bài</Button>
            </>
          ) : (
            <Button variant="primary" onClick={start} style={{ marginTop: 12 }}>Bắt đầu làm bài</Button>
          )}
        </aside>
      </div>
    </>
  )
}
