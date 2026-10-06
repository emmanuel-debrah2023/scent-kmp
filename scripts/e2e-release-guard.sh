#!/bin/bash
# Proves the gray-box E2E launch hook (e2eToken, e2eRoute) is debug-only: compiles both
# Android variants and fails if any release class mentions an E2E launch argument.
# Release has minify off, so these classes are what ships in the release dex.
# The debug classes are checked too, so a broken search can't pass as "nothing found".
#
# Usage: ./scripts/e2e-release-guard.sh   (also run by e2e-local.sh)

set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

PATTERN='e2eToken|e2eRoute'
CLASSES=composeApp/build/tmp/kotlin-classes

./gradlew -q :composeApp:compileDebugKotlinAndroid :composeApp:compileReleaseKotlinAndroid

fail() { echo "e2e-release-guard: $*" >&2; exit 1; }

[ -d "$CLASSES/release" ] || fail "no compiled release classes under $CLASSES/release"

grep -rqE "$PATTERN" "$CLASSES/debug" \
    || fail "the debug classes don't mention $PATTERN either, so this check can't see the hook. Has it moved or been renamed?"

LEAKS=$(grep -rlE "$PATTERN" "$CLASSES/release" || true)
if [ -n "$LEAKS" ]; then
    echo "e2e-release-guard: release classes reference an E2E launch argument:" >&2
    echo "$LEAKS" >&2
    fail "keep the hook in composeApp/src/androidDebug; the release source set must stay a no-op"
fi

echo "e2e-release-guard: release build carries no E2E launch arguments"
