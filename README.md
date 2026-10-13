# Ánh Dương Quiz — Quiz Management and Assessment Platform

Ứng dụng web full-stack giúp trung tâm nhỏ tạo đề, cho học sinh làm bài và xem kết quả.
Được viết theo tinh thần portfolio cho vị trí **Java Backend Fresher**.

> Mọi quyết định kỹ thuật được ghi lại trong [`docs/DECISIONS.md`](docs/DECISIONS.md).
> Cấu trúc dữ liệu và ràng buộc ở [`docs/ER.md`](docs/ER.md).
> Kiến trúc tổng quan ở [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).
> Bản demo HTML gốc (thiết kế UI/UX được phê duyệt) ở [`demo/Ánh Dương Quiz.html`](demo/Ánh%20D%C6%B0%C6%A1ng%20Quiz.html).

## 1. Tech stack

| Layer    | Công nghệ |
|----------|-----------|
| Backend  | Java 21, Spring Boot 3.3, Spring Web / Data JPA / Security / Validation, Flyway, JJWT 0.12, springdoc OpenAPI |
| Database | PostgreSQL 16 (qua Docker Compose, hoặc bất kỳ PG 14+ nào) |
| Frontend | React 18 + TypeScript + Vite, React Router 6, TanStack Query 5, React Hook Form + Zod, Tailwind CSS 3, Vitest + Testing Library |
| Infra    | Docker Compose, GitHub Actions (build + test cho backend và frontend) |

## 2. Chạy nhanh với Docker Compose (khuyến nghị)

Yêu cầu: Docker + Docker Compose.

```bash
# 1. Clone hoặc cd vào dự án
cp .env.example .env

# 2. Khởi động PostgreSQL + backend + frontend
docker compose up -d --build

# 3. Mở trình duyệt
# Frontend:  http://localhost:5173
# API docs:  http://localhost:8080/swagger-ui.html
# OpenAPI:   http://localhost:8080/v3/api-docs

# 4. Đăng nhập thử (chỉ chạy ở profile dev)
#    Học sinh:  hocsinh@demo.vn  / Demo@1234
#    Giáo viên: giaovien@demo.vn / Demo@1234
```

Khi lên production, **đổi `JWT_SECRET`** trong `.env` thành chuỗi ngẫu nhiên ≥ 32 ký tự.

## 3. Chạy thủ công (cho phát triển)

```bash
# Backend (yêu cầu PostgreSQL ở localhost:5432 hoặc trỏ SPRING_DATASOURCE_URL)
cd backend
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/quizdb \
SPRING_DATASOURCE_USERNAME=quiz \
SPRING_DATASOURCE_PASSWORD=quiz \
SPRING_PROFILES_ACTIVE=dev \
mvn spring-boot:run

# Frontend (terminal khác)
cd frontend
npm install
npm run dev
# Truy cập http://localhost:5173 - Vite proxy /api sang :8080
```

## 4. Cấu trúc repo

```
quiz-app/
├── backend/                 # Spring Boot 3 + Java 21
│   ├── src/main/java/com/anhtduong/quiz/
│   │   ├── QuizApplication.java
│   │   ├── auth/            # POST /api/auth/login, /register
│   │   ├── user/            # UserEntity, MeController
│   │   ├── security/        # JwtService, JwtAuthFilter, SecurityConfig
│   │   ├── quiz/            # StudentQuizController/Service, StudentDashboard
│   │   ├── question/        # Question, AnswerOption entities
│   │   ├── attempt/         # AttemptService, snapshot, idempotency
│   │   ├── admin/           # Teacher endpoints (CRUD quiz, publish, questions)
│   │   ├── common/          # ApiError, BusinessException, GlobalExceptionHandler
│   │   ├── config/          # OpenAPI
│   │   └── seed/            # DataSeeder (chỉ chạy ở dev/test)
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   └── db/migration/V1__init.sql
│   ├── src/test/...
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                # React + Vite + TS
│   ├── src/
│   │   ├── pages/           # 13 màn hình (Login, StudentDashboard, QuizList, QuizDetail, QuizTake, AttemptDetail, History, TeacherDashboard, QuizEdit, TeacherResults, Forbidden, UiKit, Register)
│   │   ├── components/      # Button, Field, Banner, Pill, Modal, Pagination, Skeleton, EmptyState, Stat, ThemeSwitch, TopBar, AppShell
│   │   ├── features/        # api.ts (axios client), validation.ts (zod schemas)
│   │   ├── lib/             # api, auth, theme, format, toast
│   │   ├── test/            # vitest tests
│   │   ├── App.tsx          # router + auth guard
│   │   ├── main.tsx
│   │   └── index.css        # design tokens + components
│   ├── tailwind.config.js
│   ├── vite.config.ts
│   ├── Dockerfile
│   └── nginx.conf
├── docs/
│   ├── AUDIT.md             # audit repo lúc nhận
│   ├── DECISIONS.md         # các quyết định kỹ thuật
│   ├── ER.md                # sơ đồ ER + lý do ràng buộc
│   ├── ARCHITECTURE.md      # sơ đồ kiến trúc
│   ├── API.md               # tóm tắt API
│   └── INTERVIEW.md         # walkthrough phỏng vấn (Q&A)
├── demo/Ánh Dương Quiz.html # bản demo HTML gốc (tham chiếu thiết kế)
├── .github/workflows/ci.yml # CI: build + test backend và frontend
├── docker-compose.yml
├── .env.example
└── README.md
```

