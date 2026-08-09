#!/usr/bin/env bash
# Regression tests for the blocking hooks. Run after editing any of them —
# a hook that silently exits 0 is worse than no hook, because you'll trust it.
#
#   bash .claude/hooks/test-hooks.sh

cd "$(dirname "$0")/../.." || exit 1
HOOKS=".claude/hooks"
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
PASS=0; FAIL=0

# Load Notion workspace identifiers from the gitignored env file.
NOTION_ENV=".claude/.scent-notion.env"
[ -f "$NOTION_ENV" ] && source "$NOTION_ENV"
if [ -z "${SCENT_NOTION_DS:-}" ]; then
  echo "ERROR: .claude/.scent-notion.env not found. Copy it from a team member." >&2
  exit 1
fi

check() { # check <name> <hook> <expected-exit> <json>
  printf '%s' "$4" > "$TMP/in.json"
  bash "$HOOKS/$2" < "$TMP/in.json" >/dev/null 2>&1
  local got=$?
  if [ "$got" = "$3" ]; then PASS=$((PASS+1)); printf '  ok   %s\n' "$1"
  else FAIL=$((FAIL+1)); printf '  FAIL %s (expected %s, got %s)\n' "$1" "$3" "$got"; fi
}

edit() { printf '{"tool_name":"Edit","tool_input":{"file_path":"%s","new_string":%s}}' "$1" "$2"; }
DS="$SCENT_NOTION_DS"
ticket() { printf '{"tool_name":"mcp__notion__notion-create-pages","tool_input":{"parent":"%s","properties":%s}}' "$DS" "$1"; }

