"""Tests for scripts/a11y_check.py. Run: uv run --with pytest --with pyyaml pytest scripts/tests"""

import json
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import a11y_check as ac  # noqa: E402

FIXTURES = Path(__file__).parent / "fixtures"
PIXEL_8_DENSITY = 420


def android(xml_body: str, width=1080, height=2400):
    """Wraps hand-made uiautomator nodes in a screen-sized app root."""
    return ac.parse_uiautomator(
        f'<hierarchy rotation="0"><node class="android.widget.FrameLayout" package="org.scent.project" '
        f'text="" content-desc="" checkable="false" checked="false" clickable="false" enabled="true" '
        f'focusable="false" long-clickable="false" bounds="[0,0][{width},{height}]">{xml_body}</node></hierarchy>',
        density=160,
    )


def node(text="", desc="", clickable=False, checkable=False, checked=False, bounds="[0,0][100,100]", children="", cls="android.view.View"):
    return (
        f'<node class="{cls}" package="org.scent.project" text="{text}" content-desc="{desc}" '
        f'checkable="{str(checkable).lower()}" checked="{str(checked).lower()}" clickable="{str(clickable).lower()}" '
        f'enabled="true" focusable="{str(clickable).lower()}" long-clickable="false" bounds="{bounds}">{children}</node>'
    )


def ios(children, width=402, height=874):
    return ac.parse_axe(json.dumps([
        {"type": "Application", "AXLabel": "Scent", "frame": {"x": 0, "y": 0, "width": width, "height": height}, "children": children}
    ]))


def ax(type_, label=None, value=None, frame=(0, 0, 100, 100), children=(), traits=()):
    x, y, w, h = frame
    return {"type": type_, "AXLabel": label, "AXValue": value, "enabled": True, "traits": list(traits),
            "frame": {"x": x, "y": y, "width": w, "height": h}, "children": list(children)}


def rule_ids(violations):
    return sorted({v.rule for v in violations})


# ---- Adapters ---------------------------------------------------------------------------------

def test_parse_uiautomator_real_dump_converts_px_to_dp_and_keeps_labels():
    root = ac.parse_uiautomator((FIXTURES / "android-home.uiautomator.xml").read_text(), density=PIXEL_8_DENSITY)

    notifications = next(n for n in root.walk() if n.label == "Notifications")
    # [933,164][996,227] px at 420 dpi is 24 x 24 dp.
    assert notifications.bounds.width == pytest.approx(24, abs=0.1)
    assert notifications.bounds.height == pytest.approx(24, abs=0.1)
    assert root.bounds.width == pytest.approx(411.4, abs=0.1)


def test_parse_uiautomator_reads_text_as_label_and_flags():
    root = android(node(text="Apply", clickable=True) + node(desc="New", checkable=True, checked=True))

    apply, new = root.children
    assert (apply.label, apply.clickable, apply.checkable) == ("Apply", True, False)
    assert (new.label, new.checkable, new.checked) == ("New", True, True)


def test_parse_uiautomator_strips_trailer_and_ignores_other_packages():
    xml = (
        '<hierarchy><node class="x" package="com.android.systemui" text="12:00" content-desc="" checkable="false" '
        'checked="false" clickable="false" enabled="true" focusable="false" long-clickable="false" bounds="[0,0][10,10]" />'
        "</hierarchy>UI hierchary dumped to: /dev/tty"
    )
    root = ac.parse_uiautomator(xml, density=160, package="org.scent.project")

    assert [n.label for n in root.walk() if n.label] == []


def test_parse_axe_real_dump_reads_labels_types_and_points():
    root = ac.parse_axe((FIXTURES / "ios-home.axe.json").read_text())

    view = next(n for n in root.walk() if n.label == "VIEW FRAGRANCE" and n.role == "Button")
    assert view.clickable
    assert (view.bounds.width, view.bounds.height) == (pytest.approx(282.3, abs=0.1), 48)


def test_parse_axe_exposes_toggle_value_as_checked():
    root = ios([ax("Switch", "Negotiable", value="1"), ax("CheckBox", "New", value=None)])

    switch, checkbox = root.children
    assert (switch.checkable, switch.checked) == (True, True)
    assert (checkbox.checkable, checkbox.checked) == (True, None)


def test_parse_axe_treats_selected_trait_as_exposed_state():
    root = ios([ax("Button", "New", traits=["Selected"])])

    assert root.children[0].checked is True


# ---- A11Y-001 label ---------------------------------------------------------------------------

def test_a11y_001_flags_unlabelled_clickable():
    root = android(node(clickable=True, children=node(cls="android.widget.Button")))

    assert rule_ids(ac.check(root, "android", ac.Expectations())) == ["A11Y-001"]


