#!/usr/bin/env bash
# PreToolUse hook (matcher: Bash).
#
# Blocks `git push` unless .claude/.scent-gates.json shows every gate green AND
# is newer than every tracked Kotlin source file. This is the deterministic
# version of "do not push until Gate 5 passes" — prose can be talked out of; a
# non-zero exit cannot.
#
# Exit 2 = deny the tool call, stderr goes back to Claude as the reason.

set -uo pipefail
INPUT=$(cat)

CMD=$(python3 -c '
import json,sys
try: print(json.load(sys.stdin).get("tool_input",{}).get("command",""))
except Exception: print("")
' <<< "$INPUT")

# Only interested in pushes.
case "$CMD" in
  *"git push"*|*"gh pr create"*) ;;
  *) exit 0 ;;
esac

# Escape hatch for genuine emergencies — deliberately noisy and explicit.
if [[ "$CMD" == *"SCENT_SKIP_GATES=1"* ]]; then
  echo "WARNING: pushing with gates bypassed." >&2
  exit 0
fi

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
STATE="$REPO_ROOT/.claude/.scent-gates.json"

if [ ! -f "$STATE" ]; then
  echo "BLOCKED: no gate record found. Run .claude/skills/scent-dev-loop/scripts/gates.sh and get all gates green before pushing." >&2
  exit 2
fi

VERDICT=$(python3 - "$STATE" <<'EOF'
import json, sys, os
d = json.load(open(sys.argv[1])) if os.path.getsize(sys.argv[1]) else {}
missing = [g for g in ("gate1","gate2","gate3","gate4","gate5") if d.get(g) != "pass"]
if missing:
    print("NOT_GREEN " + ",".join(missing) + " | last_failure=" + str(d.get("last_failure","")))
else:
    print("GREEN")
EOF
)

if [[ "$VERDICT" != "GREEN" ]]; then
  echo "BLOCKED: gates not green -> $VERDICT" >&2
  echo "Fix the failure, restart from Gate 1, and re-run gates.sh (full, not --fast)." >&2
  exit 2
fi

# Freshness: any Kotlin file edited after the gate run invalidates it.
NEWER=$(find "$REPO_ROOT" -name '*.kt' -newer "$STATE" \
        -not -path '*/build/*' -not -path '*/.git/*' 2>/dev/null | head -5)

if [ -n "$NEWER" ]; then
  echo "BLOCKED: source files changed after the last green gate run:" >&2
  echo "$NEWER" >&2
  echo "Re-run gates.sh before pushing." >&2
  exit 2
fi

exit 0
