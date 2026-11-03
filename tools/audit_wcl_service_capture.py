#!/usr/bin/env python3
"""Read-only structural audit of a caller-exported Paper WCL service capture.

This cannot authenticate that the capture came from a running Paper server.
"""
import argparse
import json
import re
import sys
from pathlib import Path
from datetime import datetime

EXPECTED = {
    "wwc": "WhatAWonderfulChicken",
    "www": "WhatAWonderfulWolf",
}
REQUIRED_PLUGINS = {"WonderfulCoreLibrary", *EXPECTED.values()}
SERVICE = "co.surumene.wcl.plugin.inventory.WonderfulStorageReader"
SHA256 = re.compile(r"[0-9a-fA-F]{64}\Z")


def audit(capture):
    """Return a stable machine-readable status without disclosing player data."""
    invalid, incomplete = set(), set()
    if not isinstance(capture, dict) or type(capture.get("schema_version")) is not int or capture["schema_version"] != 1:
        return {"status": "invalid", "codes": ["INVALID_SCHEMA"]}
    meta = capture.get("capture")
    if not isinstance(meta, dict):
        return {"status": "invalid", "codes": ["INVALID_CAPTURE_METADATA"]}
    for field in ("server_start", "captured_at", "paper_version"):
        if not isinstance(meta.get(field), str) or not meta[field].strip():
            invalid.add("MISSING_CAPTURE_METADATA")
    try:
        started = datetime.fromisoformat(meta["server_start"].replace("Z", "+00:00"))
        captured = datetime.fromisoformat(meta["captured_at"].replace("Z", "+00:00"))
        if started.tzinfo is None or captured.tzinfo is None or captured < started:
            invalid.add("INVALID_CAPTURE_TIME")
    except (KeyError, TypeError, ValueError):
        invalid.add("INVALID_CAPTURE_TIME")
    for field in ("primary_thread", "registration_list_complete"):
        if type(meta.get(field)) is not bool:
            invalid.add("INVALID_CAPTURE_FLAG")
        elif not meta[field]:
            incomplete.add("UNVERIFIED_" + field.upper())
    plugins = capture.get("plugins")
    registrations = capture.get("registrations")
    if not isinstance(plugins, list) or not isinstance(registrations, list):
        return {"status": "invalid", "codes": ["INVALID_COLLECTION"]}
    by_name = {}
    for plugin in plugins:
        if not isinstance(plugin, dict) or not isinstance(plugin.get("name"), str):
            invalid.add("INVALID_PLUGIN_RECORD")
            continue
        name = plugin["name"]
        if name in by_name:
            invalid.add("DUPLICATE_PLUGIN")
        by_name[name] = plugin
        if name not in REQUIRED_PLUGINS:
            continue
        if type(plugin.get("enabled")) is not bool:
            invalid.add("INVALID_PLUGIN_ENABLED")
        elif not plugin["enabled"]:
            incomplete.add("REQUIRED_PLUGIN_DISABLED")
        if not isinstance(plugin.get("version"), str) or not plugin["version"].strip():
            invalid.add("INVALID_PLUGIN_VERSION")
        if not isinstance(plugin.get("jar_sha256"), str) or not SHA256.fullmatch(plugin["jar_sha256"]):
            incomplete.add("MISSING_JAR_IDENTITY")
    if not REQUIRED_PLUGINS.issubset(by_name):
        incomplete.add("REQUIRED_PLUGIN_MISSING")
    seen = set()
    for reg in registrations:
        if not isinstance(reg, dict):
            invalid.add("INVALID_REGISTRATION")
            continue
        provider = reg.get("provider_id")
        owner = reg.get("owner_plugin")
        if not isinstance(provider, str) or not isinstance(owner, str):
            invalid.add("INVALID_REGISTRATION")
            continue
        if reg.get("service_interface") != SERVICE:
            incomplete.add("SERVICE_INTERFACE_MISMATCH")
        if provider in seen:
            invalid.add("DUPLICATE_PROVIDER")
        seen.add(provider)
        if provider not in EXPECTED:
            incomplete.add("UNEXPECTED_PROVIDER")
        elif owner != EXPECTED[provider]:
            incomplete.add("PROVIDER_OWNER_MISMATCH")
        if not isinstance(reg.get("provider_class"), str) or not reg["provider_class"].strip():
            invalid.add("INVALID_PROVIDER_CLASS")
        if type(reg.get("owner_enabled")) is not bool:
            invalid.add("INVALID_PROVIDER_ENABLED")
        elif not reg["owner_enabled"]:
            incomplete.add("PROVIDER_DISABLED")
    if not set(EXPECTED).issubset(seen):
        incomplete.add("REQUIRED_PROVIDER_MISSING")
    return {"status": "invalid" if invalid else "incomplete" if incomplete else "structurally_complete",
            "codes": sorted(invalid | incomplete)}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("capture", type=Path, help="JSON exported from an independently verified Paper probe")
    args = parser.parse_args(argv)
    try:
        # Bounded input. An exported capture is not an authority or a source of player identities.
        if args.capture.stat().st_size > 1024 * 1024:
            raise ValueError("capture too large")
        data = json.loads(args.capture.read_text(encoding="utf-8"))
        result = audit(data)
    except (OSError, ValueError, UnicodeError):
        result = {"status": "invalid", "codes": ["UNREADABLE_CAPTURE"]}
    print(json.dumps(result, sort_keys=True))
    return {"structurally_complete": 0, "incomplete": 2, "invalid": 3}[result["status"]]


if __name__ == "__main__":
    sys.exit(main())