def test_a11y_001_accepts_label_on_merged_descendant():
    root = android(node(clickable=True, children=node(desc="Notifications")))

    assert ac.check(root, "android", ac.Expectations()) == []


def test_a11y_001_does_not_borrow_label_from_nested_actionable():
    inner = node(desc="Play video", clickable=True)
    root = android(node(clickable=True, bounds="[0,0][200,200]", children=inner))

    violations = ac.check(root, "android", ac.Expectations())
    assert [(v.rule, v.label) for v in violations] == [("A11Y-001", "")]


def test_a11y_001_skips_nodes_entirely_off_screen():
    # iOS reports scrolled-away tabs in a row with no text; they can't be judged until scrolled in.
    root = ios([ax("Button", None, frame=(462, 412, 83, 44))])

    assert ac.check(root, "ios", ac.Expectations()) == []


def test_a11y_001_flags_unlabelled_ios_button():
    root = ios([ax("Button", None, frame=(0, 0, 48, 48))])

    assert rule_ids(ac.check(root, "ios", ac.Expectations())) == ["A11Y-001"]


# ---- A11Y-002 tap target ----------------------------------------------------------------------

def test_a11y_002_flags_android_target_under_48dp():
    root = android(node(desc="Home", clickable=True, bounds="[10,10][58,45]"))

    violations = ac.check(root, "android", ac.Expectations())
    assert [(v.rule, v.label) for v in violations] == [("A11Y-002", "Home")]


def test_a11y_002_uses_44pt_on_ios():
    root = ios([ax("Button", "Ok", frame=(10, 10, 44, 44)), ax("Button", "Tab", frame=(100, 10, 48, 36))])

    violations = ac.check(root, "ios", ac.Expectations())
    assert [(v.rule, v.label) for v in violations] == [("A11Y-002", "Tab")]


def test_a11y_002_skips_targets_partly_off_screen():
    root = android(node(desc="Card", clickable=True, bounds="[10,2300][1070,2320]"), height=2400)
    clipped = android(node(desc="Card", clickable=True, bounds="[0,2390][1080,2500]"), height=2400)

    assert rule_ids(ac.check(root, "android", ac.Expectations())) == ["A11Y-002"]
    assert ac.check(clipped, "android", ac.Expectations()) == []


def test_a11y_002_skips_targets_cut_off_at_the_screen_edge():
    # Both trees report a node clipped by the viewport at its visible size, e.g. a 10 dp sliver.
    root = android(node(desc="5ml", clickable=True, bounds="[0,2374][180,2400]"), height=2400)

    assert ac.check(root, "android", ac.Expectations()) == []


def test_a11y_002_respects_allowlist():
    root = android(node(desc="Home", clickable=True, bounds="[10,10][58,45]"))

    assert ac.check(root, "android", ac.Expectations(tap_target_allowlist=["Home"])) == []


# ---- A11Y-003 toggle state --------------------------------------------------------------------

def test_a11y_003_flags_checkable_without_exposed_state():
    root = ios([ax("CheckBox", "New", frame=(0, 0, 48, 48))])

    assert rule_ids(ac.check(root, "ios", ac.Expectations())) == ["A11Y-003"]


def test_a11y_003_flags_expected_toggle_that_is_not_checkable():
    root = android(node(text="New", clickable=True))

    violations = ac.check(root, "android", ac.Expectations(toggles=["New"]))
    assert [(v.rule, v.label) for v in violations] == [("A11Y-003", "New")]


def test_a11y_003_accepts_expected_toggle_with_state():
    root = android(node(text="New", clickable=True, checkable=True))

    assert ac.check(root, "android", ac.Expectations(toggles=["New"])) == []


# ---- A11Y-004 required labels -----------------------------------------------------------------

def test_a11y_004_flags_missing_required_label():
    root = android(node(text="Marketplace"))

    violations = ac.check(root, "android", ac.Expectations(required=["Marketplace", "Filters"]))
    assert [(v.rule, v.label) for v in violations] == [("A11Y-004", "Filters")]


def test_a11y_004_matches_regex_entries_in_full():
    root = android(node(text="Santal 33, NEW condition, sold by scent_seed_seller"))

    ok = ac.Expectations(required=["re:.*NEW condition.*"])
    partial = ac.Expectations(required=["re:NEW condition"])
    assert ac.check(root, "android", ok) == []
    assert rule_ids(ac.check(root, "android", partial)) == ["A11Y-004"]


# ---- A11Y-005 collapsed screen (iOS) ----------------------------------------------------------

def test_a11y_005_flags_ios_screen_collapsed_into_one_node():
    root = ios([ax("Group", "Marketplace Filters New Apply", frame=(0, 0, 402, 874))])

    violations = ac.check(root, "ios", ac.Expectations(required=["Marketplace", "Filters"]))
    assert "A11Y-005" in rule_ids(violations)


