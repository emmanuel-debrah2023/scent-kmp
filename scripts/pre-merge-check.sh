#!/bin/bash
# Local pre-merge device check: runs the per-screen a11y Maestro flows on an Android emulator and an
# iOS simulator in parallel, captures each screen's accessibility tree on the same booted device,
# and checks it with scripts/a11y_check.py. Writes build/pre-merge/report.md.
#
# Usage: scripts/pre-merge-check.sh [--platform android|ios|both|auto] [--flows changed|all]
#                                   [--base origin/main] [--screens home,profile] [--keep-devices]
#
#   --platform  auto (default): Android when app code changed, iOS when shared UI, iosMain or the
#               iOS project changed. Server-only diffs skip devices and exit 0.
#   --flows     changed (default): screens mapped from the diff via .maestro/a11y/screens.yaml;
#               an ambiguous mapping runs every screen. all: every screen.
#   --screens   comma-separated screen names; overrides --flows (re-run just the failing ones).
#   --keep-devices  leave devices booted and the a11y settings on.
#
# Exit status: 0 all screens pass, 1 a violation or an infrastructure failure, 2 bad usage/preflight.
# Needs the E2E backend (started or reused through scripts/e2e-up.sh). No agent or MCP dependency.

set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
# shellcheck source=scripts/pre-merge.env
source scripts/pre-merge.env
# shellcheck source=scripts/e2e-common.sh
source scripts/e2e-common.sh

PLATFORM=auto
FLOWS=changed
BASE=origin/main
SCREENS=""
KEEP_DEVICES=false

usage() { sed -n '2,18p' "$0" | sed 's/^# \{0,1\}//'; exit 2; }

while [ $# -gt 0 ]; do
    case "$1" in
        --platform) PLATFORM="${2:?}"; shift 2 ;;
        --flows) FLOWS="${2:?}"; shift 2 ;;
        --base) BASE="${2:?}"; shift 2 ;;
        --screens) SCREENS="${2:?}"; shift 2 ;;
        --keep-devices) KEEP_DEVICES=true; shift ;;
        -h | --help) usage ;;
        *) echo "pre-merge: unknown argument $1" >&2; usage ;;
    esac
done
case "$PLATFORM" in android | ios | both | auto) ;; *) usage ;; esac
case "$FLOWS" in changed | all) ;; *) usage ;; esac

OUT="build/pre-merge"
A11Y_DIR=".maestro/a11y"
STATE="$OUT/state"
log() { echo "[pre-merge] $*"; }
die() { echo "[pre-merge] $*" >&2; exit 2; }
checker() { uv run --quiet --script scripts/a11y_check.py "$@"; }

# ---- Scope from the diff ------------------------------------------------------------------------

command -v uv >/dev/null 2>&1 || die "uv not found (it runs scripts/a11y_check.py): brew install uv"
git rev-parse --verify --quiet "$BASE" >/dev/null || die "base $BASE not found; run git fetch origin, or pass --base"
mkdir -p "$OUT"
MERGE_BASE=$(git merge-base "$BASE" HEAD)
# Committed and uncommitted changes since the merge base, plus untracked files.
{ git diff --name-only "$MERGE_BASE"; git ls-files --others --exclude-standard; } | sort -u >"$OUT/changed.txt"

matches() { grep -Eq "$1" "$OUT/changed.txt"; }
# The check itself (flows, checker, this script) is exercised on both platforms when it changes.
TOOLING='^(\.maestro/a11y/|scripts/(pre-merge|a11y_check))'
ANDROID_CODE='^(composeApp/|shared/|gradle/|build\.gradle\.kts|settings\.gradle\.kts|gradle\.properties)'
IOS_CODE='^(composeApp/src/commonMain/.*(ui/|App\.kt)|composeApp/src/iosMain/|shared/src/iosMain/|iosApp/)'

RUN_ANDROID=false
RUN_IOS=false
case "$PLATFORM" in
    android) RUN_ANDROID=true ;;
    ios) RUN_IOS=true ;;
    both) RUN_ANDROID=true; RUN_IOS=true ;;
    auto)
        if matches "$TOOLING"; then RUN_ANDROID=true; RUN_IOS=true; fi
        if matches "$ANDROID_CODE"; then RUN_ANDROID=true; fi
        if matches "$IOS_CODE"; then RUN_IOS=true; fi
        if ! $RUN_ANDROID && ! $RUN_IOS; then
            log "no app, shared UI or iOS changes since $BASE (server, docs or config only): no device check needed."
            exit 0
        fi
        ;;
