import { describe, it, expect } from 'vitest'
import { loginSchema, registerSchema, quizSchema } from '../features/validation'

describe('validation schemas', () => {
  it('loginSchema: email trống → lỗi', () => {
    const r = loginSchema.safeParse({ email: '', password: 'abc12345' })
    expect(r.success).toBe(false)
  })
  it('loginSchema: hợp lệ', () => {
    const r = loginSchema.safeParse({ email: 'a@b.co', password: 'abc12345' })
    expect(r.success).toBe(true)
  })
  it('registerSchema: mật khẩu thiếu số → lỗi', () => {
    const r = registerSchema.safeParse({ name: 'Lan', email: 'a@b.co', password: 'onlyletters' })
    expect(r.success).toBe(false)
  })
  it('registerSchema: hợp lệ', () => {
    const r = registerSchema.safeParse({ name: 'Lan', email: 'a@b.co', password: 'Demo1234' })
    expect(r.success).toBe(true)
  })
  it('quizSchema: tên quá ngắn', () => {
    const r = quizSchema.safeParse({ title: 'ab', description: '', subject: 'Lập trình', durationMin: 5, maxAttempts: 2, passPct: 50 })
    expect(r.success).toBe(false)
  })
  it('quizSchema: hợp lệ', () => {
    const r = quizSchema.safeParse({ title: 'Java Core', description: '', subject: 'Lập trình', durationMin: 5, maxAttempts: 2, passPct: 50 })
    expect(r.success).toBe(true)
  })
})
