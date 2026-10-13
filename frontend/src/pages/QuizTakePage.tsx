import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { student } from '@/features/api'
import { Skeleton } from '@/components/Skeleton'
import { Banner } from '@/components/Banner'
import { Button } from '@/components/Button'
import { Modal } from '@/components/Modal'
import { letter, mmss, genClientKey } from '@/lib/format'
import { ApiCallError } from '@/lib/api'
import { toast } from '@/lib/toast'
import type { PublicQuestion } from '@/lib/types'

export default function QuizTakePage() {
  const { id } = useParams<{ id: string }>()
  const quizId = Number(id)
  const nav = useNavigate()
  const { data, isLoading, isError, refetch } = useQuery({ queryKey: ['quiz', quizId], queryFn: () => student.quiz(quizId), staleTime: 0 })
  const [answers, setAnswers] = useState<Record<number, number | null>>({})
  const [idx, setIdx] = useState(0)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [clientKey, setClientKey] = useState(() => sessionStorage.getItem(`take:${quizId}:ck`) || '')
  const [startedAt, setStartedAt] = useState<string>(() => sessionStorage.getItem(`take:${quizId}:start`) || new Date().toISOString())
  const [now, setNow] = useState<number>(Date.now())
  const [submitErr, setSubmitErr] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false)
  const dirty = useRef(false)

  useEffect(() => {
    if (!clientKey) {
      const k = genClientKey()
      setClientKey(k)
      sessionStorage.setItem(`take:${quizId}:ck`, k)
    }
    sessionStorage.setItem(`take:${quizId}:start`, startedAt)
  }, [clientKey, quizId, startedAt])

  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), 500)
    return () => clearInterval(t)
  }, [])

  useEffect(() => {
    if (data?.summary) {
      document.title = `${data.summary.title} • Ánh Dương Quiz`
    }
    return () => { document.title = 'Ánh Dương Quiz' }
  }, [data])

  // Confirm when leaving while taking
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (Object.keys(answers).length === 0) return
      e.preventDefault()
      e.returnValue = ''
    }
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [answers])

  const mutation = useMutation({
    mutationFn: (payload: { clientKey: string; startedAt: string; answers: { questionId: number; optionId: number | null }[] }) =>
      student.submitAttempt(quizId, payload),
    onSuccess: (res) => {
      sessionStorage.removeItem(`take:${quizId}:ck`)
      sessionStorage.removeItem(`take:${quizId}:start`)
      nav(`/attempts/${res.id}`)
    }
  })

  const qs = data?.questions ?? []
  const totalQs = qs.length
  const answered = Object.values(answers).filter((v) => v != null).length

  const deadlineMs = useMemo(() => {
    if (!data?.summary) return 0
    return new Date(startedAt).getTime() + data.summary.durationMin * 60 * 1000
  }, [data, startedAt])

  const remaining = Math.max(0, Math.floor((deadlineMs - now) / 1000))
  const lowTime = remaining > 0 && remaining <= 60

  // Auto-submit when timer hits 0
  useEffect(() => {
    if (!data) return
    if (remaining === 0 && !mutation.isPending && !mutation.isSuccess) {
      doSubmit()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [remaining, data])

  function doSubmit() {
    if (!data) return
    setBusy(true)
    setSubmitErr(null)
    const payload = {
      clientKey,
      startedAt,
      answers: qs.map((q) => ({ questionId: q.id, optionId: answers[q.id] ?? null }))
    }
    mutation.mutate(payload, {
      onError: (e) => {
        if (e instanceof ApiCallError) setSubmitErr(e.message)
        else setSubmitErr('Nộp bài thất bại. Hãy thử lại.')
        setBusy(false)
      },
      onSettled: () => setBusy(false)
    })
  }

  if (isLoading) return <Skeleton h={400} />
  if (isError || !data) {
    return <Banner tone="bad" role="alert">Không tải được bài kiểm tra. <button className="link" onClick={() => refetch()}>Thử lại</button></Banner>
  }
  if (!data.canStart) {
    return <Banner tone="warn" role="alert">{data.unavailableReason}</Banner>
  }
  const q: PublicQuestion | undefined = qs[idx]
  if (!q) return null
  const unansweredNums: number[] = []
  qs.forEach((x, i) => { if (answers[x.id] == null) unansweredNums.push(i + 1) })

  return (
    <div className="take">
      <div className="topbar" style={{ borderRadius: 14, margin: '-28px -24px 16px' }}>
        <div className="brand">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle cx="12" cy="12" r="5" fill="currentColor" />
            <g stroke="currentColor" strokeWidth="2" strokeLinecap="round">
              <line x1="12" y1="2" x2="12" y2="5" /><line x1="12" y1="19" x2="12" y2="22" />
              <line x1="2" y1="12" x2="5" y2="12" /><line x1="19" y1="12" x2="22" y2="12" />
            </g>
          </svg>
          Ánh Dương Quiz
        </div>
        <div className="who">
          <span className="rolepill">Học sinh</span>
          <Button size="sm" variant="ghost" onClick={() => { if (Object.keys(answers).length > 0) setShowLeaveConfirm(true); else nav('/quizzes') }}>Thoát, không nộp</Button>
        </div>
      </div>
      <div className="card take-bar">
        <div>
          <strong>{data.summary.title}</strong>
          <div className="muted small">Đã trả lời {answered}/{totalQs}</div>
        </div>
        <div className="row" aria-live="off">
          <div role="timer" className={'timer' + (lowTime ? ' low' : '')} aria-label="Thời gian còn lại">{mmss(remaining)}</div>
        </div>
      </div>
      <div className="take-body">
        <div className="card qpanel">
          <h2>Câu {idx + 1}. {q.text}</h2>
          <div className="opts" role="radiogroup" aria-label="Đáp án">
            {q.options.map((o, i) => (
              <label key={o.id} className={'opt' + (answers[q.id] === o.id ? ' on' : '')}>
                <input
                  type="radio"
                  name={`q-${q.id}`}
                  checked={answers[q.id] === o.id}
                  onChange={() => { dirty.current = true; setAnswers((a) => ({ ...a, [q.id]: o.id })) }}
                />
                <span className="k">{letter(i)}.</span>
                <span>{o.text}</span>
              </label>
            ))}
          </div>
          <div className="row between" style={{ marginTop: 8 }}>
            <Button disabled={idx === 0} onClick={() => setIdx((i) => Math.max(0, i - 1))}>← Câu trước</Button>
            <Button variant="primary" disabled={idx === totalQs - 1} onClick={() => setIdx((i) => Math.min(totalQs - 1, i + 1))}>Câu tiếp →</Button>
          </div>
        </div>
        <aside className="qside">
          <div className="card">
            <h3>Danh sách câu hỏi</h3>
            <div className="qnav" style={{ marginTop: 10 }}>
              {qs.map((x, i) => (
                <button
                  key={x.id}
                  className={'qn' + (answers[x.id] != null ? ' done' : '') + (i === idx ? ' cur' : '')}
                  onClick={() => setIdx(i)}
                  aria-label={`Câu ${i + 1}, ${answers[x.id] != null ? 'đã trả lời' : 'chưa trả lời'}`}
                >{i + 1}</button>
              ))}
            </div>
            <div style={{ height: 12 }} />
            <div className="progress" aria-label="Tiến độ"><i style={{ width: `${(answered / Math.max(1, totalQs)) * 100}%` }} /></div>
          </div>
          <Button variant="primary" onClick={() => setConfirmOpen(true)}>Nộp bài</Button>
        </aside>
      </div>

      <Modal
        open={confirmOpen}
        onClose={() => !busy && setConfirmOpen(false)}
        title="Nộp bài?"
        busy={busy}
        footer={
          <>
            <Button disabled={busy} onClick={() => setConfirmOpen(false)}>Quay lại làm tiếp</Button>
            <Button variant="primary" disabled={busy} onClick={() => { doSubmit(); setConfirmOpen(false) }}>{busy ? 'Đang nộp...' : 'Nộp bài'}</Button>
          </>
        }
      >
        <p>Bạn đã trả lời <strong>{answered}/{totalQs}</strong> câu.</p>
        {unansweredNums.length > 0 ? (
          <Banner tone="warn" role="alert">
            Câu {unansweredNums.join(', ')} sẽ được tính 0 điểm.
          </Banner>
        ) : null}
        {submitErr ? <Banner tone="bad" role="alert">{submitErr}</Banner> : null}
        <Banner tone="info">Nếu mạng chậm, hệ thống vẫn ghi nhận bài làm với cùng mã định danh.</Banner>
      </Modal>

      <Modal
        open={showLeaveConfirm}
        onClose={() => setShowLeaveConfirm(false)}
        title="Thoát mà không nộp?"
        footer={
          <>
            <Button onClick={() => setShowLeaveConfirm(false)}>Ở lại làm tiếp</Button>
            <Button variant="danger" onClick={() => { sessionStorage.removeItem(`take:${quizId}:ck`); sessionStorage.removeItem(`take:${quizId}:start`); nav('/quizzes') }}>Thoát</Button>
          </>
        }
      >
        <p>Các câu trả lời hiện tại sẽ không được lưu. Bạn có chắc muốn thoát?</p>
      </Modal>
    </div>
  )
}
