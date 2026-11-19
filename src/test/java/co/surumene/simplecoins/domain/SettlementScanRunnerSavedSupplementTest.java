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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DeflaterOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettlementScanRunnerSavedSupplementTest {
    @TempDir Path root;

    private Path emptyEntityAnvil() throws Exception {
        Path target = root.resolve("entities/r.0.0.mca");
        Files.createDirectories(target.getParent());
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(9); out.writeUTF("Entities");
            out.writeByte(10); out.writeInt(0);
            out.writeByte(0);
        }
        var compressed = new ByteArrayOutputStream();
        try (var z = new DeflaterOutputStream(compressed)) {
            z.write(nbt.toByteArray());
        }
        byte[] bytes = compressed.toByteArray();
        byte[] mca = new byte[3 * 4096];
        var buffer = ByteBuffer.wrap(mca);
        buffer.putInt(0, (2 << 8) | 1);
        buffer.putInt(8192, bytes.length + 1);
        mca[8196] = 2;
        System.arraycopy(bytes, 0, mca, 8197, bytes.length);
        Files.write(target, mca);
        return target;
    }

    private static final WorldScope SCOPE = new WorldScope(true, Set.of(), Set.of());

    @Test void supplementReceivesOnlySelectedSavedEntityAndItsOpenSource() throws Exception {
        var source = new WorldStorageDiscovery.SourceFile(
                "world", emptyEntityAnvil(), WorldStorageDiscovery.Kind.ENTITY);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(source), Map.of());
        var calls = new AtomicInteger();
        var pending = SettlementScanRunner.stage(
                manifest, SCOPE, Set.of(), (world, pos) -> null,
                (world, chunk) -> Map.of(), 4096, 4096,
                new java.util.HashSet<>(),
                new SavedLiveSourcePartition(List.of(), List.of()),
                (s, chunk, transaction, wallets) -> {
                    calls.incrementAndGet();
                    assertEquals(source, s);
                    assertEquals(0, chunk.x());
                    assertEquals(0, chunk.z());
                    transaction.accept(new SettlementAccumulator.Asset(
                            source.path().toAbsolutePath().normalize().toString(),
                            "entity/" + UUID.fromString("00000000-0000-0000-0000-000000000093")
                                    + "/wcl/wwc/shulker",
                            "world", null, false, false, false,
                            BigInteger.valueOf(10000), "world/unloaded-saved-WCL"));
                });
        assertEquals(1, calls.get());
        assertEquals(BigInteger.valueOf(10000), pending.pending().commit().free());
        assertNotNull(pending.savedContentWitness());
    }

    @Test void liveShadowedEntityNeverInvokesTheSavedSupplement() throws Exception {
        var source = new WorldStorageDiscovery.SourceFile(
                "world", emptyEntityAnvil(), WorldStorageDiscovery.Kind.ENTITY);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(source), Map.of());
        var calls = new AtomicInteger();
        var plan = new SavedLiveSourcePartition(
                List.of(new SavedLiveSourcePartition.Chunk("world", 0, 0)), List.of());
        plan.liveChunk(new SavedLiveSourcePartition.Chunk("world", 0, 0));
        var staged = SettlementScanRunner.stage(
                manifest, SCOPE, Set.of(), (world, pos) -> null,
                (world, chunk) -> Map.of(), 4096, 4096, new java.util.HashSet<>(),
                plan,
                (s, chunk, transaction, wallets) -> calls.incrementAndGet());
        assertEquals(0, calls.get());
        assertThrows(IllegalStateException.class, staged.pending()::commit,
                "A LIVE monetary source still has to be explicitly staged");
    }

    @Test void supplementFailureAbortsTheWholeSavedSourceGeneration() throws Exception {
        var source = new WorldStorageDiscovery.SourceFile(
                "world", emptyEntityAnvil(), WorldStorageDiscovery.Kind.ENTITY);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(source), Map.of());
        var error = assertThrows(IOException.class, () -> SettlementScanRunner.stage(
                manifest, SCOPE, Set.of(), (world, pos) -> null,
                (world, chunk) -> Map.of(), 4096, 4096,
                new java.util.HashSet<>(), null,
                (s, chunk, pending, wallets) -> {
                    throw new IOException("saved-wcl-provider-read-failed");
                }));
        assertEquals("saved-wcl-provider-read-failed", error.getMessage());
    }
}
