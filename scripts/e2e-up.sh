#!/bin/bash
# Brings up the local E2E stack: Postgres, the Ktor server with dev routes mounted
# (Flyway migrations run as it starts), and seeded data (E2E account, feed, listings).
# Anything already running is reused; only what's missing is started. A server this script
# started from different server code (another branch or worktree) is restarted instead.
#
# Usage: ./scripts/e2e-up.sh                start or reuse the stack, then seed it
#        ./scripts/e2e-up.sh --down         stop the server this script started, from any checkout
#        ./scripts/e2e-up.sh --fingerprint  print this checkout's server-code fingerprint
#
# Overrides: E2E_API_URL, E2E_PG_CONTAINER, E2E_EMAIL, E2E_PASSWORD, E2E_SEED_COUNT, E2E_EMULATOR_HOST,
#            E2E_STATE_DIR (default ~/.cache/scent), E2E_REUSE=always (keep a stale server)

set -uo pipefail
cd "$(git rev-parse --show-toplevel)" || exit 1

API_URL="${E2E_API_URL:-http://localhost:8080}"
EMAIL="${E2E_EMAIL:-e2e@scent.dev}"
PASSWORD="${E2E_PASSWORD:-ScentE2e-Passw0rd}"
SEED_COUNT="${E2E_SEED_COUNT:-10}"
EMULATOR_HOST="${E2E_EMULATOR_HOST:-10.0.2.2:8080}"
LOG_FILE="build/e2e-server.log"
SERVER_TIMEOUT_S=180
STOP_TIMEOUT_S=30
PG_TIMEOUT_S=30

fail() { echo "e2e-up: $*" >&2; exit 1; }

# ---- Server state ------------------------------------------------------------
# One server per port serves every worktree, so its state is machine-wide, not in build/.
API_PORT=$(echo "$API_URL" | sed -n 's|.*://[^/]*:\([0-9]*\).*|\1|p'); API_PORT="${API_PORT:-80}"
STATE_DIR="${E2E_STATE_DIR:-$HOME/.cache/scent}"
STATE_FILE="$STATE_DIR/e2e-server-$API_PORT.env"
# What the server is built from: its module, the modules it depends on, and the root build files.
SERVER_INPUTS=(server contracts shared gradle/libs.versions.toml settings.gradle.kts build.gradle.kts gradle.properties)

state_get() { [ -f "$STATE_FILE" ] && grep -E "^$1=" "$STATE_FILE" | head -1 | cut -d= -f2-; }

listener_pid() { lsof -tiTCP:"$API_PORT" -sTCP:LISTEN 2>/dev/null | head -1; }

# Committed trees plus uncommitted and untracked changes, so a local edit counts as different code.
fingerprint() {
    {
        for input in "${SERVER_INPUTS[@]}"; do git rev-parse "HEAD:$input" 2>/dev/null || echo "none:$input"; done
        git diff HEAD -- "${SERVER_INPUTS[@]}" 2>/dev/null
        git ls-files --others --exclude-standard -z -- "${SERVER_INPUTS[@]}" | xargs -0 shasum 2>/dev/null
    } | shasum | cut -c1-12
}

# The recorded server counts only while that exact process still listens on the port, so a
# reused PID is never killed.
recorded_server_running() {
    local pid
    pid=$(state_get SERVER_PID)
    [ -n "$pid" ] && [ "$(listener_pid)" = "$pid" ]
}

stop_recorded_server() {
    local pid checkout
    pid=$(state_get SERVER_PID)
    checkout=$(state_get CHECKOUT)
    kill "$pid" 2>/dev/null
    for _ in $(seq 1 "$STOP_TIMEOUT_S"); do [ -z "$(listener_pid)" ] && break; sleep 1; done
    if [ -n "$(listener_pid)" ]; then
        kill -9 "$pid" 2>/dev/null
        sleep 1
    fi
    [ -z "$(listener_pid)" ] || fail "the server (pid $pid) didn't stop within ${STOP_TIMEOUT_S}s"
    rm -f "$STATE_FILE"
    echo "e2e-up: stopped the server (pid $pid, started from $checkout)"
}

case "${1:-}" in
    --fingerprint)
        fingerprint
        exit 0
        ;;
    --down)
        if recorded_server_running; then
            stop_recorded_server
        else
            rm -f "$STATE_FILE"
            echo "e2e-up: no server started by this script is running on port $API_PORT"
        fi
        exit 0
        ;;
