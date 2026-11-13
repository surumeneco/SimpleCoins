#!/usr/bin/env python3
"""Fail server startup before Paper launch when either guard or main JAR is absent/broken.

Does not prove that Paper loaded plugins; combine with an independent startup gate.
Usage: python3 simplecoins_preflight.py /path/SimpleCoinsGuard.jar /path/SimpleCoins.jar
"""
import re
import sys
import zipfile
from pathlib import Path

REQUIRED = (
    ("SimpleCoinsGuard", "co.surumene.simplecoins.guard.SimpleCoinsAdmissionGuard", "STARTUP"),
    ("SimpleCoins", "co.surumene.simplecoins.SimpleCoinsPlugin", "POSTWORLD"),
)


def check_jar(path: Path, name: str, main: str, load: str) -> None:
    if path.is_symlink() or not path.is_file():
        raise ValueError(f"Missing, non-regular or linked {name} JAR: {path}")
    with zipfile.ZipFile(path) as archive:
        if archive.namelist().count("plugin.yml") != 1:
            raise ValueError(f"Invalid plugin.yml count in {path}")
        if archive.testzip() is not None:
            raise ValueError(f"Corrupted {name} JAR: {path}")
        descriptor = archive.read("plugin.yml").decode("utf-8")
        for key, expected in (("name", name), ("main", main), ("api-version", "26.2"), ("load", load)):
            pattern = rf"(?m)^{re.escape(key)}:\s*['\"]?{re.escape(expected)}['\"]?\s*$"
            if not re.search(pattern, descriptor):
                raise ValueError(f"Unexpected {key} of {name} in {path}")
        cls = main.replace(".", "/") + ".class"
        if cls not in archive.namelist():
            raise ValueError(f"Missing JavaPlugin entrypoint {cls} in {path}")
        if name == "SimpleCoinsGuard":
            if "co/surumene/simplecoins/guard/api/AdmissionPermit.class" not in archive.namelist():
                raise ValueError("Guard API class absent from guard JAR")
        else:
            if "co/surumene/simplecoins/guard/api/AdmissionPermit.class" in archive.namelist():
                raise ValueError("Duplicate Guard API class in SimpleCoins JAR")
            if not re.search(r"(?m)^\s*-\s*SimpleCoinsGuard\s*$", descriptor):
                raise ValueError("SimpleCoins descriptor missing guard dependency")


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print("Usage: simplecoins_preflight.py GUARD_JAR SIMPLECOINS_JAR", file=sys.stderr)
        return 2
    try:
        if Path(argv[0]).resolve() == Path(argv[1]).resolve():
            raise ValueError("Guard and accounting JAR paths are identical")
        for args, path in zip(REQUIRED, map(Path, argv)):
            check_jar(path, *args)
    except (OSError, ValueError, zipfile.BadZipFile, UnicodeError) as exc:
        print(f"SIMPLECOINS_PREFLIGHT_DENIED {exc}", file=sys.stderr)
        return 1
    print("SIMPLECOINS_PREFLIGHT_PASSED (JAR integrity only, Paper load not verified)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
