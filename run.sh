#!/usr/bin/env bash
# ============================================================
# Common Member Management — 서비스 관리 스크립트 (Bash)
# ============================================================
set -euo pipefail

# ─── 프로젝트 루트 경로 (스크립트 위치 기준) ───
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ─── 환경 설정 ───
# JAVA_HOME이 설정되어 있지 않으면 기본값 사용
if [ -z "${JAVA_HOME:-}" ]; then
  if [ -d "$HOME/.jdks/ms-21.0.10" ]; then
    export JAVA_HOME="$HOME/.jdks/ms-21.0.10"
  fi
fi

# ─── 색상 코드 ───
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# ─── 유틸리티 함수 ───
print_header() {
  clear
  echo -e "${CYAN}${BOLD}"
  echo "╔════════════════════════════════════════════════════╗"
  echo "║       Common Member Management — 서비스 관리       ║"
  echo "╚════════════════════════════════════════════════════╝"
  echo -e "${NC}"
}

print_menu() {
  echo -e "${BOLD}  서비스 제어${NC}"
  echo -e "  ${YELLOW}1${NC}) 모든 서비스 종료 (백엔드 + 프론트 + DB)"
  echo ""
  echo -e "${BOLD}  DB${NC}"
  echo -e "  ${YELLOW}2${NC}) DB 서비스 실행 (Docker)"
  echo -e "  ${YELLOW}3${NC}) DB 초기화 (볼륨 삭제 → 스키마/시드 재실행)"
  echo ""
  echo -e "${BOLD}  백엔드${NC}"
  echo -e "  ${YELLOW}4${NC}) 백엔드 빌드 + 실행 (java -jar)"
  echo -e "  ${YELLOW}5${NC}) 백엔드 실행 (Docker)"
  echo ""
  echo -e "${BOLD}  프론트엔드${NC}"
  echo -e "  ${YELLOW}6${NC}) 프론트엔드 실행 (npm run dev)"
  echo -e "  ${YELLOW}7${NC}) 프론트엔드 실행 (Docker) ${RED}[미구현]${NC}"
  echo ""
  echo -e "${BOLD}  전체 실행${NC}"
  echo -e "  ${YELLOW}8${NC}) 전체 실행 — java -jar + npm 방식"
  echo -e "  ${YELLOW}9${NC}) 전체 실행 — Docker 방식"
  echo ""
  echo -e "  ${YELLOW}0${NC}) 종료"
  echo ""
}

info()    { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; }
section() { echo -e "\n${BLUE}── $1 ──${NC}"; }

press_enter() {
  echo ""
  read -rp "Enter 키를 누르면 메뉴로 돌아갑니다..."
}

# ─── 서비스 함수 ───

# 모든 서비스 종료
stop_all() {
  section "모든 서비스 종료"

  # Docker 컨테이너 종료
  info "Docker 컨테이너 종료 중..."
  docker compose --profile full down 2>/dev/null || true

  # 로컬 백엔드 프로세스 종료 (java -jar)
  info "백엔드 Java 프로세스 종료 중..."
  pkill -f "admin-api.*\.jar" 2>/dev/null && info "admin-api 종료됨" || true
  pkill -f "user-api.*\.jar" 2>/dev/null  && info "user-api 종료됨"  || true

  # 로컬 프론트엔드 프로세스 종료 (next dev)
  info "프론트엔드 프로세스 종료 중..."
  pkill -f "next dev.*3010" 2>/dev/null && info "admin frontend 종료됨" || true
  pkill -f "next dev.*3020" 2>/dev/null && info "user frontend 종료됨"  || true

  info "모든 서비스가 종료되었습니다."
}

# DB + Redis 도커 실행
start_db() {
  section "DB + Redis 서비스 실행 (Docker)"
  info "MySQL (13306) + Redis (6379) 컨테이너 시작..."
  docker compose up -d db redis
  info "DB + Redis 서비스가 시작되었습니다."
  info "MySQL: localhost:13306  |  Redis: localhost:6379"
}

# DB 초기화 (볼륨 삭제 → 스키마/시드 재실행)
reset_db() {
  section "DB 초기화"

  warn "DB 볼륨을 삭제하고 스키마/시드 데이터를 재실행합니다."
  warn "기존 데이터가 모두 삭제됩니다!"
  echo ""
  read -rp "  계속하시겠습니까? (y/N): " confirm
  if [[ ! "$confirm" =~ ^[yY]$ ]]; then
    info "취소되었습니다."
    return 0
  fi

  # 백엔드 프로세스 종료 (DB 초기화 시 캐시 불일치 방지)
  info "백엔드 Java 프로세스 종료 중..."
  pkill -f "admin-api.*\.jar" 2>/dev/null && info "admin-api 종료됨" || true
  pkill -f "user-api.*\.jar" 2>/dev/null  && info "user-api 종료됨"  || true

  # DB + Redis 컨테이너 종료 및 볼륨 삭제
  info "DB 컨테이너 종료 및 볼륨 삭제 중..."
  docker compose down db redis -v 2>/dev/null || true

  # DB + Redis 재시작 (init SQL 자동 실행)
  info "DB + Redis 컨테이너 재시작 중..."
  docker compose up -d db redis

  info "DB 초기화 대기 (15초)..."
  sleep 15

  info "DB 초기화 완료 — 스키마 및 시드 데이터가 재적용되었습니다."
  info "MySQL: localhost:13306  |  Redis: localhost:6379"
  warn "백엔드가 종료되었습니다. 메뉴 4번으로 재시작하세요."
}

