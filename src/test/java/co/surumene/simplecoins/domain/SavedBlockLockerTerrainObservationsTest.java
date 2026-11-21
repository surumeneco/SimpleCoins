package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Two genuine compressed Anvil Region sources, straddling a region boundary.
 * The candidate observes both sources even if an adjacent sign is ABSENT.
 * No fake global writer epoch, BlockLocker owner proof or admission is supplied.
 */
class SavedBlockLockerTerrainObservationsTest {
    @TempDir Path directory;
    private static final int Y = 65;
    private static final int Z = 4;
    private static final int CHEST_X = 511;
    private static final int NEIGHBOR_X = 512;

    private static void string(DataOutputStream out, String name, String value)
            throws IOException {
        out.writeByte(8);
        out.writeUTF(name);
        out.writeUTF(value);
    }

    private static void integer(DataOutputStream out, String name, int value)
            throws IOException {
        out.writeByte(3);
        out.writeUTF(name);
        out.writeInt(value);
    }

    private static void textFace(DataOutputStream out, String key, String first, String second)
            throws IOException {
        out.writeByte(10);
        out.writeUTF(key);
        out.writeByte(9);
        out.writeUTF("messages");
        out.writeByte(8);
        out.writeInt(4);
        for (String line : List.of(first, second, "", ""))
            out.writeUTF(line);
        out.writeByte(0);
    }

