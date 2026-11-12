package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.HybridLiveSourceStage;
import co.surumene.simplecoins.domain.SavedLiveSourcePartition;
import co.surumene.simplecoins.domain.SettlementAccumulator;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** One real captured Paper source, plus compressed saved playerdata. */
class PaperCapturedHybridSettlementTest {
    @TempDir Path directory;

    private static final String CHEST = "world/world/1,64,1:0";
    private static final UUID OFFLINE =
            UUID.fromString("00000000-0000-0000-0000-000000008001");
    private static final UUID ONLINE =
            UUID.fromString("00000000-0000-0000-0000-000000008002");

    private Path playerdata(UUID id, int count) throws IOException {
        Path file = directory.resolve("playerdata/" + id + ".dat");
        Files.createDirectories(file.getParent());
        var stream = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(stream)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(9); out.writeUTF("Inventory"); out.writeByte(10);
            out.writeInt(count > 0 ? 1 : 0);
            if (count > 0) {
                out.writeByte(1); out.writeUTF("Slot"); out.writeByte(0);
                out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:paper");
                out.writeByte(3); out.writeUTF("count"); out.writeInt(count);
                out.writeByte(10); out.writeUTF("components");
                out.writeByte(10); out.writeUTF("minecraft:custom_data");
                out.writeByte(10); out.writeUTF("PublicBukkitValues");
                out.writeByte(3); out.writeUTF("simplecoins:denomination");
                out.writeInt(100);
                out.writeByte(0); out.writeByte(0); out.writeByte(0); out.writeByte(0);
            }
            out.writeByte(9); out.writeUTF("EnderItems"); out.writeByte(10); out.writeInt(0);
            out.writeByte(0);
        }
        var packed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(packed)) {
            gzip.write(stream.toByteArray());
        }
        Files.write(file, packed.toByteArray());
        return file;
    }

    private PaperLiveUniverseAssets.Observed captured(boolean withOnline) {
        var money = new PaperLoadedChunkAssets.Entry(
                CHEST, "world", BigInteger.valueOf(200));
        var world = new PaperLoadedWorldAssets.Report(
                "world", 1, 1, 0, 0, 0, 27, List.of(money));
        var chunk = new HybridLiveSourceStage.ChunkSource(
                new SavedLiveSourcePartition.Chunk("world", 0, 0),
                List.of(new HybridLiveSourceStage.Value(
                        CHEST, "world", BigInteger.valueOf(200))));
        if (!withOnline) {
            var empty = new PaperLivePlayerAssets.OnlineSet(0, 0, List.of());
            return new PaperLiveUniverseAssets.Observed(
                    new PaperLiveUniverseAssets.Report(
                            List.of(world), empty, 1, 1, 0, 27), List.of(chunk));
        }
        String key = "player/" + ONLINE + "/inventory/0";
        var held = new PaperLivePlayerAssets.Entry(key, ONLINE, BigInteger.valueOf(150));
        var players = new PaperLivePlayerAssets.OnlineSet(1, 63,
                List.of(new PaperLivePlayerAssets.Snapshot(
                        ONLINE, 63, List.of(held))));
        var player = new HybridLiveSourceStage.PlayerSource(ONLINE,
                List.of(new HybridLiveSourceStage.Value(key, null, BigInteger.valueOf(150))));
        return new PaperLiveUniverseAssets.Observed(
                new PaperLiveUniverseAssets.Report(
                        List.of(world), players, 1, 1, 0, 90), List.of(chunk, player));
    }

    private static final HybridLiveSourceStage.Classifier OWNER = (source, value) ->
            new SettlementAccumulator.Asset(
                    source.id(), value.physicalKey(), value.world(),
                    source instanceof HybridLiveSourceStage.PlayerSource p
                            ? p.player() : null,
                    false, false, true, value.amount(), "observed/" + value.physicalKey());

    private static WorldScope scope() { return new WorldScope(true, Set.of(), Set.of()); }

    @Test void stagesRealPaperSourceWithSavedOfflinePlayerExactlyOnce() throws Exception {
        var data = playerdata(OFFLINE, 3);
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(), Map.of(OFFLINE, data));
        var capture = PaperCapturedHybridSettlement.fromObserved(
                captured(false), new HashSet<>());
        var pending = capture.stageCandidate(
                manifest, scope(), Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(),
                4096, 4096, OWNER);
        var result = pending.pending().commit();
        assertEquals(BigInteger.valueOf(500), result.statistics().worldTotal());
        assertEquals(BigInteger.valueOf(200), result.free());
        assertEquals(BigInteger.valueOf(300), result.personalBalances().get(OFFLINE));
        assertThrows(IllegalStateException.class, () -> capture.stageCandidate(
                manifest, scope(), Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(),
                4096, 4096, OWNER));
    }

    @Test void onlinePlayerdataIsShadowedWhileLivePlayerAndOfflinePlayerAreCounted() throws Exception {
        var online = playerdata(ONLINE, 10);
        var offline = playerdata(OFFLINE, 3);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(),
                Map.of(ONLINE, online, OFFLINE, offline));
        var capture = PaperCapturedHybridSettlement.fromObserved(
                captured(true), new HashSet<>());
        var staged = capture.stageCandidate(
                manifest, scope(), Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(),
                4096, 4096, OWNER);
        var totals = staged.pending().commit();
        assertEquals(BigInteger.valueOf(650), totals.statistics().worldTotal());
        assertEquals(BigInteger.valueOf(150), totals.personalBalances().get(ONLINE));
        assertEquals(BigInteger.valueOf(300), totals.personalBalances().get(OFFLINE));
        assertEquals(BigInteger.valueOf(200), totals.free());
    }

    @Test void missingOwnershipEvidenceAbortsCaptureAndCannotBeRetried() throws Exception {
        var manifest = new WorldStorageDiscovery.Manifest(List.of(),
                Map.of(OFFLINE, playerdata(OFFLINE, 3)));
        var capture = PaperCapturedHybridSettlement.fromObserved(
                captured(false), new HashSet<>());
        assertThrows(IOException.class, () -> capture.stageCandidate(
                manifest, scope(), Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(),
                4096, 4096, (source, value) -> null));
        assertThrows(IllegalStateException.class, () -> capture.stageCandidate(
                manifest, scope(), Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(),
                4096, 4096, OWNER));
    }

    @Test void savedReadFailureDoesNotAllowRetryingStaleLiveCapture() throws Exception {
        var absent = directory.resolve("playerdata/missing.dat");
        var manifest = new WorldStorageDiscovery.Manifest(List.of(),
                Map.of(OFFLINE, absent));
        var capture = PaperCapturedHybridSettlement.fromObserved(
                captured(false), new HashSet<>());
        assertThrows(IOException.class, () -> capture.stageCandidate(
                manifest, scope(), Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(),
                4096, 4096, OWNER));
        assertThrows(IllegalStateException.class, () -> capture.stageCandidate(
                manifest, scope(), Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(),
                4096, 4096, OWNER));
    }
}
