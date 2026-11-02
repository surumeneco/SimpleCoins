#!/usr/bin/env python3
"""Deterministic offline tests for verify-be-pack.py; no Paper/Geyser client."""
import importlib.util
import json
import stat
from pathlib import Path
import tempfile
import unittest
import zipfile

SCRIPT = Path(__file__).with_name("verify-be-pack.py")
spec = importlib.util.spec_from_file_location("verify_be_pack", SCRIPT)
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)


class VerifyBedrockArtifactsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.pack = self.root / "SimpleCoins.mcpack"
        self.mapping = self.root / "simplecoins.json"
        self.names = {value: f"Coin {value}" for value in verifier.DENOMINATIONS}
        self.images = {value: b"PNG" + value.to_bytes(4, "big")
                       for value in verifier.DENOMINATIONS}
        self.build()

    def build(self, *, entries=None, mapping=None, manifest=None):
        import hashlib
        import struct
        digest = hashlib.sha256()
        for value in verifier.DENOMINATIONS:
            name = self.names[value].encode("utf-8")
            image = self.images[value]
            digest.update(struct.pack(">III", value, len(name), len(image)))
            digest.update(name)
            digest.update(image)
        fingerprint = digest.hexdigest()
        header = verifier.java_name_uuid("simplecoins:header:" + fingerprint)
        module = verifier.java_name_uuid("simplecoins:module:" + fingerprint)
        default_manifest = {"format_version": 2, "header": {"uuid": header, "version": [1, 0, 0]},
                            "modules": [{"type": "resources", "uuid": module, "version": [1, 0, 0]}]}
        textures = {"resource_pack_name": "SimpleCoins", "texture_name": "atlas.items",
                    "texture_data": {f"simplecoins:coin_{v}": {"textures": [f"textures/items/coin_{v}"]}
                                     for v in verifier.DENOMINATIONS}}
        default_entries = {"manifest.json": json.dumps(manifest or default_manifest).encode(),
                           "textures/item_texture.json": json.dumps(textures).encode()}
        default_entries.update({f"textures/items/coin_{v}.png": image
                                for v, image in self.images.items()})
        with zipfile.ZipFile(self.pack, "w") as z:
            for key, value in (entries if entries is not None else default_entries.items()):
                z.writestr(key, value)
        default_mapping = {"format_version": 2, "items": {"minecraft:paper": [
            {"type": "definition", "model": f"simplecoins:coin_{v}",
             "bedrock_identifier": f"simplecoins:coin_{v}", "display_name": self.names[v],
             "bedrock_options": {"icon": f"simplecoins:coin_{v}"},
             "components": {"minecraft:max_stack_size": 64}}
            for v in verifier.DENOMINATIONS]}}
        self.mapping.write_text(json.dumps(mapping or default_mapping), encoding="utf-8")

    def reject(self):
        with self.assertRaises(verifier.VerificationError):
            verifier.verify(self.pack, self.mapping)

    def test_valid_pair_reports_fingerprint_and_two_sha256(self):
        result = verifier.verify(self.pack, self.mapping)
        self.assertEqual(result["denominations"], 9)
        self.assertEqual(len(result["pack_sha256"]), 64)
        self.assertEqual(len(result["mapping_sha256"]), 64)

    def test_changed_display_name_without_pack_regeneration_is_rejected(self):
        obj = json.loads(self.mapping.read_text())
        obj["items"]["minecraft:paper"][0]["display_name"] = "Changed"
        self.mapping.write_text(json.dumps(obj))
        self.reject()

    def test_changed_image_without_manifest_update_is_rejected(self):
        self.images[1] = b"changed image"
        old_manifest = json.loads(zipfile.ZipFile(self.pack).read("manifest.json"))
        self.build(manifest=old_manifest)
        self.reject()

    def test_missing_png_is_rejected(self):
        with zipfile.ZipFile(self.pack) as z:
            entries = [(n, z.read(n)) for n in z.namelist() if n != "textures/items/coin_1.png"]
        self.build(entries=entries)
        self.reject()

    def test_duplicate_member_is_rejected(self):
        with zipfile.ZipFile(self.pack) as z:
            entries = [(n, z.read(n)) for n in z.namelist()]
        entries.append(entries[-1])
        self.build(entries=entries)
        self.reject()

    def test_zip_member_symlink_is_rejected(self):
        with zipfile.ZipFile(self.pack) as z:
            entries = [(n, z.read(n)) for n in z.namelist()]
        with zipfile.ZipFile(self.pack, "w") as z:
            for name, data in entries:
                if name == "textures/items/coin_1.png":
                    info = zipfile.ZipInfo(name)
                    info.create_system = 3
                    info.external_attr = (stat.S_IFLNK | 0o777) << 16
                    z.writestr(info, b"../../outside")
                else:
                    z.writestr(name, data)
        self.reject()

    def test_parent_traversal_member_is_rejected(self):
        with zipfile.ZipFile(self.pack) as z:
            entries = [(n, z.read(n)) for n in z.namelist()]
        entries.append(("../escape.txt", b"bad"))
        self.build(entries=entries)
        self.reject()

    def test_absolute_member_is_rejected(self):
        with zipfile.ZipFile(self.pack) as z:
            entries = [(n, z.read(n)) for n in z.namelist()]
        entries.append(("/absolute.txt", b"bad"))
        self.build(entries=entries)
        self.reject()

    def test_duplicate_json_member_is_rejected(self):
        raw = self.mapping.read_text()
        self.mapping.write_text(raw.replace('"format_version": 2', '"format_version": 2, "format_version": 2'))
        self.reject()

    def test_wrong_stack_size_is_rejected(self):
        obj = json.loads(self.mapping.read_text())
        obj["items"]["minecraft:paper"][0]["components"]["minecraft:max_stack_size"] = 32
        self.mapping.write_text(json.dumps(obj))
        self.reject()

    def test_same_physical_file_is_rejected(self):
        with self.assertRaises(verifier.VerificationError):
            verifier.verify(self.pack, self.pack)

    def test_symlinked_artifact_is_rejected(self):
        link = self.root / "alias.mcpack"
        link.symlink_to(self.pack)
        with self.assertRaises(verifier.VerificationError):
            verifier.verify(link, self.mapping)

    def test_symlinked_ancestor_is_rejected(self):
        link = self.root / "aliasdir"
        link.symlink_to(self.root, target_is_directory=True)
        with self.assertRaises(verifier.VerificationError):
            verifier.verify(link / self.pack.name, self.mapping)

    def test_invalid_json_is_rejected(self):
        self.mapping.write_bytes(b"not-json")
        self.reject()

    def test_missing_mapping_is_rejected(self):
        self.mapping.unlink()
        self.reject()

    def test_incorrect_manifest_module_is_rejected(self):
        with zipfile.ZipFile(self.pack) as z:
            entries = [(n, z.read(n)) for n in z.namelist()]
        for i, (name, data) in enumerate(entries):
            if name == "manifest.json":
                obj = json.loads(data)
                obj["modules"][0]["type"] = "data"
                entries[i] = (name, json.dumps(obj).encode())
        self.build(entries=entries)
        self.reject()


if __name__ == "__main__":
    unittest.main()
