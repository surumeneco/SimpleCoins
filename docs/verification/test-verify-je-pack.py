#!/usr/bin/env python3
"""Offline HTTP smoke tests for the read-only Java pack verification helper."""

import hashlib
import http.server
import os
from pathlib import Path
import subprocess
import tempfile
import threading
import zipfile


def main() -> None:
    script = Path(__file__).with_name("verify-je-pack.sh").resolve()
    if not script.is_file():
        raise AssertionError("Missing verifier")

    with tempfile.TemporaryDirectory(prefix="simplecoins-pack-verifier-") as temporary:
        root = Path(temporary)
        served = root / "served"
        served.mkdir()
        generated = served / "generated.zip"
        with zipfile.ZipFile(generated, "w") as archive:
            archive.writestr("pack.mcmeta", '{"pack":{"description":"smoke"}}')
        (served / "changed.zip").write_bytes(generated.read_bytes() + b"changed")
        invalid = served / "invalid.zip"
        invalid.write_bytes(b"not-a-zip")
        linked = root / "linked.zip"
        linked.symlink_to(generated)

        class Handler(http.server.SimpleHTTPRequestHandler):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, directory=str(served), **kwargs)

            def log_message(self, *args) -> None:
                pass

            def do_GET(self) -> None:
                if self.path == "/safe-redirect":
                    self.send_response(302)
                    self.send_header("Location", "/generated.zip")
                    self.end_headers()
                    return
                if self.path == "/unsafe-redirect":
                    self.send_response(302)
                    self.send_header("Location", "file:///etc/passwd")
                    self.end_headers()
                    return
                super().do_GET()

        server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        port = server.server_address[1]
        env = os.environ.copy()
        env["NO_PROXY"] = "127.0.0.1,localhost"
        env["no_proxy"] = "127.0.0.1,localhost"

        def run(path: str, local: Path = generated) -> subprocess.CompletedProcess[str]:
            return subprocess.run(
                ["bash", str(script), str(local)],
                input=f"http://127.0.0.1:{port}/{path}\n",
                capture_output=True,
                text=True,
                timeout=20,
                env=env,
                check=False,
            )

        try:
            identical = run("generated.zip")
            assert identical.returncode == 0, identical.stderr
            assert "Verified identical bytes" in identical.stdout
            digest = hashlib.sha1(generated.read_bytes()).hexdigest()
            assert identical.stdout.count(digest) == 2, identical.stdout

            signed_query = run("generated.zip?signature=abc%2Fdef&expires=999")
            assert signed_query.returncode == 0, signed_query.stderr
            assert "Verified identical bytes" in signed_query.stdout

            safe_redirect = run("safe-redirect")
            assert safe_redirect.returncode == 0, safe_redirect.stderr
            assert "Verified identical bytes" in safe_redirect.stdout

            changed = run("changed.zip")
            assert changed.returncode == 1
            assert "differs byte-for-byte" in changed.stderr

            missing = run("missing.zip")
            assert missing.returncode == 1
            assert "URL redacted" in missing.stderr

            invalid_archive = run("invalid.zip", invalid)
            assert invalid_archive.returncode == 1
            assert "not a valid ZIP" in invalid_archive.stderr

            unsafe = run("unsafe-redirect")
            assert unsafe.returncode == 1
            assert "URL redacted" in unsafe.stderr
            assert f"127.0.0.1:{port}" not in unsafe.stderr + unsafe.stdout

            direct_file = subprocess.run(
                ["bash", str(script), str(generated)],
                input=f"file://{generated}\n", capture_output=True, text=True,
                timeout=20, env=env, check=False,
            )
            assert direct_file.returncode == 1
            assert "URL redacted" in direct_file.stderr
            assert str(generated) not in direct_file.stderr + direct_file.stdout

            symlink = run("generated.zip", linked)
            assert symlink.returncode == 2
            assert "Usage:" in symlink.stderr

            empty_url = subprocess.run(
                ["bash", str(script), str(generated)],
                input="\n", capture_output=True, text=True, timeout=20,
                env=env, check=False,
            )
            assert empty_url.returncode == 2
            assert "No URL supplied" in empty_url.stderr

            print("PASS 10/10 Java pack verifier HTTP and failure-path checks")
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)


if __name__ == "__main__":
    main()