## 5. Tài khoản demo (chỉ chạy ở profile dev)

| Vai trò   | Email                | Mật khẩu    | Ghi chú |
|-----------|----------------------|-------------|---------|
| Học sinh  | `hocsinh@demo.vn`    | `Demo@1234` | Đã có sẵn attempts trong demo |
| Giáo viên | `giaovien@demo.vn`   | `Demo@1234` | Tạo/xuất bản/xoá bài |
| Học sinh  | `minh@demo.vn`       | `Demo@1234` | Thêm |
| Học sinh  | `ha@demo.vn`         | `Demo@1234` | Thêm |

`SPRING_PROFILES_ACTIVE=prod` sẽ tắt seed và chỉ chạy với dữ liệu thật.

## 6. Lệnh kiểm thử

```bash
# Backend
cd backend
mvn -B -ntp test          # chạy test tích hợp + unit
mvn -B -ntp -DskipTests package

# Frontend
cd frontend
npm test                  # chạy vitest
npm run build             # build production
```

Test backend cần PostgreSQL 16 đang chạy ở `localhost:5433` (xem CI để biết cách dùng
service `postgres`). Schema sẽ tự reset một lần trước khi class đầu tiên chạy; Flyway tạo lại.

## 7. API tóm tắt

| Method | Path | Role | Mô tả |
|--------|------|------|-------|
| POST | /api/auth/register | public | Đăng ký (luôn tạo STUDENT) |
| POST | /api/auth/login | public | Đăng nhập, trả JWT |
| GET  | /api/users/me | any | Thông tin user hiện tại |
| GET  | /api/me/dashboard | STUDENT | Greeting + stats + 3 quiz nổi + 5 attempts gần nhất |
| GET  | /api/quizzes | STUDENT | Danh sách quiz PUBLISHED |
| GET  | /api/quizzes/{id} | STUDENT | Chi tiết quiz + câu hỏi (KHÔNG có đáp án đúng) |
| POST | /api/quizzes/{id}/attempts | STUDENT | Nộp bài (idempotent qua `clientKey`) |
| GET  | /api/attempts/me | STUDENT | Lịch sử phân trang |
| GET  | /api/attempts/{id} | STUDENT | Chi tiết attempt + review |
| GET  | /api/admin/stats | TEACHER | Stats cho dashboard giáo viên |
| GET/POST | /api/admin/quizzes | TEACHER | CRUD quiz |
| GET/PUT | /api/admin/quizzes/{id} | TEACHER | Đọc/sửa quiz (giới hạn khi đã có attempts) |
| POST | /api/admin/quizzes/{id}/publish | TEACHER | Xuất bản (422 nếu thiếu điều kiện) |
| POST | /api/admin/quizzes/{id}/unpublish | TEACHER | Gỡ xuất bản |
| POST | /api/admin/quizzes/{id}/questions | TEACHER | Thêm câu hỏi |
| PUT/DELETE | /api/admin/questions/{id} | TEACHER | Sửa/xoá câu hỏi (giới hạn khi locked) |
| GET  | /api/admin/quizzes/{id}/attempts | TEACHER | Bảng kết quả của giáo viên |

