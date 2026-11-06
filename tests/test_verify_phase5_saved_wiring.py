"""Bounded negative-evidence probes for compiled Phase 5 saved asset paths."""
import importlib.util
from pathlib import Path
import struct
import tempfile
import unittest
from zipfile import ZipFile

SOURCE = Path(__file__).resolve().parents[1] / 'tools' / 'verify_phase5_saved_wiring.py'
MODULE = importlib.util.spec_from_file_location('phase5_wiring', SOURCE)
app = importlib.util.module_from_spec(MODULE)
MODULE.loader.exec_module(app)

ROOT = 'co/surumene/simplecoins/'
DOMAIN = ROOT + 'domain/'
MAIN = ROOT + 'SimpleCoinsPlugin'
REQUIRED = (
    'WorldStorageDiscovery', 'WorldSnapshotTraversal', 'AnvilRegionReader',
    'PlayerDataReader', 'SettlementScanRunner', 'SettlementAccumulator',
    'PlayerDataSettlementImport', 'BlockEntitySettlementImport',
    'FreeEntitySettlementImport', 'ResolvedEntitySettlementImport',
    'SavedEntityUuidIntegrity',
)


def synthetic_class(constants=()):
    parts = []
    for value in constants:
        encoded = value.encode('ascii')
        parts.append(b'\x01' + struct.pack('>H', len(encoded)) + encoded)
    return b'\xca\xfe\xba\xbe' + b'\0\0\0\x41' + struct.pack('>H', len(parts) + 1) + b''.join(parts)


class SavedWiringTest(unittest.TestCase):
    def inspect(self, *, absent=(), main_refs=None, scan_refs=None, traversal_refs=None,
                corrupted=None, duplicate=False):
        refs_main = [DOMAIN + 'SettlementScanRunner'] if main_refs is None else main_refs
        refs_scan = [DOMAIN + n for n in (
            'WorldSnapshotTraversal', 'SettlementAccumulator', 'PlayerDataSettlementImport',
            'BlockEntitySettlementImport', 'FreeEntitySettlementImport',
            'ResolvedEntitySettlementImport')] if scan_refs is None else scan_refs
        refs_traversal = [DOMAIN + n for n in
                          ('AnvilRegionReader', 'PlayerDataReader', 'SavedEntityUuidIntegrity')
                          ] if traversal_refs is None else traversal_refs
        with tempfile.TemporaryDirectory() as directory:
            jarfile = Path(directory) / 'plugin.jar'
            with ZipFile(jarfile, 'w') as jar:
                jar.writestr('plugin.yml', 'name: SimpleCoins\nmain: co.surumene.simplecoins.SimpleCoinsPlugin\n')
                jar.writestr(MAIN + '.class', synthetic_class(refs_main))
                for name in REQUIRED:
                    if name in absent:
                        continue
                    constants = refs_scan if name == 'SettlementScanRunner' else (
                        refs_traversal if name == 'WorldSnapshotTraversal' else
                        ['requiredSourceIds', 'discover'] if name == 'WorldStorageDiscovery' else [])
                    payload = b'BAD' if name == corrupted else synthetic_class(constants)
                    jar.writestr(DOMAIN + name + '.class', payload)
                if duplicate:
                    jar.writestr(DOMAIN + 'AnvilRegionReader.class', synthetic_class())
            return app.inspect_jar(jarfile)

    def test_complete_static_hints_are_candidate_only_not_acceptance(self):
        report = self.inspect()
        self.assertEqual('candidate-only', report['status'])
        self.assertEqual([], report['missing'])
        self.assertIn('do not prove', report['limitation'])

    def test_missing_saved_player_reader_is_missing_not_zero_assets(self):
        report = self.inspect(absent=('PlayerDataReader',))
        self.assertEqual('missing', report['status'])
        self.assertIn('class:PlayerDataReader', report['missing'])

    def test_scanner_without_saved_entity_import_is_incomplete(self):
        refs = [DOMAIN + 'WorldSnapshotTraversal', DOMAIN + 'SettlementAccumulator',
                DOMAIN + 'PlayerDataSettlementImport', DOMAIN + 'BlockEntitySettlementImport',
                DOMAIN + 'FreeEntitySettlementImport']
        report = self.inspect(scan_refs=refs)
        self.assertIn('scanner:ResolvedEntitySettlementImport', report['missing'])

    def test_traversal_without_playerdata_reader_reference_is_incomplete(self):
        report = self.inspect(traversal_refs=[DOMAIN + 'AnvilRegionReader',
                                                 DOMAIN + 'SavedEntityUuidIntegrity'])
        self.assertIn('traversal:PlayerDataReader', report['missing'])

    def test_similarly_named_main_class_does_not_count_as_wired(self):
        report = self.inspect(main_refs=[DOMAIN + 'SettlementScanRunnerFake'])
        self.assertIn('main:SettlementScanRunner', report['missing'])

    def test_similarly_named_scanner_import_does_not_count_as_wired(self):
        refs = [DOMAIN + n for n in (
            'WorldSnapshotTraversal', 'SettlementAccumulator',
            'PlayerDataSettlementImport', 'BlockEntitySettlementImport',
            'FreeEntitySettlementImport', 'ResolvedEntitySettlementImportFake')]
        report = self.inspect(scan_refs=refs)
        self.assertIn('scanner:ResolvedEntitySettlementImport', report['missing'])

    def test_arbitrary_utf8_string_is_not_a_class_reference(self):
        # A string literal naming a class is not proof of a JVM class reference.
        self.assertFalse(app.has_class_reference(
            frozenset(['prefix ' + DOMAIN + 'SettlementScanRunner' + ' suffix']),
            DOMAIN + 'SettlementScanRunner'))

    def test_descriptor_embedded_in_arbitrary_text_is_not_a_reference(self):
        # A log message can quote a descriptor without linking the class.
        self.assertFalse(app.has_class_reference(
            frozenset(['log:L' + DOMAIN + 'SettlementScanRunner;:done']),
            DOMAIN + 'SettlementScanRunner'))

    def test_unregistered_entrypoint_is_not_a_candidate(self):
        report = self.inspect(main_refs=['registerEvents'])
        self.assertIn('main:SettlementScanRunner', report['missing'])

    def test_invalid_required_class_fails_closed(self):
        with self.assertRaises(app.InvalidClassFile):
            self.inspect(corrupted='WorldSnapshotTraversal')

    def test_duplicate_class_name_fails_closed(self):
        with self.assertRaises(ValueError):
            self.inspect(duplicate=True)


if __name__ == '__main__':
    unittest.main()
