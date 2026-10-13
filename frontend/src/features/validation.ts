import { z } from 'zod'

export const loginSchema = z.object({
  email: z.string().min(1, 'Nhập email hợp lệ.').email('Nhập email hợp lệ.'),
  password: z.string().min(1, 'Nhập mật khẩu.')
})
export type LoginInput = z.infer<typeof loginSchema>

export const registerSchema = z.object({
  name: z.string().min(2, 'Họ tên cần ít nhất 2 ký tự.').max(120, 'Họ tên tối đa 120 ký tự.'),
  email: z.string().min(1, 'Nhập email hợp lệ.').email('Nhập email hợp lệ.').max(180),
  password: z.string().min(8, 'Mật khẩu cần ít nhất 8 ký tự, gồm cả chữ và số.')
    .regex(/[A-Za-z]/, 'Mật khẩu cần có ít nhất một chữ cái.')
    .regex(/\d/, 'Mật khẩu cần có ít nhất một chữ số.')
})
export type RegisterInput = z.infer<typeof registerSchema>

export const quizSchema = z.object({
  title: z.string().min(3, 'Tên bài cần ít nhất 3 ký tự.').max(180, 'Tên bài tối đa 180 ký tự.'),
  description: z.string().max(2000).default(''),
  subject: z.string().min(1, 'Chọn môn học.').max(80),
  durationMin: z.coerce.number().int('Thời gian từ 1 đến 240 phút.').min(1, 'Thời gian từ 1 đến 240 phút.').max(240, 'Thời gian từ 1 đến 240 phút.'),
  maxAttempts: z.coerce.number().int('Số lần làm từ 1 đến 20.').min(1, 'Số lần làm từ 1 đến 20.').max(20, 'Số lần làm từ 1 đến 20.'),
  passPct: z.coerce.number().int('Điểm đạt từ 0 đến 100%.').min(0, 'Điểm đạt từ 0 đến 100%.').max(100, 'Điểm đạt từ 0 đến 100%.'),
  closesAt: z.string().optional().nullable()
})
export type QuizInput = z.infer<typeof quizSchema>

export const optionInputSchema = z.object({
  id: z.number().int().optional().nullable(),
  text: z.string().min(1, 'Đáp án này đang để trống.').max(1000),
  correct: z.boolean()
})
export const questionSchema = z.object({
  text: z.string().min(5, 'Nội dung câu hỏi cần ít nhất 5 ký tự.').max(2000),
  explanation: z.string().max(2000).optional().nullable(),
  options: z.array(optionInputSchema).min(2, 'Mỗi câu cần từ 2 đến 5 đáp án.').max(5, 'Mỗi câu cần từ 2 đến 5 đáp án.')
})
export type QuestionInput = z.infer<typeof questionSchema>
