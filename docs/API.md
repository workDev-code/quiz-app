# API documentation (M7)

Tài liệu này tóm tắt các endpoint. Có thể xem trực tiếp tại:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Mọi response lỗi có cùng format:
```json
{
  "status": 422,
  "code": "VALIDATION_FAILED",
  "message": "Dữ liệu chưa hợp lệ.",
  "details": { "field": "..." },
  "path": "/api/...",
  "timestamp": "2026-10-13T07:00:00Z"
}
```

## Auth (public)

### POST /api/auth/register
Tạo tài khoản **STUDENT** mới.
```json
{ "name": "Nguyễn Văn A", "email": "a@b.co", "password": "Demo1234" }
```
- 201 Created + `{ token, expiresInSeconds, user }`
- 422 nếu validation lỗi
- 409 `EMAIL_EXISTS` nếu email đã tồn tại

### POST /api/auth/login
```json
{ "email": "hocsinh@demo.vn", "password": "Demo@1234" }
```
- 200 OK + `{ token, expiresInSeconds, user }`
- 401 `BAD_CREDENTIALS` nếu sai email/mật khẩu
- 422 nếu payload không hợp lệ

## Người dùng

### GET /api/users/me (auth)
Trả về `PublicUser` (id, name, email, role). 401 nếu chưa đăng nhập.

## Học sinh

### GET /api/me/dashboard (STUDENT)
Trả về số liệu thật (không fake):
- `availableCount`, `totalAttempts`, `avgPercent` (null nếu chưa có attempts)
- `quizzes`: tối đa 3 quiz đang mở, còn lượt
- `recent`: 5 attempts gần nhất

### GET /api/quizzes (STUDENT)
Danh sách `Summary` (chỉ PUBLISHED) gồm:
- thông tin cơ bản
- `attemptsUsed`, `attemptsLeft`, `bestPercent`, `available`

### GET /api/quizzes/{id} (STUDENT)
`Detail` gồm:
- `summary`
- `unavailableReason` (null khi bắt đầu được)
- `canStart`
- `questions[]` **chỉ khi `canStart`**. **Không** chứa `correct` hay `explanation`.
- 404 nếu quiz DRAFT hoặc không tồn tại.

### POST /api/quizzes/{id}/attempts (STUDENT)
```json
{
  "clientKey": "ck-...",
  "startedAt": "2026-10-13T07:00:00Z",
  "answers": [{ "questionId": 1, "optionId": 2 }, { "questionId": 2, "optionId": null }]
}
```
- 201 với attempt mới, 200 với attempt cũ nếu `clientKey` đã dùng (idempotency).
- 404 `QUIZ_NOT_FOUND`, 409 `QUIZ_CLOSED`, 409 `MAX_ATTEMPTS`, 422 `INVALID_QUESTION`, 422 `OPTION_NOT_IN_QUESTION`, 422 `DUPLICATE_ANSWER`, 422 `TIME_EXPIRED`.

### GET /api/attempts/me?page&size&quizId (STUDENT)
`HistoryPage` phân trang, kèm `avgPercent`, `totalAttempts`.

### GET /api/attempts/{id} (STUDENT)
`Detail` với review từng câu. 403 `NOT_YOUR_ATTEMPT` nếu attempt của người khác.

## Giáo viên

### GET /api/admin/stats (TEACHER)
`{ publishedCount, draftCount, attemptCount }`

### GET /api/admin/quizzes (TEACHER)
Danh sách `AdminSummary` (cả DRAFT và PUBLISHED).

### POST /api/admin/quizzes (TEACHER)
Tạo quiz mới (luôn là DRAFT). Body:
```json
{
  "title": "Tên bài", "description": "...", "subject": "Lập trình",
  "durationMin": 10, "maxAttempts": 3, "passPct": 60, "closesAt": "2026-12-31T23:59:59Z"
}
```

### GET / PUT /api/admin/quizzes/{id} (TEACHER)
Đọc/sửa thông tin. Khi đã có attempts: KHÔNG thể xoá câu/đáp án, KHÔNG thể đổi đáp án đúng (xem ở endpoint questions).

### POST /api/admin/quizzes/{id}/publish (TEACHER)
422 `QUIZ_NOT_PUBLISHABLE` với danh sách các điều kiện chưa thoả.

### POST /api/admin/quizzes/{id}/unpublish (TEACHER)
Về DRAFT, giữ nguyên attempts.

### POST /api/admin/quizzes/{id}/questions (TEACHER)
Body:
```json
{
  "text": "Câu hỏi?",
  "explanation": "Giải thích",
  "options": [{ "text": "A", "correct": true }, { "text": "B", "correct": false }]
}
```

### PUT /api/admin/questions/{id} (TEACHER)
Sửa câu. Khi locked, không được xoá option hay đổi correct.

### DELETE /api/admin/questions/{id} (TEACHER)
- 409 `QUIZ_HAS_ATTEMPTS` nếu đã có attempts
- 409 `LAST_QUESTION` nếu là câu cuối của quiz PUBLISHED

### GET /api/admin/quizzes/{id}/attempts (TEACHER)
Bảng kết quả của giáo viên.
