# Walkthrough phỏng vấn (M7)

Tài liệu này viết bằng tiếng Việt đơn giản, dành cho buổi phỏng vấn fresher Java backend.
Mục tiêu: người phỏng vấn đọc 5 phút có thể hỏi, người được phỏng vấn đọc 15 phút có thể trả lời.

## 1. Tổng quan hệ thống

- Học sinh đăng nhập → xem danh sách bài → làm bài → xem kết quả.
- Giáo viên tạo bài → thêm câu hỏi → xuất bản → xem kết quả.
- Chấm điểm **phía server**, snapshot lưu vào DB, lịch sử không đổi khi quiz sửa.

## 2. Luồng "Học sinh nộp bài" (POST /api/quizzes/{id}/attempts)

Bước qua code, dòng quan trọng nhất là `AttemptService.submit()`:

1. **Lấy user từ JWT** qua `CurrentUser.id()`.
2. **Kiểm tra idempotency**: `attempts.findByUserIdAndClientKey(userId, clientKey)`.
   - Nếu có → trả về attempt cũ với status 200 (không tạo mới).
3. **Lấy quiz** bằng `findWithQuestions`. Quiz phải PUBLISHED, không thì 404.
4. **Kiểm tra đóng cửa**: `closesAt` quá khứ → 409 `QUIZ_CLOSED`.
5. **Kiểm tra lượt**: `attempts.countByUserIdAndQuizId(...)` ≥ `maxAttempts` → 409 `MAX_ATTEMPTS`.
   - Lưu ý: kiểm tra idempotency TRƯỚC max-attempts, nên hai request trùng clientKey
     luôn trả về một attempt, không "đốt" lượt.
6. **Kiểm tra thời gian**: `now > startedAt + durationMin + 30s` → 422 `TIME_EXPIRED`.
7. **Validate từng answer**:
   - questionId phải thuộc quiz (`byQid`).
   - mỗi questionId chỉ xuất hiện một lần (`seen`).
   - nếu optionId != null thì option phải thuộc câu hỏi đó.
8. **Chấm điểm server-side**:
   - Với mỗi câu, so sánh `chosenOption.id` với option `correct=true`.
   - `score` = số câu đúng; `percent` = round(100 * score / total).
   - `passed` = `percent >= passPct`.
9. **Snapshot**: mỗi câu ghi 1 dòng vào `attempt_answers` với
   `question_text`, `option_text`, `chosen_option_id/text`, `correct_option_id/text`, `explanation`.
10. **Lưu attempt**: trong `@Transactional`. Nếu race trùng clientKey,
    `DataIntegrityViolationException` được bắt và trả về attempt cũ.

Điểm nhấn khi phỏng vấn:
- Snapshot quan trọng vì quiz có thể bị sửa sau khi học sinh nộp. Nếu không snapshot,
  lịch sử sẽ hiển thị sai.
- Idempotency chạy ở **2 lớp**: check trước + xử lý race sau. Cả hai cùng cần thiết.

## 3. Mối quan hệ DB

```
USERS (1) ─── (N) ATTEMPTS ─── (1) QUIZZES
                                 │
                                 └── (N) QUESTIONS ── (N) ANSWER_OPTIONS
ATTEMPTS (1) ─── (N) ATTEMPT_ANSWERS
```

Quan hệ chính:
- USERS.email UNIQUE — mỗi email một tài khoản.
- QUIZZES.status CHECK — chỉ DRAFT/PUBLISHED.
- QUIZZES.pass_pct 0..100, max_attempts ≥ 1 — validation tầng DB.
- QUESTIONS.quiz_id ON DELETE CASCADE — xoá quiz thì câu hỏi đi theo.
- ANSWER_OPTIONS.question_id ON DELETE CASCADE.
- ATTEMPTS.user_id + quiz_id ON DELETE RESTRICT — không vô tình mất lịch sử.
- ATTEMPTS UNIQUE (user_id, client_key) — bảo đảm idempotency ở DB.
- ATTEMPT_ANSWERS không FK tới questions/options — vì snapshot phải giữ nguyên khi quiz sửa.

Index:
- `attempts(user_id, submitted_at DESC)` — lịch sử học sinh.
- `attempts(quiz_id, submitted_at DESC)` — bảng kết quả giáo viên.
- `questions(quiz_id)`, `answer_options(question_id)`.

## 4. Bảo mật

- **JWT HS384** với secret 32 ký tự lấy từ env. Không bao giờ commit secret thật.
- **Lưu token ở đâu?** Chọn: lưu trong memory của React + `sessionStorage` để reload giữ
  được phiên trong cùng tab. Cookie httpOnly an toàn hơn nhưng cần HTTPS + SameSite chặt;
  với mục tiêu demo local, memory + sessionStorage đơn giản và đủ tốt.
