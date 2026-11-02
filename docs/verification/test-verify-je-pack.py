#!/usr/bin/env python3
"""Offline HTTP smoke tests for the read-only Java pack verification helper."""

import hashlib
import http.server
import os
import shlex
import shutil
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
        linked_dir = root / "linked-dir"
        linked_dir.symlink_to(served, target_is_directory=True)

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

            # PACK-HASH-02: a regular ZIP reached through a symlinked
            # ancestor must not become a trusted generated archive.
            ancestor_link = run("generated.zip", linked_dir / "generated.zip")
            assert ancestor_link.returncode == 2
            assert "contains a symbolic link" in ancestor_link.stderr
            assert "Verified identical bytes" not in ancestor_link.stdout

            # A missing generated ZIP or a directory must fail before URL
            # intake: neither is a valid artifact to compare or advertise.
            missing_local_archive = run("generated.zip", root / "absent.zip")
            assert missing_local_archive.returncode == 2
            assert "Usage:" in missing_local_archive.stderr
            assert "Verified identical bytes" not in missing_local_archive.stdout

            directory_local_archive = run("generated.zip", served)
            assert directory_local_archive.returncode == 2
            assert "Usage:" in directory_local_archive.stderr
            assert "Verified identical bytes" not in directory_local_archive.stdout

            empty_url = subprocess.run(
                ["bash", str(script), str(generated)],
                input="\n", capture_output=True, text=True, timeout=20,
                env=env, check=False,
            )
            assert empty_url.returncode == 2
            assert "No URL supplied" in empty_url.stderr

            option_like_url = subprocess.run(
                ["bash", str(script), str(generated)],
                input="--help\n", capture_output=True, text=True,
                timeout=20, env=env, check=False,
            )
            assert option_like_url.returncode == 1
            assert "Hosted pack download failed (URL redacted)." in option_like_url.stderr
            assert "Usage: curl" not in option_like_url.stdout + option_like_url.stderr

            # PACK-HOSTED-01: mutate the local archive immediately after
            # the first successful byte comparison, before SHA-1 evidence.
            # A verifier must not report success with divergent digests.
            with tempfile.TemporaryDirectory(prefix="simplecoins-cmp-hook-") as hook_dir:
                real_cmp = shutil.which("cmp")
                assert real_cmp is not None
                wrapper = Path(hook_dir) / "cmp"
                wrapper.write_text(
                    "#!/bin/sh\n"
                    + shlex.quote(real_cmp) + ' "$@" || exit $?\n'
                    + 'printf x >> "$SIMPLECOINS_TEST_MUTATE_FILE"\n'
                )
                wrapper.chmod(0o755)
                mutation_env = env.copy()
                mutation_env["PATH"] = str(hook_dir) + os.pathsep + env["PATH"]
                mutation_env["SIMPLECOINS_TEST_MUTATE_FILE"] = str(generated)
                mutated = subprocess.run(
                    ["bash", str(script), str(generated)],
                    input=f"http://127.0.0.1:{port}/generated.zip\n",
                    capture_output=True, text=True, timeout=20,
                    env=mutation_env, check=False,
                )
                assert mutated.returncode == 1, mutated.stdout + mutated.stderr
                assert "Generated archive changed during verification." in mutated.stderr
                assert "Verified identical bytes" not in mutated.stdout

            print("PASS 15/15 Java pack verifier HTTP and failure-path checks")
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)


if __name__ == "__main__":
    main()
