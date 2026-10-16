#!/bin/bash
# Khởi động đầy đủ (giống deploy): postgres + backend + frontend, tất cả trong Docker
set -e
cd "$(dirname "$0")/.."
docker compose up -d --build
echo ""
echo "Frontend: http://localhost:5173"
echo "API docs: http://localhost:8080/swagger-ui.html"
