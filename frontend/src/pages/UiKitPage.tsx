import { useState } from 'react'
import { Banner } from '@/components/Banner'
import { Button } from '@/components/Button'
import { Field, TextInput, TextArea, Select } from '@/components/Field'
import { Pill, statusPill, resultPill, openPill, attemptsPill } from '@/components/Pill'
import { Modal } from '@/components/Modal'
import { Pagination } from '@/components/Pagination'
import { Skeleton } from '@/components/Skeleton'
import { EmptyState } from '@/components/EmptyState'
import { Stat } from '@/components/Stat'
import { ThemeSwitch } from '@/components/ThemeSwitch'
import { setTheme } from '@/lib/theme'

export default function UiKitPage() {
  const [modal, setModal] = useState(false)
  return (
    <div className="stack">
      <h1>UI Kit</h1>
      <p className="muted">Mỗi thành phần dùng trong ứng dụng. So sánh trực tiếp với <code>demo/Ánh Dương Quiz.html</code>.</p>
      <div className="row">
        <ThemeSwitch />
        <Button size="sm" onClick={() => setTheme('light')}>Sáng</Button>
        <Button size="sm" onClick={() => setTheme('dark')}>Tối</Button>
        <Button size="sm" onClick={() => setTheme('system')}>Theo máy</Button>
      </div>

      <div className="card">
        <h3>Buttons</h3>
        <div className="row" style={{ marginTop: 8, gap: 8, flexWrap: 'wrap' }}>
          <Button variant="primary">Chính</Button>
          <Button>Phụ</Button>
          <Button variant="ghost">Trong suốt</Button>
          <Button variant="danger">Nguy hiểm</Button>
          <Button disabled>Đã tắt</Button>
        </div>
      </div>

      <div className="card">
        <h3>Form</h3>
        <div className="form-grid" style={{ marginTop: 8 }}>
          <Field label="Email"><TextInput type="email" defaultValue="hocsinh@demo.vn" /></Field>
          <Field label="Mật khẩu" hint="Ít nhất 8 ký tự"><TextInput type="password" /></Field>
          <Field label="Sai" error="Trường này bắt buộc."><TextInput invalid defaultValue="" /></Field>
          <Field label="Mô tả"><TextArea rows={3} defaultValue="Nội dung..." /></Field>
          <Field label="Môn"><Select defaultValue="Lập trình"><option>Lập trình</option><option>Tiếng Anh</option></Select></Field>
        </div>
      </div>

      <div className="card">
        <h3>Pills & Banners</h3>
        <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
          {statusPill('PUBLISHED')}
          {statusPill('DRAFT')}
          {resultPill(true)}
          {resultPill(false)}
          {openPill(true)}
          {openPill(false)}
          {attemptsPill(2)}
          {attemptsPill(0)}
          <Pill tone="info">Lập trình</Pill>
        </div>
        <div className="stack-s" style={{ marginTop: 12 }}>
          <Banner tone="info">Thông báo thường.</Banner>
          <Banner tone="good">Đã lưu thành công.</Banner>
          <Banner tone="warn">Cảnh báo cho người dùng.</Banner>
          <Banner tone="bad" role="alert">Có lỗi xảy ra.</Banner>
        </div>
      </div>

      <div className="card">
        <h3>Stat</h3>
        <div className="stats"><Stat label="Số" value={42} /><Stat label="Phần trăm" value={83} suffix="%" /></div>
      </div>

      <div className="card">
        <h3>Skeleton & Empty</h3>
        <Skeleton h={32} />
        <div style={{ height: 8 }} />
        <EmptyState title="Chưa có dữ liệu" desc="Hãy tạo một bản ghi." action={<Button>+ Tạo mới</Button>} />
      </div>

      <div className="card">
        <h3>Modal & Pagination</h3>
        <div className="row" style={{ gap: 8 }}>
          <Button onClick={() => setModal(true)}>Mở modal</Button>
        </div>
        <Pagination page={0} totalPages={5} onChange={() => {}} />
      </div>
      <Modal open={modal} onClose={() => setModal(false)} title="Modal mẫu" footer={<>
        <Button onClick={() => setModal(false)}>Đóng</Button>
        <Button variant="primary" onClick={() => setModal(false)}>OK</Button>
      </>}>
        <p>Đây là nội dung modal. Bấm ESC hoặc bấm ra ngoài để đóng.</p>
      </Modal>
    </div>
  )
}
