#!/usr/bin/env python3
"""Maestro flow linter. Rules are documented in .maestro/README.md.

Line-based on purpose: a YAML parser drops comments, and `# UNSAFE: <reason>`
comments are part of what this checks.
"""

import re
import sys
from pathlib import Path

ACTIONS = {"tapOn", "doubleTapOn", "longPressOn", "inputText", "swipe", "scroll"}
ASSERTIONS = {"assertVisible", "assertNotVisible", "extendedWaitUntil"}

# extendedWaitUntil needs a timeout to function; only unusually long waits are overrides.
MAX_WAIT_TIMEOUT_MS = 15000

COMMAND = re.compile(r"^\s*-\s+([A-Za-z]+)\b")
TIMEOUT_KEY = re.compile(r"(?:^|[\s{,])(\w*[Tt]imeout\w*):\s*(\d+)?")
ID_SELECTOR = re.compile(r"(?:^|[\s{,-])id:\s")
OPTIONAL_TRUE = re.compile(r"(?:^|[\s{,])optional:\s*true\b")
POINT = re.compile(r"(?:^|[\s{,])point:")
UNSAFE = re.compile(r"#\s*UNSAFE:\s*\S")


def split_comment(line):
    """Return (code, comment), ignoring '#' inside quoted strings."""
    quote = None
    for i, ch in enumerate(line):
        if quote:
            if ch == quote:
                quote = None
        elif ch in ("'", '"'):
            quote = ch
        elif ch == "#":
            return line[:i], line[i:]
    return line, ""


def lint_file(path):
    lines = path.read_text().splitlines()
    separator = next((i for i, l in enumerate(lines) if l.strip() == "---"), -1)
    violations = []
    pending_action = None  # (line_no, command) awaiting an assertion
    current_command = None

    def report(line_no, rule, message):
        violations.append(f"{path}:{line_no}: {rule}: {message}")

    for index in range(separator + 1, len(lines)):
        line_no = index + 1
        code, comment = split_comment(lines[index])
        justified = bool(UNSAFE.search(comment))

        command = COMMAND.match(code)
        if command:
            current_command = command.group(1)
            if current_command in ACTIONS:
                if pending_action:
                    report(
                        pending_action[0],
                        "assert-after-action",
                        f"`{pending_action[1]}` is followed by `{current_command}` on line "
                        f"{line_no} with no assertVisible / assertNotVisible / extendedWaitUntil between them",
                    )
                pending_action = (line_no, current_command)
            elif current_command in ASSERTIONS:
                pending_action = None

        if POINT.search(code):
            report(line_no, "no-coordinate-tap", "coordinate taps (`point:`) are banned; select by text or accessibility label")

        for match in TIMEOUT_KEY.finditer(code):
            key, value = match.group(1), match.group(2)
            is_normal_wait = (
                current_command == "extendedWaitUntil"
                and key == "timeout"
                and value is not None
                and int(value) <= MAX_WAIT_TIMEOUT_MS
            )
            if not is_normal_wait and not justified:
                report(line_no, "unsafe-timeout", f"`{key}` override needs a trailing `# UNSAFE: <reason>`")

        if OPTIONAL_TRUE.search(code) and not justified:
            report(line_no, "unsafe-optional", "`optional: true` needs a trailing `# UNSAFE: <reason>`")

        if ID_SELECTOR.search(code) and not justified:
            report(line_no, "unsafe-id-selector", "`id:` selectors need a trailing `# UNSAFE: <reason>`; prefer text or accessibility label")

    if pending_action:
        report(
            pending_action[0],
            "assert-after-action",
            f"`{pending_action[1]}` ends the flow with nothing asserting its outcome",
        )
    return violations


def collect(targets):
    for target in map(Path, targets):
        if target.is_dir():
            yield from sorted(target.rglob("*.yaml"))
        elif target.exists():
            yield target
        else:
            print(f"e2e-lint: no such file or directory: {target}", file=sys.stderr)
            sys.exit(2)


def main(argv):
    defaults = [d for d in (".maestro/flows", ".maestro/subflows") if Path(d).exists()]
    files = [f for f in collect(argv or defaults) if f.name != "config.yaml"]
    violations = [v for f in files for v in lint_file(f)]
    for violation in violations:
        print(violation)
    print(f"e2e-lint: {len(files)} flow(s) checked, {len(violations)} violation(s)")
    return 1 if violations else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
