import { Link } from 'react-router-dom'
import { Button } from '@/components/Button'
import { Banner } from '@/components/Banner'

export default function ForbiddenPage() {
  return (
    <div className="stack" style={{ maxWidth: 520, margin: '60px auto' }}>
      <h1>Không có quyền truy cập</h1>
      <Banner tone="bad" role="alert">Bạn không có quyền xem trang này. Nếu bạn nghĩ đây là nhầm lẫn, hãy liên hệ giáo viên.</Banner>
      <div className="row" style={{ gap: 8 }}>
        <Link to="/"><Button>Về trang chính</Button></Link>
        <Link to="/login"><Button variant="ghost">Đăng nhập tài khoản khác</Button></Link>
      </div>
    </div>
  )
}