esac

ALL_SCREENS=$(find "$A11Y_DIR" -maxdepth 1 -name '*.yaml' ! -name screens.yaml -exec basename {} .yaml \; | sort)
if [ -n "$SCREENS" ]; then
    SELECTED=$(echo "$SCREENS" | tr ',' '\n' | sed '/^$/d')
    for s in $SELECTED; do
        [ -f "$A11Y_DIR/$s.yaml" ] || die "no flow for screen '$s' (have: $(echo "$ALL_SCREENS" | tr '\n' ' '))"
    done
elif [ "$FLOWS" = all ]; then
    SELECTED="$ALL_SCREENS"
else
    SELECTED=$(checker screens --config "$A11Y_DIR/screens.yaml" --changed "$OUT/changed.txt")
    if [ -z "$SELECTED" ]; then
        log "the diff maps to no single screen; running every screen rather than skipping"
        SELECTED="$ALL_SCREENS"
    fi
fi
log "platforms: android=$RUN_ANDROID ios=$RUN_IOS; screens: $(echo "$SELECTED" | tr '\n' ' ')"

# ---- Preflight ----------------------------------------------------------------------------------

require_maestro
if $RUN_ANDROID; then
    command -v adb >/dev/null 2>&1 || die "adb not found: add the Android SDK platform-tools to PATH"
    command -v emulator >/dev/null 2>&1 || die "emulator not found: add the Android SDK emulator directory to PATH"
    if ! adb devices | grep -q '^emulator-'; then
        emulator -list-avds | grep -qx "$PRE_MERGE_AVD" || die "AVD $PRE_MERGE_AVD not found (emulator -list-avds); set PRE_MERGE_AVD"
    fi
