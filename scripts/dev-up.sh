#!/bin/bash
# Khởi động môi trường dev:
#  - PostgreSQL chạy trong Docker (ít thay đổi, không cần reload)
#  - Backend chạy bằng `mvn spring-boot:run` (DevTools tự reload khi sửa .java)
#  - Frontend chạy bằng `npm run dev` (Vite HMR tự reload khi sửa .tsx)
set -e
cd "$(dirname "$0")/.."

echo "==> Start Postgres (Docker)..."
docker compose up -d postgres

echo ""
echo "==> Mở 2 terminal mới:"
echo "    Terminal 1: cd backend && mvn spring-boot:run"
echo "    Terminal 2: cd frontend && npm run dev"
echo ""
echo "==> Sau đó mở http://localhost:5173"
