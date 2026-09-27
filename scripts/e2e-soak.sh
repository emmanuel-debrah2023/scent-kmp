#!/bin/bash
# Flakiness gate: runs one flow N times from a cleared app state and prints the pass rate.
# Only an N/N result earns the flow the `suite` tag. Failing runs keep Maestro's
# --debug-output (screenshots + hierarchy per step) under .maestro/.debug-output/.
# Usage: ./scripts/e2e-soak.sh <flow> [--runs N]   (default N: 5)

set -uo pipefail
cd "$(git rev-parse --show-toplevel)"
source scripts/e2e-common.sh

FLOW="${1:-}"
RUNS=5
if [ -z "$FLOW" ] || [ ! -f "$FLOW" ]; then
    echo "usage: $0 <flow.yaml> [--runs N]" >&2
    exit 2
fi
shift
while [ $# -gt 0 ]; do
    case "$1" in
        --runs) RUNS="${2:?--runs needs a number}"; shift 2 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done

./scripts/e2e-lint.sh "$FLOW" || exit 1
require_maestro

APP_ID=$(sed -n 's/^appId:[[:space:]]*//p' "$FLOW" | head -1 | tr -d '"'"'"'')
if [ -z "$APP_ID" ]; then
    echo "no appId in the header of $FLOW" >&2
    exit 2
fi

disable_animations
./gradlew --quiet :composeApp:installDebug || exit 1

FLOW_NAME=$(basename "$FLOW" .yaml)
DEBUG_ROOT=".maestro/.debug-output/$FLOW_NAME"
rm -rf "$DEBUG_ROOT"
mkdir -p "$DEBUG_ROOT"
PASSED=0

for run in $(seq 1 "$RUNS"); do
    adb shell pm clear "$APP_ID" >/dev/null
    RUN_DIR="$DEBUG_ROOT/run-$run"
    if maestro test --debug-output "$RUN_DIR" "$FLOW" >"$RUN_DIR.log" 2>&1; then
        PASSED=$((PASSED + 1))
        rm -rf "$RUN_DIR" "$RUN_DIR.log"
        echo "run $run/$RUNS: pass"
    else
        echo "run $run/$RUNS: FAIL — debug output in $RUN_DIR, log in $RUN_DIR.log"
    fi
done

echo "soak: $FLOW passed $PASSED/$RUNS"

if [ "$PASSED" -ne "$RUNS" ]; then
    exit 1
fi

python3 - "$FLOW" <<'EOF'
import re, sys
path = sys.argv[1]
lines = open(path).read().split("\n")
sep = next(i for i, l in enumerate(lines) if l.strip() == "---")
header = lines[:sep]
tags_at = next((i for i, l in enumerate(header) if l.startswith("tags:")), None)
if tags_at is None:
    header += ["tags:", "  - suite"]
else:
    inline = re.match(r"tags:\s*\[(.*)\]\s*$", header[tags_at])
    block = []
    for l in ([] if inline else header[tags_at + 1:]):
        if not l.startswith("  - "):
            break
        block.append(l.strip()[2:].strip())
    existing = [t.strip() for t in inline.group(1).split(",")] if inline else block
    if "suite" in existing:
        sys.exit(0)
    if inline:
        header[tags_at] = "tags: [" + ", ".join([t for t in existing if t] + ["suite"]) + "]"
    else:
        header.insert(tags_at + 1 + len(block), "  - suite")
open(path, "w").write("\n".join(header + lines[sep:]))
print(f"soak: tagged {path} with `suite`")
EOF
