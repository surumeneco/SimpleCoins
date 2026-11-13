import importlib.util
import tempfile
import unittest
import zipfile
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1] / "tools" / "simplecoins_preflight.py"
spec = importlib.util.spec_from_file_location("simplecoins_preflight", MODULE)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)


def jar(path, name, main, load, *, no_api=False, absent_main=False):
    with zipfile.ZipFile(path, "w") as out:
        deps = "depend:\n  - SimpleCoinsGuard\n" if name == "SimpleCoins" else ""
        out.writestr("plugin.yml", f"name: {name}\nmain: {main}\napi-version: '26.2'\nload: {load}\n" + deps)
        if not absent_main:
            out.writestr(main.replace(".", "/") + ".class", b"dummy-fixture-class")
        if name == "SimpleCoinsGuard" and not no_api:
            out.writestr("co/surumene/simplecoins/guard/api/AdmissionPermit.class", b"api")


class PreflightTest(unittest.TestCase):
    def test_pair_and_missing_jar_and_invalid_guard_api(self):
        with tempfile.TemporaryDirectory() as temp:
            g = Path(temp) / "Guard.jar"
            m = Path(temp) / "SimpleCoins.jar"
            jar(g, *mod.REQUIRED[0])
            jar(m, *mod.REQUIRED[1])
            self.assertEqual(0, mod.main([str(g), str(m)]))
            m.unlink()
            self.assertEqual(1, mod.main([str(g), str(m)]))
            jar(m, *mod.REQUIRED[1])
            jar(g, *mod.REQUIRED[0], no_api=True)
            self.assertEqual(1, mod.main([str(g), str(m)]))
            jar(g, *mod.REQUIRED[0])
            jar(m, *mod.REQUIRED[1], absent_main=True)
            self.assertEqual(1, mod.main([str(g), str(m)]))

    def test_symlink_and_same_paths_are_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            g = Path(temp) / "Guard.jar"
            m = Path(temp) / "SimpleCoins.jar"
            link = Path(temp) / "GuardLinked.jar"
            jar(g, *mod.REQUIRED[0])
            jar(m, *mod.REQUIRED[1])
            link.symlink_to(g)
            self.assertEqual(1, mod.main([str(link), str(m)]))
            self.assertEqual(1, mod.main([str(g), str(g)]))


if __name__ == "__main__":
    unittest.main()
