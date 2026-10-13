import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { student } from '@/features/api'
import { Skeleton } from '@/components/Skeleton'
import { Banner } from '@/components/Banner'
import { Button } from '@/components/Button'
import { Pill, resultPill } from '@/components/Pill'
import { fmtDateTime } from '@/lib/format'

export default function AttemptDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data, isLoading, isError, refetch } = useQuery({ queryKey: ['attempt', id], queryFn: () => student.attempt(Number(id)) })
  if (isLoading) return <Skeleton h={400} />
  if (isError || !data) return <Banner tone="bad" role="alert">Không tải được bài làm. <button className="link" onClick={() => refetch()}>Thử lại</button></Banner>

  return (
    <>
      <Link to="/history" className="link">← Quay lại lịch sử</Link>
      <div className="ph">
        <div>
          <h1>Kết quả: {data.quizTitle}</h1>
          <p className="muted small">Nộp lúc {fmtDateTime(data.submittedAt)} • Lần {data.seq}</p>
        </div>
      </div>
      <Banner tone={data.passed ? 'good' : 'bad'} role="status">
        Bạn {data.passed ? 'đã đạt' : 'chưa đạt'} bài kiểm tra này.
      </Banner>
      <div className="card" style={{ marginTop: 12 }}>
        <div className="score">
          <div className="big mono">{data.score}<span className="muted" style={{ fontSize: '.5em' }}>/{data.total}</span></div>
          <div>
            <div className="mono muted">{data.percent}%</div>
            {resultPill(data.passed)}
          </div>
          <div className="muted small">Điểm đạt {data.passPct}% • Lần {data.seq}</div>
          <div style={{ marginLeft: 'auto' }} className="row">
            {data.canRetry ? <Link to={`/quizzes/${data.quizId}`}><Button variant="primary">Làm lại</Button></Link> : null}
            <Link to="/history"><Button>Lịch sử</Button></Link>
            <Link to="/quizzes"><Button variant="ghost">Bài khác</Button></Link>
          </div>
        </div>
      </div>
      <div style={{ height: 16 }} />
      <h2>Xem lại bài làm</h2>
      <div className="review" style={{ marginTop: 12 }}>
        {data.items.map((it, i) => (
          <div className="card rv" key={it.questionId}>
            <h3>Câu {i + 1}. {it.text}</h3>
            <div className="row" style={{ gap: 8 }}>
              {!it.answered ? <Pill tone="warn">Chưa trả lời</Pill>
                : it.correct ? <Pill tone="good">Đúng</Pill>
                : <Pill tone="bad">Sai</Pill>}
            </div>
            <div className="opts" style={{ marginTop: 8 }}>
              {it.options.map((o) => {
                const isCorrect = o.correct
                const isChosen = o.chosen
                let cls = 'choice'
                if (isCorrect) cls += ' ok'
                else if (isChosen) cls += ' no'
                return (
                  <div key={o.id} className={cls}>
                    <span>{o.text}</span>
                    <span className="row" style={{ gap: 6 }}>
                      {isCorrect ? <Pill tone="good">Đáp án đúng</Pill> : null}
                      {isChosen ? <Pill tone="bad">Bạn chọn</Pill> : null}
                    </span>
                  </div>
                )
              })}
            </div>
            {it.explanation ? <div className="expl">{it.explanation}</div> : null}
          </div>
        ))}
      </div>
    </>
  )
}