# 백엔드 빌드 + java -jar 실행
start_backend_jar() {
  section "백엔드 서비스 빌드 + 실행 (java -jar)"

  # JAVA_HOME 확인
  if [ -z "${JAVA_HOME:-}" ]; then
    error "JAVA_HOME이 설정되지 않았습니다."
    return 1
  fi
  info "JAVA_HOME: $JAVA_HOME"

  # DB가 실행 중인지 확인
  if ! docker ps --format '{{.Names}}' | grep -q "member-mange-db"; then
    warn "DB 컨테이너가 실행 중이 아닙니다. 먼저 시작합니다..."
    start_db
    info "DB 초기화 대기 (10초)..."
    sleep 10
  fi

  # Gradle 빌드
  info "Gradle 빌드 시작..."
  cd "$SCRIPT_DIR/backend"
  ./gradlew clean build -x test
  cd "$SCRIPT_DIR"

  # 기존 백엔드 프로세스 종료
  pkill -f "admin-api.*\.jar" 2>/dev/null || true
  pkill -f "user-api.*\.jar" 2>/dev/null  || true

  # admin-api 실행 (백그라운드)
  info "admin-api 시작 (포트 5000)..."
  "$JAVA_HOME/bin/java" -jar backend/admin-api/build/libs/*.jar &

  # user-api 실행 (백그라운드)
  info "user-api 시작 (포트 6100)..."
  "$JAVA_HOME/bin/java" -jar backend/user-api/build/libs/*.jar &

  info "백엔드 서비스 시작 완료"
  info "Admin API: http://localhost:5000  |  User API: http://localhost:6100"
}

# 백엔드 도커 실행
start_backend_docker() {
  section "백엔드 서비스 실행 (Docker)"
  info "admin-api (5000) + user-api (6100) 컨테이너 빌드 및 시작..."
  docker compose --profile full up -d --build admin-api user-api db redis
  info "백엔드 Docker 서비스 시작 완료"
  info "Admin API: http://localhost:5000  |  User API: http://localhost:6100"
}

# 프론트엔드 npm 실행
start_frontend_npm() {
  section "프론트엔드 서비스 실행 (npm run dev)"

  # 기존 프론트엔드 프로세스 종료
  pkill -f "next dev.*3010" 2>/dev/null || true
  pkill -f "next dev.*3020" 2>/dev/null || true

  # admin frontend
  info "admin frontend 시작 (포트 3010)..."
  cd "$SCRIPT_DIR/frontend/admin"
  npm run dev &

  # user frontend
  info "user frontend 시작 (포트 3020)..."
  cd "$SCRIPT_DIR/frontend/user"
  npm run dev &

  cd "$SCRIPT_DIR"

  info "프론트엔드 서비스 시작 완료"
  info "Admin: http://localhost:3010  |  User: http://localhost:3020"
}

# 프론트엔드 도커 실행 (미구현)
start_frontend_docker() {
  section "프론트엔드 서비스 실행 (Docker)"
  warn "프론트엔드 Dockerfile이 아직 구현되지 않았습니다."
  warn "frontend/admin/Dockerfile 및 frontend/user/Dockerfile 생성 후 사용 가능합니다."
}

# 전체 실행 — java -jar + npm 방식
start_all_local() {
  section "전체 서비스 실행 (java -jar + npm 방식)"
  start_db
  info "DB 초기화 대기 (10초)..."
  sleep 10
  start_backend_jar
  info "백엔드 시작 대기 (5초)..."
  sleep 5
  start_frontend_npm
  echo ""
  info "═══════════════════════════════════════════════"
  info "  전체 서비스 실행 완료!"
  info "  Admin API   : http://localhost:5000"
  info "  User  API   : http://localhost:6100"
  info "  Admin Front : http://localhost:3010"
  info "  User  Front : http://localhost:3020"
  info "  MySQL       : localhost:13306"
  info "  Redis       : localhost:6379"
  info "═══════════════════════════════════════════════"
}

# 전체 실행 — Docker 방식
start_all_docker() {
  section "전체 서비스 실행 (Docker 방식)"
  info "모든 서비스 Docker 컨테이너 빌드 및 시작..."
  docker compose --profile full up -d --build
  echo ""
  info "═══════════════════════════════════════════════"
  info "  Docker 전체 서비스 실행 완료!"
  info "  Admin API   : http://localhost:5000"
  info "  User  API   : http://localhost:6100"
  info "  MySQL       : localhost:13306"
  info "  Redis       : localhost:6379"
  warn "  프론트엔드는 Docker 미지원 — npm run dev 사용 필요"
  info "═══════════════════════════════════════════════"
}

# ─── 메인 루프 ───
while true; do
  print_header
  print_menu

  read -rp "  선택 [0-9]: " choice

  case "$choice" in
    1) stop_all ;;
    2) start_db ;;
    3) reset_db ;;
    4) start_backend_jar ;;
    5) start_backend_docker ;;
    6) start_frontend_npm ;;
    7) start_frontend_docker ;;
    8) start_all_local ;;
    9) start_all_docker ;;
    0)
      echo ""
      info "스크립트를 종료합니다."
      exit 0
      ;;
    *)
      warn "잘못된 선택입니다. 0~9 사이의 숫자를 입력해주세요."
      ;;
  esac

  press_enter
done
