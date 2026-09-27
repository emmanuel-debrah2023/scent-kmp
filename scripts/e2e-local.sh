#!/bin/bash
# Installs the debug APK and runs the Maestro smoke flow against a connected emulator/device.

set -e

if ! command -v maestro >/dev/null 2>&1; then
    echo "maestro CLI not found on PATH — see the Maestro section in README.md for install steps." >&2
    exit 1
fi

./gradlew :composeApp:installDebug

maestro test .maestro/flows/smoke-launch.yaml
