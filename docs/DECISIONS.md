# Quyết định kỹ thuật (ghi khi phát sinh)

| # | Quyết định | Lý do |
|---|------------|-------|
| D1 | Lưu JWT trong **memory** (biến React) + `localStorage` chỉ lưu `userId`/`role` để khôi phục session sau reload, kết hợp refresh token ngắn hạn 60 phút. | httpOnly cookie cần HTTPS + SameSite chặt hơn khi deploy; với mục tiêu demo local-first và portfolio, lưu memory + sessionStorage là an toàn và đơn giản. Ghi rõ hạn chế trong README. |
| D2 | Dùng **Testcontainers** cho cả test tích hợp, không mock H2. | Có Docker sẵn; một schema thật giúp bắt lỗi SQL (CHECK, UNIQUE) chính xác. |
| D3 | `attempt_answers` lưu **snapshot** (question_text, options_text, chosen_option_id, correct_option_id) thay vì FK nullable. | Bài đã chấm phải hiển thị đúng ngay cả khi giáo viên sửa/snapshot lỗi thời. |
| D4 | Idempotency: kiểm tra `client_key` TRƯỚC khi đếm `maxAttempts`. | Hai request giống nhau gửi liên tiếp không được tạo thêm lượt. |
| D5 | Thời gian làm bài: server lưu `started_at`, reject nếu `now > started_at + durationMin + 30s grace`. | Tránh client sửa giờ; grace 30s để bù độ trễ mạng. |
| D6 | `closes_at` lưu `TIMESTAMP WITH TIME ZONE` (UTC). | Tránh lỗi múi giờ giữa client và server. |
| D7 | Xóa user/quiz: **không cascade**. Đánh dấu `disabled` nếu cần. Mỗi lần xóa quiz có attempts trả 409. | Lịch sử học tập phải còn nguyên vẹn để xem lại. |
| D8 | React Router **không dùng** next-style data loading. Dùng `loader` đơn giản + TanStack Query cache. | Phù hợp SPA thuần, dễ debug. |
| D9 | TanStack Query staleTime 30s cho danh sách, 0 cho attempt submit. | Tránh UI "giật" khi quay lại trang, nhưng luôn lấy dữ liệu mới khi nộp bài. |
| D10 | Theme switch theo 3 chế độ (system/light/dark) lưu trong `localStorage`. | Đúng yêu cầu demo. |
| D11 | Dùng `springdoc-openapi-starter-webmvc-ui` để sinh OpenAPI tại `/swagger-ui.html`. | API docs miễn phí, đẹp. |
| D12 | Demo HTML (`demo/Ánh Dương Quiz.html`) **giữ nguyên trong repo** dưới `demo/` làm nguồn tham khảo. | Người phỏng vấn / reviewer có thể so sánh trực tiếp. |
| D13 | Frontend dev server proxy `/api` sang `http://localhost:8080` qua `vite.config.ts`. | Không cần CORS trong môi trường dev. |
| D14 | CORS chỉ whitelist `http://localhost:5173` và `http://localhost:4173` mặc định. | Chặt, đủ cho dev. |
| D15 | Sử dụng `BCrypt` với strength 10 (mặc định Spring Security). | Đủ nhanh cho môi trường dev và an toàn cho prod. |
