# curl Recipes for API Smoke Testing

Patterns for hand-testing and scripting against the Ktor server.

## Basics

```bash
BASE=http://localhost:8080/api/v1

# POST JSON
curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"a@b.com","password":"password123"}'

# Capture just the status code (for negative-case assertions)
curl -s -o /dev/null -w "%{http_code}\n" $BASE/auth/me

# Fail the script if the request fails (-f) — use for must-succeed calls
curl -sf ...

# Authenticated request
curl -s $BASE/auth/me -H "Authorization: Bearer $TOKEN"
```

## Extracting fields with jq

```bash
TOKEN=$(curl -sf -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d "$BODY" | jq -r .token)

# Guard against jq returning the literal string "null"
[ "$TOKEN" != "null" ] || { echo "no token"; exit 1; }
```

## Decoding a JWT payload without jwt.io

```bash
# JWT base64url → base64: translate _- to /+ first
echo "$TOKEN" | cut -d. -f2 | tr '_-' '/+' | base64 -d | jq .
# Check expiry is ~24h out:
#   .exp - .iat should be 86400
```

## Idempotent test data

Always generate unique identifiers per run so re-running never hits unique
constraints:

```bash
STAMP=$(date +%s)
EMAIL="smoke${STAMP}@test.com"
USERNAME="smoke${STAMP}"
```

## Render free tier (staging)

- First request after idle can take 30–60s. Warm it first:
  `curl -s --max-time 90 https://your-app.onrender.com/ >/dev/null`
- Or set `--max-time 90` on the first real request only; keep subsequent
  requests at a normal timeout so genuine hangs still fail fast.

## Negative-case checklist for any protected route

1. No `Authorization` header → 401
2. `Bearer` + malformed token → 401
3. Valid token, resource owned by another user → 403/404 (authorization test —
   the one most often missed)
4. Expired token → 401 (generate one with a past `exp` using the same secret if
   you need to test this specifically)

## Timing and verbosity

```bash
# Where is the time going? (DNS, connect, TTFB, total)
curl -s -o /dev/null -w "dns:%{time_namelookup} connect:%{time_connect} ttfb:%{time_starttransfer} total:%{time_total}\n" $BASE/...

# Full request/response headers when debugging content-type or auth issues
curl -v ... 2>&1 | grep -E '^[<>]'
```

## httpie / .http files (optional alternatives)

- IntelliJ/Android Studio support `.http` scratch files — good for
  interactive poking; keep the bash scripts as the source of truth for
  repeatable checks since they run in CI and pre-deploy hooks.
