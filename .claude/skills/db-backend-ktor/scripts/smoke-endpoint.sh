#!/usr/bin/env bash
# Generic smoke harness for any protected endpoint on the Scent backend.
# Bootstraps a fresh user (register) to obtain a token, then runs the standard
# auth negative-case trio plus an authenticated request against the target route.
#
# Requirements: curl, jq
# Usage:
#   bash smoke-endpoint.sh GET  /api/v1/posts
#   bash smoke-endpoint.sh POST /api/v1/posts '{"textContent":"hi","fragranceIds":["1"]}'
#   BASE=https://your-app.onrender.com bash smoke-endpoint.sh GET /api/v1/auth/me
#
# Expected: authenticated call returns EXPECT (default 200); unauthenticated
# variants return 401. Override with EXPECT=201 etc.
set -euo pipefail

METHOD="${1:?usage: smoke-endpoint.sh METHOD PATH [JSON_BODY]}"
ROUTE="${2:?usage: smoke-endpoint.sh METHOD PATH [JSON_BODY]}"
BODY="${3:-}"
BASE="${BASE:-http://localhost:8080}"
EXPECT="${EXPECT:-200}"
MAXTIME="${MAXTIME:-90}"

pass=0; fail=0
check() { # check <label> <expected> <actual>
  if [ "$2" = "$3" ]; then echo "  ✔ $1 ($3)"; pass=$((pass+1));
  else echo "  ✘ $1 — expected $2, got $3"; fail=$((fail+1)); fi
}

req() { # req <token-or-empty> -> prints http_code, body saved to /tmp/smoke-body.json
  local auth=()
  [ -n "$1" ] && auth=(-H "Authorization: Bearer $1")
  local data=()
  [ -n "$BODY" ] && data=(-H "Content-Type: application/json" -d "$BODY")
  curl -s --max-time "$MAXTIME" -o /tmp/smoke-body.json -w "%{http_code}" \
    -X "$METHOD" "${auth[@]}" "${data[@]}" "$BASE$ROUTE"
}

echo "== Target: $METHOD $BASE$ROUTE"

echo "-- bootstrap user"
STAMP=$(date +%s)
REG=$(curl -sf --max-time "$MAXTIME" -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"smoke${STAMP}\",\"email\":\"smoke${STAMP}@test.com\",\"password\":\"password123\"}")
TOKEN=$(echo "$REG" | jq -r .token)
[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] || { echo "  ✘ could not obtain token: $REG"; exit 1; }
echo "  ✔ token obtained"

echo "-- authenticated request"
C=$(req "$TOKEN")
check "authenticated $METHOD $ROUTE" "$EXPECT" "$C"
echo "  response: $(head -c 300 /tmp/smoke-body.json)"

echo "-- negative cases"
C=$(req "")
check "no token rejected" 401 "$C"

C=$(req "not.a.real.token")
check "garbage token rejected" 401 "$C"

# Cross-user authorization check (only meaningful for user-scoped resources):
# bootstrap a SECOND user and hit the same route — for user-scoped GETs this
# should NOT return the first user's data. We can only assert it doesn't 5xx;
# review the output manually for ownership routes.
REG2=$(curl -sf --max-time "$MAXTIME" -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"smokeb${STAMP}\",\"email\":\"smokeb${STAMP}@test.com\",\"password\":\"password123\"}")
TOKEN2=$(echo "$REG2" | jq -r .token)
C=$(req "$TOKEN2")
if [ "${C:0:1}" != "5" ]; then echo "  ✔ second user's token handled without server error ($C)"; pass=$((pass+1));
else echo "  ✘ second user's token caused $C"; fail=$((fail+1)); fi
echo "  second-user response (verify no data leakage for owner-scoped routes):"
echo "  $(head -c 300 /tmp/smoke-body.json)"

echo
echo "== $pass passed, $fail failed"
[ "$fail" -eq 0 ]