esac

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

start_server() {
    local gradle_pid
    mkdir -p build "$STATE_DIR"
    echo "e2e-up: starting the server with dev routes mounted; log in $LOG_FILE"
    nohup ./gradlew :server:run -DSTREAM_PROVIDER=fake -DIMAGE_PROVIDER=fake -DASK_BACK_AGENT=fake >"$LOG_FILE" 2>&1 &
    gradle_pid=$!
    for _ in $(seq 1 "$SERVER_TIMEOUT_S"); do
        server_up && break
        kill -0 "$gradle_pid" 2>/dev/null || { tail -20 "$LOG_FILE" >&2; fail "the server exited during startup"; }
        sleep 1
    done
    server_up || { tail -20 "$LOG_FILE" >&2; fail "the server didn't answer within ${SERVER_TIMEOUT_S}s"; }
    # Record the process listening on the port (the server JVM), not the gradlew client.
    printf 'SERVER_PID=%s\nCHECKOUT=%s\nFINGERPRINT=%s\n' "$(listener_pid)" "$PWD" "$FINGERPRINT" >"$STATE_FILE"
    echo "e2e-up: server up; Flyway migrations applied on startup"
}

FINGERPRINT=$(fingerprint)
if ! server_up; then
    rm -f "$STATE_FILE"
    start_server
elif ! recorded_server_running; then
    rm -f "$STATE_FILE"
    echo "e2e-up: server already up at $API_URL but not started by e2e-up.sh, so its code is unknown (reusing it)."
    echo "e2e-up: if it's out of date, stop it and rerun this script so it can track the server."
elif [ "$(state_get FINGERPRINT)" = "$FINGERPRINT" ]; then
    echo "e2e-up: server already up at $API_URL with this checkout's server code (reusing it)"
elif [ "${E2E_REUSE:-}" = "always" ]; then
    echo "e2e-up: warning: the server at $API_URL runs different server code (started from $(state_get CHECKOUT)); reusing it because E2E_REUSE=always"
else
    echo "e2e-up: the server at $API_URL runs different server code (started from $(state_get CHECKOUT)); restarting it"
    stop_recorded_server
    start_server
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
    [ "$status" = "404" ] && fail "dev routes aren't mounted. Restart the server with: ./gradlew :server:run -DSTREAM_PROVIDER=fake -DIMAGE_PROVIDER=fake -DASK_BACK_AGENT=fake"
    fail "seeding $what failed with HTTP $status"
}

USER_JSON=$(printf '{"email":"%s","username":"scent_e2e","password":"%s","displayName":"Scent E2E"}' "$EMAIL" "$PASSWORD")
check_seeded "E2E account $EMAIL" "$(post /api/v1/dev/seed-user "$USER_JSON")" 200 201

# Seeding adds rows, so clear the last run's seed posts and listings first: every start ends with one seed set.
reset_seed() { # reset_seed <what> <path>
    local status
    status=$(post "$2")
    [ "$status" = "200" ] && { echo "e2e-up: cleared previous seed $1"; return; }
    [ "$status" = "404" ] && fail "the seed reset routes aren't mounted: restart the server from a checkout that has them"
    fail "clearing seed $1 failed with HTTP $status"
}
reset_seed "feed posts" /api/v1/dev/reset-seed-feed
reset_seed "listings" /api/v1/dev/reset-seed-listings
# Listings first: seed-feed tags its video post with the newest seeded fragrance (the Home hero).
check_seeded "$SEED_COUNT listings" "$(post "/api/v1/dev/seed-listings?count=$SEED_COUNT")" 201
# The stored video URL takes the request's Host, so send the emulator's address or the app can't reach it.
check_seeded "$SEED_COUNT feed posts and a video post" \
    "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API_URL/api/v1/dev/seed-feed?count=$SEED_COUNT" -H "Host: $EMULATOR_HOST")" 201

echo
echo "E2E stack ready at $API_URL. Sign in as $EMAIL."
echo "Each run replaces the seed data: $SEED_COUNT feed posts plus one video post, and $SEED_COUNT listings."
# An if, not `[ ] && echo`: as the last command, a false test would make a reused server exit 1.
if recorded_server_running; then
    echo "Stop the server with: ./scripts/e2e-up.sh --down"
fi
