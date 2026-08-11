#!/usr/bin/env bash
# PreToolUse hook (matcher: Edit|Write|MultiEdit).
#
# Adding a dependency is a supply-chain event and a decision the user owns.
# This forces it to surface as a conversation turn instead of appearing inside
# a diff nobody read closely.
#
# Blocks:
#   - new `module = "g:a"` / `id = "..."` entries in gradle/libs.versions.toml
#   - inline `implementation("g:a:v")` coordinates in any build.gradle.kts
#
# Does NOT block:
#   - implementation(libs.foo.bar)     — the catalog is where the gate happened
#   - implementation(projects.shared)  — internal module
#   - version bumps of existing entries (caught by review, not by this)
#
# Approve with:  bash .claude/hooks/approve-dep.sh io.ktor:ktor-client-cio
#
# Honest limitation: like the push gate's escape hatch, this is a speed bump
# rather than a vault — the approval command is runnable by anything with shell
# access. Its value is that it makes the decision *visible*, at the moment it's
# being made, rather than silent.

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

case "$FILE_PATH" in
  *libs.versions.toml|*build.gradle.kts) ;;
  *) exit 0 ;;
esac

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
APPROVED="$REPO_ROOT/.claude/.approved-deps"
touch "$APPROVED" 2>/dev/null

FOUND=$(printf '%s' "$NEW_TEXT" | python3 -c '
import re, sys
text = sys.stdin.read()
coords = set()

# version catalog entries:  module = "group:artifact"   /   id = "plugin.id"
for m in re.finditer(r"""(?:module|id)\s*=\s*["\x27]([^"\x27]+)["\x27]""", text):
    coords.add(m.group(1))

# inline coordinates in a gradle script:  implementation("g:a:v")
for m in re.finditer(
    r"""(?:implementation|api|compileOnly|runtimeOnly|ksp|kapt|testImplementation|androidTestImplementation|debugImplementation)\s*\(\s*["\x27]([^"\x27:]+:[^"\x27:]+(?::[^"\x27]+)?)["\x27]""",
    text):
    coords.add(m.group(1))

print("\n".join(sorted(c for c in coords if ":" in c or "." in c)))
')

[ -z "$FOUND" ] && exit 0

UNAPPROVED=""
while IFS= read -r coord; do
  [ -z "$coord" ] && continue
  # match on group:artifact, ignoring any version suffix
  BASE=$(cut -d: -f1,2 <<<"$coord")
  if ! grep -qxF "$BASE" "$APPROVED" 2>/dev/null; then
    UNAPPROVED="$UNAPPROVED  - $coord"$'\n'
  fi
done <<<"$FOUND"

if [ -n "$UNAPPROVED" ]; then
  {
    echo "BLOCKED: this edit adds dependencies to $FILE_PATH that the user has not approved:"
    printf '%s' "$UNAPPROVED"
    echo
    echo "Before retrying, present the case to the user for each one:"
    echo "  - what it does and what it replaces or saves writing"
    echo "  - klibs.io link, current stable version, and verified target coverage"
    echo "    (use the kmp-libraries-expert skill — do not state versions from memory)"
    echo "  - which module it lands in, and whether it supports every target that module needs"
    echo "  - the one thing that might make them say no (maintenance, licence, size,"
    echo "    ADS-STE100 fit — see .claude/rules/ads-dependencies.md)"
    echo
    echo "Then wait. Once the user agrees, they (or you, with their go-ahead) run:"
    echo "  bash .claude/hooks/approve-dep.sh <group:artifact>"
  } >&2
  exit 2
fi

exit 0
