#!/bin/bash
# Lints Maestro flows against the rules in .maestro/README.md.
# Usage: ./scripts/e2e-lint.sh [flow-or-dir ...]   (default: .maestro/flows and .maestro/subflows)

set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
exec python3 scripts/e2e_lint.py "$@"
