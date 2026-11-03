#!/usr/bin/env python3
"""Draft Red: hosted JE pack verifier must reject URL userinfo without logging it."""

import http.server
import os
from pathlib import Path
import subprocess
import tempfile
import threading
import zipfile


def main() -> None:
    verifier = Path(__file__).with_name("verify-je-pack.sh")
    with tempfile.TemporaryDirectory(prefix="simplecoins-pack-userinfo-") as temp:
        root = Path(temp)
        generated = root / "generated.zip"
        with zipfile.ZipFile(generated, "w") as archive:
            archive.writestr("pack.mcmeta", '{"pack":{"description":"userinfo-test"}}')

        class Handler(http.server.SimpleHTTPRequestHandler):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, directory=str(root), **kwargs)

            def log_message(self, *args) -> None:
                pass

        server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        port = server.server_address[1]
        env = os.environ.copy()
        env["NO_PROXY"] = "127.0.0.1,localhost"
        env["no_proxy"] = "127.0.0.1,localhost"

        def run(url: str) -> subprocess.CompletedProcess[str]:
            return subprocess.run(
                ["bash", str(verifier), str(generated)],
                input=url + "\n",
                text=True, capture_output=True, timeout=20, env=env, check=False,
            )

        try:
            clean = run(f"http://127.0.0.1:{port}/generated.zip")
            assert clean.returncode == 0, clean.stderr
            assert "Verified identical bytes" in clean.stdout

            credentials = "fixture-user:fixture-password"
            unsafe = run(f"http://{credentials}@127.0.0.1:{port}/generated.zip")
            combined = unsafe.stdout + unsafe.stderr
            assert credentials not in combined, "Verifier leaked userinfo to output"
            assert unsafe.returncode != 0, (
                "Expected Red: verifier accepted a hosted pack URL with embedded credentials"
            )
            assert "Verified identical bytes" not in unsafe.stdout
            print("PASS: userinfo rejected without disclosure; plain HTTP control succeeds")
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)


if __name__ == "__main__":
    main()
