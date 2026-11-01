package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettlementScanRunnerTest {
    @TempDir Path directory;

    private Path playerData(UUID player) throws IOException {
        return playerData(player, 0);
    }

    private Path playerData(UUID player, int coinCount) throws IOException {
        Path file = directory.resolve("playerdata/" + player + ".dat");
        Files.createDirectories(file.getParent());
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(9); out.writeUTF("Inventory"); out.writeByte(10);
            out.writeInt(coinCount > 0 ? 1 : 0);
            if (coinCount > 0) {
                out.writeByte(1); out.writeUTF("Slot"); out.writeByte(0);
                out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:paper");
                out.writeByte(3); out.writeUTF("count"); out.writeInt(coinCount);
                out.writeByte(10); out.writeUTF("components");
                out.writeByte(10); out.writeUTF("minecraft:custom_data");
                out.writeByte(10); out.writeUTF("PublicBukkitValues");
                out.writeByte(3); out.writeUTF("simplecoins:denomination"); out.writeInt(100);
                out.writeByte(0); // PublicBukkitValues
                out.writeByte(0); // custom_data
                out.writeByte(0); // components
                out.writeByte(0); // inventory item
            }
            out.writeByte(9); out.writeUTF("EnderItems"); out.writeByte(10); out.writeInt(0);
            out.writeByte(0);
        }
        var compressed = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(compressed)) {
            gzip.write(nbt.toByteArray());
        }
        Files.write(file, compressed.toByteArray());
        return file;
    }

    private Path emptyRegion() throws IOException {
        return region(false);
    }

    private Path region(boolean withChest) throws IOException {
        Path file = directory.resolve("region/r.0.0.mca");
        Files.createDirectories(file.getParent());
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(9); out.writeUTF("block_entities");
            out.writeByte(10); out.writeInt(withChest ? 1 : 0);
            if (withChest) {
                out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:chest");
                out.writeByte(3); out.writeUTF("x"); out.writeInt(0);
                out.writeByte(3); out.writeUTF("y"); out.writeInt(64);
                out.writeByte(3); out.writeUTF("z"); out.writeInt(0);
                out.writeByte(0); // chest compound
            }
            out.writeByte(0);
        }
        var compressed = new ByteArrayOutputStream();
        try (var deflate = new DeflaterOutputStream(compressed)) {
            deflate.write(nbt.toByteArray());
        }
        byte[] payload = compressed.toByteArray();
        byte[] region = new byte[3 * 4096];
        ByteBuffer.wrap(region).putInt(0, (2 << 8) | 1);
        ByteBuffer.wrap(region).putInt(8192, payload.length + 1);
        region[8196] = 2;
        System.arraycopy(payload, 0, region, 8197, payload.length);
        Files.write(file, region);
        return file;
    }

    private Path equippedEntityRegion(UUID entityId) throws IOException {
        Path file = directory.resolve("entities/r.0.0.mca");
        Files.createDirectories(file.getParent());
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(9); out.writeUTF("Entities"); out.writeByte(10); out.writeInt(1);
            out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:wolf");
            out.writeByte(11); out.writeUTF("UUID"); out.writeInt(4);
            out.writeInt((int)(entityId.getMostSignificantBits() >>> 32));
            out.writeInt((int)entityId.getMostSignificantBits());
            out.writeInt((int)(entityId.getLeastSignificantBits() >>> 32));
            out.writeInt((int)entityId.getLeastSignificantBits());
            out.writeByte(10); out.writeUTF("equipment");
            out.writeByte(10); out.writeUTF("mainhand");
            out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:paper");
            out.writeByte(3); out.writeUTF("count"); out.writeInt(1);
            out.writeByte(10); out.writeUTF("components");
            out.writeByte(10); out.writeUTF("minecraft:custom_data");
            out.writeByte(10); out.writeUTF("PublicBukkitValues");
            out.writeByte(3); out.writeUTF("simplecoins:denomination"); out.writeInt(100);
            out.writeByte(0); // PublicBukkitValues
            out.writeByte(0); // custom_data
            out.writeByte(0); // components
            out.writeByte(0); // mainhand
            out.writeByte(0); // equipment
            out.writeByte(0); // Entity
            out.writeByte(0); // root
        }
        var compressed = new ByteArrayOutputStream();
        try (var deflate = new DeflaterOutputStream(compressed)) {
            deflate.write(nbt.toByteArray());
        }
        byte[] payload = compressed.toByteArray();
        byte[] region = new byte[3 * 4096];
        ByteBuffer.wrap(region).putInt(0, (2 << 8) | 1);
        ByteBuffer.wrap(region).putInt(8192, payload.length + 1);
        region[8196] = 2;
        System.arraycopy(payload, 0, region, 8197, payload.length);
        Files.write(file, region);
        return file;
    }

    @Test void ownedEntityEquipmentRequiresExplicitResolvedOwnership() throws Exception {
        UUID entityId = UUID.randomUUID(), owner = UUID.randomUUID();
        Path file = equippedEntityRegion(entityId);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", file, WorldStorageDiscovery.Kind.ENTITY)),
                Map.of());
        var scope = new WorldScope(true, Set.of(), Set.of());
        assertThrows(IOException.class, () -> SettlementScanRunner.stage(manifest,
                scope, Set.of(), (world, position) -> null,
                (world, chunk) -> Map.of(), 4096, 4096));
        var staged = SettlementScanRunner.stage(manifest, scope, Set.of(),
                (world, position) -> null,
                (world, chunk) -> Map.of(entityId.toString(),
                        new ResolvedEntitySettlementImport.Ownership(owner, false, true)),
                4096, 4096);
        assertEquals(BigInteger.valueOf(100),
                staged.pending().commit().personalBalances().get(owner));
    }

    @Test void stagesAllManifestSourcesWithoutAutomaticallyCommitting() throws Exception {
        UUID player = UUID.randomUUID();
        Path region = emptyRegion(), data = playerData(player);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", region, WorldStorageDiscovery.Kind.REGION)),
                Map.of(player, data));
        var staged = SettlementScanRunner.stage(manifest,
                new WorldScope(true, Set.of(), Set.of()), Set.of(),
                (world, position) -> { throw new IOException("No empty container needs ownership"); },
                (world, chunk) -> Map.of(), 4096, 4096);
        assertEquals(manifest.requiredSourceIds(), staged.requiredSourceIds());
        assertEquals(1, staged.report().regionFiles());
        assertEquals(1, staged.report().playerFiles());
        // The coordinator must leave publication to a separate verified gate.
        assertEquals(BigInteger.ZERO, staged.pending().commit().statistics().worldTotal());
    }

    @Test void invalidPlayerdataAbortsStagingWithoutReturningPartialResult() throws Exception {
        UUID player = UUID.randomUUID();
        Path data = playerData(player);
        Files.write(data, new byte[]{1, 2, 3});
        var manifest = new WorldStorageDiscovery.Manifest(List.of(), Map.of(player, data));
        assertThrows(IOException.class, () -> SettlementScanRunner.stage(manifest,
                new WorldScope(true, Set.of(), Set.of()), Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(), 4096, 4096));
    }

    @Test void stagesSavedCoinsAndCurrentBanStateWithoutPublishing() throws Exception {
        UUID player = UUID.randomUUID();
        Path data = playerData(player, 2);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(), Map.of(player, data));
        var scope = new WorldScope(true, Set.of(), Set.of());
        var personal = SettlementScanRunner.stage(manifest, scope, Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(), 4096, 4096);
        assertEquals(BigInteger.valueOf(200),
                personal.pending().commit().personalBalances().get(player));
        var banned = SettlementScanRunner.stage(manifest, scope, Set.of(player),
                (world, position) -> null, (world, chunk) -> Map.of(), 4096, 4096);
        assertEquals(BigInteger.valueOf(200), banned.pending().commit().statistics().dead());
    }

    @Test void unresolvedBlockOwnershipAbortsTheEntireStagingRun() throws Exception {
        Path file = region(true);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", file, WorldStorageDiscovery.Kind.REGION)),
                Map.of());
        assertThrows(IOException.class, () -> SettlementScanRunner.stage(manifest,
                new WorldScope(true, Set.of(), Set.of()), Set.of(),
                (world, position) -> { throw new IOException("BlockLocker ownership not resolved"); },
                (world, chunk) -> Map.of(), 4096, 4096));
    }
}
