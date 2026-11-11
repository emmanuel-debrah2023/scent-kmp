#!/usr/bin/env -S uv run --quiet --script
# /// script
# requires-python = ">=3.12"
# dependencies = ["pyyaml>=6"]
# ///
"""Pre-merge accessibility checker for Scent.

Reads the accessibility tree a screen reader would consume, normalises it, and applies fixed
rules, so a device dump is judged the same way every time. Two adapters feed one node model:

- iOS: `axe describe-ui` JSON (AXe reads the simulator's accessibility tree, points).
- Android: `uiautomator dump` XML (the AccessibilityNodeInfo tree TalkBack reads, px -> dp).
  Maestro's `hierarchy` output was the first choice, but it has no `checkable` field, so an
  unchecked toggle is indistinguishable from plain text there.

Rules (stable IDs, used in the report):
  A11Y-001  every actionable node has a label, its own or a merged descendant's
  A11Y-002  tap targets >= 48x48 dp (Android) / 44x44 pt (iOS), per-screen allowlist
  A11Y-003  toggles expose a checked state
  A11Y-004  the screen's required labels are present
  A11Y-005  (iOS) the screen isn't collapsed into one opaque node

Usage:
  a11y_check.py check --platform ios --dump home.json --screen home \
      [--expectations .maestro/a11y/expectations/home.yaml] [--density 420] [--json out.json]
  a11y_check.py screens --config .maestro/a11y/screens.yaml --changed changed.txt
  a11y_check.py empty --platform ios --dump home.json        (exit 0 when the tree is empty)
  a11y_check.py digest --platform ios --dump home.json       (fingerprint, to tell a settled screen)
  a11y_check.py report --dir build/pre-merge --status status.tsv --out report.md --json report.json

`check` exits 1 when there are violations, 2 on bad input.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Iterator

import yaml

MIN_TARGET = {"android": 48.0, "ios": 44.0}
ANDROID_BASELINE_DPI = 160

# AXe `type` values that VoiceOver treats as operable controls.
IOS_ACTIONABLE = {
    "Button", "Link", "Switch", "CheckBox", "RadioButton", "Toggle", "Slider", "Stepper",
    "TextField", "TextArea", "SecureTextField", "SearchField", "Tab", "PopUpButton", "MenuItem",
}
IOS_TOGGLES = {"Switch", "CheckBox", "RadioButton", "Toggle"}
IOS_ON = {"1", "on", "true", "checked", "selected"}
IOS_OFF = {"0", "off", "false", "unchecked", "not selected"}

RULE_TITLES = {
    "A11Y-001": "Actionable element has no label",
    "A11Y-002": "Tap target below minimum size",
    "A11Y-003": "Toggle does not expose its state",
    "A11Y-004": "Required label missing",
    "A11Y-005": "Screen collapsed into one accessibility node",
}


# ---- Model ------------------------------------------------------------------------------------

@dataclass(frozen=True)
class Bounds:
    x: float
    y: float
    width: float
    height: float

    def clear_of_edges(self, outer: Bounds, margin: float = 1.0) -> bool:
        """Inside [outer] and not touching its edges. Both trees report a node the viewport clips
        at its visible size, so a node touching an edge may be bigger than it looks."""
        return (
            self.width > 0 and self.height > 0
            and self.x >= outer.x + margin and self.y >= outer.y + margin
            and self.x + self.width <= outer.x + outer.width - margin
            and self.y + self.height <= outer.y + outer.height - margin
        )

    def intersects(self, outer: Bounds) -> bool:
        return (
            self.x < outer.x + outer.width and self.x + self.width > outer.x
            and self.y < outer.y + outer.height and self.y + self.height > outer.y
        )

    def text(self) -> str:
        return f"{self.x:.0f},{self.y:.0f} {self.width:.0f}x{self.height:.0f}"


@dataclass
class Node:
    label: str = ""
    role: str = ""
    bounds: Bounds = field(default_factory=lambda: Bounds(0, 0, 0, 0))
    clickable: bool = False
    checkable: bool = False
    checked: bool | None = None  # None: no state exposed
    enabled: bool = True
    children: list[Node] = field(default_factory=list)

    @property
    def actionable(self) -> bool:
        return self.clickable or self.checkable

    def walk(self) -> Iterator[Node]:
        yield self
        for child in self.children:
            yield from child.walk()


@dataclass
class Expectations:
    required: list[str] = field(default_factory=list)
    toggles: list[str] = field(default_factory=list)
    tap_target_allowlist: list[str] = field(default_factory=list)


@dataclass
class Violation:
    rule: str
    label: str
    bounds: str
    detail: str


# ---- Adapters ---------------------------------------------------------------------------------

_BOUNDS = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")


def parse_uiautomator(xml_text: str, density: float, package: str | None = None) -> Node:
    """uiautomator XML -> Node tree, bounds in dp. Nodes from other packages are dropped."""
    end = xml_text.rfind("</hierarchy>")
    if end != -1:
        xml_text = xml_text[: end + len("</hierarchy>")]
    scale = ANDROID_BASELINE_DPI / density
    hierarchy = ET.fromstring(xml_text)

    def convert(element: ET.Element) -> Node:
        a = element.attrib
        match = _BOUNDS.fullmatch(a.get("bounds", "")) or _BOUNDS.fullmatch("[0,0][0,0]")
        left, top, right, bottom = (int(v) for v in match.groups())
        checkable = a.get("checkable") == "true"
        return Node(
            label=(a.get("content-desc") or a.get("text") or "").strip(),
            role=a.get("class", ""),
            bounds=Bounds(left * scale, top * scale, (right - left) * scale, (bottom - top) * scale),
            clickable=a.get("clickable") == "true" or a.get("long-clickable") == "true",
            checkable=checkable,
            checked=(a.get("checked") == "true") if checkable else None,
            enabled=a.get("enabled", "true") == "true",
            children=[
                convert(c) for c in element
                if c.tag == "node" and (package is None or c.attrib.get("package") == package)
            ],
        )

    roots = [c for c in hierarchy if c.tag == "node" and (package is None or c.attrib.get("package") == package)]
    if len(roots) == 1:
        return convert(roots[0])
    children = [convert(c) for c in roots]
    return Node(role="hierarchy", bounds=_union(children), children=children)


def parse_axe(json_text: str) -> Node:
    """`axe describe-ui` JSON -> Node tree, bounds in pt."""
    data = json.loads(json_text) if json_text.strip() else []
    items = data if isinstance(data, list) else [data]

    def convert(item: dict) -> Node:
        f = item.get("frame") or {}
        role = item.get("type") or item.get("role") or ""
        value = item.get("AXValue")
        traits = {str(t).lower() for t in item.get("traits") or []}
        is_toggle = role in IOS_TOGGLES
        checked: bool | None = None
        if "selected" in traits:
            checked = True
        elif isinstance(value, str) and value.strip().lower() in IOS_ON | IOS_OFF:
            checked = value.strip().lower() in IOS_ON
        elif isinstance(value, (bool, int)) and is_toggle:
            checked = bool(value)
        return Node(
            label=(item.get("AXLabel") or item.get("title") or "").strip(),
            role=role,
            bounds=Bounds(float(f.get("x", 0)), float(f.get("y", 0)), float(f.get("width", 0)), float(f.get("height", 0))),
            clickable=role in IOS_ACTIONABLE or "button" in traits or "link" in traits,
            checkable=is_toggle,
            checked=checked,
            enabled=item.get("enabled", True) is not False,
            children=[convert(c) for c in item.get("children") or []],
        )

    roots = [convert(i) for i in items]
    if len(roots) == 1:
        return roots[0]
    return Node(role="hierarchy", bounds=_union(roots), children=roots)


def _union(nodes: list[Node]) -> Bounds:
    if not nodes:
        return Bounds(0, 0, 0, 0)
    x0 = min(n.bounds.x for n in nodes)
    y0 = min(n.bounds.y for n in nodes)
    x1 = max(n.bounds.x + n.bounds.width for n in nodes)
    y1 = max(n.bounds.y + n.bounds.height for n in nodes)
    return Bounds(x0, y0, x1 - x0, y1 - y0)


def load_dump(platform: str, path: Path, density: float, package: str | None) -> Node:
    text = path.read_text()
    if platform == "ios":
        return parse_axe(text)
    return parse_uiautomator(text, density=density, package=package)


def digest(root: Node) -> str:
    """Fingerprint of what the checker reads, so two dumps of a settled screen compare equal."""
    parts = [
        f"{n.role}|{n.label}|{n.bounds.text()}|{n.clickable}|{n.checkable}|{n.checked}|{n.enabled}"
        for n in root.walk()
    ]
    return hashlib.sha256("\n".join(parts).encode()).hexdigest()


def is_empty_tree(root: Node) -> bool:
    """True when the dump has nothing a screen reader could read: no root, or no labels below it."""
    return not any(n.label for n in root.walk() if n is not root)


# ---- Rules ------------------------------------------------------------------------------------

def _matches(pattern: str, label: str) -> bool:
    if pattern.startswith("re:"):
        return re.fullmatch(pattern[3:], label, flags=re.DOTALL) is not None
    return label == pattern


def _merged_label(node: Node) -> str:
    """The node's own label, else the first label among descendants that aren't separately actionable."""
    if node.label:
        return node.label
    for child in node.children:
        if child.actionable:
            continue
        found = _merged_label(child)
        if found:
            return found
    return ""


def check(root: Node, platform: str, expectations: Expectations) -> list[Violation]:
    violations: list[Violation] = []
    screen = root.bounds
    minimum = MIN_TARGET[platform]
    unit = "dp" if platform == "android" else "pt"
    nodes = list(root.walk())

    for n in nodes:
        if n is root or not n.actionable:
            continue
        label = _merged_label(n)
        # Scrolled entirely off screen: iOS drops their text until they scroll in, so don't judge.
        if not label and n.bounds.intersects(screen):
            violations.append(Violation("A11Y-001", "", n.bounds.text(), f"{n.role or 'node'} is actionable but has no label"))
        allowed = any(_matches(p, label) for p in expectations.tap_target_allowlist) if label else False
        if n.bounds.clear_of_edges(screen) and not allowed and (n.bounds.width < minimum or n.bounds.height < minimum):
            violations.append(Violation(
                "A11Y-002", label, n.bounds.text(),
                f"{n.bounds.width:.0f}x{n.bounds.height:.0f}{unit}, minimum {minimum:.0f}x{minimum:.0f}{unit}",
            ))
        if n.checkable and n.checked is None:
            violations.append(Violation("A11Y-003", label, n.bounds.text(), f"{n.role} exposes no checked/selected state"))

    for pattern in expectations.toggles:
        matches = [n for n in nodes if n is not root and _matches(pattern, _merged_label(n))]
        if not any(n.checkable or n.checked is not None for n in matches):
            where = matches[0].bounds.text() if matches else ""
            detail = "found, but not exposed as a toggle" if matches else "not found on screen"
            violations.append(Violation("A11Y-003", pattern, where, f"expected toggle {detail}"))

    labels = [n.label for n in nodes if n is not root and n.label]
    for pattern in expectations.required:
        if not any(_matches(pattern, label) for label in labels):
            violations.append(Violation("A11Y-004", pattern, "", "required label not in the accessibility tree"))

    if platform == "ios" and len(expectations.required) > 1 and len(set(labels)) <= 1:
        violations.append(Violation(
            "A11Y-005", labels[0] if labels else "", screen.text(),
            f"{len(expectations.required)} elements expected, the tree exposes {len(set(labels))} labelled node(s)",
        ))
    return violations


# ---- Expectations and screen mapping ----------------------------------------------------------

def load_expectations(path: Path | None) -> Expectations:
    if path is None or not path.exists():
        return Expectations()
    data = yaml.safe_load(path.read_text()) or {}
    return Expectations(
        required=[str(x) for x in data.get("required") or []],
        toggles=[str(x) for x in data.get("toggles") or []],
        tap_target_allowlist=[str(x) for x in data.get("tap_target_allowlist") or []],
    )


def _glob_to_regex(glob: str) -> re.Pattern[str]:
    out, i = [], 0
    while i < len(glob):
        if glob.startswith("**/", i):
            out.append("(?:.*/)?")
            i += 3
        elif glob.startswith("**", i):
            out.append(".*")
            i += 2
        elif glob[i] == "*":
            out.append("[^/]*")
            i += 1
        elif glob[i] == "?":
            out.append("[^/]")
            i += 1
        else:
            out.append(re.escape(glob[i]))
            i += 1
    return re.compile("".join(out))


# Changed app code that no screen claims could affect any of them, so it runs every screen.
APP_CODE = [_glob_to_regex(g) for g in ("composeApp/src/**", "shared/src/**", "iosApp/**")]
TEST_CODE = _glob_to_regex("**/src/*Test*/**")


def screens_for_changes(config: Path, changed: list[str]) -> list[str]:
    data = yaml.safe_load(config.read_text()) or {}
    screens: dict = data.get("screens") or {}
    shared = [_glob_to_regex(g) for g in data.get("shared") or []]
    every = sorted(screens)
    picked: set[str] = set()
    for path in changed:
        if TEST_CODE.fullmatch(path):
            continue
        if any(p.fullmatch(path) for p in shared):
            return every
        owners = [name for name, spec in screens.items()
                  if any(_glob_to_regex(g).fullmatch(path) for g in (spec or {}).get("sources") or [])]
        if owners:
            picked.update(owners)
        elif any(p.fullmatch(path) for p in APP_CODE):
            return every
    return sorted(picked)


# ---- Report -----------------------------------------------------------------------------------

def write_report(result_dir: Path, status_file: Path, out_md: Path, out_json: Path) -> int:
    rows = []
    for line in status_file.read_text().splitlines() if status_file.exists() else []:
        if not line.strip():
            continue
        platform, screen, flow, retried, capture, *note = line.split("\t") + [""]
        result_path = result_dir / platform / f"{screen}.result.json"
        violations = json.loads(result_path.read_text())["violations"] if result_path.exists() else []
        if capture == "ok" and not result_path.exists():
            # The checker never produced a verdict (unreadable dump): that is never a pass.
            capture = "unchecked"
            note = [note[0] or "the checker produced no result for this dump"]
        if flow != "pass" or capture != "ok":
            verdict = "infra-fail"
        elif violations:
            verdict = "fail"
        else:
            verdict = "pass"
        rows.append({
            "platform": platform, "screen": screen, "flow": flow, "retried": retried == "yes",
            "capture": capture, "note": note[0].strip(), "verdict": verdict, "violations": violations,
        })

    md = ["# Pre-merge device check", "", "| Platform | Screen | Flow | Retried | Tree | Violations | Result |",
          "|---|---|---|---|---|---|---|"]
    for r in rows:
        md.append(f"| {r['platform']} | {r['screen']} | {r['flow']} | {'yes' if r['retried'] else 'no'} | "
                  f"{r['capture']} | {len(r['violations'])} | **{r['verdict']}** |")
    if not rows:
        md.append("| - | - | - | - | - | - | nothing ran |")
    notes = [r for r in rows if r["note"]]
    if notes:
        md += ["", "## Infrastructure notes", ""]
        md += [f"- `{r['platform']}/{r['screen']}`: {r['note']}" for r in notes]
    failing = [r for r in rows if r["violations"]]
    if failing:
        md += ["", "## Violations", ""]
        for r in failing:
            md += [f"### {r['platform']} · {r['screen']}", "", "| Rule | Label | Bounds | Detail |", "|---|---|---|---|"]
            for v in r["violations"]:
                label = v["label"].replace("|", "\\|") or "(none)"
                md.append(f"| {v['rule']} {RULE_TITLES[v['rule']]} | {label} | {v['bounds'] or '-'} | {v['detail']} |")
            md.append("")
    out_md.write_text("\n".join(md).rstrip() + "\n")
    out_json.write_text(json.dumps({"results": rows}, indent=2) + "\n")
    return 0 if rows and all(r["verdict"] == "pass" for r in rows) else 1


# ---- CLI --------------------------------------------------------------------------------------

def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)

    c = sub.add_parser("check")
    c.add_argument("--platform", choices=["android", "ios"], required=True)
    c.add_argument("--dump", type=Path, required=True)
    c.add_argument("--screen", required=True)
    c.add_argument("--expectations", type=Path)
    c.add_argument("--density", type=float, default=ANDROID_BASELINE_DPI)
    c.add_argument("--package", default=None)
    c.add_argument("--json", type=Path)

    for name in ("empty", "digest"):
        e = sub.add_parser(name)
        e.add_argument("--platform", choices=["android", "ios"], required=True)
        e.add_argument("--dump", type=Path, required=True)
        e.add_argument("--density", type=float, default=ANDROID_BASELINE_DPI)
        e.add_argument("--package", default=None)

    s = sub.add_parser("screens")
    s.add_argument("--config", type=Path, required=True)
    s.add_argument("--changed", type=Path, required=True)

    r = sub.add_parser("report")
    r.add_argument("--dir", type=Path, required=True)
    r.add_argument("--status", type=Path, required=True)
    r.add_argument("--out", type=Path, required=True)
    r.add_argument("--json", type=Path, required=True)

    args = parser.parse_args(argv)

    if args.command == "screens":
        changed = [line.strip() for line in args.changed.read_text().splitlines() if line.strip()]
        print("\n".join(screens_for_changes(args.config, changed)))
        return 0
    if args.command == "report":
        return write_report(args.dir, args.status, args.out, args.json)

    try:
        root = load_dump(args.platform, args.dump, args.density, args.package)
    except (OSError, ValueError, ET.ParseError) as error:
        print(f"a11y_check: cannot read {args.dump}: {error}", file=sys.stderr)
        return 2
    if args.command == "empty":
        return 0 if is_empty_tree(root) else 1
    if args.command == "digest":
        print(digest(root))
        return 0

    violations = check(root, args.platform, load_expectations(args.expectations))
    if args.json:
        args.json.parent.mkdir(parents=True, exist_ok=True)
        args.json.write_text(json.dumps(
            {"platform": args.platform, "screen": args.screen, "violations": [asdict(v) for v in violations]}, indent=2,
        ) + "\n")
    for v in violations:
        print(f"[{args.platform}] {args.screen}: {v.rule} {v.label or '(no label)'} @ {v.bounds or '-'}: {v.detail}")
    return 1 if violations else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
