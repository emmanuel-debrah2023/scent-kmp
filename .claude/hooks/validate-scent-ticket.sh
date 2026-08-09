#!/usr/bin/env bash
# PreToolUse hook (matcher: mcp__.*notion.*create.*).
#
# "Every ticket must include ADS-STE100 compliance in its acceptance criteria,
# no exceptions" — enforced at the tool call rather than trusted to the prompt.
# When Claude is batching eight tickets out of a code review, that is the turn
# where a prompted rule quietly degrades. Exit 2 makes it structural.
#
# Validates each create call against the Scent tracker has:
#   - non-empty Acceptance Criteria containing an ADS-STE100 checkpoint
#   - a Feature Branch whose prefix matches the Task type
# Ignores Notion pages created anywhere else.

set -uo pipefail

# Load Notion workspace identifiers from the gitignored env file.
NOTION_ENV="$(dirname "$0")/../../.claude/.scent-notion.env"
[ -f "$NOTION_ENV" ] && source "$NOTION_ENV"
if [ -z "${SCENT_NOTION_DS:-}" ]; then
  echo "ERROR: SCENT_NOTION_DS not set. Copy .claude/.scent-notion.env from a team member." >&2
  exit 1
fi
# Strip the collection:// prefix — the Python script only needs the UUID.
export SCENT_DS_UUID="${SCENT_NOTION_DS#collection://}"

INPUT=$(cat)

RESULT=$(printf '%s' "$INPUT" | python3 -c '
import json, sys, re, os

try:
    payload = json.load(sys.stdin)
except Exception:
    print("OK"); raise SystemExit

if "create" not in payload.get("tool_name", "").lower():
    print("OK"); raise SystemExit

raw = json.dumps(payload.get("tool_input", {}), ensure_ascii=False)

# Only police the Scent tracker, not every Notion page Claude might create.
SCENT_DS = os.environ.get("SCENT_DS_UUID", "")
if SCENT_DS not in raw and "Tasks Tracker" not in raw:
    print("OK"); raise SystemExit

problems = []

if "Acceptance Criteria" not in raw:
    problems.append("no Acceptance Criteria property set")
else:
    tail = raw.split("Acceptance Criteria", 1)[1][:1500]
    if "ADS-STE100" not in tail:
        problems.append("Acceptance Criteria has no ADS-STE100 compliance checkpoint")
    if len(re.sub(r"[^A-Za-z]", "", tail[:150])) < 20:
        problems.append("Acceptance Criteria looks empty or placeholder")

if "Feature Branch" not in raw:
    problems.append("no Feature Branch property set")
else:
    m = re.search(r"(feature|fix|chore)/[a-z0-9]+(-[a-z0-9]+)*", raw)
    if not m:
        problems.append("Feature Branch missing or not in <type>/<kebab-case> form "
                        "(feature/..., fix/..., chore/...)")
    else:
        prefix = m.group(1)
        expected = None
        if "Feature request" in raw:
            expected = "feature"
        elif "Bug" in raw:
            expected = "fix"
        elif any(t in raw for t in ("Polish", "Docs", "Tech Task")):
            expected = "chore"
        if expected and prefix != expected:
            problems.append("Feature Branch prefix %s/ does not match Task type (expected %s/)"
                            % (prefix, expected))

print("OK" if not problems else "FAIL::" + " ; ".join(problems))
')

if [[ "$RESULT" == FAIL::* ]]; then
  echo "BLOCKED: Scent ticket is incomplete — ${RESULT#FAIL::}" >&2
  echo "See .claude/skills/scent-ticket/references/acceptance-criteria.md and retry with the missing fields filled in." >&2
  exit 2
fi

exit 0
