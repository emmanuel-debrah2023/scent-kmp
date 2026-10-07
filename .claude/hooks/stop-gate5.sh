#!/usr/bin/env bash
# Stop hook.
#
# Enforces scent-dev-loop Gate 5 (`./gradlew ktlintCheck detekt allTests`) at
# the end of every turn that left build-relevant changes behind. Exit 2 blocks
# the turn from ending and feeds the failure back to Claude.
#
# Cheap by design:
#   - no Kotlin/Gradle/SQL changes vs main            -> exit 0 immediately
#   - same tree as the last green run (fingerprint)   -> exit 0 immediately
#   - after MAX_BLOCKS consecutive failures on the same tree, let the turn end
#     so a broken environment cannot trap the session in a loop.
#
# SCENT_GATE5_CMD overrides the command (used by test-hooks.sh).

set -uo pipefail
cat >/dev/null # drain the hook payload; nothing in it is needed

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT" || exit 0

CMD="${SCENT_GATE5_CMD:-./gradlew ktlintCheck detekt allTests}"
PASS_FILE=".claude/.scent-gate5-pass"
FAIL_FILE=".claude/.scent-gate5-fails"
MAX_BLOCKS=3
RELEVANT='\.(kt|kts|sql|toml|properties|conf)$|(^|/)(gradlew|gradle\.properties)$|^config/'

BASE=""
for ref in origin/main main; do
  git rev-parse --verify -q "$ref" >/dev/null && { BASE="$ref"; break; }
done

CHANGED=$({
  [ -n "$BASE" ] && git diff --name-only "$BASE"...HEAD
  git diff --name-only HEAD
  git ls-files --others --exclude-standard
} 2>/dev/null | grep -E "$RELEVANT" | sort -u)

[ -z "$CHANGED" ] && exit 0

FINGERPRINT=$({
  git rev-parse HEAD
  git diff HEAD
  git ls-files --others --exclude-standard | grep -E "$RELEVANT" | sort | xargs -I{} sh -c 'echo {}; cat "{}"'
} 2>/dev/null | shasum | cut -d' ' -f1)

if [ -f "$PASS_FILE" ] && [ "$(cat "$PASS_FILE")" = "$FINGERPRINT" ]; then
  exit 0
fi

OUT=$(mktemp)
trap 'rm -f "$OUT"' EXIT

if bash -c "$CMD" >"$OUT" 2>&1; then
  echo "$FINGERPRINT" > "$PASS_FILE"
  rm -f "$FAIL_FILE"
  exit 0
fi

PREV=""; COUNT=0
[ -f "$FAIL_FILE" ] && read -r PREV COUNT < "$FAIL_FILE"
[ "$PREV" = "$FINGERPRINT" ] || COUNT=0
COUNT=$((COUNT + 1))
echo "$FINGERPRINT $COUNT" > "$FAIL_FILE"

if [ "$COUNT" -ge "$MAX_BLOCKS" ]; then
  echo "WARNING: Gate 5 still failing after $COUNT attempts on this tree; letting the turn end. Surface the failure to the user." >&2
  tail -n 40 "$OUT" >&2
  exit 0
fi

{
  echo "BLOCKED: Gate 5 failed ($CMD). Fix it and restart from Gate 1 (attempt $COUNT/$MAX_BLOCKS)."
  tail -n 60 "$OUT"
} >&2
exit 2
