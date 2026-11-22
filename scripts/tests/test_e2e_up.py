"""Tests for scripts/e2e-up.sh. Run: uv run --with pytest --with pyyaml pytest scripts/tests"""

import os
import shutil
import signal
import socket
import subprocess
import sys
import time
from pathlib import Path

import pytest

SCRIPT = Path(__file__).resolve().parents[1] / "e2e-up.sh"
FAKE_SERVER = Path(__file__).parent / "fixtures" / "fake_e2e_server.py"


def free_port():
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def listening(port):
    with socket.socket() as s:
        return s.connect_ex(("127.0.0.1", port)) == 0


def wait_until(condition, timeout=10):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if condition():
            return True
        time.sleep(0.1)
    return False


def listener_pid(port):
    out = subprocess.run(["lsof", "-tiTCP:%d" % port, "-sTCP:LISTEN"], capture_output=True, text=True).stdout
    return int(out.split()[0]) if out.strip() else None


@pytest.fixture
def postgres():
    """A Postgres port that accepts connections."""
    pg = socket.socket()
    pg.bind(("127.0.0.1", 0))
    pg.listen()
    yield pg.getsockname()[1]
    pg.close()


@pytest.fixture
def api_port():
    port = free_port()
    yield port
    pid = listener_pid(port)
    if pid:
        os.kill(pid, signal.SIGTERM)


@pytest.fixture
def running_server(api_port):
    """A fake API server already up on the port, as its own process."""
    proc = subprocess.Popen([sys.executable, str(FAKE_SERVER), str(api_port)])
    assert wait_until(lambda: listening(api_port))
    yield proc
    proc.kill()
    proc.wait()


@pytest.fixture
def repo(tmp_path, postgres):
    """A throwaway checkout holding the script, a .env and a gradlew that starts the fake server."""
    checkout = tmp_path / "checkout"
    subprocess.run(["git", "init", "-q", str(checkout)], check=True)
    (checkout / "scripts").mkdir()
    shutil.copy(SCRIPT, checkout / "scripts" / "e2e-up.sh")
    (checkout / ".env").write_text(f"DATABASE_URL=jdbc:postgresql://127.0.0.1:{postgres}/scent\n")
    gradlew = checkout / "gradlew"
    gradlew.write_text(f'#!/bin/bash\nexec "{sys.executable}" "{FAKE_SERVER}" "$FAKE_API_PORT"\n')
    gradlew.chmod(0o755)
    return checkout


@pytest.fixture
def state_dir(tmp_path):
    return tmp_path / "state"


def run_e2e_up(repo, api_port, state_dir, *args, **env):
    return subprocess.run(
        ["bash", "scripts/e2e-up.sh", *args],
        cwd=repo,
        env={
            "PATH": "/usr/bin:/bin:/usr/sbin:/sbin",
            "HOME": str(state_dir.parent),
            "E2E_API_URL": f"http://127.0.0.1:{api_port}",
            "E2E_STATE_DIR": str(state_dir),
            "FAKE_API_PORT": str(api_port),
            **env,
        },
        capture_output=True,
        text=True,
        timeout=60,
    )


def state_file(state_dir, api_port):
    return state_dir / f"e2e-server-{api_port}.env"


def write_state(state_dir, api_port, **values):
    state_dir.mkdir(parents=True, exist_ok=True)
    state_file(state_dir, api_port).write_text("".join(f"{k}={v}\n" for k, v in values.items()))


def read_state(state_dir, api_port):
    lines = state_file(state_dir, api_port).read_text().splitlines()
    return dict(line.split("=", 1) for line in lines)


def fingerprint(repo, api_port, state_dir):
    result = run_e2e_up(repo, api_port, state_dir, "--fingerprint")
    assert result.returncode == 0, result.stderr
    return result.stdout.strip()


def test_unknown_server_is_reused_and_left_running(repo, api_port, state_dir, running_server):
    result = run_e2e_up(repo, api_port, state_dir)

    assert result.returncode == 0, result.stderr
    assert "not started by e2e-up.sh" in result.stdout
    assert "E2E stack ready" in result.stdout
    assert running_server.poll() is None


def test_unknown_server_does_not_offer_to_stop_it(repo, api_port, state_dir, running_server):
    result = run_e2e_up(repo, api_port, state_dir)

    assert "--down" not in result.stdout


def test_server_with_matching_fingerprint_is_reused(repo, api_port, state_dir, running_server):
    write_state(state_dir, api_port, SERVER_PID=running_server.pid, CHECKOUT=repo,
                FINGERPRINT=fingerprint(repo, api_port, state_dir))

    result = run_e2e_up(repo, api_port, state_dir)

    assert result.returncode == 0, result.stderr
    assert "reusing it" in result.stdout
    assert running_server.poll() is None


def test_stale_server_is_restarted_and_recorded(repo, api_port, state_dir, running_server):
    write_state(state_dir, api_port, SERVER_PID=running_server.pid, CHECKOUT="/elsewhere", FINGERPRINT="stale")

    result = run_e2e_up(repo, api_port, state_dir)

    assert result.returncode == 0, result.stderr
    assert "restarting" in result.stdout
    assert running_server.wait(timeout=10) is not None
    state = read_state(state_dir, api_port)
    assert int(state["SERVER_PID"]) == listener_pid(api_port) != running_server.pid
    assert state["FINGERPRINT"] == fingerprint(repo, api_port, state_dir)


def test_stale_server_is_kept_when_reuse_is_forced(repo, api_port, state_dir, running_server):
    write_state(state_dir, api_port, SERVER_PID=running_server.pid, CHECKOUT="/elsewhere", FINGERPRINT="stale")

    result = run_e2e_up(repo, api_port, state_dir, E2E_REUSE="always")

    assert result.returncode == 0, result.stderr
    assert "E2E_REUSE=always" in result.stdout
    assert running_server.poll() is None


def test_missing_server_is_started_and_recorded(repo, api_port, state_dir):
    result = run_e2e_up(repo, api_port, state_dir)

    assert result.returncode == 0, result.stderr
    state = read_state(state_dir, api_port)
    assert int(state["SERVER_PID"]) == listener_pid(api_port)
    assert state["FINGERPRINT"] == fingerprint(repo, api_port, state_dir)
    assert "./scripts/e2e-up.sh --down" in result.stdout


def test_down_stops_a_server_started_from_another_checkout(repo, api_port, state_dir, running_server):
    write_state(state_dir, api_port, SERVER_PID=running_server.pid, CHECKOUT="/elsewhere", FINGERPRINT="any")

    result = run_e2e_up(repo, api_port, state_dir, "--down")

    assert result.returncode == 0, result.stderr
    assert running_server.wait(timeout=10) is not None
    assert not state_file(state_dir, api_port).exists()


def test_down_leaves_an_unrecorded_server_running(repo, api_port, state_dir, running_server):
    result = run_e2e_up(repo, api_port, state_dir, "--down")

    assert result.returncode == 0, result.stderr
    assert running_server.poll() is None


def test_fingerprint_changes_with_uncommitted_server_code(repo, api_port, state_dir):
    before = fingerprint(repo, api_port, state_dir)
    (repo / "server").mkdir()
    (repo / "server" / "Routes.kt").write_text("fun main() {}\n")

    assert fingerprint(repo, api_port, state_dir) != before