Mọi lỗi trả về theo format: `{status, code, message, details, path, timestamp}`.

## 8. Quy tắc nghiệp vụ chính

- **Idempotency**: `attempts.user_id + client_key` UNIQUE; client gửi lại cùng `clientKey`
  trả về attempt cũ (200) thay vì tạo mới. Race được xử lý qua
  `DataIntegrityViolationException`.
- **Snapshot**: bài đã chấm lưu `question_text`, `option_text`, `chosen_option_id`,
  `correct_option_id`, `explanation` vào `attempt_answers` - sửa quiz không ảnh hưởng lịch sử.
- **Locked quiz**: quiz có attempt thì không được xoá câu/đáp án hay đổi đáp án đúng (409).
- **Publish check (server)**: 5 điều kiện - title ≥ 3, ≥ 1 câu, mỗi câu ≥ 2 đáp án,
  mỗi câu đúng 1 đáp án, `closesAt` còn tương lai.
- **DRAFT ẩn với học sinh**: trả 404 để tránh enumeration.
- **Timer**: server xác nhận `startedAt + durationMin + 30s grace`; về 0 sẽ auto-submit.

## 9. Bảo mật

- BCrypt password hashing, không log hash.
- JWT HS384, 60 phút, secret từ env.
- JWT được lưu trong bộ nhớ React + `sessionStorage` (xem `docs/DECISIONS.md`).
- CORS whitelist qua `ALLOWED_ORIGINS`.
- Lỗi 401 đẩy về `/login`; lỗi 403 có trang riêng.
- `@PreAuthorize` + SecurityFilterChain (defense-in-depth).
- **Không commit secret thật vào git.** Có `.env.example` để tham chiếu.

## 10. Đã kiểm thử

| Loại | Phạm vi |
|------|---------|
| Unit (backend) | publish checks (5 keys) |
| Integration (backend, PostgreSQL thật) | auth + role, học sinh không xem được attempt của người khác, DRAFT 404, JSON quiz không có `correct`/`explanation`, chấm điểm đúng, idempotency (cùng + khác clientKey), 422 cho option sai / question sai, duplicate question |
| Unit (frontend, vitest) | validation schemas (login, register, quiz) |
| E2E manual (qua curl/dev server) | flow: giáo viên tạo + publish, học sinh làm + xem kết quả, idempotency race, 401 → /login, 403 trang riêng, theme light/dark, responsive 400/600/860/1280 |

## 11. Có gì CHƯA test / giới hạn đã biết

- **Timer auto-submit** chỉ test bằng tay (giảm `durationMin` xuống 1 phút và đợi).
  Có thể thêm test tích hợp riêng với `Clock` mock nếu cần.
- **Theme `system`**: tuân theo `prefers-color-scheme`; không có toggle nhanh trong
  UI lúc chuyển trang đầu tiên (đã có nút chuyển trong TopBar).
- **Concurrency race idempotency**: có test thủ công nhưng chưa có test tự động với
  goroutines. Lý do: Spring test runner serialize per class.
- **Phân trang lịch sử**: đã có API; UI dùng mặc định 10/trang.
- **i18n**: hiện chỉ tiếng Việt, đúng như yêu cầu demo.
- **Testcontainers**: dùng được nhưng trong máy local docker-java cũ nên ta
  dùng trực tiếp PostgreSQL 16 chạy qua Docker Compose. CI dùng service `postgres`
  chính thức của GitHub Actions.

## 12. Tài liệu liên quan

- `docs/INTERVIEW.md` — câu hỏi phỏng vấn Java backend kèm câu trả lời ngắn.
- `docs/ARCHITECTURE.md` — sơ đồ kiến trúc (Mermaid).
- `docs/ER.md` — sơ đồ ER + lý do ràng buộc.
- `docs/DECISIONS.md` — quyết định kỹ thuật kèm lý do.
- `docs/AUDIT.md` — đánh giá repo khi nhận.

## 13. License

Mã nguồn mở cho mục đích học tập/portfolio. Không dùng cho production mà chưa review
bảo mật, scaling, secret rotation.
