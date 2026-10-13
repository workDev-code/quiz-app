# ER và mối quan hệ (M7)

## Sơ đồ ER (Mermaid)

```mermaid
erDiagram
    USERS ||--o{ ATTEMPTS : "làm"
    QUIZZES ||--o{ QUESTIONS : "chứa"
    QUESTIONS ||--o{ ANSWER_OPTIONS : "có"
    QUIZZES ||--o{ ATTEMPTS : "được làm"
    ATTEMPTS ||--o{ ATTEMPT_ANSWERS : "gồm"
    ANSWER_OPTIONS ||--o{ ATTEMPT_ANSWERS : "được chọn (optional)"

    USERS {
        bigint id PK
        varchar name
        varchar email UK
        varchar password_hash
        varchar role "STUDENT | TEACHER"
        boolean disabled
        timestamptz created_at
        timestamptz updated_at
    }
    QUIZZES {
        bigint id PK
        varchar title
        text description
        varchar subject
        varchar status "DRAFT | PUBLISHED"
        int duration_min
        int max_attempts
        int pass_pct
        timestamptz closes_at "nullable"
        timestamptz created_at
        timestamptz updated_at
    }
    QUESTIONS {
        bigint id PK
        bigint quiz_id FK
        text text
        text explanation "nullable"
        int position
        timestamptz created_at
    }
    ANSWER_OPTIONS {
        bigint id PK
        bigint question_id FK
        text text
        boolean correct
        int position
    }
    ATTEMPTS {
        bigint id PK
        bigint user_id FK
        bigint quiz_id FK
        int seq "lần thứ bao nhiêu của user trên quiz"
        int score
        int total
        int percent
        boolean passed
        varchar client_key UK_with_user
        timestamptz started_at
        timestamptz submitted_at
    }
    ATTEMPT_ANSWERS {
        bigint id PK
        bigint attempt_id FK
        bigint question_id "snapshot"
        text question_text "snapshot"
        bigint chosen_option_id "snapshot, nullable"
        text chosen_option_text "snapshot, nullable"
        bigint correct_option_id "snapshot"
        text correct_option_text "snapshot"
        text explanation_snapshot "snapshot, nullable"
        int question_position
        int position
    }
```

## Lý do từng ràng buộc

- **users.email UNIQUE**: một email chỉ đăng ký một tài khoản; 422 `EMAIL_EXISTS` khi trùng.
- **quizzes.status CHECK (`DRAFT` hoặc `PUBLISHED`)**: mọi nhánh if/else phải dựa vào giá trị enum, không dùng chuỗi tự do.
- **quizzes.pass_pct CHECK (0..100)**: % đạt hợp lệ; `0` nghĩa là luôn đạt, `100` nghĩa là phải đúng toàn bộ.
- **quizzes.max_attempts CHECK (>=1)**: tối thiểu 1 lượt/người dùng.
- **questions.quiz_id NOT NULL + ON DELETE CASCADE**: câu hỏi thuộc về một quiz; khi quiz bị xóa (hiếm, sau khi xóa attempts), câu hỏi đi theo.
- **answer_options.question_id NOT NULL + ON DELETE CASCADE**: đáp án thuộc về câu hỏi.
- **attempts.user_id NOT NULL + ON DELETE RESTRICT**: bảo vệ lịch sử; muốn xóa user phải xóa attempts trước (hiếm).
- **attempts.quiz_id ON DELETE RESTRICT**: không cho xóa quiz có attempt; nếu cần, unpublished + archive trước.
- **attempts UNIQUE (user_id, client_key)**: bảo đảm idempotency ở tầng DB; race được xử lý bằng `DataIntegrityViolationException`.
- **attempts CHECK (total >= 0, score >= 0, score <= total)**: chống gian lận/lỗi chấm.
- **attempt_answers.chosen_option_id NULL**: cho phép câu chưa trả lời (0 điểm).
- **attempt_answers.question_text / option_text / explanation snapshot**: bài đã chấm hiển thị đúng ngay cả khi giáo viên sửa sau.
- **idx(user_id, submitted_at DESC)**: truy vấn lịch sử "tôi đã làm những gì" - tải trang chính.
- **idx(quiz_id, submitted_at DESC)**: xếp bảng kết quả giáo viên xem.
- **idx(questions.quiz_id)**: lấy câu hỏi của một quiz.
