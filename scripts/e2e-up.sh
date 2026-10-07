#!/bin/bash
# Brings up the local E2E stack: Postgres, the Ktor server with dev routes mounted
# (Flyway migrations run as it starts), and seeded data (E2E account, feed, listings).
# Anything already running is reused; only what's missing is started.
#
# Usage: ./scripts/e2e-up.sh          start or reuse the stack, then seed it
#        ./scripts/e2e-up.sh --down   stop a server this script started
#
# Overrides: E2E_API_URL, E2E_PG_CONTAINER, E2E_EMAIL, E2E_PASSWORD, E2E_SEED_COUNT, E2E_EMULATOR_HOST

set -uo pipefail
cd "$(git rev-parse --show-toplevel)"

API_URL="${E2E_API_URL:-http://localhost:8080}"
EMAIL="${E2E_EMAIL:-e2e@scent.dev}"
PASSWORD="${E2E_PASSWORD:-ScentE2e-Passw0rd}"
SEED_COUNT="${E2E_SEED_COUNT:-10}"
EMULATOR_HOST="${E2E_EMULATOR_HOST:-10.0.2.2:8080}"
PID_FILE="build/e2e-server.pid"
LOG_FILE="build/e2e-server.log"
SERVER_TIMEOUT_S=180
PG_TIMEOUT_S=30

fail() { echo "e2e-up: $*" >&2; exit 1; }

if [ "${1:-}" = "--down" ]; then
    if [ -f "$PID_FILE" ] && kill "$(cat "$PID_FILE")" 2>/dev/null; then
        echo "e2e-up: stopped the server (pid $(cat "$PID_FILE"))"
    else
        echo "e2e-up: no server started by this script is running"
    fi
    rm -f "$PID_FILE"
    exit 0
fi

# ---- .env ------------------------------------------------------------------
[ -f .env ] || fail "no .env at the repo root. In a worktree, symlink the main checkout's: ln -s <main-checkout>/.env .env"

# ---- Postgres ----------------------------------------------------------------
DB_URL=$(grep -E '^(LOCAL_)?DATABASE_URL=' .env | head -1 | cut -d= -f2-)
DB_HOST=$(echo "$DB_URL" | sed -n 's|.*//\([^:/]*\).*|\1|p'); DB_HOST="${DB_HOST:-localhost}"
DB_PORT=$(echo "$DB_URL" | sed -n 's|.*//[^:/]*:\([0-9]*\).*|\1|p'); DB_PORT="${DB_PORT:-5432}"

pg_reachable() { (exec 3<>"/dev/tcp/$DB_HOST/$DB_PORT") 2>/dev/null; }

if pg_reachable; then
    echo "e2e-up: Postgres already up on $DB_HOST:$DB_PORT"
else
    command -v docker >/dev/null 2>&1 || fail "Postgres isn't reachable on $DB_HOST:$DB_PORT and docker isn't installed"
    CONTAINER="${E2E_PG_CONTAINER:-$(docker ps -a --format '{{.Names}} {{.Image}}' | awk '$2 ~ /^postgres(:|$)/ { print $1; exit }')}"
    [ -n "$CONTAINER" ] || fail "no Postgres container found. Create one per .claude/skills/db-backend-ktor/references/local-postgres.md, matching the credentials in .env"
    echo "e2e-up: starting Postgres container $CONTAINER"
    docker start "$CONTAINER" >/dev/null || fail "could not start container $CONTAINER"
    for _ in $(seq 1 "$PG_TIMEOUT_S"); do pg_reachable && break; sleep 1; done
    pg_reachable || fail "Postgres didn't accept connections on $DB_HOST:$DB_PORT within ${PG_TIMEOUT_S}s"
fi

# ---- Server (migrations run on start) ---------------------------------------
server_up() { curl -s -o /dev/null --max-time 2 "$API_URL/"; }

if server_up; then
    echo "e2e-up: server already up at $API_URL (reusing it)"
else
    mkdir -p build
    echo "e2e-up: starting the server with dev routes mounted; log in $LOG_FILE"
    nohup ./gradlew :server:run -DSTREAM_PROVIDER=fake -DIMAGE_PROVIDER=fake >"$LOG_FILE" 2>&1 &
    echo $! >"$PID_FILE"
    for _ in $(seq 1 "$SERVER_TIMEOUT_S"); do
        server_up && break
        kill -0 "$(cat "$PID_FILE")" 2>/dev/null || { tail -20 "$LOG_FILE" >&2; rm -f "$PID_FILE"; fail "the server exited during startup"; }
        sleep 1
    done
    server_up || { tail -20 "$LOG_FILE" >&2; fail "the server didn't answer within ${SERVER_TIMEOUT_S}s"; }
    echo "e2e-up: server up; Flyway migrations applied on startup"
fi

# ---- Seed ------------------------------------------------------------------
post() { # post <path> [json-body] -> prints the HTTP status
    if [ -n "${2:-}" ]; then
        curl -s -o /dev/null -w '%{http_code}' -X POST "$API_URL$1" -H 'Content-Type: application/json' -d "$2"
    else
        curl -s -o /dev/null -w '%{http_code}' -X POST "$API_URL$1"
    fi
}

check_seeded() { # check_seeded <what> <status> <accepted statuses...>
    local what="$1" status="$2"; shift 2
    for ok in "$@"; do [ "$status" = "$ok" ] && { echo "e2e-up: seeded $what (HTTP $status)"; return; }; done
    [ "$status" = "404" ] && fail "dev routes aren't mounted. Restart the server with: ./gradlew :server:run -DSTREAM_PROVIDER=fake -DIMAGE_PROVIDER=fake"
    fail "seeding $what failed with HTTP $status"
}

USER_JSON=$(printf '{"email":"%s","username":"scent_e2e","password":"%s","displayName":"Scent E2E"}' "$EMAIL" "$PASSWORD")
check_seeded "E2E account $EMAIL" "$(post /api/v1/dev/seed-user "$USER_JSON")" 200 201
# The stored video URL takes the request's Host, so send the emulator's address or the app can't reach it.
check_seeded "$SEED_COUNT feed posts and a video post" \
    "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API_URL/api/v1/dev/seed-feed?count=$SEED_COUNT" -H "Host: $EMULATOR_HOST")" 201
check_seeded "$SEED_COUNT listings" "$(post "/api/v1/dev/seed-listings?count=$SEED_COUNT")" 201

echo
echo "E2E stack ready at $API_URL. Sign in as $EMAIL."
echo "Feed posts and listings are additive: each run adds $SEED_COUNT more, plus one video post."
[ -f "$PID_FILE" ] && echo "Stop the server with: ./scripts/e2e-up.sh --down"
