#!/usr/bin/env bash
# PreToolUse hook (matcher: Edit|Write|MultiEdit).
#
# A java.* reference in commonMain compiles on Android and breaks Kotlin/Native.
# It survives every fast feedback loop and fails at the slowest one. Catching it
# at write time is worth far more than catching it at Gate 1.
#
# Scope: files under commonMain/ or commonTest/ only. androidMain, jvmMain, and
# the whole server module are untouched — java.time in AuthRoutes.kt is correct.
#
# Also blocks kotlin.Result / runCatching, since `Result<T>` in this codebase is
# a typealias for Either<AppError, T> and the two are easy to confuse.

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
    text = "\n".join(e.get("new_string", "") for e in ti["edits"])
print(ti.get("file_path", ""))
print(text.replace("\n", "\x01"))
')

FILE_PATH=$(sed -n '1p' <<<"$PARSED")
NEW_TEXT=$(sed -n '2,$p' <<<"$PARSED" | tr '\001' '\n')

case "$FILE_PATH" in *.kt|*.kts) ;; *) exit 0 ;; esac
case "$FILE_PATH" in *commonMain/*|*commonTest/*) ;; *) exit 0 ;; esac

hit() {
  {
    echo "BLOCKED: $1 in $FILE_PATH"
    echo "This is commonMain — it must compile for iOS as well as Android, and this compiles on Android only."
    echo "$2"
    echo "Full substitution table: .claude/rules/commonmain-purity.md"
  } >&2
  exit 2
}

grep -qE '^\s*import\s+(java|javax)\.' <<<"$NEW_TEXT" && \
  hit "JVM-only import" "Use the multiplatform equivalent: kotlinx-datetime for time, kotlin.random.Random, kotlin.uuid.Uuid, okio for files."

grep -qE '^\s*import\s+android\.' <<<"$NEW_TEXT" && \
  hit "Android-only import" "Move this to androidMain, or put it behind an expect/actual — and complete the iOS actual in the same change."

grep -qE '\bjava\.(util|time|io|text|nio|lang|math|security)\.' <<<"$NEW_TEXT" && \
  hit "fully-qualified JVM type" "Same substitutions apply as for imports."

grep -qE '\bSystem\.(currentTimeMillis|nanoTime|getenv|getProperty|out|err)\b' <<<"$NEW_TEXT" && \
  hit "System.* call" "Use Clock.System.now().toEpochMilliseconds() for time. For env/config, pass values in rather than reading them here."

grep -qE '\bThread\.(sleep|currentThread)\b' <<<"$NEW_TEXT" && \
  hit "Thread.* call" "Use delay(n) inside a coroutine."

grep -qE '\bSimpleDateFormat\b|\bDispatchers\.IO\b' <<<"$NEW_TEXT" && \
  hit "JVM-only API" "SimpleDateFormat -> kotlinx-datetime formatting. Dispatchers.IO -> Dispatchers.Default (IO is JVM-only)."

if grep -qE '\brunCatching\s*[({]|^\s*import\s+kotlin\.Result\b' <<<"$NEW_TEXT"; then
  {
    echo "BLOCKED: kotlin.Result / runCatching in $FILE_PATH"
    echo "In this codebase Result<T> is a typealias for Either<AppError, T>. kotlin.Result type-checks against the wrong thing while reading as correct."
    echo "Return AppError.<Category>.<Case>().asLeft() or value.asRight(). See .claude/rules/ads-either-error-handling.md."
  } >&2
  exit 2
fi

exit 0
