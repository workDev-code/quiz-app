import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { bank, teacher, ApiCallError } from '@/features/api'
import { Banner } from '@/components/Banner'
import { Button } from '@/components/Button'
import { Field, TextInput, Select } from '@/components/Field'
import { Pill, difficultyPill, questionTypeLabel } from '@/components/Pill'
import { Skeleton } from '@/components/Skeleton'
import { EmptyState } from '@/components/EmptyState'
import { toast } from '@/lib/toast'
import { letter } from '@/lib/format'
import type { BankItem } from '@/lib/types'

const DIFFICULTIES = [
  { value: '', label: 'Mọi độ khó' },
  { value: 'EASY', label: 'Dễ' },
  { value: 'MEDIUM', label: 'Trung bình' },
  { value: 'HARD', label: 'Khó' }
]

export default function QuestionBankPage() {
  const qc = useQueryClient()

  const [search, setSearch] = useState('')
  const [debounced, setDebounced] = useState('')
  const [difficulty, setDifficulty] = useState('')
  const [tagId, setTagId] = useState<number | null>(null)
  const [quizId, setQuizId] = useState<number | null>(null)
  const [newTag, setNewTag] = useState('')

  // Gõ xong 300ms mới gọi API, tránh dội request mỗi ký tự.
  useEffect(() => {
    const t = setTimeout(() => setDebounced(search), 300)
    return () => clearTimeout(t)
  }, [search])

  const tagsQ = useQuery({ queryKey: ['bank-tags'], queryFn: bank.tags })
  const quizzesQ = useQuery({ queryKey: ['teacher-quizzes'], queryFn: teacher.list })

  const listQ = useQuery({
    queryKey: ['question-bank', debounced, difficulty, tagId],
    queryFn: () => bank.search({ search: debounced, difficulty, tagId })
  })

  // Đề đang chọn: biết câu nào đã có sẵn để không bấm "thêm" trùng (server sẽ trả 409).
  const targetQ = useQuery({
    queryKey: ['admin-quiz', quizId],
    queryFn: () => teacher.get(quizId!),
    enabled: quizId != null
  })
  const alreadyInQuiz = useMemo(
    () => new Set((targetQ.data?.questions ?? []).map((q) => q.id)),
    [targetQ.data]
  )
  const targetIsPublished = targetQ.data?.status === 'PUBLISHED'

  function refreshAll() {
    qc.invalidateQueries({ queryKey: ['question-bank'] })
    qc.invalidateQueries({ queryKey: ['admin-quiz', quizId] })
    qc.invalidateQueries({ queryKey: ['teacher-quizzes'] })
  }

  const linkMut = useMutation({
    mutationFn: (questionId: number) => bank.linkToQuiz(quizId!, questionId),
    onSuccess: () => { toast('Đã thêm câu hỏi vào bài', 'good'); refreshAll() },
    onError: (e) => { if (e instanceof ApiCallError) toast(e.message, 'bad') }
  })

  const createTagMut = useMutation({
    mutationFn: (name: string) => bank.createTag(name),
    onSuccess: () => { setNewTag(''); toast('Đã tạo thẻ', 'good'); qc.invalidateQueries({ queryKey: ['bank-tags'] }) },
    onError: (e) => { if (e instanceof ApiCallError) toast(e.message, 'bad') }
  })

  const deleteTagMut = useMutation({
    mutationFn: (id: number) => bank.deleteTag(id),
    onSuccess: () => {
      if (tagId != null) setTagId(null)
      toast('Đã xoá thẻ', 'good')
      qc.invalidateQueries({ queryKey: ['bank-tags'] })
      qc.invalidateQueries({ queryKey: ['question-bank'] })
    },
    onError: (e) => { if (e instanceof ApiCallError) toast(e.message, 'bad') }
  })

  const items = listQ.data ?? []

  return (
    <div className="stack">
      <Link to="/teacher" className="link">← Quay lại</Link>

      <div className="ph">
        <div>
          <h1>Ngân hàng câu hỏi</h1>
          <p className="muted" style={{ margin: '6px 0 0' }}>
            Câu hỏi tồn tại độc lập. Thêm vào bài nào cũng chỉ tạo liên kết, không nhân bản câu hỏi.
          </p>
        </div>
        <div className="muted small">
          {items.length} câu {listQ.isFetching && '· đang tải…'}
        </div>
      </div>

      <div className="card stack">
        <div className="form-grid">
          <Field label="Tìm trong nội dung câu hỏi">
            <TextInput
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Ví dụ: kiểu dữ liệu"
              aria-label="Tìm câu hỏi"
            />
          </Field>
          <Field label="Độ khó">
            <Select value={difficulty} onChange={(e) => setDifficulty(e.target.value)} aria-label="Lọc theo độ khó">
              {DIFFICULTIES.map((d) => <option key={d.value} value={d.value}>{d.label}</option>)}
            </Select>
          </Field>
          <Field label="Thẻ chủ đề">
            <Select
              value={tagId == null ? '' : String(tagId)}
              onChange={(e) => setTagId(e.target.value === '' ? null : Number(e.target.value))}
              aria-label="Lọc theo thẻ"
            >
              <option value="">Mọi thẻ</option>
              {(tagsQ.data ?? []).map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </Select>
          </Field>
          <Field label="Thêm vào bài" hint="Chọn bài để bật nút thêm">
            <Select
              value={quizId == null ? '' : String(quizId)}
              onChange={(e) => setQuizId(e.target.value === '' ? null : Number(e.target.value))}
              aria-label="Chọn bài để thêm câu hỏi"
            >
              <option value="">— Chưa chọn bài —</option>
              {(quizzesQ.data ?? []).map((q) => (
                <option key={q.id} value={q.id}>
                  {q.title}{q.status === 'PUBLISHED' ? ' (đang xuất bản)' : ''}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        {targetIsPublished ? (
          <Banner tone="warn" role="status">
            Bài đang chọn đã <strong>xuất bản</strong>. Muốn thêm câu hỏi bạn phải gỡ xuất bản trước.
          </Banner>
        ) : null}

        <TagManager
          tags={tagsQ.data ?? []}
          value={newTag}
          onChange={setNewTag}
          onCreate={() => { const n = newTag.trim(); if (n) createTagMut.mutate(n) }}
          onDelete={(id) => deleteTagMut.mutate(id)}
          busy={createTagMut.isPending || deleteTagMut.isPending}
        />
      </div>

      {listQ.isLoading ? (
        <div className="list">{[0, 1, 2].map((i) => <Skeleton key={i} h={86} />)}</div>
      ) : listQ.isError ? (
        <Banner tone="bad" role="alert">
          Không tải được ngân hàng câu hỏi.{' '}
          <button className="link" onClick={() => listQ.refetch()}>Thử lại</button>
        </Banner>
      ) : items.length === 0 ? (
        <EmptyState
          title="Không có câu hỏi nào khớp"
          desc="Thử xoá bớt bộ lọc, hoặc tạo câu hỏi mới trong một bài kiểm tra rồi gắn thẻ cho nó."
          action={<Link to="/teacher" className="link">Về danh sách bài</Link>}
        />
      ) : (
        <div className="list">
          {items.map((q) => (
            <BankRow
              key={q.id}
              q={q}
              quizId={quizId}
              alreadyInQuiz={alreadyInQuiz.has(q.id)}
              targetIsPublished={targetIsPublished}
              linking={linkMut.isPending && linkMut.variables === q.id}
              onLink={() => linkMut.mutate(q.id)}
            />
          ))}
        </div>
      )}
    </div>
  )
}

function TagManager({ tags, value, onChange, onCreate, onDelete, busy }: {
  tags: { id: number; name: string }[]
  value: string
  onChange: (v: string) => void
  onCreate: () => void
  onDelete: (id: number) => void
  busy: boolean
}) {
  return (
    <div className="stack" style={{ gap: 8 }}>
      <div className="row between">
        <span className="small muted">Thẻ chủ đề dùng để lọc nhanh câu hỏi theo mạng lưới kiến thức.</span>
        <form
          className="row"
          onSubmit={(e) => { e.preventDefault(); onCreate() }}
        >
          <TextInput
            value={value}
            onChange={(e) => onChange(e.target.value)}
            placeholder="Tên thẻ mới"
            aria-label="Tên thẻ mới"
            style={{ minWidth: 180 }}
          />
          <Button type="submit" disabled={busy || !value.trim()}>Tạo thẻ</Button>
        </form>
      </div>
      {tags.length === 0 ? (
        <span className="muted small">Chưa có thẻ nào.</span>
      ) : (
        <div className="chips">
          {tags.map((t) => (
            <span key={t.id} className="chip" style={{ display: 'inline-flex', alignItems: 'center', gap: 8, cursor: 'default' }}>
              {t.name}
              <button
                className="link"
                onClick={() => onDelete(t.id)}
                disabled={busy}
                aria-label={`Xoá thẻ ${t.name}`}
                style={{ color: 'var(--bad)' }}
              >
                ✕
              </button>
            </span>
          ))}
        </div>
      )}
    </div>
  )
}

function BankRow({ q, quizId, alreadyInQuiz, targetIsPublished, linking, onLink }: {
  q: BankItem
  quizId: number | null
  alreadyInQuiz: boolean
  targetIsPublished: boolean
  linking: boolean
  onLink: () => void
}) {
  const [showAnswers, setShowAnswers] = useState(false)

  const disabled = quizId == null || alreadyInQuiz || targetIsPublished

  return (
    <div className="item" style={{ alignItems: 'flex-start' }}>
      <div className="t" style={{ flex: 1 }}>
        <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
          {difficultyPill(q.difficulty)}
          <Pill>{questionTypeLabel(q.type)}</Pill>
          {q.usageCount > 0 ? <Pill tone="info">Dùng trong {q.usageCount} bài</Pill> : <Pill>Chưa dùng ở đâu</Pill>}
          {q.usedInPublishedQuiz ? <Pill tone="warn">Đang có trong bài đã xuất bản</Pill> : null}
          {q.tags.map((t) => <Pill key={t.id} tone="neutral">{t.name}</Pill>)}
        </div>

        <div style={{ marginTop: 6 }}>{q.text}</div>

        {showAnswers ? (
          <ol className="rules" style={{ marginTop: 8 }}>
            {q.options.map((o, i) => (
              <li key={o.id}>
                {o.text}{o.correct ? <strong style={{ color: 'var(--good)' }}> — đáp án đúng</strong> : null}
              </li>
            ))}
            {q.explanation ? <li className="muted">Giải thích: {q.explanation}</li> : null}
          </ol>
        ) : (
          <div className="muted small" style={{ marginTop: 4 }}>{q.options.length} đáp án</div>
        )}

        <button className="link small" onClick={() => setShowAnswers((v) => !v)} style={{ marginTop: 6 }}>
          {showAnswers ? 'Ẩn đáp án' : 'Xem đáp án'}
        </button>
      </div>

      <div className="row">
        <Button size="sm" onClick={onLink} disabled={disabled || linking}>
          {alreadyInQuiz ? 'Đã có trong bài' : linking ? 'Đang thêm…' : 'Thêm vào bài'}
        </Button>
      </div>
    </div>
  )
}