    private static byte[] chunk(int chunkX, boolean chest, boolean sign)
            throws IOException {
        int x = chest ? CHEST_X : NEIGHBOR_X;
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeByte(10);
            out.writeUTF("");
            integer(out, "xPos", chunkX);
            integer(out, "zPos", 0);
            out.writeByte(9);
            out.writeUTF("sections");
            out.writeByte(10);
            out.writeInt(1);
            out.writeByte(3);
            out.writeUTF("Y");
            out.writeInt(4);
            out.writeByte(10);
            out.writeUTF("block_states");
            out.writeByte(9);
            out.writeUTF("palette");
            out.writeByte(10);
            out.writeInt(chest || sign ? 2 : 1);
            string(out, "Name", "minecraft:air");
            out.writeByte(0);
            if (chest || sign) {
                string(out, "Name",
                        chest ? "minecraft:chest" : "minecraft:oak_wall_sign");
                out.writeByte(10);
                out.writeUTF("Properties");
                string(out, "facing", chest ? "north" : "east");
                if (chest) string(out, "type", "single");
                out.writeByte(0);
                out.writeByte(0);
            }
            if (chest || sign) {
                out.writeByte(12);
                out.writeUTF("data");
                out.writeInt(256);
                // 4096 cells in 4 bits (16 cells/long).
                int index = ((Y & 15) << 8) | ((Z & 15) << 4) | (x & 15);
                for (int word = 0; word < 256; word++)
                    out.writeLong(word == index / 16
                            ? 1L << ((index % 16) * 4) : 0L);
            }
            out.writeByte(0); // block_states compound
            out.writeByte(0); // section compound
            out.writeByte(9);
            out.writeUTF("block_entities");
            out.writeByte(10);
            out.writeInt(chest || sign ? 1 : 0);
            if (chest || sign) {
                string(out, "id", chest ? "minecraft:chest" : "minecraft:sign");
                integer(out, "x", x);
                integer(out, "y", Y);
                integer(out, "z", Z);
                if (sign) {
                    textFace(out, "front_text", "[Private]", "Alice");
                    textFace(out, "back_text", "", "");
                }
                out.writeByte(0); // block entity
            }
            out.writeByte(0); // chunk root
        }
        return bytes.toByteArray();
    }

    private static Path region(Path directory, int regionX, byte[] chunk)
            throws IOException {
        Files.createDirectories(directory);
        var compressed = new ByteArrayOutputStream();
        try (var zip = new DeflaterOutputStream(compressed)) {
            zip.write(chunk);
        }
        byte[] payload = compressed.toByteArray();
        assertTrue(payload.length + 5 <= 4096, "Fixture must fit one Anvil sector");
        byte[] region = new byte[3 * 4096];
        int slot = regionX == 0 ? 31 : 0;
        ByteBuffer.wrap(region).putInt(slot * 4, (2 << 8) | 1);
        ByteBuffer.wrap(region).putInt(8192, payload.length + 1);
        region[8196] = 2; // zlib
        System.arraycopy(payload, 0, region, 8197, payload.length);
        Path path = directory.resolve("r." + regionX + ".0.mca");
        Files.write(path, region);
        return path.toAbsolutePath().normalize();
    }

    private record Fixture(
            Path region0, Path region1, WorldScope scope,
            WorldStorageDiscovery.Manifest manifest) {}

    private Fixture fixture(boolean sign) throws IOException {
        Path root = directory.resolve(sign ? "with-sign" : "no-sign");
        Path saved = root.resolve("region");
        Path a = region(saved, 0, chunk(31, true, false));
        Path b = region(saved, 1, chunk(32, false, sign));
        WorldScope scope = new WorldScope(true, Set.of(), Set.of());
        var discovered = WorldStorageDiscovery.discover(Map.of("world", root), scope, 1000);
        assertEquals(2, discovered.worldFiles().size(), "Both real Region files were discovered");
        return new Fixture(a, b, scope, discovered);
    }

    private static SavedBlockLockerTerrainObservations.Snapshot capture(Fixture f)
            throws IOException {
        return SavedBlockLockerTerrainObservations.capture(
                f.manifest(), f.scope(), 1024 * 1024, 4 * 1024 * 1024, 4,
                Set.of("[Private]"), Set.of("[More Users]"), Set.of("Everyone"),
                raw -> raw);
    }

    private static void assertSelectedNeighborhoodSources(Fixture f,
            SavedBlockLockerTerrainObservations.ObservedChest chest) {
        assertEquals(Set.of(f.region0(), f.region1()),
                Set.copyOf(chest.sources().stream()
                        .map(source -> source.path().toAbsolutePath().normalize()).toList()),
                "A local chest observation depends on the adjacent Region for SIGN PRESENCE/ABSENCE");
        assertEquals(2, chest.sources().size(), "Do not repeat any Region identity");
        assertTrue(chest.sources().stream().allMatch(f.manifest().worldFiles()::contains),
                "Every local evidence source must be in the authoritative Manifest");
    }

    @Test void evenAbsentAdjacentSignRequiresNeighborRegionProvenance() throws Exception {
        Fixture f = fixture(false);
        var result = capture(f);
        assertEquals(1, result.chests().size());
        var chest = result.chests().getFirst();
        assertEquals(List.of(new SavedBlockLockerChestSigns.Position(CHEST_X, Y, Z)),
                chest.observation().storageMembers());
        assertTrue(chest.observation().attachedSigns().isEmpty());
        assertSelectedNeighborhoodSources(f, chest);
        result.witness().requireUnchanged();
    }

    @Test void attachedWallSignInNextRegionMustAppearInSourceProvenance() throws Exception {
        Fixture f = fixture(true);
        var result = capture(f);
        assertEquals(1, result.chests().size());
        var chest = result.chests().getFirst();
        assertEquals(1, chest.observation().attachedSigns().size());
        assertEquals(new SavedBlockLockerChestSigns.Position(NEIGHBOR_X, Y, Z),
                chest.observation().attachedSigns().getFirst().position());
        assertEquals(BlockLockerHeaderMatcher.Type.PRIVATE,
                chest.observation().attachedSigns().getFirst().profiles().type());
        assertSelectedNeighborhoodSources(f, chest);
        result.witness().requireUnchanged();
    }

    @Test void unknownAdjacentRegionCannotProveNoProtection() throws Exception {
        Fixture f = fixture(false);
        Files.delete(f.region1());
        var discovered = WorldStorageDiscovery.discover(
                Map.of("world", f.region0().getParent().getParent()), f.scope(), 1000);
        var missing = new Fixture(f.region0(), f.region1(), f.scope(), discovered);
        assertThrows(IOException.class, () -> capture(missing),
                "A missing adjacent Chunk cannot silently become an unprotected chest");
    }

    private static SavedBlockLockerOwnerCandidate.ExternalProof syntheticComplete(
            boolean observedUnprotected) {
        // Synthetic LOCAL evidence only. G0 all-writer epoch is still unknown.
        return new SavedBlockLockerOwnerCandidate.ExternalProof(
                true, true, true, true, observedUnprotected);
    }

    @Test void actualTwoRegionObservationCanBeBoundToLocalUnpublishedFreeCandidate()
            throws Exception {
        Fixture f = fixture(false);
        var observed = capture(f);
        var index = observed.assembleOwnerIndex(f.manifest(),
                candidate -> syntheticComplete(true));
        var holder = index.lookup("world", CHEST_X + "," + Y + "," + Z);
        assertTrue(holder.verifiedNonProtection());
        assertTrue(holder.verifiedOwner().isEmpty());
        assertFalse(holder.everyoneEffective());
        observed.witness().requireUnchanged();
    }

    @Test void unknownOrIncompleteIndependentProofCannotCreateIndex() throws Exception {
        Fixture clear = fixture(false);
        var observed = capture(clear);
        assertThrows(IOException.class, () -> observed.assembleOwnerIndex(
                clear.manifest(), candidate ->
                        SavedBlockLockerOwnerCandidate.ExternalProof.unknown()));
        assertThrows(IOException.class, () -> observed.assembleOwnerIndex(
                clear.manifest(), candidate -> syntheticComplete(false)));
        assertThrows(IOException.class, () -> observed.assembleOwnerIndex(
                clear.manifest(), candidate -> null));
        Fixture unresolved = fixture(true);
        var withSign = capture(unresolved);
        assertThrows(IOException.class, () -> withSign.assembleOwnerIndex(
                unresolved.manifest(), candidate -> syntheticComplete(false)),
                "Private sign without verified owner UUID must remain HOLD");
    }

    @Test void foreignDiscoveryOrChangedSavedBytesCannotReuseObservedIndex()
            throws Exception {
        Fixture f = fixture(false);
        var observed = capture(f);
        var rediscovered = WorldStorageDiscovery.discover(
                Map.of("world", f.region0().getParent().getParent()), f.scope(), 1000);
        assertEquals(f.manifest().worldFiles(), rediscovered.worldFiles());
        assertThrows(IOException.class, () -> observed.assembleOwnerIndex(
                rediscovered, candidate -> syntheticComplete(true)));
        assertThrows(IOException.class, () -> observed.assembleOwnerIndex(
                new WorldStorageDiscovery.Manifest(f.manifest().worldFiles(), Map.of()),
                candidate -> syntheticComplete(true)));
        byte[] edited = Files.readAllBytes(f.region1());
        edited[edited.length - 1] ^= 1;
        Files.write(f.region1(), edited);
        assertThrows(IOException.class, () -> observed.assembleOwnerIndex(
                f.manifest(), candidate -> syntheticComplete(true)));
    }

    @Test void forgedIncompleteNegativeSourceOrDuplicatePhysicalChestFailsAtomically()
            throws Exception {
        Fixture f = fixture(false);
        var observed = capture(f);
        var original = observed.chests().getFirst();
        var incomplete = new SavedBlockLockerTerrainObservations.ObservedChest(
                original.world(), List.of(original.sources().getFirst()), original.observation());
        var forged = new SavedBlockLockerTerrainObservations.Snapshot(
                List.of(incomplete), observed.witness(), observed.report());
        assertThrows(IOException.class, () -> forged.assembleOwnerIndex(
                f.manifest(), candidate -> syntheticComplete(true)));
        var repeated = new SavedBlockLockerTerrainObservations.Snapshot(
                List.of(original, original), observed.witness(), observed.report());
        assertThrows(IOException.class, () -> repeated.assembleOwnerIndex(
                f.manifest(), candidate -> syntheticComplete(true)));
        var wrongWorld = new SavedBlockLockerTerrainObservations.Snapshot(
                List.of(new SavedBlockLockerTerrainObservations.ObservedChest(
                        "other-world", original.sources(), original.observation())),
                observed.witness(), observed.report());
        assertThrows(IOException.class, () -> wrongWorld.assembleOwnerIndex(
                f.manifest(), candidate -> syntheticComplete(true)));
    }

    @Test void forgedEveryoneSignCannotBorrowGenuineWitness() throws Exception {
        Fixture f = fixture(false);
        var genuine = capture(f);
        var trueChest = genuine.chests().getFirst();
        // Before the fix the publicly constructible Snapshot lets a caller
        // attach fabricated positive BlockLocker signs to genuine saved bytes.
        var fakeSign = new SavedBlockLockerChestSigns.AttachedSign(
                new SavedBlockLockerChestSigns.Position(CHEST_X, Y, Z + 1),
                new BlockLockerSavedProfileResolution.SignProfiles(
                        BlockLockerHeaderMatcher.Type.PRIVATE,
                        java.util.Optional.empty(), true, false));
        var forged = new SavedBlockLockerTerrainObservations.Snapshot(
                List.of(new SavedBlockLockerTerrainObservations.ObservedChest(
                        trueChest.world(), trueChest.sources(),
                        new SavedBlockLockerChestSigns.Observation(
                                trueChest.observation().storageMembers(),
                                List.of(fakeSign)))),
                genuine.witness(), genuine.report());
        assertThrows(IOException.class, () -> forged.assembleOwnerIndex(
                f.manifest(), chest -> syntheticComplete(false)),
                "A caller-manufactured Snapshot must not accept invented Everyone signage");
    }

    @Test void completeGenuinelyEmptySavedDiscoveryHasAnEmptyUnpublishedOwnerIndex()
            throws Exception {
        Path world = directory.resolve("empty-world");
        Files.createDirectories(world);
        var scope = new WorldScope(true, Set.of(), Set.of());
        var manifest = WorldStorageDiscovery.discover(Map.of("world", world), scope, 100);
        var saved = SavedBlockLockerTerrainObservations.capture(
                manifest, scope, 1024 * 1024, 4 * 1024 * 1024, 4,
                Set.of("[Private]"), Set.of("[More Users]"), Set.of("Everyone"),
                raw -> raw);
        assertTrue(saved.chests().isEmpty());
        var index = saved.assembleOwnerIndex(manifest,
                chest -> { throw new AssertionError("No physical chest may request owner proof"); });
        assertThrows(IOException.class, () -> index.lookup("world", "0,65,0"),
                "An empty genuine index cannot infer unprotected ownership");
        saved.witness().requireUnchanged();
    }
}
