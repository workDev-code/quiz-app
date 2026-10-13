import axios, { AxiosError } from 'axios'
import { useAuth } from './auth'

export interface ApiErrorBody {
  status: number
  code: string
  message: string
  details: unknown
  path: string
  timestamp: string
}

export class ApiCallError extends Error {
  status: number
  code: string
  details: unknown
  constructor(b: ApiErrorBody) {
    super(b.message)
    this.status = b.status
    this.code = b.code
    this.details = b.details
  }
}

const client = axios.create({ baseURL: '/api' })

client.interceptors.request.use((config) => {
  const t = useAuth.getState().token
  if (t) config.headers.set('Authorization', `Bearer ${t}`)
  return config
})

client.interceptors.response.use(
  (r) => r,
  (err: AxiosError<ApiErrorBody>) => {
    if (err.response?.data && typeof err.response.data === 'object' && 'code' in err.response.data) {
      const b = err.response.data
      // 401 => đẩy về login kèm banner
      if (b.status === 401) {
        useAuth.getState().clear()
        if (location.pathname !== '/login' && location.pathname !== '/register') {
          sessionStorage.setItem('flash', 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
          location.href = '/login'
        }
      }
      return Promise.reject(new ApiCallError(b))
    }
    return Promise.reject(err)
  }
)

export default client
