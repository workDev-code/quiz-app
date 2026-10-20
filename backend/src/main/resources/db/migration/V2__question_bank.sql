-- V2__question_bank.sql : nâng cấp lên ngân hàng câu hỏi (question bank)
-- - questions có thêm difficulty + type
-- - quiz_questions là bảng liên kết N-N giữa quiz và question
-- - tags, question_tags để gắn nhãn chủ đề
-- - Dữ liệu cũ trong questions.quiz_id được migrate sang quiz_questions
-- - questions.quiz_id giữ lại (nullable) để backward-compatible; sẽ drop ở V3

SET TIME ZONE 'UTC';

-- 1) Thêm cột mới vào questions
ALTER TABLE questions
    ADD COLUMN difficulty VARCHAR(16) NOT NULL DEFAULT 'MEDIUM'
        CHECK (difficulty IN ('EASY','MEDIUM','HARD')),
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'SINGLE_CHOICE'
        CHECK (type IN ('SINGLE_CHOICE','MULTIPLE_CHOICE','TRUE_FALSE'));

CREATE INDEX idx_questions_difficulty ON questions(difficulty);
CREATE INDEX idx_questions_type ON questions(type);

-- 2) Cho phép questions.quiz_id NULL (để dùng lại câu hỏi)
ALTER TABLE questions ALTER COLUMN quiz_id DROP NOT NULL;
ALTER TABLE questions ALTER COLUMN position DROP NOT NULL;

-- 3) Bảng mapping quiz_questions
CREATE TABLE quiz_questions (
    quiz_id        BIGINT      NOT NULL REFERENCES quizzes(id)  ON DELETE CASCADE,
    question_id    BIGINT      NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    position       INTEGER     NOT NULL,
    points         NUMERIC(6,2) NOT NULL DEFAULT 1.00,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (quiz_id, question_id)
);
CREATE INDEX idx_quiz_questions_quiz_pos  ON quiz_questions(quiz_id, position);
CREATE INDEX idx_quiz_questions_question    ON quiz_questions(question_id);

-- 4) Bảng tags + question_tags
CREATE TABLE tags (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(60) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE question_tags (
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    tag_id      BIGINT NOT NULL REFERENCES tags(id)       ON DELETE CASCADE,
    PRIMARY KEY (question_id, tag_id)
);
CREATE INDEX idx_question_tags_tag ON question_tags(tag_id);

-- 5) Migrate dữ liệu cũ: copy (quiz_id, id, position) → quiz_questions
INSERT INTO quiz_questions (quiz_id, question_id, position, points)
SELECT quiz_id, id, COALESCE(position, 0), 1.00
FROM questions
WHERE quiz_id IS NOT NULL
ON CONFLICT (quiz_id, question_id) DO NOTHING;
