#!/usr/bin/env bash
# Approve a dependency so gate-dependency-additions.sh will let it through.
#
#   bash .claude/hooks/approve-dep.sh io.ktor:ktor-client-cio
#   bash .claude/hooks/approve-dep.sh --list
#   bash .claude/hooks/approve-dep.sh --clear
#
# Approvals are per-repo and persist (they describe what's allowed in this
# codebase, not a one-off unlock). Version is ignored — approval is at the
# group:artifact level, so a later version bump doesn't need re-approving.

set -euo pipefail
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
APPROVED="$REPO_ROOT/.claude/.approved-deps"
mkdir -p "$(dirname "$APPROVED")"; touch "$APPROVED"

case "${1:-}" in
  --list)
    echo "Approved dependencies:"; sed 's/^/  /' "$APPROVED"; exit 0 ;;
  --clear)
    : > "$APPROVED"; echo "Cleared."; exit 0 ;;
  "")
    echo "usage: approve-dep.sh <group:artifact> [more...] | --list | --clear" >&2
    exit 1 ;;
esac

for coord in "$@"; do
  BASE=$(cut -d: -f1,2 <<<"$coord")
  if grep -qxF "$BASE" "$APPROVED"; then
    echo "already approved: $BASE"
  else
    echo "$BASE" >> "$APPROVED"
    echo "approved: $BASE"
  fi
done

sort -u -o "$APPROVED" "$APPROVED"
