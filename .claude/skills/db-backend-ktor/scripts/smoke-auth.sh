#!/usr/bin/env bash
# Smoke-test the Scent auth endpoints (register → login → /me + negative cases).
# Requirements: curl, jq
# Usage:
#   bash smoke-auth.sh                                     # local (http://localhost:8080)
#   BASE=https://your-app.onrender.com bash smoke-auth.sh  # staging (Render)
set -euo pipefail

BASE="${BASE:-http://localhost:8080}/api/v1/auth"
STAMP=$(date +%s)
EMAIL="smoke${STAMP}@test.com"
USERNAME="smoke${STAMP}"
PASSWORD="password123"
MAXTIME="${MAXTIME:-90}"   # generous for Render free-tier cold starts

pass=0; fail=0
check() { # check <label> <expected_code> <actual_code>
  if [ "$2" = "$3" ]; then echo "  ✔ $1 ($3)"; pass=$((pass+1));
  else echo "  ✘ $1 — expected $2, got $3"; fail=$((fail+1)); fi
}

echo "== Target: $BASE"

echo "-- register"
REG=$(curl -sf --max-time "$MAXTIME" -X POST "$BASE/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
TOKEN=$(echo "$REG" | jq -r .token)
[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] && { echo "  ✔ token returned"; pass=$((pass+1)); } \
  || { echo "  ✘ no token in register response: $REG"; fail=$((fail+1)); }

echo "-- login"
LOGIN_CODE=$(curl -s -o /tmp/login.json -w "%{http_code}" -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
check "login with correct password" 200 "$LOGIN_CODE"

echo "-- /me (protected)"
ME=$(curl -s -o /tmp/me.json -w "%{http_code}" "$BASE/me" -H "Authorization: Bearer $TOKEN")
check "/me with valid token" 200 "$ME"
ME_EMAIL=$(jq -r '.email // empty' /tmp/me.json)
if [ "$ME_EMAIL" = "$EMAIL" ] || [ -z "$ME_EMAIL" ]; then
  echo "  ✔ /me returned expected user (or no email field to compare)"
else
  echo "  ✘ /me returned a different user: $ME_EMAIL"; fail=$((fail+1))
fi

echo "-- token payload sanity"
PAYLOAD=$(echo "$TOKEN" | cut -d. -f2 | tr '_-' '/+' | base64 -d 2>/dev/null || true)
echo "  payload: $PAYLOAD"
echo "$PAYLOAD" | jq -e '.userId' >/dev/null 2>&1 \
  && { echo "  ✔ userId claim present"; pass=$((pass+1)); } \
  || { echo "  ✘ userId claim missing"; fail=$((fail+1)); }

echo "-- negative cases"
C=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"wrongpass\"}")
check "wrong password rejected" 401 "$C"

C=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/me")
check "no token rejected" 401 "$C"

C=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/me" -H "Authorization: Bearer not.a.real.token")
check "garbage token rejected" 401 "$C"

C=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"dupe$STAMP\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
# accept either 409 or 400 depending on how the route reports duplicates
if [ "$C" = "409" ] || [ "$C" = "400" ]; then echo "  ✔ duplicate email rejected ($C)"; pass=$((pass+1));
else echo "  ✘ duplicate email — expected 409/400, got $C"; fail=$((fail+1)); fi

echo
echo "== $pass passed, $fail failed"
[ "$fail" -eq 0 ]