def test_a11y_005_ignores_android():
    root = android(node(text="Marketplace"))

    violations = ac.check(root, "android", ac.Expectations(required=["Marketplace", "Filters"]))
    assert "A11Y-005" not in rule_ids(violations)


def test_real_ios_home_dump_reports_known_small_targets():
    root = ac.parse_axe((FIXTURES / "ios-home.axe.json").read_text())

    small = {v.label for v in ac.check(root, "ios", ac.Expectations()) if v.rule == "A11Y-002"}
    assert {"Favourite", "Home", "Market"} <= small


# ---- Report -----------------------------------------------------------------------------------

def report_rows(tmp_path, status_lines, results=None):
    for (platform, screen), violations in (results or {}).items():
        (tmp_path / platform).mkdir(exist_ok=True)
        (tmp_path / platform / f"{screen}.result.json").write_text(json.dumps({"violations": violations}))
    (tmp_path / "status.tsv").write_text("".join(line + "\n" for line in status_lines))
    code = ac.write_report(tmp_path, tmp_path / "status.tsv", tmp_path / "report.md", tmp_path / "report.json")
    return code, json.loads((tmp_path / "report.json").read_text())["results"]


def test_report_never_passes_a_screen_without_a_checker_result(tmp_path):
    code, rows = report_rows(tmp_path, ["android\task\tpass\tno\tok\t"])

    assert code == 1
    assert rows[0]["verdict"] == "infra-fail"


def test_report_separates_violations_from_infrastructure_failures(tmp_path):
    violation = {"rule": "A11Y-002", "label": "Home", "bounds": "", "detail": ""}
    code, rows = report_rows(
        tmp_path,
        ["ios\thome\tpass\tno\tok\t", "ios\task\tpass\tyes\tok\t", "android\thome\tfail\tyes\tskipped\tflow failed twice"],
        {("ios", "home"): [violation], ("ios", "ask"): []},
    )

    assert code == 1
    assert [(r["screen"], r["platform"], r["verdict"], r["retried"]) for r in rows] == [
        ("home", "ios", "fail", False), ("ask", "ios", "pass", True), ("home", "android", "infra-fail", True),
    ]


# ---- Settle digest ----------------------------------------------------------------------------

def test_digest_ignores_key_order_but_tracks_moving_bounds():
    first = ac.parse_axe('[{"type": "Button", "AXLabel": "Ok", "frame": {"x": 1, "y": 2, "width": 50, "height": 50}}]')
    reordered = ac.parse_axe('[{"frame": {"height": 50, "width": 50, "y": 2, "x": 1}, "AXLabel": "Ok", "type": "Button"}]')
    moved = ac.parse_axe('[{"type": "Button", "AXLabel": "Ok", "frame": {"x": 1, "y": 40, "width": 50, "height": 50}}]')

    assert ac.digest(first) == ac.digest(reordered)
    assert ac.digest(first) != ac.digest(moved)


# ---- Empty tree, expectations, screen mapping -------------------------------------------------

def test_is_empty_tree_true_for_rootless_or_label_free_dump():
    assert ac.is_empty_tree(ac.parse_axe("[]"))
    assert ac.is_empty_tree(ios([ax("Group", None)]))
    assert not ac.is_empty_tree(ios([ax("Button", "Ok")]))


def test_load_expectations_reads_yaml(tmp_path):
    path = tmp_path / "home.yaml"
    path.write_text("required:\n  - Home\ntoggles:\n  - New\ntap_target_allowlist:\n  - Market\n")

    assert ac.load_expectations(path) == ac.Expectations(required=["Home"], toggles=["New"], tap_target_allowlist=["Market"])


def test_screens_for_changes_maps_globs_and_falls_back_to_all(tmp_path):
    screens = tmp_path / "screens.yaml"
    screens.write_text(
        "screens:\n"
        "  home:\n    sources: ['composeApp/src/commonMain/kotlin/ui/home/**']\n"
        "  marketplace:\n    sources: ['composeApp/src/commonMain/kotlin/ui/marketplace/**']\n"
        "shared:\n  - 'composeApp/src/commonMain/kotlin/ui/components/**'\n"
    )

    assert ac.screens_for_changes(screens, ["composeApp/src/commonMain/kotlin/ui/home/HomeFullBleedScreen.kt"]) == ["home"]
    assert ac.screens_for_changes(screens, ["composeApp/src/commonMain/kotlin/ui/components/Chip.kt"]) == ["home", "marketplace"]
    assert ac.screens_for_changes(screens, ["composeApp/src/commonMain/kotlin/ui/ask/AskScreen.kt"]) == ["home", "marketplace"]
    assert ac.screens_for_changes(screens, ["server/src/main/kotlin/Routes.kt", "README.md"]) == []
