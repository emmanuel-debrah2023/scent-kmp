#!/usr/bin/env bash
# PreToolUse hook (matcher: Edit|Write|MultiEdit).
#
# "Don't suppress rules just to get green" is exactly the kind of instruction
# that erodes at iteration 4 of a stubborn lint failure. This makes it a wall.
#
# Blocks writes that introduce @Suppress / ktlint-disable on a Kotlin file,
# that touch a detekt/ktlint baseline, or that add a force-unwrap. Not a ban on
# ever suppressing — a ban on doing it silently. The user can add it by hand, or
# Claude can ask first.

set -uo pipefail
INPUT=$(cat)

PARSED=$(printf '%s' "$INPUT" | python3 -c '
import json, sys
try:
    ti = json.load(sys.stdin).get("tool_input", {})
except Exception:
    print(); print(); raise SystemExit
text = ti.get("content") or ti.get("new_string") or ""
if not text and isinstance(ti.get("edits"), list):
    text = " ".join(e.get("new_string", "") for e in ti["edits"])
print(ti.get("file_path", ""))
print(text.replace("\n", " "))
')

FILE_PATH=$(sed -n '1p' <<<"$PARSED")
NEW_TEXT=$(sed -n '2,$p' <<<"$PARSED")

case "$FILE_PATH" in
  *detekt-baseline.xml|*baseline.xml|*.editorconfig)
    echo "BLOCKED: editing $FILE_PATH suppresses static analysis wholesale." >&2
    echo "Fix the violations, or explain to the user why the rule is wrong for this codebase and let them decide." >&2
    exit 2 ;;
esac

case "$FILE_PATH" in *.kt|*.kts) ;; *) exit 0 ;; esac

if grep -qE '@Suppress|@SuppressWarnings|ktlint-disable' <<<"$NEW_TEXT"; then
  echo "BLOCKED: this edit adds a lint/analysis suppression to $FILE_PATH." >&2
  echo "ADS-STE100 workflow: fix the violation instead. If the rule genuinely does not fit this codebase, say so to the user and let them approve the suppression explicitly." >&2
  exit 2
fi

# Force-unwrap is a hard no across the null-safety boundary.
# Excludes '!!=' and standalone '!!' in strings is not worth chasing — false
# positives here are cheap (Claude just rewrites the line safely).
if grep -qE '[]A-Za-z0-9_)]!!' <<<"$NEW_TEXT"; then
  echo "BLOCKED: this edit introduces a force-unwrap (!!) in $FILE_PATH." >&2
  echo "ADS-STE100 forbids !!. Use ?: with a default, or validate in the mapper and return AppError.NetworkError.ParseError().asLeft()." >&2
  exit 2
fi

exit 0
