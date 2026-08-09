#!/usr/bin/env bash
# Scent dev-loop gate runner.
#
# Runs the mechanical quality gates and records the outcome to
# .claude/.scent-gates.json so loop state lives on disk rather than in
# conversation context (which compaction can drop mid-loop).
#
#   ./gates.sh                     run compile + static analysis + tests
#   ./gates.sh --fast              same, but skip the full `allTests` sweep
#   ./gates.sh --gate2 pass "note" record the architecture review verdict
#   ./gates.sh --status            print current state, exit 0 if all green
#
# Exit 0 = every recorded gate is green. Exit 1 = something failed.

set -uo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT" || exit 1
STATE=".claude/.scent-gates.json"
mkdir -p .claude

py() { command -v python3 >/dev/null && python3 "$@"; }

read_state() {
  [ -f "$STATE" ] || echo '{}' > "$STATE"
  cat "$STATE"
}

set_field() { # set_field <key> <json-value>
  py - "$STATE" "$1" "$2" <<'EOF'
import json, sys, os
path, key, val = sys.argv[1], sys.argv[2], sys.argv[3]
d = json.load(open(path)) if os.path.getsize(path) else {}
try:    d[key] = json.loads(val)
except Exception: d[key] = val
json.dump(d, open(path, "w"), indent=2)
EOF
}

bump_iteration() {
  py - "$STATE" <<'EOF'
import json, sys, os, datetime
path = sys.argv[1]
d = json.load(open(path)) if os.path.getsize(path) else {}
d["iteration"] = int(d.get("iteration", 0)) + 1
d["updated_at"] = datetime.datetime.now().isoformat(timespec="seconds")
json.dump(d, open(path, "w"), indent=2)
print(d["iteration"])
EOF
}

# ---------------------------------------------------------------- subcommands

if [ "${1:-}" = "--gate2" ]; then
  set_field gate2 "\"${2:-fail}\""
  set_field gate2_notes "\"${3:-}\""
  echo "gate2 recorded: ${2:-fail}"
  exit 0
fi

if [ "${1:-}" = "--status" ]; then
  read_state
  py - "$STATE" <<'EOF'
import json, sys, os
d = json.load(open(sys.argv[1])) if os.path.getsize(sys.argv[1]) else {}
gates = ["gate1", "gate2", "gate3", "gate4"]
sys.exit(0 if all(d.get(g) == "pass" for g in gates) else 1)
EOF
  exit $?
fi

# ------------------------------------------------------------------ full run

FAST=0
[ "${1:-}" = "--fast" ] && FAST=1

ITER=$(bump_iteration)
echo "=== Scent gates — iteration $ITER ==="

fail() { set_field "$1" '"fail"'; set_field last_failure "\"$2\""; echo "FAIL: $2"; exit 1; }

echo "--- Gate 1: compile"
if ! ./gradlew --quiet :shared:compileKotlinJvm :composeApp:compileDebugKotlin; then
  fail gate1 "compile"
fi
set_field gate1 '"pass"'

echo "--- Gate 3: ktlint + detekt"
./gradlew --quiet ktlintFormat >/dev/null 2>&1
if ! ./gradlew --quiet ktlintCheck detekt; then
  fail gate3 "ktlint/detekt"
fi
set_field gate3 '"pass"'

echo "--- Gate 4: unit tests"
if [ "$FAST" = "1" ]; then
  TEST_TASKS=":shared:jvmTest :composeApp:testDebugUnitTest"
else
  TEST_TASKS="allTests"
fi
if ! ./gradlew --quiet $TEST_TASKS; then
  fail gate4 "unit tests ($TEST_TASKS)"
fi
set_field gate4 '"pass"'
set_field tests_scope "\"$TEST_TASKS\""

set_field last_failure '""'
[ "$FAST" = "0" ] && set_field gate5 '"pass"' || set_field gate5 '"partial"'

echo
echo "=== gates green (iteration $ITER) ==="
[ "$FAST" = "1" ] && echo "NOTE: --fast skipped the full allTests sweep; Gate 5 is not satisfied and push will still be blocked."
cat "$STATE"
