import type { Role } from './auth'

export interface QuizSummary {
  id: number
  title: string
  description: string
  subject: string
  questionCount: number
  durationMin: number
  maxAttempts: number
  passPct: number
  closesAt: string | null
  attemptsUsed: number
  bestPercent: number | null
  available: boolean
  attemptsLeft: number
}

export interface QuizDetail {
  summary: QuizSummary
  unavailableReason: string | null
  canStart: boolean
  questions?: PublicQuestion[]
}
export interface PublicQuestion { id: number; text: string; options: { id: number; text: string }[] }

export interface AdminOption { id: number; text: string; correct: boolean }
export interface AdminQuestion { id: number; text: string; explanation: string | null; options: AdminOption[]; correctId: number | null }
export interface AdminCheck { key: string; label: string; ok: boolean; detail: string }
export interface AdminFull {
  id: number; title: string; description: string; subject: string;
  status: string; durationMin: number; maxAttempts: number; passPct: number;
  closesAt: string | null; updatedAt: string; attemptCount: number; locked: boolean;
  questions: AdminQuestion[]; checks: AdminCheck[];
}
export interface AdminSummary {
  id: number; title: string; subject: string; status: string;
  questionCount: number; attemptCount: number; avgPercent: number | null;
  closesAt: string | null; updatedAt: string;
}

export interface AttemptBrief {
  id: number; quizId: number; quizTitle: string; seq: number;
  score: number; total: number; percent: number; passed: boolean; submittedAt: string;
}
export interface ReviewOption { id: number; text: string; correct: boolean; chosen: boolean }
export interface ReviewQuestion {
  questionId: number; text: string; chosenOptionId: number | null;
  correctOptionId: number; explanation: string | null;
  answered: boolean; correct: boolean; options: ReviewOption[];
}
export interface AttemptDetail {
  id: number; quizId: number; quizTitle: string; seq: number;
  score: number; total: number; percent: number; passed: boolean;
  passPct: number; maxAttempts: number; attemptsLeft: number; canRetry: boolean;
  submittedAt: string; items: ReviewQuestion[];
}
export interface AttemptHistoryItem {
  id: number; quizId: number; quizTitle: string; seq: number;
  score: number; total: number; percent: number; passed: boolean; submittedAt: string;
}
export interface AttemptHistoryPage {
  items: AttemptHistoryItem[]; page: number; size: number;
  total: number; totalPages: number; avgPercent: number | null; totalAttempts: number;
}

export interface DashboardRecent {
  id: number; quizId: number; quizTitle: string; seq: number;
  score: number; total: number; percent: number; passed: boolean; submittedAt: string;
}
export interface Dashboard {
  availableCount: number; totalAttempts: number; avgPercent: number | null;
  quizzes: QuizSummary[]; recent: DashboardRecent[];
}

export interface TeacherStats { publishedCount: number; draftCount: number; attemptCount: number }
