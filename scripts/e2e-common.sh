#!/bin/bash
# Shared helpers for the e2e-*.sh scripts. Source it, don't run it.

require_maestro() {
    # The installer puts the CLI in ~/.maestro/bin, which new shells often don't have on PATH yet.
    if ! command -v maestro >/dev/null 2>&1 && [ -x "$HOME/.maestro/bin/maestro" ]; then
        export PATH="$HOME/.maestro/bin:$PATH"
    fi
    if ! command -v maestro >/dev/null 2>&1; then
        echo "maestro CLI not found on PATH or in ~/.maestro/bin — see the Maestro section in README.md for install steps." >&2
        exit 1
    fi
}

require_device() {
    if ! command -v adb >/dev/null 2>&1; then
        echo "adb not found on PATH — add the Android SDK platform-tools directory to PATH." >&2
        exit 1
    fi
    if ! adb devices | awk 'NR > 1 && $2 == "device" { found = 1 } END { exit !found }'; then
        echo "No emulator or device connected. Start one first, e.g. \`emulator -avd Pixel_8\`, then check \`adb devices\`." >&2
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
