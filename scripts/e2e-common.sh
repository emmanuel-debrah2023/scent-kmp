#!/bin/bash
# Shared helpers for the e2e-*.sh scripts. Source it, don't run it.

require_maestro() {
    if ! command -v maestro >/dev/null 2>&1; then
        echo "maestro CLI not found on PATH — see the Maestro section in README.md for install steps." >&2
        exit 1
    fi
}

# Animations introduce timing flakiness Maestro has to wait out — disable them
# for the run and always restore whatever the emulator had before, even on failure.
ANIMATION_SETTINGS=(window_animation_scale transition_animation_scale animator_duration_scale)
ORIGINAL_SCALES=()

restore_animation_scales() {
    for i in "${!ORIGINAL_SCALES[@]}"; do
        adb shell settings put global "${ANIMATION_SETTINGS[$i]}" "${ORIGINAL_SCALES[$i]}"
    done
}

disable_animations() {
    for setting in "${ANIMATION_SETTINGS[@]}"; do
        value=$(adb shell settings get global "$setting" | tr -d '\r')
        [ "$value" = "null" ] && value=1
        ORIGINAL_SCALES+=("$value")
    done
    trap restore_animation_scales EXIT
    for setting in "${ANIMATION_SETTINGS[@]}"; do
        adb shell settings put global "$setting" 0
    done
}
