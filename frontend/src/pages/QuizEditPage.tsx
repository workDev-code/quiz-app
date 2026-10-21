import { useEffect, useState } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { bank, teacher } from '@/features/api'
import { Skeleton } from '@/components/Skeleton'
import { Banner } from '@/components/Banner'
import { Button } from '@/components/Button'
import { Field, TextInput, TextArea, Select } from '@/components/Field'
import { Pill, statusPill, difficultyPill } from '@/components/Pill'
import { Modal } from '@/components/Modal'
import { quizSchema, questionSchema, type QuizInput, type QuestionInput } from '@/features/validation'
import { ApiCallError } from '@/lib/api'
import { toast } from '@/lib/toast'
import { fmtDate } from '@/lib/format'
import { letter } from '@/lib/format'
import type { AdminFull, AdminQuestion, AdminOption } from '@/lib/types'

type Tab = 'info' | 'questions' | 'publish'

export default function QuizEditPage() {
  const { id } = useParams<{ id: string }>()
  const quizId = id ? Number(id) : null
  const nav = useNavigate()
  const qc = useQueryClient()
  const isNew = quizId == null
  const [tab, setTab] = useState<Tab>('info')
  const [editingQ, setEditingQ] = useState<AdminQuestion | null>(null)
  const [confirmUnpublish, setConfirmUnpublish] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState<number | null>(null)
  const [pubErrs, setPubErrs] = useState<{ label: string; detail: string }[] | null>(null)
  const [pickOpen, setPickOpen] = useState(false)
  const [confirmUnlink, setConfirmUnlink] = useState<number | null>(null)

  const unlinkMut = useMutation({
    mutationFn: (qid: number) => bank.unlinkFromQuiz(quizId!, qid),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-quiz', quizId] })
      qc.invalidateQueries({ queryKey: ['question-bank'] })
      qc.invalidateQueries({ queryKey: ['teacher-quizzes'] })
      toast('Đã gỡ khỏi bài (câu hỏi vẫn còn trong ngân hàng)', 'good')
    },
    onError: (e) => { if (e instanceof ApiCallError) toast(e.message, 'bad') }
  })

  const detailQ = useQuery({
    queryKey: ['admin-quiz', quizId],
    queryFn: () => teacher.get(quizId!),
    enabled: !isNew
  })

  const { register, handleSubmit, reset, watch, formState: { errors, isDirty, isSubmitting } } = useForm<QuizInput>({
    resolver: zodResolver(quizSchema)
  })
  const watched = watch()

  useEffect(() => {
    if (!isNew && detailQ.data) {
      const d = detailQ.data
      reset({
        title: d.title,
        description: d.description,
        subject: d.subject,
        durationMin: d.durationMin,
        maxAttempts: d.maxAttempts,
        passPct: d.passPct,
        closesAt: d.closesAt ? d.closesAt.slice(0, 10) : null
      })
    }
  }, [detailQ.data, isNew, reset])

  const createMut = useMutation({ mutationFn: (b: QuizInput) => teacher.create(b), onSuccess: (d) => { qc.invalidateQueries({ queryKey: ['teacher-quizzes'] }); qc.invalidateQueries({ queryKey: ['teacher-stats'] }); nav(`/teacher/quizzes/${d.id}`) } })
  const updateMut = useMutation({ mutationFn: (b: QuizInput) => teacher.update(quizId!, b), onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-quiz', quizId] }); qc.invalidateQueries({ queryKey: ['teacher-quizzes'] }) } })
  const publishMut = useMutation({ mutationFn: () => teacher.publish(quizId!), onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-quiz', quizId] }); qc.invalidateQueries({ queryKey: ['teacher-quizzes'] }); setTab('publish') }, onError: (e) => { if (e instanceof ApiCallError && Array.isArray(e.details)) setPubErrs(e.details as { label: string; detail: string }[]) } })
  const unpublishMut = useMutation({ mutationFn: () => teacher.unpublish(quizId!), onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-quiz', quizId] }) })

  function onSave(data: QuizInput) {
    setPubErrs(null)
    if (isNew) createMut.mutate(data)
    else updateMut.mutate(data)
  }

  if (!isNew && detailQ.isLoading) return <Skeleton h={400} />
  if (!isNew && (detailQ.isError || !detailQ.data)) return <Banner tone="bad" role="alert">Không tải được bài kiểm tra. <button className="link" onClick={() => detailQ.refetch()}>Thử lại</button></Banner>
  const d: AdminFull | undefined = detailQ.data

  return (
    <>
      <Link to="/teacher" className="link">← Quay lại</Link>
      <div className="ph">
        <div>
          <h1>{d?.title || 'Bài kiểm tra mới'}</h1>
          <div className="row" style={{ marginTop: 8 }}>
            {d ? statusPill(d.status) : null}
            {d?.locked ? <Pill tone="warn">Bài đã có lượt làm</Pill> : null}
          </div>
        </div>
      </div>
      {d?.locked ? <Banner tone="warn" role="status">Bài này đã có học sinh làm nên bạn không thể xoá câu hỏi, xoá đáp án, hay đổi đáp án đúng. Bạn vẫn có thể sửa nội dung, thêm câu hỏi và thay đổi cài đặt.</Banner> : null}
      <div className="tabs" role="tablist">
        <button className="tab" role="tab" aria-selected={tab === 'info'} onClick={() => setTab('info')}>Thông tin</button>
        <button className="tab" role="tab" aria-selected={tab === 'questions'} onClick={() => d && setTab('questions')} disabled={!d}>Câu hỏi ({d?.questions.length ?? 0})</button>
        <button className="tab" role="tab" aria-selected={tab === 'publish'} onClick={() => d && setTab('publish')} disabled={!d}>Xuất bản</button>
      </div>
      {tab === 'info' && (
        <form className="card" onSubmit={handleSubmit(onSave)}>
          <div className="form-grid">
            <Field label="Tên bài" error={errors.title?.message}>
              <TextInput {...register('title')} invalid={!!errors.title} />
            </Field>
            <Field label="Môn học" error={errors.subject?.message}>
              <Select {...register('subject')} invalid={!!errors.subject}>
                <option value="Lập trình">Lập trình</option>
                <option value="Cơ sở dữ liệu">Cơ sở dữ liệu</option>
                <option value="Tiếng Anh">Tiếng Anh</option>
                <option value="Toán">Toán</option>
                <option value="Khác">Khác</option>
              </Select>
            </Field>
            <Field label="Thời gian (phút)" error={errors.durationMin?.message}>
              <TextInput type="number" min={1} max={240} {...register('durationMin')} invalid={!!errors.durationMin} />
            </Field>
            <Field label="Số lần làm tối đa" error={errors.maxAttempts?.message}>
              <TextInput type="number" min={1} max={20} {...register('maxAttempts')} invalid={!!errors.maxAttempts} />
            </Field>
            <Field label="Điểm đạt (%)" error={errors.passPct?.message}>
              <TextInput type="number" min={0} max={100} {...register('passPct')} invalid={!!errors.passPct} />
            </Field>
            <Field label="Hạn đóng" hint="Bỏ trống nếu không giới hạn" error={errors.closesAt?.message}>
              <TextInput type="date" {...register('closesAt')} invalid={!!errors.closesAt} />
            </Field>
          </div>
          <div style={{ height: 12 }} />
          <Field label="Mô tả" error={errors.description?.message}>
            <TextArea rows={4} {...register('description')} invalid={!!errors.description} />
          </Field>
          <div className="row between" style={{ marginTop: 16 }}>
            <div>{isDirty ? <Pill tone="warn">Chưa lưu</Pill> : <span className="muted small">Mọi thay đổi đã được lưu.</span>}</div>
            <Button variant="primary" type="submit" disabled={isSubmitting}>{isNew ? 'Tạo bài' : 'Lưu thay đổi'}</Button>
          </div>
        </form>
      )}
      {tab === 'questions' && d ? (
        <QuestionEditor
          d={d}
          editingQ={editingQ}
          setEditingQ={setEditingQ}
          onDelete={(qid) => setConfirmDelete(qid)}
          onUnlink={(qid) => setConfirmUnlink(qid)}
          onOpenBank={() => setPickOpen(true)}
        />
      ) : null}
      {tab === 'publish' && d ? (
        <PublishTab d={d} onPublish={() => { setPubErrs(null); publishMut.mutate() }} onUnpublish={() => setConfirmUnpublish(true)}
          publishing={publishMut.isPending} pubErrs={pubErrs} />
      ) : null}
      <Modal open={confirmUnpublish} onClose={() => setConfirmUnpublish(false)} title="Gỡ xuất bản?"
        footer={<>
          <Button onClick={() => setConfirmUnpublish(false)}>Huỷ</Button>
          <Button variant="danger" onClick={() => { unpublishMut.mutate(); setConfirmUnpublish(false) }}>Gỡ xuất bản</Button>
        </>}>
        <p>Bài sẽ chuyển về <strong>Bản nháp</strong>. Học sinh sẽ không thể vào làm bài, các lượt đã nộp vẫn giữ nguyên.</p>
      </Modal>
      <Modal open={confirmUnlink != null} onClose={() => setConfirmUnlink(null)} title="Gỡ câu hỏi khỏi bài này?"
        footer={<>
          <Button onClick={() => setConfirmUnlink(null)}>Huỷ</Button>
          <Button variant="danger" disabled={unlinkMut.isPending} onClick={() => {
            if (confirmUnlink == null) return
            unlinkMut.mutate(confirmUnlink)
            setConfirmUnlink(null)
          }}>Gỡ khỏi bài</Button>
        </>}>
        <p>Câu hỏi sẽ không còn trong bài này nhưng <strong>vẫn được giữ trong ngân hàng câu hỏi</strong>, các bài khác dùng nó cũng không bị ảnh hưởng.</p>
      </Modal>

      <PickFromBankModal open={pickOpen} onClose={() => setPickOpen(false)} quizId={quizId!} />

      <Modal open={confirmDelete != null} onClose={() => setConfirmDelete(null)} title="Xoá câu hỏi này?"
        footer={<>
          <Button onClick={() => setConfirmDelete(null)}>Huỷ</Button>
          <Button variant="danger" onClick={() => {
            if (confirmDelete == null) return
            teacher.deleteQuestion(confirmDelete).then(() => {
              qc.invalidateQueries({ queryKey: ['admin-quiz', quizId] })
              toast('Đã xoá câu hỏi', 'good')
              setConfirmDelete(null)
              setEditingQ(null)
            }).catch((e) => { if (e instanceof ApiCallError) toast(e.message, 'bad'); setConfirmDelete(null) })
          }}>Xoá</Button>
        </>}>
        <p>
          Hành động này <strong>xoá câu hỏi khỏi ngân hàng</strong> — tức là gỡ khỏi <em>tất cả</em> các bài đang dùng nó.
          Nếu chỉ muốn bỏ khỏi bài này thì dùng <strong>Gỡ khỏi bài</strong> ở danh sách câu hỏi.
          Nếu bài đã có lượt làm, máy chủ sẽ từ chối.
        </p>
      </Modal>
    </>
  )
}

function QuestionEditor({ d, editingQ, setEditingQ, onDelete, onUnlink, onOpenBank }: {
  d: AdminFull
  editingQ: AdminQuestion | null
  setEditingQ: (q: AdminQuestion | null) => void
  onDelete: (qid: number) => void
  onUnlink: (qid: number) => void
  onOpenBank: () => void
}) {
  const qc = useQueryClient()
  const quizId = d.id
  const isLocked = d.locked
  return (
    <div className="stack">
      <div className="list">
        {d.questions.length === 0 ? <Banner tone="info" role="status">Bài chưa có câu hỏi nào. Bấm "Thêm câu hỏi" để bắt đầu.</Banner> : null}
        {d.questions.map((q, i) => (
          <div key={q.id} className="item">
            <div className="t">
              <div className="qrow">
                <div>
                  <div className="qno">Câu {i + 1}</div>
                  <div><strong>{q.text.slice(0, 80)}{q.text.length > 80 ? '…' : ''}</strong></div>
                  <div className="muted small">{q.options.length} đáp án</div>
                </div>
                <div className="row">
                  {difficultyPill(q.difficulty)}
                  {q.correctId == null ? <Pill tone="bad">Chưa có đáp án đúng</Pill> : <Pill tone="good">Có đáp án đúng</Pill>}
                  <Button size="sm" onClick={() => setEditingQ(q)}>Sửa</Button>
                  <Button size="sm" variant="danger" disabled={isLocked} onClick={() => onUnlink(q.id)}
                    title={isLocked ? 'Bài đã có lượt làm nên không được gỡ câu hỏi' : 'Chỉ gỡ khỏi bài này, câu hỏi vẫn còn trong ngân hàng'}>
                    Gỡ khỏi bài
                  </Button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
      {d.status === 'PUBLISHED' ? (
        <Banner tone="warn" role="status">
          Bài đang xuất bản nên không thể thêm hoặc gỡ câu hỏi. Hãy gỡ xuất bản trước nếu cần chỉnh sửa.
        </Banner>
      ) : (
        <div className="row">
          <Button variant="primary" onClick={() => setEditingQ({ id: 0, text: '', explanation: '', difficulty: 'MEDIUM', options: [{ id: 0, text: '', correct: false }, { id: 0, text: '', correct: false }], correctId: null })}>+ Tạo câu hỏi mới</Button>
          <Button onClick={onOpenBank}>Mượn từ ngân hàng</Button>
        </div>
      )}
      {editingQ ? (
        <QuestionForm
          d={d}
          initial={editingQ}
          isLocked={isLocked}
          onCancel={() => setEditingQ(null)}
          onSaved={() => { qc.invalidateQueries({ queryKey: ['admin-quiz', quizId] }); setEditingQ(null) }}
          onDelete={editingQ.id > 0 ? () => onDelete(editingQ.id) : undefined}
        />
      ) : null}
    </div>
  )
}

function QuestionForm({ d, initial, isLocked, onCancel, onSaved, onDelete }: { d: AdminFull; initial: AdminQuestion; isLocked: boolean; onCancel: () => void; onSaved: () => void; onDelete?: () => void }) {
  const [text, setText] = useState(initial.text)
  const [explanation, setExplanation] = useState(initial.explanation || '')
  const [opts, setOpts] = useState<AdminOption[]>(initial.options.length > 0 ? initial.options : [{ id: 0, text: '', correct: false }, { id: 0, text: '', correct: false }])
  const [err, setErr] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const isNew = initial.id === 0

  function setOpt(i: number, patch: Partial<AdminOption>) {
    setOpts((o) => o.map((x, k) => k === i ? { ...x, ...patch } : x))
  }
  function addOpt() {
    if (opts.length >= 5) return
    setOpts((o) => [...o, { id: 0, text: '', correct: false }])
  }
  function delOpt(i: number) {
    setOpts((o) => o.filter((_, k) => k !== i))
  }
  function setCorrect(i: number) {
    setOpts((o) => o.map((x, k) => ({ ...x, correct: k === i })))
  }

  async function save() {
    setErr(null)
    const parsed = questionSchema.safeParse({ text, explanation, options: opts.map((o) => ({ id: o.id || undefined, text: o.text, correct: o.correct })) })
    if (!parsed.success) {
      setErr(parsed.error.issues[0]?.message || 'Câu hỏi chưa hợp lệ.')
      return
    }
    setBusy(true)
    try {
      if (isNew) await teacher.addQuestion(d.id, parsed.data)
      else await teacher.updateQuestion(initial.id, parsed.data)
      toast('Đã lưu câu hỏi', 'good')
      onSaved()
    } catch (e) {
      if (e instanceof ApiCallError) setErr(e.message)
      else setErr('Lưu thất bại.')
    } finally { setBusy(false) }
  }

  return (
    <div className="card qed" style={{ borderWidth: 1.5 }}>
      <Field label="Nội dung câu hỏi" hint="Ít nhất 5 ký tự">
        <TextArea rows={3} value={text} onChange={(e) => setText(e.target.value)} />
      </Field>
      <div>
        <div className="row between" style={{ marginBottom: 6 }}>
          <strong>Các đáp án</strong>
          <Button size="sm" disabled={opts.length >= 5} onClick={addOpt}>+ Thêm đáp án</Button>
        </div>
        {opts.map((o, i) => (
          <div className="optrow" key={i}>
            <input type="radio" name="correct" disabled={isLocked && d.status === 'PUBLISHED'} checked={o.correct} onChange={() => setCorrect(i)} aria-label={`Đáp án đúng ${letter(i)}`} />
            <TextInput value={o.text} onChange={(e) => setOpt(i, { text: e.target.value })} placeholder={`Đáp án ${letter(i)}`} />
            <Button size="sm" variant="danger" disabled={opts.length <= 2 || (isLocked && d.status === 'PUBLISHED')} onClick={() => delOpt(i)}>Xoá</Button>
          </div>
        ))}
      </div>
      <Field label="Giải thích (tuỳ chọn)">
        <TextArea rows={2} value={explanation} onChange={(e) => setExplanation(e.target.value)} />
      </Field>
      {err ? <Banner tone="bad" role="alert">{err}</Banner> : null}
      {isLocked ? <Banner tone="info" role="status">Bài đã có lượt làm nên không thể xoá đáp án hay đổi đáp án đúng.</Banner> : null}
      <div className="row end" style={{ marginTop: 6, gap: 8 }}>
        {onDelete ? <Button variant="danger" onClick={onDelete}>Xoá câu hỏi</Button> : null}
        <Button onClick={onCancel}>Huỷ</Button>
        <Button variant="primary" disabled={busy} onClick={save}>{busy ? 'Đang lưu...' : 'Lưu câu hỏi'}</Button>
      </div>
    </div>
  )
}

function PublishTab({ d, onPublish, onUnpublish, publishing, pubErrs }: { d: AdminFull; onPublish: () => void; onUnpublish: () => void; publishing: boolean; pubErrs: { label: string; detail: string }[] | null }) {
  return (
    <div className="stack">
      <div className="card">
        <h3>Điều kiện xuất bản</h3>
        <ul className="checks" style={{ marginTop: 8 }}>
          {d.checks.map((c) => (
            <li key={c.key}>
              <span className={'ck ' + (c.ok ? 'ok' : 'no')} aria-hidden="true">{c.ok ? '✓' : '✕'}</span>
              <div>
                <div>{c.label}</div>
                {c.detail ? <div className="muted small">{c.detail}</div> : null}
              </div>
            </li>
          ))}
        </ul>
        {pubErrs && pubErrs.length > 0 ? (
          <div style={{ marginTop: 12 }}>
            <Banner tone="bad" role="alert">
              <strong>Bài chưa đủ điều kiện:</strong>
              <ul>{pubErrs.map((e, i) => <li key={i}>{e.label}{e.detail ? ` — ${e.detail}` : ''}</li>)}</ul>
            </Banner>
          </div>
        ) : null}
      </div>
      <div className="card row between">
        <div>
          <h3>Trạng thái: {statusPill(d.status)}</h3>
          <p className="muted small">{d.attemptCount} lượt đã nộp.</p>
        </div>
        {d.status === 'PUBLISHED' ? (
          <Button variant="danger" onClick={onUnpublish}>Gỡ xuất bản</Button>
        ) : (
          <Button variant="primary" disabled={publishing} onClick={onPublish}>{publishing ? 'Đang xuất bản...' : 'Xuất bản'}</Button>
        )}
      </div>
    </div>
  )
}

/**
 * Modal mượn câu hỏi đã có sẵn trong ngân hàng.
 * Câu hỏi không bị copy — server chỉ tạo thêm một dòng trong quiz_questions.
 */
function PickFromBankModal({ open, onClose, quizId }: { open: boolean; onClose: () => void; quizId: number }) {
  const qc = useQueryClient()
  const [search, setSearch] = useState('')
  const [debounced, setDebounced] = useState('')
  const [difficulty, setDifficulty] = useState('')

  useEffect(() => {
    const t = setTimeout(() => setDebounced(search), 300)
    return () => clearTimeout(t)
  }, [search])

  useEffect(() => { if (!open) { setSearch(''); setDebounced(''); setDifficulty('') } }, [open])

  const detailQ = useQuery({ queryKey: ['admin-quiz', quizId], queryFn: () => teacher.get(quizId), enabled: open })
  const listQ = useQuery({
    queryKey: ['question-bank', debounced, difficulty, null],
    queryFn: () => bank.search({ search: debounced, difficulty }),
    enabled: open
  })

  const inQuiz = new Set((detailQ.data?.questions ?? []).map((q) => q.id))

  const linkMut = useMutation({
    mutationFn: (questionId: number) => bank.linkToQuiz(quizId, questionId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-quiz', quizId] })
      qc.invalidateQueries({ queryKey: ['question-bank'] })
      qc.invalidateQueries({ queryKey: ['teacher-quizzes'] })
      toast('Đã thêm vào bài', 'good')
    },
    onError: (e) => { if (e instanceof ApiCallError) toast(e.message, 'bad') }
  })

  const items = listQ.data ?? []

  return (
    <Modal open={open} onClose={onClose} title="Mượn câu hỏi từ ngân hàng" busy={linkMut.isPending}>
      <div className="stack">
        <p className="muted small" style={{ margin: 0 }}>
          Câu hỏi được thêm nguyên vẹn, không tạo bản sao. Sửa câu hỏi trong ngân hàng sẽ áp dụng cho bài này.
        </p>

        <div className="row" style={{ gap: 10, alignItems: 'flex-end' }}>
          <div style={{ flex: 1 }}>
            <Field label="Tìm câu hỏi">
              <TextInput value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Từ khoá…" aria-label="Tìm câu hỏi" />
            </Field>
          </div>
          <div style={{ width: 160 }}>
            <Field label="Độ khó">
              <Select value={difficulty} onChange={(e) => setDifficulty(e.target.value)} aria-label="Lọc theo độ khó">
                <option value="">Mọi độ khó</option>
                <option value="EASY">Dễ</option>
                <option value="MEDIUM">Trung bình</option>
                <option value="HARD">Khó</option>
              </Select>
            </Field>
          </div>
        </div>

        {listQ.isLoading ? <Skeleton h={220} /> : items.length === 0 ? (
          <Banner tone="info" role="status">Không có câu hỏi nào khớp bộ lọc.</Banner>
        ) : (
          <div className="list" style={{ maxHeight: 380, overflowY: 'auto' }}>
            {items.map((q) => {
              const already = inQuiz.has(q.id)
              return (
                <div key={q.id} className="item" style={{ alignItems: 'flex-start' }}>
                  <div className="t" style={{ flex: 1 }}>
                    <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
                      {difficultyPill(q.difficulty)}
                      {q.usageCount > 0 ? <Pill tone="info">Dùng trong {q.usageCount} bài</Pill> : null}
                      {q.usedInPublishedQuiz ? <Pill tone="warn">Trong bài đã xuất bản</Pill> : null}
                    </div>
                    <div style={{ marginTop: 4 }}>{q.text}</div>
                  </div>
                  <Button
                    size="sm"
                    disabled={already || linkMut.isPending}
                    onClick={() => linkMut.mutate(q.id)}
                  >
                    {already ? 'Đã có trong bài' : linkMut.isPending && linkMut.variables === q.id ? 'Đang thêm…' : 'Thêm'}
                  </Button>
                </div>
              )
            })}
          </div>
        )}

        <div className="row end">
          <Button onClick={onClose}>Xong</Button>
        </div>
      </div>
    </Modal>
  )
}
