#!/usr/bin/env bash
# ============================================================
# DuckDNS(무료 도메인) + Let's Encrypt 로 고정 HTTPS 배포
# 카메라(getUserMedia)용 정식 인증서. 주소가 안 바뀝니다.
#
# 실행 예)
#   DUCK_DOMAIN=gachonmiin DUCK_TOKEN=xxxxxxxx bash https-deploy.sh
#
#   DUCK_DOMAIN : duckdns 에서 만든 이름 ('.duckdns.org' 앞부분만)
#   DUCK_TOKEN  : duckdns 페이지 상단의 token
# ============================================================
set -e

if [ -z "$DUCK_DOMAIN" ] || [ -z "$DUCK_TOKEN" ]; then
  echo "❌ DUCK_DOMAIN 과 DUCK_TOKEN 을 지정하세요."
  echo "   예) DUCK_DOMAIN=gachonmiin DUCK_TOKEN=abcd-1234 bash https-deploy.sh"
  exit 1
fi

FQDN="${DUCK_DOMAIN}.duckdns.org"
echo "==> 대상 도메인: https://$FQDN"

cd ~/mcm 2>/dev/null || { echo "❌ 먼저 setup-server.sh 로 ~/mcm 구성부터 하세요"; exit 1; }

# ------------------------------------------------------------
# 1) DuckDNS 에 이 서버의 공인 IP 등록 (도메인 -> 서버 연결)
# ------------------------------------------------------------
echo "==> 1. DuckDNS IP 갱신"
RESP=$(curl -fsSL "https://www.duckdns.org/update?domains=${DUCK_DOMAIN}&token=${DUCK_TOKEN}&ip=" || true)
echo "   duckdns 응답: $RESP  (OK 면 성공)"
if [ "$RESP" != "OK" ]; then
  echo "❌ DuckDNS 갱신 실패. 도메인/토큰을 다시 확인하세요."
  exit 1
fi

# ------------------------------------------------------------
# 2) 필요한 패키지 설치 (nginx, certbot)
# ------------------------------------------------------------
echo "==> 2. nginx / certbot 설치"
export DEBIAN_FRONTEND=noninteractive
apt-get update -y >/dev/null
apt-get install -y nginx certbot python3-certbot-nginx >/dev/null

# ------------------------------------------------------------
# 3) 프론트/백엔드가 같은 도메인(https)으로 동작하도록 재빌드
#    - 프론트 API 주소를 https://FQDN/api 로 박아서 빌드
#    - 같은 출처이므로 CORS 문제 없음
# ------------------------------------------------------------
echo "==> 3. .env 갱신 후 컨테이너 재빌드 (API=https://$FQDN)"
cat > .env <<ENVEOF
NEXT_PUBLIC_API_BASE=https://$FQDN
PUBLIC_API_BASE_URL=https://$FQDN
FRONTEND_BASE_URL=https://$FQDN
ALLOWED_ORIGINS=https://$FQDN
ENVEOF
docker compose up -d --build

# ------------------------------------------------------------
# 4) nginx 리버스 프록시 설정
#    /      -> 프론트(3000)
#    /api/  -> 백엔드(8080)
# ------------------------------------------------------------
echo "==> 4. nginx 리버스 프록시 설정"
cat > /etc/nginx/sites-available/mcm <<NGINXEOF
server {
    listen 80;
    server_name $FQDN;

    client_max_body_size 50M;

    # 백엔드 API
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    # 프론트 (Next.js)
    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
NGINXEOF

ln -sf /etc/nginx/sites-available/mcm /etc/nginx/sites-enabled/mcm
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl restart nginx

# ------------------------------------------------------------
# 5) Let's Encrypt 인증서 발급 + nginx 에 https 자동 적용
# ------------------------------------------------------------
echo "==> 5. Let's Encrypt 인증서 발급"
certbot --nginx -d "$FQDN" \
  --non-interactive --agree-tos \
  -m "admin@$FQDN" \
  --redirect

echo ""
echo "======================================================"
echo "✅ 고정 HTTPS 배포 완료 (카메라 사용 가능)"
echo ""
echo "  🔒 서비스 주소 (제출/QR 에 사용):"
echo "     https://$FQDN"
echo ""
echo "  🔌 백엔드 헬스체크:"
echo "     https://$FQDN/api/health"
echo ""
echo "  ℹ️ 이 주소는 고정입니다. 재부팅해도 안 바뀝니다."
echo "     인증서는 certbot 이 자동 갱신합니다."
echo "======================================================"
