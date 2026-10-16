# Dev workflow (reload liên tục)

Database ít thay đổi → để trong Docker. Backend/frontend sửa nhiều → chạy trực tiếp trên máy để Vite HMR và Spring DevTools tự reload.

## Khởi động

```bash
# 1. Bật Postgres trong Docker (chạy nền, ít khi cần restart)
./scripts/dev-up.sh

# 2. Mở Terminal 2: chạy backend
cd backend
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/quizdb \
SPRING_DATASOURCE_USERNAME=quiz \
SPRING_DATASOURCE_PASSWORD=quiz \
SPRING_PROFILES_ACTIVE=dev \
mvn spring-boot:run

# 3. Mở Terminal 3: chạy frontend
cd frontend
npm run dev
```

Mở http://localhost:5173.

## Tốc độ reload

| Sửa gì | Phản ứng |
|--------|----------|
| File .tsx hoặc .css | Vite HMR cập nhật trong khoảng 0.3 giây, không cần reload trang |
| File .java | Spring DevTools restart context trong khoảng 2-3 giây, frontend vẫn chạy |
| File pom.xml hoặc package.json | Phải chạy lại terminal tương ứng |
| File migration V*.sql | Backend tự chạy Flyway khi restart |

## Biến môi trường gọn hơn

Tạo file `.env` ở thư mục gốc (đã có .gitignore loại trừ):

```bash
cat > .env <<ENV
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/quizdb
SPRING_DATASOURCE_USERNAME=quiz
SPRING_DATASOURCE_PASSWORD=quiz
SPRING_PROFILES_ACTIVE=dev
ENV
```

Rồi chỉ cần `mvn spring-boot:run` (Spring Boot tự load .env).

## Khi cần reset database

```bash
docker compose down -v         # xoá volume (xoá hết dữ liệu)
./scripts/dev-up.sh            # Postgres restart, tự tạo schema + seed
```

## Khi cần test full stack trong Docker (giống deploy)

```bash
./scripts/prod-up.sh          # bật cả 3 service, build lại image
```

## Lưu ý

- Backend port 8080, Frontend port 5173, Postgres port 5432 — giống full Docker setup, không xung đột.
- Vite proxy /api/* sang http://localhost:8080 đã cấu hình sẵn trong frontend/vite.config.ts.
- Spring DevTools chỉ hoạt động khi chạy qua `mvn spring-boot:run` (development mode), không bật khi chạy từ JAR.