fi
if $RUN_IOS; then
    [ "$(uname -s)" = Darwin ] || die "iOS needs macOS"
    command -v xcrun >/dev/null 2>&1 || die "xcrun not found: install Xcode"
    command -v xcodebuild >/dev/null 2>&1 || die "xcodebuild not found: install Xcode"
    command -v axe >/dev/null 2>&1 || die "axe not found: brew install cameroncooke/axe/axe"
    if [ -z "$PRE_MERGE_IOS_UDID" ]; then
        PRE_MERGE_IOS_UDID=$(xcrun simctl list devices available -j | python3 -c '
import json, sys
name = sys.argv[1]
runtimes = json.load(sys.stdin)["devices"]
ios = sorted((r for r in runtimes if ".iOS-" in r), key=lambda r: [int(p) for p in r.rsplit("iOS-", 1)[1].split("-")])
for runtime in reversed(ios):
    for d in runtimes[runtime]:
        if d["name"] == name:
            print(d["udid"]); sys.exit(0)
' "$PRE_MERGE_IOS_DEVICE")
    fi
    [ -n "$PRE_MERGE_IOS_UDID" ] || die "no available simulator named '$PRE_MERGE_IOS_DEVICE'; set PRE_MERGE_IOS_DEVICE or PRE_MERGE_IOS_UDID"
fi

# Fresh results, but keep DerivedData so iOS builds stay incremental.
rm -rf "$OUT/android" "$OUT/ios" "$STATE" "$OUT/report.md" "$OUT/report.json" "$OUT/status.tsv"
mkdir -p "$OUT/android" "$OUT/ios" "$STATE"

log "starting or reusing the E2E backend"
./scripts/e2e-up.sh 2>&1 | sed 's/^/[backend] /' || die "the E2E backend didn't come up; see the [backend] lines above"

# ---- Cleanup ------------------------------------------------------------------------------------

# shellcheck disable=SC2329 # invoked by the EXIT trap below
cleanup() {
    local status=$?
    trap - EXIT INT TERM
    jobs -p | xargs -r kill 2>/dev/null || true
    if [ -f "$STATE/android-serial" ]; then
        local serial
        serial=$(cat "$STATE/android-serial")
        if [ -f "$STATE/android-font-scale" ] && ! $KEEP_DEVICES; then
            adb -s "$serial" shell settings put system font_scale "$(cat "$STATE/android-font-scale")" >/dev/null 2>&1 || true
            for setting in "${ANIMATION_SETTINGS[@]}"; do
                [ -f "$STATE/android-$setting" ] &&
                    adb -s "$serial" shell settings put global "$setting" "$(cat "$STATE/android-$setting")" >/dev/null 2>&1
            done
        fi
        if [ -f "$STATE/android-booted" ] && ! $KEEP_DEVICES; then
            echo "[android] shutting down the emulator this run booted"
            adb -s "$serial" emu kill >/dev/null 2>&1 || true
        fi
    fi
    if [ -f "$STATE/ios-content-size" ] && ! $KEEP_DEVICES; then
        xcrun simctl ui "$PRE_MERGE_IOS_UDID" content_size "$(cat "$STATE/ios-content-size")" >/dev/null 2>&1 || true
    fi
    if [ -f "$STATE/ios-booted" ] && ! $KEEP_DEVICES; then
        echo "[ios] shutting down the simulator this run booted"
        xcrun simctl shutdown "$PRE_MERGE_IOS_UDID" >/dev/null 2>&1 || true
    fi
    exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT TERM

# ---- Shared steps -------------------------------------------------------------------------------

# status <platform> <screen> <flow pass|fail> <retried yes|no> <tree ok|ax-not-ready|skipped> [note]
status() { printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$1" "$2" "$3" "$4" "$5" "${6:-}" >>"$STATE/$1.status"; }

# fail_platform <platform> <note>: marks every selected screen as an infrastructure failure.
fail_platform() {
    for s in $SELECTED; do status "$1" "$s" fail no skipped "$2"; done
    echo "[$1] $2" >&2
}

# run_flow <platform> <device> <app id> <flow> <junit out>: one run, output prefixed.
run_flow() {
    maestro --device "$2" test -e "APP_ID=$3" --format JUNIT --output "$5" "$4" 2>&1 | sed "s/^/[$1] /"
}

# dump_path <platform> <screen>: where that screen's raw tree is saved.
dump_path() {
    if [ "$1" = ios ]; then echo "$OUT/ios/$2.json"; else echo "$OUT/android/$2.xml"; fi
}

# capture <platform> <screen> <dump command...>: dumps until two consecutive dumps match.
capture() {
    local platform="$1" screen="$2"; shift 2
    local dump previous="" current attempt
    dump=$(dump_path "$platform" "$screen")
    local args=(--platform "$platform" --dump "$dump")
    [ "$platform" = android ] && args+=(--density "$ANDROID_DENSITY" --package "$PRE_MERGE_ANDROID_APP_ID")
    for attempt in $(seq 1 "$PRE_MERGE_SETTLE_ATTEMPTS"); do
        "$@" >"$dump" 2>"$OUT/$platform/$screen.capture.err" || true
        current=$(checker digest "${args[@]}" 2>/dev/null || echo "unreadable-$attempt")
        [ "$current" = "$previous" ] && return 0
        previous="$current"
        sleep 1
    done
    echo "[$platform] $screen: tree still changing after $PRE_MERGE_SETTLE_ATTEMPTS dumps; checking the last one"
}

# check_screen <platform> <screen>: runs the checker over the captured dump.
check_screen() {
    local platform="$1" screen="$2" dump
    dump=$(dump_path "$platform" "$screen")
    local args=(--platform "$platform" --dump "$dump" --screen "$screen" --json "$OUT/$platform/$screen.result.json")
    [ -f "$A11Y_DIR/expectations/$screen.yaml" ] && args+=(--expectations "$A11Y_DIR/expectations/$screen.yaml")
    [ "$platform" = android ] && args+=(--density "$ANDROID_DENSITY" --package "$PRE_MERGE_ANDROID_APP_ID")
    checker check "${args[@]}" || true
}

# run_screens <platform> <device> <app id> <dump command...>
run_screens() {
    local platform="$1" device="$2" app_id="$3"; shift 3
    local screen flow retried
    for screen in $SELECTED; do
        flow="$A11Y_DIR/$screen.yaml"
        retried=no
        if [ "$platform" = android ] && ! android_app_ready; then
            status android "$screen" fail no skipped "emulator not ready or the app couldn't be reinstalled"
            continue
        fi
        echo "[$platform] flow $screen"
        if ! run_flow "$platform" "$device" "$app_id" "$flow" "$OUT/$platform/$screen.junit.xml"; then
            echo "[$platform] flow $screen failed; retrying once"
            retried=yes
            if ! run_flow "$platform" "$device" "$app_id" "$flow" "$OUT/$platform/$screen.junit.xml"; then
                status "$platform" "$screen" fail yes skipped "flow failed twice; see $OUT/$platform.log"
                continue
            fi
        fi
        capture "$platform" "$screen" "$@"
        if ! tree_ready "$platform" "$screen" "$@"; then
            status "$platform" "$screen" pass "$retried" tree-not-ready \
                "empty or unreadable tree $PRE_MERGE_AX_READY_ATTEMPTS times ($([ "$platform" = ios ] && echo 'AX not ready on the simulator' || echo 'uiautomator dump failed'))"
            continue
        fi
        check_screen "$platform" "$screen"
        status "$platform" "$screen" pass "$retried" ok
    done
}

# tree_ready <platform> <screen> <dump command...>: re-dumps an empty or unreadable tree a few times.
# An empty tree is never checked: it would pass every rule.
tree_ready() {
    local platform="$1" screen="$2"; shift 2
    local dump attempt
    dump=$(dump_path "$platform" "$screen")
    local args=(--platform "$platform" --dump "$dump")
    [ "$platform" = android ] && args+=(--density "$ANDROID_DENSITY" --package "$PRE_MERGE_ANDROID_APP_ID")
    for attempt in $(seq 1 "$PRE_MERGE_AX_READY_ATTEMPTS"); do
        # `empty` exits 1 only for a readable tree with labels in it.
        local empty=0
        [ -s "$dump" ] && { checker empty "${args[@]}" 2>/dev/null || empty=$?; }
        [ "$empty" = 1 ] && return 0
        echo "[$platform] $screen: empty or unreadable tree, retry $attempt/$PRE_MERGE_AX_READY_ATTEMPTS"
        sleep 2
        [ "$platform" = android ] && android_ready
        "$@" >"$dump" 2>"$OUT/$platform/$screen.capture.err" || true
    done
    return 1
}

# ---- Android ------------------------------------------------------------------------------------

# android_ready: adb connected, boot finished and the package manager answering.
android_ready() {
    local waited=0
    adb wait-for-device
    until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] &&
        adb shell pm path android >/dev/null 2>&1; do
        sleep 2; waited=$((waited + 2))
        [ "$waited" -lt "$PRE_MERGE_BOOT_TIMEOUT_S" ] || return 1
    done
}

# android_app_ready: the emulator can restart parts of Android under load and briefly lose the app.
android_app_ready() {
    android_ready || return 1
    adb shell pm path "$PRE_MERGE_ANDROID_APP_ID" >/dev/null 2>&1 && return 0
    echo "[android] $PRE_MERGE_ANDROID_APP_ID missing after an emulator restart; reinstalling"
    adb install -r "$ANDROID_APK" >/dev/null
}

android_job() {
    local serial
    serial=$(adb devices | awk '$1 ~ /^emulator-/ && $2 == "device" { print $1; exit }')
    if [ -z "$serial" ]; then
        echo "[android] booting $PRE_MERGE_AVD"
        # A true cold boot: quick-booting the saved snapshot restarted Android mid-run and lost the app.
        nohup emulator -avd "$PRE_MERGE_AVD" -no-window -no-snapshot -no-audio -memory "$PRE_MERGE_EMULATOR_MEMORY" \
            >"$OUT/android-emulator.log" 2>&1 &
        touch "$STATE/android-booted"
        local waited=0
        while [ -z "$serial" ] && [ "$waited" -lt "$PRE_MERGE_BOOT_TIMEOUT_S" ]; do
            sleep 2; waited=$((waited + 2))
            serial=$(adb devices | awk '$1 ~ /^emulator-/ { print $1; exit }')
        done
        [ -n "$serial" ] || { fail_platform android "emulator didn't appear within ${PRE_MERGE_BOOT_TIMEOUT_S}s; see $OUT/android-emulator.log"; return; }
    else
        echo "[android] reusing running emulator $serial"
    fi
    echo "$serial" >"$STATE/android-serial"
    export ANDROID_SERIAL="$serial"
    android_ready || { fail_platform android "emulator didn't finish booting within ${PRE_MERGE_BOOT_TIMEOUT_S}s"; return; }

    # Large text, no animations. TalkBack stays off: it changes how Maestro's taps are delivered.
    adb shell settings get system font_scale | tr -d '\r' | sed 's/^null$/1.0/' >"$STATE/android-font-scale"
    for setting in "${ANIMATION_SETTINGS[@]}"; do
        adb shell settings get global "$setting" | tr -d '\r' | sed 's/^null$/1/' >"$STATE/android-$setting"
        adb shell settings put global "$setting" 0
    done
    adb shell settings put system font_scale "$PRE_MERGE_ANDROID_FONT_SCALE"
    ANDROID_DENSITY=$(adb shell wm density | tr -d '\r' | awk -F': ' '/Override/ { o = $2 } /Physical/ { p = $2 } END { print (o ? o : p) }')
    echo "[android] font scale $PRE_MERGE_ANDROID_FONT_SCALE, density $ANDROID_DENSITY"

    echo "[android] building and installing the debug app"
    if ! ./gradlew :composeApp:installDebug -q >"$OUT/android-build.log" 2>&1; then
        tail -20 "$OUT/android-build.log" | sed 's/^/[android] /'
        fail_platform android "build failed; see $OUT/android-build.log"
        return
    fi

    run_screens android "$serial" "$PRE_MERGE_ANDROID_APP_ID" adb -s "$serial" exec-out uiautomator dump --compressed /dev/tty
}

# ---- iOS ----------------------------------------------------------------------------------------

ios_job() {
    local udid="$PRE_MERGE_IOS_UDID"
    if xcrun simctl list devices | grep -q "$udid) (Booted)"; then
        echo "[ios] reusing booted simulator $udid"
    else
        echo "[ios] booting $PRE_MERGE_IOS_DEVICE ($udid)"
        xcrun simctl boot "$udid" || { fail_platform ios "simulator $udid didn't boot"; return; }
        touch "$STATE/ios-booted"
    fi
    xcrun simctl bootstatus "$udid" -b >/dev/null || { fail_platform ios "simulator $udid didn't finish booting"; return; }

    # iOS 26+ fresh simulators leave these off, and AXe then reads an empty tree.
    xcrun simctl spawn "$udid" defaults write com.apple.Accessibility AccessibilityEnabled -bool true
    xcrun simctl spawn "$udid" defaults write com.apple.Accessibility ApplicationAccessibilityEnabled -bool true
    xcrun simctl ui "$udid" content_size | tr -d ' ' >"$STATE/ios-content-size"

    echo "[ios] building $PRE_MERGE_IOS_SCHEME for the simulator"
    if ! xcodebuild -project "$PRE_MERGE_IOS_PROJECT" -scheme "$PRE_MERGE_IOS_SCHEME" -configuration Debug \
        -destination "platform=iOS Simulator,id=$udid" -derivedDataPath "$OUT/DerivedData" build \
        >"$OUT/ios-build.log" 2>&1; then
        grep -E 'error:|e: ' "$OUT/ios-build.log" | head -20 | sed 's/^/[ios] /'
        fail_platform ios "build failed; see $OUT/ios-build.log"
        return
    fi
    xcrun simctl install "$udid" "$OUT/DerivedData/Build/Products/Debug-iphonesimulator/$PRE_MERGE_IOS_APP_NAME" ||
        { fail_platform ios "install failed"; return; }

    # Sign in at the default text size: at accessibility sizes the keyboard covers SIGN IN.
    xcrun simctl ui "$udid" content_size "$PRE_MERGE_IOS_SIGN_IN_CONTENT_SIZE"
    echo "[ios] signing in"
    if ! run_flow ios "$udid" "$PRE_MERGE_IOS_BUNDLE_ID" "$A11Y_DIR/setup/ios-sign-in.yaml" "$OUT/ios/sign-in.junit.xml" &&
        ! run_flow ios "$udid" "$PRE_MERGE_IOS_BUNDLE_ID" "$A11Y_DIR/setup/ios-sign-in.yaml" "$OUT/ios/sign-in.junit.xml"; then
        fail_platform ios "sign-in failed twice; see $OUT/ios.log"
        return
    fi
    xcrun simctl ui "$udid" content_size "$PRE_MERGE_IOS_CONTENT_SIZE"
    echo "[ios] Dynamic Type $PRE_MERGE_IOS_CONTENT_SIZE"

    run_screens ios "$udid" "$PRE_MERGE_IOS_BUNDLE_ID" axe describe-ui --udid "$udid"
}

# ---- Run ----------------------------------------------------------------------------------------

ANDROID_DENSITY=160
ANDROID_APK=composeApp/build/outputs/apk/debug/composeApp-debug.apk
if $RUN_ANDROID; then android_job > >(tee "$OUT/android.log") 2>&1 & fi
if $RUN_IOS; then ios_job > >(tee "$OUT/ios.log") 2>&1 & fi
wait

cat "$STATE"/*.status >"$OUT/status.tsv" 2>/dev/null || true
REPORT_STATUS=0
checker report --dir "$OUT" --status "$OUT/status.tsv" --out "$OUT/report.md" --json "$OUT/report.json" || REPORT_STATUS=$?
echo
cat "$OUT/report.md"
log "report: $OUT/report.md (machine-readable: $OUT/report.json)"
exit "$REPORT_STATUS"
