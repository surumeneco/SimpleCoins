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


def synthetic_class(constants=(), *, literals=()):
    """Emit real CONSTANT_Class entries, plus optionally unreferenced UTF8 strings."""
    parts = []
    for value in constants:
        index = len(parts) + 1
        encoded = value.encode('ascii')
        parts.append(b'\x01' + struct.pack('>H', len(encoded)) + encoded)
        parts.append(b'\x07' + struct.pack('>H', index))
    for value in literals:
        encoded = value.encode('ascii')
        parts.append(b'\x01' + struct.pack('>H', len(encoded)) + encoded)
    return b'\xca\xfe\xba\xbe' + b'\0\0\0\x41' + struct.pack('>H', len(parts) + 1) + b''.join(parts)


class SavedWiringTest(unittest.TestCase):
    def inspect(self, *, absent=(), main_refs=None, scan_refs=None, traversal_refs=None,
                corrupted=None, duplicate=False, main_literals=(), scan_literals=()):
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
                jar.writestr(MAIN + '.class', synthetic_class(refs_main, literals=main_literals))
                for name in REQUIRED:
                    if name in absent:
                        continue
                    constants = refs_scan if name == 'SettlementScanRunner' else (
                        refs_traversal if name == 'WorldSnapshotTraversal' else
                        ['requiredSourceIds', 'discover'] if name == 'WorldStorageDiscovery' else [])
                    payload = b'BAD' if name == corrupted else synthetic_class(
                        constants, literals=scan_literals if name == 'SettlementScanRunner' else ())
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

    def test_scanner_reference_in_second_anonymous_class_is_detected(self):
        refs = [DOMAIN + n for n in (
            'WorldSnapshotTraversal', 'SettlementAccumulator', 'PlayerDataSettlementImport',
            'BlockEntitySettlementImport', 'FreeEntitySettlementImport',
            'ResolvedEntitySettlementImport')]
        with tempfile.TemporaryDirectory() as directory:
            jarfile = Path(directory) / 'plugin.jar'
            with ZipFile(jarfile, 'w') as jar:
                jar.writestr('plugin.yml',
                             'name: SimpleCoins\nmain: co.surumene.simplecoins.SimpleCoinsPlugin\n')
                jar.writestr(MAIN + '.class',
                             synthetic_class([DOMAIN + 'SettlementScanRunner']))
                for name in REQUIRED:
                    constants = (['requiredSourceIds', 'discover']
                                 if name == 'WorldStorageDiscovery' else
                                 [DOMAIN + n for n in ('AnvilRegionReader',
                                  'PlayerDataReader', 'SavedEntityUuidIntegrity')]
                                 if name == 'WorldSnapshotTraversal' else [])
                    jar.writestr(DOMAIN + name + '.class', synthetic_class(constants))
                jar.writestr(DOMAIN + 'SettlementScanRunner$2.class',
                             synthetic_class(refs))
            report = app.inspect_jar(jarfile)
        self.assertEqual('candidate-only', report['status'])
        self.assertFalse(any(key.startswith('scanner:') for key in report['missing']))

    def test_corrupt_second_anonymous_class_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            jarfile = Path(directory) / 'plugin.jar'
            with ZipFile(jarfile, 'w') as jar:
                jar.writestr('plugin.yml',
                             'name: SimpleCoins\nmain: co.surumene.simplecoins.SimpleCoinsPlugin\n')
                jar.writestr(MAIN + '.class', synthetic_class())
                jar.writestr(DOMAIN + 'SettlementScanRunner$2.class', b'BAD')
            with self.assertRaises(app.InvalidClassFile):
                app.inspect_jar(jarfile)

    def test_similarly_named_main_class_is_not_a_reference(self):
        report = self.inspect(main_refs=[DOMAIN + 'SettlementScanRunnerFake'])
        self.assertIn('main:SettlementScanRunner', report['missing'])

    def test_similarly_named_scanner_import_is_not_a_reference(self):
        refs = [DOMAIN + n for n in ('WorldSnapshotTraversal', 'SettlementAccumulator',
                'PlayerDataSettlementImport', 'BlockEntitySettlementImport',
                'FreeEntitySettlementImport', 'ResolvedEntitySettlementImportFake')]
        report = self.inspect(scan_refs=refs)
        self.assertIn('scanner:ResolvedEntitySettlementImport', report['missing'])

    def test_unreferenced_class_name_in_utf8_log_is_not_wiring(self):
        report = self.inspect(main_refs=[],
                              main_literals=['debug:' + DOMAIN + 'SettlementScanRunner'])
        self.assertIn('main:SettlementScanRunner', report['missing'])

    def test_unreferenced_descriptor_in_log_is_not_scanner_import(self):
        refs = [DOMAIN + n for n in ('WorldSnapshotTraversal', 'SettlementAccumulator',
                'PlayerDataSettlementImport', 'BlockEntitySettlementImport',
                'FreeEntitySettlementImport')]
        report = self.inspect(scan_refs=refs,
            scan_literals=['log:L' + DOMAIN + 'ResolvedEntitySettlementImport;:done'])
        self.assertIn('scanner:ResolvedEntitySettlementImport', report['missing'])

    def test_unregistered_entrypoint_is_not_a_candidate(self):
        report = self.inspect(main_refs=['registerEvents'])
        self.assertIn('main:SettlementScanRunner', report['missing'])

    def test_invalid_required_class_fails_closed(self):
        with self.assertRaises(app.InvalidClassFile):
            self.inspect(corrupted='WorldSnapshotTraversal')

    def test_duplicate_class_name_fails_closed(self):
        with self.assertRaises(ValueError):
            self.inspect(duplicate=True)


    def test_forward_constant_class_name_index_is_resolved(self):
        """A tagged JVM class may refer to a later constant-pool UTF8 entry."""
        name = DOMAIN + 'SettlementScanRunner'
        encoded = name.encode('ascii')
        payload = (b'\xca\xfe\xba\xbe' + b'\0\0\0\x41'
                   + struct.pack('>H', 3) + b'\x07\x00\x02'
                   + b'\x01' + struct.pack('>H', len(encoded)) + encoded)
        self.assertEqual(frozenset({name}), app.class_references(payload))

    def test_invalid_class_name_pool_index_fails_closed(self):
        """A dangling CONSTANT_Class must never count as scanner wiring."""
        payload = (b'\xca\xfe\xba\xbe' + b'\0\0\0\x41'
                   + struct.pack('>H', 2) + b'\x07\x00\x02')
        with self.assertRaises(app.InvalidClassFile):
            app.class_references(payload)

    def test_truncated_class_name_index_fails_closed(self):
        """A partial CONSTANT_Class index is not a valid reference."""
        payload = (b'\xca\xfe\xba\xbe' + b'\0\0\0\x41'
                   + struct.pack('>H', 2) + b'\x07\x00')
        with self.assertRaises(app.InvalidClassFile):
            app.class_references(payload)


if __name__ == '__main__':
    unittest.main()
