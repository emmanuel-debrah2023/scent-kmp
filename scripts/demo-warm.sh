#!/bin/bash
# Wakes a sleeping Render instance before a demo and reports how long it took, then checks the
# assistant is configured. Run it a few minutes before the demo: a free instance sleeps again after
# about 15 minutes with no traffic, so run it again after a long gap.
#
# Usage: ./scripts/demo-warm.sh https://<your-app>.onrender.com
#
# The health check never calls the model, so it cannot tell a retired model from a working one. For
# that, ask once in the app and look for "Ask-back falling back to rules" in the server log.

set -uo pipefail

BASE_URL="${1:-}"
MAX_WAIT_S=180

[ -n "$BASE_URL" ] || { echo "usage: $0 <base-url>" >&2; exit 2; }
BASE_URL="${BASE_URL%/}"

start=$SECONDS
until [ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$BASE_URL/")" = "200" ]; do
    if [ $((SECONDS - start)) -ge "$MAX_WAIT_S" ]; then
        echo "demo-warm: no answer from $BASE_URL within ${MAX_WAIT_S}s" >&2
        exit 1
    fi
    sleep 2
done
echo "demo-warm: up after $((SECONDS - start))s"

health=$(curl -s --max-time 20 "$BASE_URL/api/v1/agent/health")
echo "demo-warm: agent health $health"
case "$health" in
    *'"status":"configured"'*) ;;
    *) echo "demo-warm: the assistant is not configured, so asks will use the rules answer" >&2; exit 1 ;;
esac
