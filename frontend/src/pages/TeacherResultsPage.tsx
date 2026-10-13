import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { teacher } from '@/features/api'
import { Skeleton } from '@/components/Skeleton'
import { Banner } from '@/components/Banner'
import { Stat } from '@/components/Stat'
import { Pill, resultPill } from '@/components/Pill'
import { fmtDateTime } from '@/lib/format'

export default function TeacherResultsPage() {
  const { id } = useParams<{ id: string }>()
  const quizId = Number(id)
  const detail = useQuery({ queryKey: ['admin-quiz', quizId], queryFn: () => teacher.get(quizId) })
  const list = useQuery({ queryKey: ['admin-quiz-attempts', quizId], queryFn: () => teacher.attempts(quizId) })
  if (detail.isLoading || list.isLoading) return <Skeleton h={400} />
  if (detail.isError || list.isError || !detail.data || !list.data) return <Banner tone="bad" role="alert">Không tải được kết quả.</Banner>
  const items = list.data.items as Array<{ id: number; studentName: string; seq: number; score: number; total: number; percent: number; passed: boolean; submittedAt: string }>
  const total = items.length
  const passed = items.filter((i) => i.passed).length
  const avg = total === 0 ? null : Math.round(items.reduce((s, i) => s + i.percent, 0) / total)
  return (
    <>
      <Link to="/teacher" className="link">← Quay lại</Link>
      <div className="ph">
        <div>
          <h1>Kết quả: {detail.data.title}</h1>
          <p className="muted small">{total} lượt làm • {detail.data.questions.length} câu hỏi • điểm đạt {detail.data.passPct}%</p>
        </div>
      </div>
      <div className="stats">
        <Stat label="Lượt làm" value={total} />
        <Stat label="Đạt" value={passed} />
        <Stat label="Điểm TB" value={avg ?? '—'} suffix={avg == null ? '' : '%'} />
      </div>
      <div style={{ height: 16 }} />
      {items.length === 0 ? <Banner tone="info" role="status">Chưa có học sinh nào nộp bài này.</Banner> : (
        <div className="tbl-wrap">
          <table>
            <thead><tr><th>Học sinh</th><th>Lần</th><th>Nộp lúc</th><th className="num">Điểm</th><th>Kết quả</th></tr></thead>
            <tbody>
              {items.map((it) => (
                <tr key={it.id}>
                  <td>{it.studentName}</td>
                  <td className="mono">{it.seq}</td>
                  <td className="mono small">{fmtDateTime(it.submittedAt)}</td>
                  <td className="num mono">{it.score}/{it.total} • {it.percent}%</td>
                  <td>{resultPill(it.passed)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
