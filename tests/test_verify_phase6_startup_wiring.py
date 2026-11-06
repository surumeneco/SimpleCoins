import importlib.util
from pathlib import Path
import struct
import tempfile
import unittest
from zipfile import ZipFile

src = Path(__file__).resolve().parents[1] / "tools" / "verify_phase6_startup_wiring.py"
spec = importlib.util.spec_from_file_location("phase6_verifier", src)
app = importlib.util.module_from_spec(spec)
spec.loader.exec_module(app)
LISTENER = "co/surumene/simplecoins/SettlementAdmissionListener"
SCANNER = "co/surumene/simplecoins/domain/SettlementScanRunner"

def fake_class(constants=()):
    encoded = []
    for text in constants:
        raw = text.encode("ascii")
        encoded.append(b"\x01" + struct.pack(">H", len(raw)) + raw)
    return b"\xca\xfe\xba\xbe" + b"\0\0\0\x41" + struct.pack(">H", 1 + len(encoded)) + b"".join(encoded)

class StartupWiringTest(unittest.TestCase):
    def inspect(self, refs=(), listener=True):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "test.jar"
            with ZipFile(path, "w") as jar:
                jar.writestr("plugin.yml", "main: co.surumene.simplecoins.SimpleCoinsPlugin\n")
                jar.writestr("co/surumene/simplecoins/SimpleCoinsPlugin.class", fake_class(refs))
                if listener:
                    jar.writestr(LISTENER + ".class", fake_class())
            return app.inspect_jar(path)

    def test_missing_wiring_in_main_even_when_listener_class_present(self):
        result = self.inspect(["registerEvents"])
        self.assertEqual("missing", result["status"])
        self.assertFalse(result["checks"]["main_mentions_listener"])

    def test_complete_hints_are_only_candidate(self):
        result = self.inspect([LISTENER, SCANNER, "registerEvents"])
        self.assertEqual("candidate-only", result["status"])
        self.assertIn("does not prove", result["limitation"])

    def test_no_scanner_link_is_missing(self):
        result = self.inspect([LISTENER, "registerEvents"])
        self.assertIn("main_mentions_scan_or_coordinator", result["missing"])

    def test_no_adapter_class_is_missing(self):
        result = self.inspect([LISTENER, SCANNER, "registerEvents"], listener=False)
        self.assertIn("listener_class_present", result["missing"])

    def test_invalid_and_truncated_class_files_fail(self):
        with self.assertRaises(app.InvalidClassFile):
            app.utf8_pool(b"bad")
        with self.assertRaises(app.InvalidClassFile):
            app.utf8_pool(b"\xca\xfe\xba\xbe" + b"\0\0\0\x41" + b"\0\x02" + b"\x01\0\x05ab")

if __name__ == "__main__":
    unittest.main()
