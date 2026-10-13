import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { teacher } from '@/features/api'
import { Skeleton } from '@/components/Skeleton'
import { EmptyState } from '@/components/EmptyState'
import { Button } from '@/components/Button'
import { Banner } from '@/components/Banner'
import { Stat } from '@/components/Stat'
import { statusPill } from '@/components/Pill'
import { fmtDate } from '@/lib/format'

export default function TeacherDashboardPage() {
  const stats = useQuery({ queryKey: ['teacher-stats'], queryFn: teacher.stats })
  const list = useQuery({ queryKey: ['teacher-quizzes'], queryFn: teacher.list })
  return (
    <>
      <div className="ph">
        <div>
          <h1>Bảng điều khiển giáo viên</h1>
          <p className="muted">Quản lý bài kiểm tra và xem kết quả học sinh.</p>
        </div>
        <Link to="/teacher/quizzes/new"><Button variant="primary">+ Tạo bài kiểm tra</Button></Link>
      </div>
      {stats.isLoading || list.isLoading ? (
        <div className="stats"><Skeleton h={68} /><Skeleton h={68} /><Skeleton h={68} /></div>
      ) : stats.isError || list.isError ? (
        <Banner tone="bad" role="alert">Không tải được dữ liệu. <button className="link" onClick={() => { stats.refetch(); list.refetch() }}>Thử lại</button></Banner>
      ) : (
        <>
          <div className="stats">
            <Stat label="Đã xuất bản" value={stats.data?.publishedCount ?? 0} />
            <Stat label="Bản nháp" value={stats.data?.draftCount ?? 0} />
            <Stat label="Lượt làm bài" value={stats.data?.attemptCount ?? 0} />
          </div>
          <div style={{ height: 24 }} />
          <h2>Bài kiểm tra của bạn</h2>
          {list.data && list.data.length === 0 ? (
            <EmptyState title="Chưa có bài nào" desc="Bấm nút phía trên để tạo bài đầu tiên." action={<Link to="/teacher/quizzes/new" className="btn primary">+ Tạo bài kiểm tra</Link>} />
          ) : (
            <div className="tbl-wrap" style={{ marginTop: 12 }}>
              <table>
                <thead><tr><th>Bài</th><th>Môn</th><th className="num">Số câu</th><th className="num">Lượt làm</th><th className="num">Điểm TB</th><th>Hạn</th><th>Trạng thái</th><th></th></tr></thead>
                <tbody>
                  {(list.data ?? []).map((q) => (
                    <tr key={q.id}>
                      <td><Link to={`/teacher/quizzes/${q.id}`} className="link">{q.title}</Link></td>
                      <td>{q.subject}</td>
                      <td className="num mono">{q.questionCount}</td>
                      <td className="num mono">{q.attemptCount}</td>
                      <td className="num mono">{q.avgPercent == null ? '—' : `${q.avgPercent}%`}</td>
                      <td className="mono small">{fmtDate(q.closesAt)}</td>
                      <td>{statusPill(q.status)}</td>
                      <td><Link to={`/teacher/quizzes/${q.id}/results`} className="link">Kết quả</Link></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </>
  )
}