echo "block-suppressions.sh"
check "blocks @Suppress"        block-suppressions.sh 2 "$(edit a/F.kt '"@Suppress(\"LongMethod\")"')"
check "blocks ktlint-disable"   block-suppressions.sh 2 "$(edit a/F.kt '"// ktlint-disable no-wildcard-imports"')"
check "blocks force-unwrap"     block-suppressions.sh 2 "$(edit a/F.kt '"val n = dto.name!!"')"
check "blocks baseline edit"    block-suppressions.sh 2 '{"tool_name":"Write","tool_input":{"file_path":"config/detekt-baseline.xml","content":"<x/>"}}'
check "allows safe elvis"       block-suppressions.sh 0 "$(edit a/F.kt '"val n = dto.name ?: \"Unknown\""')"
check "allows != and !flag"     block-suppressions.sh 0 "$(edit a/F.kt '"if (a != b && !flag) {}"')"
check "ignores non-kotlin"      block-suppressions.sh 0 "$(edit README.md '"foo!! @Suppress"')"

echo "validate-scent-ticket.sh"
check "blocks missing AC"       validate-scent-ticket.sh 2 "$(ticket '{"Task name":"X"}')"
check "blocks AC without ADS"   validate-scent-ticket.sh 2 "$(ticket '{"Task name":"X","Task type":"🐞 Bug","Acceptance Criteria":"- Login works and a regression test covers it","Feature Branch":"fix/login"}')"
check "blocks wrong branch pfx" validate-scent-ticket.sh 2 "$(ticket '{"Task name":"X","Task type":"🐞 Bug","Acceptance Criteria":"- Login works and a regression test covers the failing case\n- Complies with ADS-STE100 (Either error handling)","Feature Branch":"feature/login"}')"
check "allows well-formed"      validate-scent-ticket.sh 0 "$(ticket '{"Task name":"X","Task type":"🐞 Bug","Acceptance Criteria":"- Login works and a regression test covers the failing case\n- Complies with ADS-STE100 (Either error handling)","Feature Branch":"fix/auth-token-refresh"}')"
check "ignores other databases" validate-scent-ticket.sh 0 '{"tool_name":"mcp__notion__notion-create-pages","tool_input":{"parent":"collection://other","properties":{"Name":"Groceries"}}}'

echo "gate-dependency-additions.sh"
DEPS=".claude/.approved-deps"; rm -f "$DEPS"
write() { printf '{"tool_name":"Write","tool_input":{"file_path":"%s","content":%s}}' "$1" "$2"; }
check "blocks new catalog module"  gate-dependency-additions.sh 2 "$(write gradle/libs.versions.toml '"coil = { module = \"io.coil-kt:coil-compose\", version = \"2.6.0\" }"')"
check "blocks new catalog plugin"  gate-dependency-additions.sh 2 "$(write gradle/libs.versions.toml '"sqldelight = { id = \"app.cash.sqldelight\", version = \"2.0.2\" }"')"
check "blocks inline coordinate"   gate-dependency-additions.sh 2 "$(write composeApp/build.gradle.kts '"implementation(\"com.squareup.retrofit2:retrofit:2.11.0\")"')"
check "allows libs.* reference"    gate-dependency-additions.sh 0 "$(write composeApp/build.gradle.kts '"implementation(libs.ktor.client.core)\nimplementation(libs.koin.core)"')"
check "allows project reference"   gate-dependency-additions.sh 0 "$(write composeApp/build.gradle.kts '"implementation(projects.shared)"')"
check "ignores non-build files"    gate-dependency-additions.sh 0 "$(write a/F.kt '"implementation(\"g:a:1.0\")"')"
bash .claude/hooks/approve-dep.sh io.coil-kt:coil-compose >/dev/null
check "allows once approved"       gate-dependency-additions.sh 0 "$(write gradle/libs.versions.toml '"coil = { module = \"io.coil-kt:coil-compose\", version = \"2.6.0\" }"')"
check "still blocks a different one" gate-dependency-additions.sh 2 "$(write gradle/libs.versions.toml '"other = { module = \"com.example:thing\", version = \"1.0\" }"')"
rm -f "$DEPS"

echo "block-jvm-in-commonmain.sh"
CM="shared/src/commonMain/kotlin/Foo.kt"
AM="shared/src/androidMain/kotlin/Foo.kt"
SV="server/src/main/kotlin/AuthRoutes.kt"
check "blocks java.time import"    block-jvm-in-commonmain.sh 2 "$(edit $CM '"import java.time.LocalDateTime"')"
check "blocks android import"      block-jvm-in-commonmain.sh 2 "$(edit $CM '"import android.content.Context"')"
check "blocks fq java type"        block-jvm-in-commonmain.sh 2 "$(edit $CM '"val id = java.util.UUID.randomUUID()"')"
check "blocks System.currentTime"  block-jvm-in-commonmain.sh 2 "$(edit $CM '"val t = System.currentTimeMillis()"')"
check "blocks Dispatchers.IO"      block-jvm-in-commonmain.sh 2 "$(edit $CM '"withContext(Dispatchers.IO) { load() }"')"
check "blocks Thread.sleep"        block-jvm-in-commonmain.sh 2 "$(edit $CM '"Thread.sleep(100)"')"
check "blocks runCatching"         block-jvm-in-commonmain.sh 2 "$(edit $CM '"return runCatching { api.load() }"')"
check "allows kotlinx-datetime"    block-jvm-in-commonmain.sh 0 "$(edit $CM '"import kotlinx.datetime.Clock\nval t = Clock.System.now().toEpochMilliseconds()"')"
check "allows Either return"       block-jvm-in-commonmain.sh 0 "$(edit $CM '"return dto.toDomain().getOrNull()?.asRight() ?: AppError.Unknown().asLeft()"')"
check "allows java.time in android" block-jvm-in-commonmain.sh 0 "$(edit $AM '"import java.time.LocalDateTime"')"
check "allows java.time in server"  block-jvm-in-commonmain.sh 0 "$(edit $SV '"import java.time.LocalDateTime"')"

echo "block-unverified-push.sh"
rm -f .claude/.scent-gates.json
check "blocks push, no record"  block-unverified-push.sh 2 '{"tool_name":"Bash","tool_input":{"command":"git push origin main"}}'
check "ignores non-push bash"   block-unverified-push.sh 0 '{"tool_name":"Bash","tool_input":{"command":"git status"}}'
printf '{"gate1":"pass","gate2":"pass","gate3":"pass","gate4":"pass","gate5":"pass"}' > .claude/.scent-gates.json
check "allows push when green"  block-unverified-push.sh 0 '{"tool_name":"Bash","tool_input":{"command":"git push origin main"}}'
printf '{"gate1":"pass","gate2":"fail","gate3":"pass","gate4":"pass","gate5":"pass"}' > .claude/.scent-gates.json
check "blocks push, gate2 red"  block-unverified-push.sh 2 '{"tool_name":"Bash","tool_input":{"command":"git push origin main"}}'
rm -f .claude/.scent-gates.json

echo
echo "$PASS passed, $FAIL failed"
[ "$FAIL" = "0" ]
