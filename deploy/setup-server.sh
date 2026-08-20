#!/usr/bin/env bash
# ============================================================
# 가비아 서버 최초 배포 스크립트 (Ubuntu 기준)
# root 로 브라우저 터미널 접속 후, 아래 한 줄만 실행하세요:
#   SERVER_IP=<공인IP> bash setup-server.sh
# ============================================================
set -e

if [ -z "$SERVER_IP" ]; then
  echo "❌ SERVER_IP 를 지정하세요.  예) SERVER_IP=211.234.56.78 bash setup-server.sh"
  exit 1
fi

echo "==> 1. Docker 설치 (이미 있으면 건너뜀)"
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
fi
docker compose version >/dev/null 2>&1 || { apt-get update && apt-get install -y docker-compose-plugin; }

echo "==> 2. 배포 폴더 준비"
mkdir -p ~/mcm && cd ~/mcm

echo "==> 3. 소스 클론 (없으면 clone, 있으면 pull)"
[ -d sjf_BE ]    && (cd sjf_BE && git pull)    || git clone https://github.com/likelion-gachon1/sjf_BE.git
[ -d sjf_track ] && (cd sjf_track && git pull) || git clone https://github.com/likelion-gachon1/sjf_track.git

echo "==> 4. 프론트 Dockerfile 없으면 생성"
if [ ! -f sjf_track/Dockerfile ]; then
cat > sjf_track/Dockerfile <<'DOCKEREOF'
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG NEXT_PUBLIC_API_BASE
ENV NEXT_PUBLIC_API_BASE=$NEXT_PUBLIC_API_BASE
RUN npm run build
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /app/package*.json ./
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public
COPY --from=builder /app/next.config.mjs ./next.config.mjs
EXPOSE 3000
CMD ["npm", "run", "start"]
DOCKEREOF
fi

echo "==> 5. docker-compose.yml 생성"
cat > docker-compose.yml <<'COMPOSEEOF'
services:
  backend:
    build:
      context: ./sjf_BE
    container_name: mcm-backend
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      DB_URL: "jdbc:h2:file:/app/data/sjf;DB_CLOSE_ON_EXIT=FALSE"
      DB_USERNAME: "sa"
      DB_PASSWORD: ""
      H2_CONSOLE_ENABLED: "false"
      STORAGE_PATH: "/app/uploads"
      SESSION_TTL_HOURS: "24"
      FRONTEND_BASE_URL: "${FRONTEND_BASE_URL}"
      PUBLIC_API_BASE_URL: "${PUBLIC_API_BASE_URL}"
      ALLOWED_ORIGINS: "${ALLOWED_ORIGINS}"
    volumes:
      - backend-data:/app/data
      - backend-uploads:/app/uploads
  frontend:
    build:
      context: ./sjf_track
      args:
        NEXT_PUBLIC_API_BASE: "${NEXT_PUBLIC_API_BASE}"
    container_name: mcm-frontend
    restart: unless-stopped
    ports:
      - "3000:3000"
    depends_on:
      - backend
volumes:
  backend-data:
  backend-uploads:
COMPOSEEOF

echo "==> 6. .env 생성 (공인 IP 주입)"
cat > .env <<ENVEOF
NEXT_PUBLIC_API_BASE=http://$SERVER_IP:8080
FRONTEND_BASE_URL=http://$SERVER_IP:3000
PUBLIC_API_BASE_URL=http://$SERVER_IP:8080
ALLOWED_ORIGINS=http://$SERVER_IP:3000
ENVEOF

echo "==> 7. 빌드 & 실행 (몇 분 걸립니다)"
docker compose up -d --build

echo ""
echo "✅ 배포 완료!"
echo "   프론트:  http://$SERVER_IP:3000"
echo "   백엔드:  http://$SERVER_IP:8080/api/health"
docker compose ps
