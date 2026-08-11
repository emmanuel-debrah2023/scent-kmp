#!/usr/bin/env bash
# PostToolUse hook (matcher: Edit|Write|MultiEdit).
#
# Formats the single file that was just edited. This is Gate 3's style half,
# moved out of the loop: the formatter runs because it runs, not because the
# model remembered to run it.
#
# Deliberately does NOT shell out to `./gradlew ktlintFormat` — a full Gradle
# invocation after every edit costs more than the whole loop saves. Uses the
# standalone ktlint binary on one file when available, and otherwise stays
# silent (the full ktlintCheck in gates.sh remains the real gate).
#
# Install the standalone binary once:
#   brew install ktlint      # or: https://github.com/pinterest/ktlint/releases

set -uo pipefail
INPUT=$(cat)

FILE_PATH=$(python3 -c '
import json,sys
try: print(json.load(sys.stdin).get("tool_input",{}).get("file_path",""))
except Exception: print("")
' <<< "$INPUT")

case "$FILE_PATH" in *.kt|*.kts) ;; *) exit 0 ;; esac
[ -f "$FILE_PATH" ] || exit 0

if command -v ktlint >/dev/null 2>&1; then
  ktlint --format --relative --log-level=none "$FILE_PATH" >/dev/null 2>&1 || true
fi

exit 0
