#!/bin/bash
# Lints every flow, installs the debug APK and runs one Maestro flow against a connected emulator/device.
# Usage: ./scripts/e2e-local.sh [flow]   (default: .maestro/flows/smoke-launch.yaml)

set -e
cd "$(git rev-parse --show-toplevel)"
source scripts/e2e-common.sh

FLOW="${1:-.maestro/flows/smoke-launch.yaml}"

./scripts/e2e-lint.sh
./scripts/e2e-release-guard.sh
require_maestro
require_device
disable_animations

./gradlew :composeApp:installDebug

maestro test "$FLOW"
