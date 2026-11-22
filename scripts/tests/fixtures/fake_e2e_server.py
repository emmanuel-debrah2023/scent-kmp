"""A stand-in for the E2E Ktor server, run as its own process so tests can stop and restart it.

Usage: python3 fake_e2e_server.py <port>
"""

import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


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


if __name__ == "__main__":
    ThreadingHTTPServer(("127.0.0.1", int(sys.argv[1])), FakeServer).serve_forever()
