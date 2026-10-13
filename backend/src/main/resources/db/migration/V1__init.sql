-- V1__init.sql : schema gốc của Ánh Dương Quiz
SET TIME ZONE 'UTC';

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('STUDENT','TEACHER')),
    disabled      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE quizzes (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(180) NOT NULL,
    description   TEXT         NOT NULL DEFAULT '',
    subject       VARCHAR(80)  NOT NULL,
    status        VARCHAR(20)  NOT NULL CHECK (status IN ('DRAFT','PUBLISHED')),
    duration_min  INTEGER      NOT NULL CHECK (duration_min BETWEEN 1 AND 240),
    max_attempts  INTEGER      NOT NULL CHECK (max_attempts BETWEEN 1 AND 20),
    pass_pct      INTEGER      NOT NULL CHECK (pass_pct BETWEEN 0 AND 100),
    closes_at     TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_quizzes_status ON quizzes(status);

CREATE TABLE questions (
    id          BIGSERIAL PRIMARY KEY,
    quiz_id     BIGINT      NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    text        TEXT        NOT NULL,
    explanation TEXT,
    position    INTEGER     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_questions_quiz ON questions(quiz_id);

CREATE TABLE answer_options (
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT      NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    text        TEXT        NOT NULL,
    correct     BOOLEAN     NOT NULL DEFAULT FALSE,
    position    INTEGER     NOT NULL
);
CREATE INDEX idx_options_question ON answer_options(question_id);

CREATE TABLE attempts (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    quiz_id      BIGINT      NOT NULL REFERENCES quizzes(id) ON DELETE RESTRICT,
    seq          INTEGER     NOT NULL,
    score        INTEGER     NOT NULL CHECK (score >= 0),
    total        INTEGER     NOT NULL CHECK (total >= 0 AND total >= score),
    percent      INTEGER     NOT NULL CHECK (percent BETWEEN 0 AND 100),
    passed       BOOLEAN     NOT NULL,
    pass_pct     INTEGER     NOT NULL,
    client_key   VARCHAR(80) NOT NULL,
    started_at   TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_attempts_user_client UNIQUE (user_id, client_key)
);
CREATE INDEX idx_attempts_user      ON attempts(user_id, submitted_at DESC);
CREATE INDEX idx_attempts_quiz      ON attempts(quiz_id, submitted_at DESC);
CREATE INDEX idx_attempts_user_quiz ON attempts(user_id, quiz_id);

CREATE TABLE attempt_answers (
    id                    BIGSERIAL PRIMARY KEY,
    attempt_id            BIGINT      NOT NULL REFERENCES attempts(id) ON DELETE CASCADE,
    question_id_snapshot  BIGINT      NOT NULL,
    question_text         TEXT        NOT NULL,
    chosen_option_id      BIGINT,
    chosen_option_text    TEXT,
    correct_option_id     BIGINT      NOT NULL,
    correct_option_text   TEXT        NOT NULL,
    explanation_snapshot  TEXT,
    question_position     INTEGER     NOT NULL,
    position              INTEGER     NOT NULL
);
CREATE INDEX idx_attempt_answers_attempt ON attempt_answers(attempt_id, position);
