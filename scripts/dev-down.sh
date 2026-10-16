#!/bin/bash
# Dừng Postgres (backend & frontend tự tắt khi đóng terminal)
set -e
cd "$(dirname "$0")/.."
docker compose down
