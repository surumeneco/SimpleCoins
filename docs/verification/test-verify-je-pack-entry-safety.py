#!/usr/bin/env python3
"""TASK-20261101-006 / JE-ZIP-ENTRY-01..03 (CAT Draft only)."""

import http.server
import os
from pathlib import Path
import subprocess
import tempfile
import threading
import zipfile


def main() -> None:
    verifier = Path(__file__).with_name("verify-je-pack.sh")
    assert verifier.is_file(), "Missing read-only pack verifier"
    with tempfile.TemporaryDirectory(prefix="simplecoins-je-zip-entry-") as temp:
        root = Path(temp)
        packs = {
            "traversal.zip": "../escape.txt",
            "absolute.zip": "/absolute.txt",
            "valid.zip": None,
        }
        for name, unsafe_entry in packs.items():
            with zipfile.ZipFile(root / name, "w") as archive:
                archive.writestr("pack.mcmeta", '{"pack":{"description":"test"}}')
                if unsafe_entry is not None:
                    archive.writestr(unsafe_entry, "not an installable asset")

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

        def verify(name: str) -> subprocess.CompletedProcess[str]:
            return subprocess.run(
                ["bash", str(verifier), str(root / name)],
                input=f"http://127.0.0.1:{port}/{name}\n",
                text=True,
                capture_output=True,
                env=env,
                timeout=20,
                check=False,
            )

        try:
            normal = verify("valid.zip")
            assert normal.returncode == 0, normal.stderr
            assert "Verified identical bytes" in normal.stdout

            traversal = verify("traversal.zip")
            assert traversal.returncode != 0, (
                "JE-ZIP-ENTRY-01: parent-traversal ZIP entry was accepted"
            )

            absolute = verify("absolute.zip")
            assert absolute.returncode != 0, (
                "JE-ZIP-ENTRY-02: absolute ZIP entry was accepted"
            )
            print("PASS 3/3 hosted ZIP entry safety vectors")
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)


if __name__ == "__main__":
    main()
