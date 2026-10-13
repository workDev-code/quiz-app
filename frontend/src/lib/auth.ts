import { create } from 'zustand'

export type Role = 'STUDENT' | 'TEACHER'

export interface AuthUser {
  id: number
  name: string
  email: string
  role: Role
}

interface AuthState {
  user: AuthUser | null
  token: string | null
  expiresAt: number | null
  setAuth: (a: { token: string; user: AuthUser; expiresInSeconds: number }) => void
  clear: () => void
}

export const useAuth = create<AuthState>((set) => ({
  user: null,
  token: null,
  expiresAt: null,
  setAuth: ({ token, user, expiresInSeconds }) => {
    sessionStorage.setItem('token', token)
    sessionStorage.setItem('user', JSON.stringify(user))
    sessionStorage.setItem('expiresAt', String(Date.now() + expiresInSeconds * 1000))
    set({ user, token, expiresAt: Date.now() + expiresInSeconds * 1000 })
  },
  clear: () => {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('user')
    sessionStorage.removeItem('expiresAt')
    set({ user: null, token: null, expiresAt: null })
  }
}))

/** Khôi phục session từ sessionStorage khi reload trang. */
export function bootstrapAuth() {
  const token = sessionStorage.getItem('token')
  const userStr = sessionStorage.getItem('user')
  const expStr = sessionStorage.getItem('expiresAt')
  if (token && userStr && expStr) {
    const expiresAt = Number(expStr)
    if (expiresAt > Date.now()) {
      try {
        const user = JSON.parse(userStr) as AuthUser
        useAuth.setState({ token, user, expiresAt })
      } catch { /* ignore */ }
    } else {
      sessionStorage.clear()
    }
  }
}
