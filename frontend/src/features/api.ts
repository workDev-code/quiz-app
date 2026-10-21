import api, { ApiCallError } from '@/lib/api'
import type {
  QuizSummary, QuizDetail, AdminFull, AdminSummary, AdminQuestion, AttemptBrief,
  AttemptDetail, AttemptHistoryPage, Dashboard, TeacherStats, BankItem, BankTag
} from '@/lib/types'
import type { AuthUser } from '@/lib/auth'

export const auth = {
  async login(email: string, password: string) {
    const r = await api.post<{ token: string; expiresInSeconds: number; user: AuthUser }>('/auth/login', { email, password })
    return r.data
  },
  async register(name: string, email: string, password: string) {
    const r = await api.post<{ token: string; expiresInSeconds: number; user: AuthUser }>('/auth/register', { name, email, password })
    return r.data
  },
  async me() {
    const r = await api.get<AuthUser>('/users/me')
    return r.data
  }
}

export const student = {
  async dashboard() { const r = await api.get<Dashboard>('/me/dashboard'); return r.data },
  async quizzes()   { const r = await api.get<QuizSummary[]>('/quizzes'); return r.data },
  async quiz(id: number) { const r = await api.get<QuizDetail>(`/quizzes/${id}`); return r.data },
  async submitAttempt(quizId: number, body: { clientKey: string; startedAt: string; answers: { questionId: number; optionId: number | null }[] }) {
    const r = await api.post<AttemptBrief>(`/quizzes/${quizId}/attempts`, body)
    return r.data
  },
  async history(page = 0, size = 10, quizId?: number) {
    const r = await api.get<AttemptHistoryPage>('/attempts/me', { params: { page, size, quizId } })
    return r.data
  },
  async attempt(id: number) { const r = await api.get<AttemptDetail>(`/attempts/${id}`); return r.data }
}

export const teacher = {
  async stats() { const r = await api.get<TeacherStats>('/admin/stats'); return r.data },
  async list() { const r = await api.get<AdminSummary[]>('/admin/quizzes'); return r.data },
  async get(id: number) { const r = await api.get<AdminFull>(`/admin/quizzes/${id}`); return r.data },
  async create(body: unknown) { const r = await api.post<AdminFull>('/admin/quizzes', body); return r.data },
  async update(id: number, body: unknown) { const r = await api.put<AdminFull>(`/admin/quizzes/${id}`, body); return r.data },
  async publish(id: number) { const r = await api.post<AdminFull>(`/admin/quizzes/${id}/publish`); return r.data },
  async unpublish(id: number) { const r = await api.post<AdminFull>(`/admin/quizzes/${id}/unpublish`); return r.data },
  async addQuestion(quizId: number, body: unknown) { const r = await api.post<AdminFull>(`/admin/quizzes/${quizId}/questions`, body); return r.data },
  async updateQuestion(questionId: number, body: unknown) { const r = await api.put<AdminFull>(`/admin/questions/${questionId}`, body); return r.data },
  async deleteQuestion(questionId: number) { const r = await api.delete<AdminFull>(`/admin/questions/${questionId}`); return r.data },
  async attempts(quizId: number) { const r = await api.get<{ quiz: { id: number; title: string; passPct: number; questionCount: number }, items: unknown[] }>(`/admin/quizzes/${quizId}/attempts`); return r.data }
}

/**
 * Ngân hàng câu hỏi: câu hỏi tồn tại độc lập, đề chỉ "mượn" qua quiz_questions.
 */
export const bank = {
  async search(params: { search?: string; difficulty?: string; tagId?: number | null } = {}) {
    // Chỉ gửi bộ lọc thật sự có - backend bỏ qua tham số rỗng.
    const qs: Record<string, string> = {}
    if (params.search?.trim()) qs.search = params.search.trim()
    if (params.difficulty) qs.difficulty = params.difficulty
    if (params.tagId != null) qs.tagId = String(params.tagId)
    const r = await api.get<BankItem[]>('/admin/questions', { params: qs })
    return r.data
  },
  async tags() { const r = await api.get<BankTag[]>('/admin/tags'); return r.data },
  async createTag(name: string) { const r = await api.post<BankTag>('/admin/tags', { name }); return r.data },
  async deleteTag(id: number) { await api.delete(`/admin/tags/${id}`) },
  async linkToQuiz(quizId: number, questionId: number) {
    await api.post(`/admin/quizzes/${quizId}/questions/${questionId}`)
  },
  async unlinkFromQuiz(quizId: number, questionId: number) {
    await api.delete(`/admin/quizzes/${quizId}/questions/${questionId}`)
  }
}

export { ApiCallError }
