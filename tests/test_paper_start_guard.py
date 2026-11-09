"""Local offline tests of fail-closed Paper start guard, no network or server needed."""
from __future__ import annotations
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zipfile

SCRIPT = Path(__file__).resolve().parents[1] / 'tools' / 'paper_start_guard.sh'

class PaperStartGuardTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.home = Path(self.tmp.name)
        (self.home / 'plugins').mkdir()
        self.jar = self.home / 'plugins' / 'SimpleCoins.jar'
        self.guard = self.home / 'plugins' / 'SimpleCoinsGuard.jar'
        self.make_jar()
        self.make_guard()
        self.props = self.home / 'server.properties'
        self.props.write_text('white-list=true\nenforce-whitelist=true\n', encoding='utf-8')
        self.allow = self.home / 'whitelist.json'
        self.allow.write_text('[]\n', encoding='utf-8')
        self.marker = self.home / 'launched.txt'

    def make_jar(self, *, main='co.surumene.simplecoins.SimpleCoinsPlugin'):
        with zipfile.ZipFile(self.jar, 'w') as z:
            z.writestr('plugin.yml', f'name: SimpleCoins\nmain: {main}\ndepend:\n  - SimpleCoinsGuard\n')
            z.writestr('co/surumene/simplecoins/SimpleCoinsPlugin.class', b'\xca\xfe\xba\xbe')

    def make_guard(self, *, main='co.surumene.simplecoins.guard.SimpleCoinsAdmissionGuard'):
        with zipfile.ZipFile(self.guard, 'w') as z:
            z.writestr('plugin.yml', f'name: SimpleCoinsGuard\nmain: {main}\n')
            z.writestr('co/surumene/simplecoins/guard/SimpleCoinsAdmissionGuard.class', b'\xca\xfe\xba\xbe')

    def run_guard(self, *, digest=None, guard_digest=None):
        self.marker.unlink(missing_ok=True)
        sha = digest if digest is not None else hashlib.sha256(self.jar.read_bytes()).hexdigest()
        guard_sha = guard_digest if guard_digest is not None else hashlib.sha256(self.guard.read_bytes()).hexdigest()
        return subprocess.run(['bash', str(SCRIPT), str(self.home), sha, guard_sha, '--', sys.executable,
             '-c', f'from pathlib import Path; Path({str(self.marker)!r}).write_text("ran")'],
             capture_output=True, text=True, timeout=5)

    def assert_denied(self, proc):
        self.assertNotEqual(0, proc.returncode, proc.stderr)
        self.assertFalse(self.marker.exists())
        self.assertIn('START_GUARD_DENIED', proc.stderr)

    def test_valid_closed_configuration_launches(self):
        result = self.run_guard()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertTrue(self.marker.exists())
        self.assertIn('START_GUARD_PASSED', result.stderr)

    def test_single_word_launcher_is_allowed(self):
        sha = hashlib.sha256(self.jar.read_bytes()).hexdigest()
        result = subprocess.run(['bash', str(SCRIPT), str(self.home), sha, hashlib.sha256(self.guard.read_bytes()).hexdigest(), '--', '/usr/bin/true'], capture_output=True, text=True)
        self.assertEqual(0, result.returncode, result.stderr)

    def test_missing_guard_refuses(self):
        self.guard.unlink()
        self.assert_denied(self.run_guard(guard_digest='0'*64))

    def test_wrong_guard_digest_refuses(self):
        self.assert_denied(self.run_guard(guard_digest='a'*64))

    def test_wrong_guard_entrypoint_refuses(self):
        self.make_guard(main='guard.Wrong')
        self.assert_denied(self.run_guard())

    def test_main_missing_depend_refuses(self):
        with zipfile.ZipFile(self.jar, 'w') as z:
            z.writestr('plugin.yml', 'name: SimpleCoins\nmain: co.surumene.simplecoins.SimpleCoinsPlugin\n')
            z.writestr('co/surumene/simplecoins/SimpleCoinsPlugin.class', b'\xca\xfe\xba\xbe')
        self.assert_denied(self.run_guard())

    def test_symlinked_guard_refuses(self):
        real = self.home / 'guard-backup.jar'
        real.write_bytes(self.guard.read_bytes())
        self.guard.unlink()
        self.guard.symlink_to(real)
        self.assert_denied(self.run_guard())

    def test_missing_plugin_refuses(self):
        self.jar.unlink()
        self.assert_denied(self.run_guard(digest='0'*64))

    def test_wrong_jar_hash_refuses(self):
        self.assert_denied(self.run_guard(digest='f'*64))

    def test_wrong_entrypoint_refuses(self):
        self.make_jar(main='bad.Other')
        self.assert_denied(self.run_guard())

    def test_missing_class_refuses(self):
        with zipfile.ZipFile(self.jar, 'w') as z:
            z.writestr('plugin.yml', 'name: SimpleCoins\nmain: co.surumene.simplecoins.SimpleCoinsPlugin\ndepend:\n  - SimpleCoinsGuard\n')
        self.assert_denied(self.run_guard())

    def test_whitelist_not_enabled_refuses(self):
        self.props.write_text('white-list=false\nenforce-whitelist=true\n')
        self.assert_denied(self.run_guard())

    def test_whitelist_override_refuses(self):
        self.props.write_text('white-list=true\nwhite-list=false\nenforce-whitelist=true\n')
        self.assert_denied(self.run_guard())

    def test_missing_enforce_whitelist_refuses(self):
        self.props.write_text('white-list=true\n')
        self.assert_denied(self.run_guard())

    def test_existing_whitelisted_user_refuses(self):
        self.allow.write_text(json.dumps([{'uuid': '00000000-0000-0000-0000-000000000001', 'name':'Player'}]))
        self.assert_denied(self.run_guard())

    def test_corrupt_whitelist_refuses(self):
        self.allow.write_text('oops')
        self.assert_denied(self.run_guard())

    def test_symlinked_plugin_refuses(self):
        real = self.home / 'reference.jar'
        real.write_bytes(self.jar.read_bytes())
        self.jar.unlink()
        self.jar.symlink_to(real)
        self.assert_denied(self.run_guard())

if __name__ == '__main__':
    unittest.main()
