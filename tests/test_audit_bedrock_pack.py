import copy
import json
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from audit_bedrock_pack import VALUES, audit


def fixture(root):
    pack = root / "coins.mcpack"
    mapping = root / "coins.json"
    ids = [f"simplecoins:coin_{n}" for n in VALUES]
    files = {
        "manifest.json": json.dumps({"format_version": 2, "header": {"name": "SimpleCoins", "uuid": "00000000-0000-0000-0000-000000000001"},
                                     "modules": [{"type": "resources", "uuid": "00000000-0000-0000-0000-000000000002"}]}).encode(),
        "textures/item_texture.json": json.dumps({"resource_pack_name": "SimpleCoins", "texture_name": "atlas.items",
                                                 "texture_data": {name: {"textures": ["textures/items/coin_" + str(n)]}
                                                                  for name, n in zip(ids, VALUES)}}).encode(),
    }
    files.update({f"textures/items/coin_{n}.png": b"png-placeholder" for n in VALUES})
    data = {"format_version": 2, "items": {"minecraft:paper": [
        {"type": "definition", "model": name, "bedrock_identifier": name, "display_name": name,
         "bedrock_options": {"icon": name}, "components": {"minecraft:max_stack_size": 64}}
        for name in ids]}}
    return pack, mapping, files, data


def write(pack, mapping, files, data):
    with zipfile.ZipFile(pack, "w") as archive:
        for name, value in files.items():
            archive.writestr(name, value)
    mapping.write_text(json.dumps(data), encoding="utf-8")


class AuditTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.pack, self.mapping, self.files, self.data = fixture(Path(self.tmp.name))

    def check(self, code=None):
        write(self.pack, self.mapping, self.files, self.data)
        result = audit(self.pack, self.mapping)
        if code:
            self.assertEqual("invalid", result["status"])
            self.assertIn(code, result["codes"])
        else:
            self.assertEqual({"status": "valid_structure", "codes": []}, result)

    def test_complete_synthetic_pair(self):
        self.check()

    def test_missing_png(self):
        self.files.pop("textures/items/coin_1.png")
        self.check("UNEXPECTED_OR_MISSING_ZIP_ENTRY")

    def test_extra_zip_member(self):
        self.files["../escape.txt"] = b"bad"
        self.check("UNEXPECTED_OR_MISSING_ZIP_ENTRY")

    def test_empty_png(self):
        self.files["textures/items/coin_1.png"] = b""
        self.check("EMPTY_TEXTURE")

    def test_invalid_atlas_reference(self):
        atlas = json.loads(self.files["textures/item_texture.json"])
        atlas["texture_data"]["simplecoins:coin_1"]["textures"] = ["textures/items/coin_5"]
        self.files["textures/item_texture.json"] = json.dumps(atlas).encode()
        self.check("TEXTURE_REFERENCE_MISMATCH")

    def test_missing_mapping_id(self):
        self.data["items"]["minecraft:paper"].pop()
        self.check("INVALID_MAPPING")

    def test_wrong_mapping_icon(self):
        self.data["items"]["minecraft:paper"][0]["bedrock_options"]["icon"] = "wrong"
        self.check("INVALID_MAPPING_ENTRY")

    def test_duplicate_mapping_id(self):
        self.data["items"]["minecraft:paper"][1] = copy.deepcopy(self.data["items"]["minecraft:paper"][0])
        self.check("DUPLICATE_MAPPING_ID")

    def test_same_manifest_uuids(self):
        manifest = json.loads(self.files["manifest.json"])
        manifest["modules"][0]["uuid"] = manifest["header"]["uuid"]
        self.files["manifest.json"] = json.dumps(manifest).encode()
        self.check("DUPLICATE_PACK_UUID")

    def test_corrupt_zip(self):
        self.pack.write_bytes(b"not-a-zip")
        self.mapping.write_text("{}")
        self.assertIn("UNREADABLE_ARTIFACT", audit(self.pack, self.mapping)["codes"])


if __name__ == "__main__":
    unittest.main()