- **BCrypt** với cost 10, không log hash.
- **CORS**: whitelist qua env `ALLOWED_ORIGINS`.
- **401/403**: SecurityFilterChain trả lỗi JSON đúng format. Frontend đẩy 401 về `/login`,
  trang riêng cho 403.
- **Role check 2 lớp**: SecurityFilterChain (`hasRole("TEACHER")`) + `@PreAuthorize` nếu cần.
- **DRAFT ẩn với học sinh**: trả 404 để tránh enumeration (học sinh không biết quiz tồn tại).

## 5. Vì sao từng ràng buộc

- **passPct 0..100**: tự bảo vệ lỗi nhập liệu; nếu > 100% sẽ không bao giờ đạt.
- **maxAttempts ≥ 1**: cho phép "1 lần/người", không thể 0 lần.
- **UNIQUE(user_id, client_key)**: bảo đảm idempotency tầng DB. Dù code có bug,
  DB vẫn từ chối duplicate.
- **CHECK score <= total**: chống gian lận/lỗi chấm.
- **CASCADE vs RESTRICT**: cascade khi dữ liệu con "thuộc về" cha (câu hỏi của quiz);
  restrict khi dữ liệu con là lịch sử độc lập (attempts).

## 6. 10 câu hỏi phỏng vấn thường gặp

**1. Server lưu password như thế nào?**
BCrypt với cost 10. Không bao giờ log hash, không trả về hash qua API. So sánh bằng
`encoder.matches(raw, hashed)`.

**2. Tại sao chọn JWT thay cho session?**
Stateless - backend không cần lưu session. Dễ scale ngang, không cần sticky session.
Đánh đổi: không thể thu hồi token trước khi hết hạn (giải pháp: thêm blacklist ngắn hạn
nếu cần).

**3. Idempotency chạy ở đâu, bao nhiêu lớp?**
- **Tầng service**: kiểm tra trước khi tạo (tránh tốn transaction).
- **Tầng DB**: UNIQUE constraint.
- **Tầng exception**: bắt `DataIntegrityViolationException` để xử lý race.
Nếu chỉ một lớp, có thể bị race khi 2 request đến gần nhau.

**4. Tại sao không trả đáp án đúng cho học sinh trong `GET /api/quizzes/{id}`?**
Vì học sinh có thể nhìn thấy trong tab Network. Phải chấm phía server. Khi học sinh
nộp bài, server so sánh với option `correct=true` trong DB và trả về review qua
`GET /api/attempts/{id}`.

**5. Tại sao lưu snapshot trong `attempt_answers`?**
Nếu giáo viên sửa câu hỏi hoặc đáp án sau khi học sinh nộp, lịch sử sẽ hiển thị sai.
Snapshot giữ nguyên nội dung tại thời điểm nộp. Tốn thêm dung lượng nhưng đảm bảo
"ký ức học tập" là bất biến.

**6. `closesAt` lưu timezone nào?**
`TIMESTAMPTZ` trong PostgreSQL, lưu UTC. Khi so sánh với `Instant.now()` luôn ở UTC.
Frontend hiển thị theo locale `vi-VN` qua `toLocaleString()`.

**7. Test cái gì, không test cái gì?**
- Test: luồng nghiệp vụ (idempotency, locked quiz, chấm điểm, validation).
- Không test: thư viện bên thứ ba (Spring Security, JJWT), UI, performance.

**8. Làm sao chạy backend + DB + frontend chỉ với 1 lệnh?**
`docker compose up -d --build`. PostgreSQL chạy ở container `quiz-postgres`, backend
ở `quiz-backend`, frontend ở `quiz-frontend` (nginx phục vụ dist + proxy /api).

**9. Nếu app scale lên 100k học sinh, đâu là nút thắt?**
- `POST /api/quizzes/{id}/attempts`: contention trên `attempts` khi có flash event.
  Giải pháp: connection pool lớn, có thể cần queue hoặc cache câu hỏi ở Redis.
- `GET /api/attempts/me`: query history có LIMIT 10 nên rẻ.
- `GET /api/admin/quizzes/{id}/attempts`: tổng hợp, có thể dùng materialized view.

**10. Câu nào là câu hỏi Spring khó nhất bạn từng gặp?**
`@Transactional` với lazy load và `open-in-view = false`. Lần đầu mình gặp
`LazyInitializationException` vì session đã đóng trước khi mình truy cập `options`.
Cách xử lý: gọi `getOptions().size()` trong transaction để trigger load, hoặc dùng
`JOIN FETCH` trong JPQL (nhưng phải cẩn thận MultipleBagFetchException).
