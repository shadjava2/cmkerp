#!/usr/bin/env bash
# Déploiement / mise à jour cmkerp-gateway (+ Redis) sur serveur multi-apps Docker.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

COMPOSE=(docker compose -f "$ROOT/docker-compose.yml" --env-file "$ROOT/.env")
RED=$'\033[0;31m'
GRN=$'\033[0;32m'
YLW=$'\033[1;33m'
NC=$'\033[0m'

die()  { echo "${RED}✖ $*${NC}" >&2; exit 1; }
ok()   { echo "${GRN}✔ $*${NC}"; }
info() { echo "${YLW}→ $*${NC}"; }

env_get() {
  # Lit une clé du .env sans interpréter &, *, ?, etc.
  local key="$1"
  local line
  line="$(grep -E "^${key}=" "$ROOT/.env" | tail -n1 || true)"
  [[ -n "$line" ]] || { echo ""; return 0; }
  local val="${line#*=}"
  val="${val%$'\r'}"
  if [[ "$val" == \"*\" ]]; then
    val="${val:1:${#val}-2}"
  elif [[ "$val" == \'*\' ]]; then
    val="${val:1:${#val}-2}"
  fi
  printf '%s' "$val"
}

need_env() {
  [[ -f "$ROOT/.env" ]] || die "Créer .env : cp .env.deploy.example .env && nano .env"
  export SPRING_PROFILES_ACTIVE="$(env_get SPRING_PROFILES_ACTIVE)"
  export GATEWAY_HOST_PORT="$(env_get GATEWAY_HOST_PORT)"
  export JAVA_OPTS="$(env_get JAVA_OPTS)"
  export CMK_PRIMARY_DB_URL="$(env_get CMK_PRIMARY_DB_URL)"
  export CMK_PRIMARY_DB_USER="$(env_get CMK_PRIMARY_DB_USER)"
  export CMK_PRIMARY_DB_PASSWORD="$(env_get CMK_PRIMARY_DB_PASSWORD)"
  export CMK_JWT_SECRET="$(env_get CMK_JWT_SECRET)"
  export MAIL_PASSWORD="$(env_get MAIL_PASSWORD)"
  [[ -n "${CMK_PRIMARY_DB_URL}" ]] || die "CMK_PRIMARY_DB_URL manquant dans .env"
  [[ -n "${CMK_PRIMARY_DB_USER}" ]] || die "CMK_PRIMARY_DB_USER manquant dans .env"
  [[ -n "${CMK_PRIMARY_DB_PASSWORD}" ]] || die "CMK_PRIMARY_DB_PASSWORD manquant dans .env"
  [[ -n "${CMK_JWT_SECRET}" ]] || die "CMK_JWT_SECRET manquant dans .env"
}

ensure_docker() {
  command -v docker >/dev/null || die "docker manquant"
  docker info >/dev/null 2>&1 || die "Docker ne répond pas (daemon / groupe docker)"
  docker compose version >/dev/null 2>&1 || die "Plugin docker compose requis"
}

ensure_network() {
  if ! docker network inspect cmk-console-net >/dev/null 2>&1; then
    die "Réseau cmk-console-net absent. Démarre d'abord la console: cd /opt/cmk-console && ./scripts/deploy.sh up"
  fi
}

cmd_help() {
  cat <<'EOF'
  ./scripts/deploy.sh install   # 1ère fois (.env + build + up)
  ./scripts/deploy.sh update    # git pull + rebuild + recreate
  ./scripts/deploy.sh up|down|status|logs|test|build

Cloudflare: api.cmkerp.com → http://cmk-gateway:8999
EOF
}

cmd_install() {
  ensure_docker
  if [[ ! -f "$ROOT/.env" ]]; then
    cp "$ROOT/.env.deploy.example" "$ROOT/.env"
    info "Édite .env (DB + JWT) puis: ./scripts/deploy.sh install"
    exit 0
  fi
  need_env
  ensure_network
  "${COMPOSE[@]}" build --pull
  "${COMPOSE[@]}" up -d --remove-orphans
  ok "Gateway démarré"
  cmd_status
  info "Attends ~1-3 min le boot Spring, puis: ./scripts/deploy.sh test"
}

cmd_update() {
  ensure_docker
  need_env
  ensure_network
  if git -C "$ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    BRANCH="$(git -C "$ROOT" rev-parse --abbrev-ref HEAD)"
    info "git pull origin $BRANCH"
    git -C "$ROOT" pull --ff-only origin "$BRANCH" || die "git pull échoué"
  fi
  "${COMPOSE[@]}" build
  "${COMPOSE[@]}" up -d --force-recreate --remove-orphans
  ok "Mise à jour déployée"
  cmd_test
}

cmd_up() {
  ensure_docker; need_env; ensure_network
  "${COMPOSE[@]}" up -d --remove-orphans
  cmd_status
}

cmd_down() {
  ensure_docker
  [[ -f "$ROOT/.env" ]] || touch "$ROOT/.env"
  "${COMPOSE[@]}" down
  ok "down OK (réseau console conservé)"
}

cmd_build() {
  ensure_docker; need_env
  "${COMPOSE[@]}" build --pull
}

cmd_status() {
  ensure_docker
  docker ps -a --filter "name=cmk-gateway" --filter "name=cmk-redis" \
    --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
}

cmd_logs() {
  ensure_docker; need_env
  "${COMPOSE[@]}" logs -f --tail=150
}

cmd_test() {
  ensure_docker; need_env
  local fail=0 port="${GATEWAY_HOST_PORT:-8999}"
  cmd_status
  echo
  local code
  code="$(curl -sS -o /dev/null -w '%{http_code}' --connect-timeout 5 --max-time 20 \
    "http://127.0.0.1:${port}/cmkerp-gateway/actuator/health" || echo 000)"
  if [[ "$code" == "200" ]]; then
    ok "Gateway local actuator → HTTP $code"
  else
    echo "${RED}✖ Gateway local → HTTP $code (http://127.0.0.1:${port}/cmkerp-gateway/actuator/health)${NC}"
    fail=1
  fi
  code="$(curl -sS -o /dev/null -w '%{http_code}' --connect-timeout 8 --max-time 20 \
    "https://api.cmkerp.com/cmkerp-gateway/actuator/health" || echo 000)"
  case "$code" in
    200|401|403) ok "api.cmkerp.com actuator → HTTP $code" ;;
    *) echo "${RED}✖ api.cmkerp.com → HTTP $code (CF hostname = http://cmk-gateway:8999 ?)${NC}"; fail=1 ;;
  esac
  [[ "$fail" -eq 0 ]] || die "Au moins un check a échoué"
  ok "Checks OK"
}

main() {
  case "${1:-help}" in
    install) cmd_install ;;
    update) cmd_update ;;
    up) cmd_up ;;
    down) cmd_down ;;
    build) cmd_build ;;
    status) cmd_status ;;
    logs) cmd_logs ;;
    test) cmd_test ;;
    help|-h|--help) cmd_help ;;
    *) die "Commande inconnue: $1" ;;
  esac
}
main "$@"
