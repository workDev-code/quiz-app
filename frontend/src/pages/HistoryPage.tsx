import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { student } from '@/features/api'
import { Skeleton } from '@/components/Skeleton'
import { EmptyState } from '@/components/EmptyState'
import { Button } from '@/components/Button'
import { Pagination } from '@/components/Pagination'
import { Pill, resultPill } from '@/components/Pill'
import { fmtDateTime } from '@/lib/format'
import { Banner } from '@/components/Banner'

export default function HistoryPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading, isError, refetch } = useQuery({ queryKey: ['history', page], queryFn: () => student.history(page, 10) })
  if (isLoading) return <Skeleton h={400} />
  if (isError || !data) return <Banner tone="bad" role="alert">Không tải được lịch sử. <button className="link" onClick={() => refetch()}>Thử lại</button></Banner>
  return (
    <>
      <div className="ph">
        <div>
          <h1>Lịch sử làm bài</h1>
          <p className="muted">Tổng {data.totalAttempts} lượt, điểm trung bình {data.avgPercent ?? '—'}%.</p>
        </div>
      </div>
      {data.items.length === 0 ? (
        <EmptyState title="Chưa có bài làm nào" desc="Bắt đầu một bài kiểm tra để thấy lịch sử ở đây." action={<Link to="/quizzes" className="btn primary">Đi đến danh sách</Link>} />
      ) : (
        <div className="tbl-wrap">
          <table>
            <thead><tr><th>Bài</th><th>Lần</th><th>Nộp lúc</th><th className="num">Điểm</th><th>Kết quả</th><th></th></tr></thead>
            <tbody>
              {data.items.map((it) => (
                <tr key={it.id}>
                  <td>{it.quizTitle}</td>
                  <td className="mono">{it.seq}</td>
                  <td className="mono small">{fmtDateTime(it.submittedAt)}</td>
                  <td className="num mono">{it.score}/{it.total} • {it.percent}%</td>
                  <td>{resultPill(it.passed)}</td>
                  <td><Link to={`/attempts/${it.id}`} className="link">Xem</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
    </>
  )
}
