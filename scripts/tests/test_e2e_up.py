"""Tests for scripts/e2e-up.sh. Run: uv run --with pytest --with pyyaml pytest scripts/tests"""

import shutil
import socket
import subprocess
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

import pytest

SCRIPT = Path(__file__).resolve().parents[1] / "e2e-up.sh"


class FakeServer(BaseHTTPRequestHandler):
    """Answers like a running E2E server with dev routes mounted."""

    def do_GET(self):
        self._reply(200)

    def do_POST(self):
        self._reply(200 if "/reset-seed-" in self.path else 201)

    def _reply(self, status):
        self.send_response(status)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def log_message(self, *_):
        pass


@pytest.fixture
def running_stack():
    """A Postgres port that accepts connections and an API server that is already up."""
    pg = socket.socket()
    pg.bind(("127.0.0.1", 0))
    pg.listen()
    api = ThreadingHTTPServer(("127.0.0.1", 0), FakeServer)
    threading.Thread(target=api.serve_forever, daemon=True).start()
    yield pg.getsockname()[1], f"http://127.0.0.1:{api.server_port}"
    api.shutdown()
    pg.close()


@pytest.fixture
def repo(tmp_path, running_stack):
    """A throwaway checkout holding the script and a .env that points at the fake Postgres."""
    pg_port, _ = running_stack
    subprocess.run(["git", "init", "-q", str(tmp_path)], check=True)
    (tmp_path / "scripts").mkdir()
    shutil.copy(SCRIPT, tmp_path / "scripts" / "e2e-up.sh")
    (tmp_path / ".env").write_text(f"DATABASE_URL=jdbc:postgresql://127.0.0.1:{pg_port}/scent\n")
    return tmp_path


def run_e2e_up(repo, api_url):
    return subprocess.run(
        ["bash", "scripts/e2e-up.sh"],
        cwd=repo,
        env={"PATH": "/usr/bin:/bin:/usr/sbin:/sbin", "E2E_API_URL": api_url},
        capture_output=True,
        text=True,
        timeout=60,
    )


def test_reused_server_exits_zero(repo, running_stack):
    result = run_e2e_up(repo, running_stack[1])

    assert result.returncode == 0, result.stderr
    assert "reusing it" in result.stdout
    assert "E2E stack ready" in result.stdout


def test_reused_server_does_not_offer_to_stop_it(repo, running_stack):
    result = run_e2e_up(repo, running_stack[1])

    assert "--down" not in result.stdout
