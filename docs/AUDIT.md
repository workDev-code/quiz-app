# Audit (M0)

## Tình trạng repo khi nhận
- Branch hiện tại rất trống: chỉ có thư mục `demo/` chứa file `Ánh Dương Quiz.html` (1159 dòng) - đây là bản demo UI/UX HTML đã được phê duyệt, có dữ liệu mẫu và một "máy chủ giả lập" chạy trong trình duyệt.
- Không có backend Java, không có frontend React, không có Dockerfile, không có tài liệu, không có CI.
- Quyết định: giữ nguyên `demo/` để làm nguồn tham chiếu thiết kế; xây dựng lại toàn bộ ứng dụng theo stack đã chốt.

## Có thể tái sử dụng từ demo
- **Design tokens** (CSS variables, typography scale, spacing, light/dark) - đã được ánh xạ sang Tailwind.
- **Mọi bản sao tiếng Việt** (banner, error message, label, tooltip) - copy nguyên từ demo.
- **Quy tắc nghiệp vụ** đã mô tả trong demo (publish checks, locked-quiz rules, idempotency, thời gian làm bài...) - dùng làm đặc tả kiểm thử.
- **Cấu trúc dữ liệu mẫu** - 4 bài kiểm tra, 4 người dùng, nhiều attempts - dùng làm dữ liệu seed.

## Không tái sử dụng
- "Máy chủ giả lập" trong demo chỉ chạy trong bộ nhớ trình duyệt, không phải backend thật.
- Class `ApiError` mô phỏng chỉ để dùng nội bộ UI; phải tạo lại dưới dạng `@RestControllerAdvice` trong Spring.

## Môi trường
- Đã xác nhận có sẵn: Java 21 (OpenJDK 21.0.12.1), Maven 3.8.7, Node 24.20.0, Docker 29.7.2, Docker Compose 5.5.0.
- PostgreSQL 16 sẽ chạy qua Docker Compose cho cột mốc dev/prod-like.
- Testcontainers sẽ chạy PostgreSQL 16 trong pipeline test (yêu cầu Docker chạy được - đã kiểm tra `docker info` thành công).

## Rủi ro và cách giảm thiểu
1. **Phạm vi lớn**: 13 màn hình + ~20 component + 6 bảng + 15+ test tích hợp. Chia thành 8 mốc (M0..M7), commit sau mỗi mốc.
2. **Khớp demo HTML**: dùng cùng token, cùng câu tiếng Việt, kiểm tra ở 400/768/1280 px.
3. **An toàn bài đã chấm**: snapshot đáp án + câu hỏi trong `attempt_answers`; ràng buộc ở DB + service.
4. **Race idempotency**: ràng buộc UNIQUE (user_id, client_key) + xử lý `DataIntegrityViolationException` để trả về 200 với attempt cũ.
