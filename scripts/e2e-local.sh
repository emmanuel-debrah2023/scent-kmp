#!/bin/bash
# Installs the debug APK and runs the Maestro smoke flow against a connected emulator/device.

set -e

if ! command -v maestro >/dev/null 2>&1; then
    echo "maestro CLI not found on PATH — see the Maestro section in README.md for install steps." >&2
    exit 1
fi

# Animations introduce timing flakiness Maestro has to wait out — disable them
# for the run and always restore whatever the emulator had before, even on failure.
ANIMATION_SETTINGS=(window_animation_scale transition_animation_scale animator_duration_scale)
ORIGINAL_SCALES=()

for setting in "${ANIMATION_SETTINGS[@]}"; do
    value=$(adb shell settings get global "$setting" | tr -d '\r')
    [ "$value" = "null" ] && value=1
    ORIGINAL_SCALES+=("$value")
done

restore_animation_scales() {
    for i in "${!ANIMATION_SETTINGS[@]}"; do
        adb shell settings put global "${ANIMATION_SETTINGS[$i]}" "${ORIGINAL_SCALES[$i]}"
    done
}
trap restore_animation_scales EXIT

for setting in "${ANIMATION_SETTINGS[@]}"; do
    adb shell settings put global "$setting" 0
done

./gradlew :composeApp:installDebug

maestro test .maestro/flows/smoke-launch.yaml
