package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.HybridLiveSourceStage;
import co.surumene.simplecoins.domain.SavedLiveSourcePartition;
import co.surumene.simplecoins.domain.SettlementAccumulator;
import co.surumene.simplecoins.domain.WclHybridLiveOverlay;
import co.surumene.simplecoins.domain.WclOwnerEvidence;
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

class PaperCapturedHybridSettlementWclTest {
    @TempDir Path directory;
    private static final UUID CHICKEN =
            UUID.fromString("00000000-0000-0000-0000-000000000081");
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000082");
    private static final SavedLiveSourcePartition.Chunk CHUNK =
            new SavedLiveSourcePartition.Chunk("world", 0, 0);
    private static final String CHEST = "world/world/1,64,1:0";
    private static final WorldScope SCOPE = new WorldScope(true, Set.of(), Set.of());

    private PaperLiveUniverseAssets.Observed observed(boolean aliased) {
        String alias = "world/world/entity/" + CHICKEN + "/wcl/wwc/shulker";
        var entry = new PaperLoadedChunkAssets.Entry(CHEST, "world", BigInteger.valueOf(200));
        var values = new java.util.ArrayList<HybridLiveSourceStage.Value>();
        values.add(new HybridLiveSourceStage.Value(CHEST, "world", BigInteger.valueOf(200)));
        var entries = new java.util.ArrayList<PaperLoadedChunkAssets.Entry>();
        entries.add(entry);
        if (aliased) {
            values.add(new HybridLiveSourceStage.Value(alias, "world", BigInteger.valueOf(10000)));
            entries.add(new PaperLoadedChunkAssets.Entry(alias, "world", BigInteger.valueOf(10000)));
        }
        var world = new PaperLoadedWorldAssets.Report(
                "world", 1, 1, 0, 0, 0, 27, List.copyOf(entries));
        var online = new PaperLivePlayerAssets.OnlineSet(0, 0, List.of());
        return new PaperLiveUniverseAssets.Observed(
                new PaperLiveUniverseAssets.Report(List.of(world), online, 1, 1, 0, 27),
                List.of(new HybridLiveSourceStage.ChunkSource(CHUNK, values)));
    }

    private Path emptyPlayerdata(UUID id) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(9); out.writeUTF("Inventory");
            out.writeByte(10); out.writeInt(0);
            out.writeByte(9); out.writeUTF("EnderItems");
            out.writeByte(10); out.writeInt(0);
            out.writeByte(0);
        }
        Path target = directory.resolve("playerdata/" + id + ".dat");
        Files.createDirectories(target.getParent());
        try (var gzip = new GZIPOutputStream(Files.newOutputStream(target))) {
            gzip.write(bytes.toByteArray());
        }
        return target;
    }

    private WclHybridLiveOverlay.Selected owner(String slot, int amount) {
        return new WclHybridLiveOverlay.Selected(CHUNK, CHICKEN, "wwc",
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.OWNER, OWNER),
                Map.of(slot, BigInteger.valueOf(amount)));
    }

    private static final HybridLiveSourceStage.Classifier VANILLA = (source, value) ->
            new SettlementAccumulator.Asset(source.id(), value.physicalKey(),
                    value.world(), null, false, false, false, value.amount(),
                    value.physicalKey());

    @Test void wwcShulkerIsNowPartOfTheSameUnpublishedPendingSettlement()
            throws Exception {
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(), Map.of(OWNER, emptyPlayerdata(OWNER)));
        var captured = PaperCapturedHybridSettlement.fromObservedWithWcl(
                observed(false), new HashSet<>(), List.of(owner("shulker", 10000)));
        var staged = captured.stageCandidate(manifest, SCOPE, Set.of(),
                (world, block) -> null, (world, chunk) -> Map.of(),
                4096, 4096, VANILLA);
        var result = staged.pending().commit();
        assertEquals(BigInteger.valueOf(10200), result.statistics().worldTotal());
        assertEquals(BigInteger.valueOf(10000), result.personalBalances().get(OWNER));
        assertEquals(BigInteger.valueOf(200), result.free());
        assertEquals(BigInteger.ZERO, result.dead());
    }

    @Test void bannedWclOwnerBecomesDeadAssetWithoutChangingFreeAmount() throws Exception {
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(), Map.of(OWNER, emptyPlayerdata(OWNER)));
        var captured = PaperCapturedHybridSettlement.fromObservedWithWcl(
                observed(false), new HashSet<>(), List.of(owner("shulker", 10000)));
        var result = captured.stageCandidate(manifest, SCOPE, Set.of(OWNER),
                (world, block) -> null, (world, chunk) -> Map.of(),
                4096, 4096, VANILLA).pending().commit();
        assertEquals(BigInteger.valueOf(200), result.statistics().worldTotal());
        assertEquals(BigInteger.valueOf(200), result.free());
        assertEquals(BigInteger.valueOf(10000), result.dead());
        assertEquals(1, result.deadLocations().size());
    }

    @Test void alreadyCountedDisplayShulkerIsNeverAddedTwice() throws Exception {
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(), Map.of(OWNER, emptyPlayerdata(OWNER)));
        var captured = PaperCapturedHybridSettlement.fromObservedWithWcl(
                observed(true), new HashSet<>(), List.of(owner("shulker", 10000)));
        assertThrows(IOException.class, () -> captured.stageCandidate(
                manifest, SCOPE, Set.of(), (world, block) -> null,
                (world, chunk) -> Map.of(), 4096, 4096, VANILLA));
        assertThrows(IllegalStateException.class, () -> captured.stageCandidate(
                manifest, SCOPE, Set.of(), (world, block) -> null,
                (world, chunk) -> Map.of(), 4096, 4096, VANILLA));
    }

    @Test void unknownWclOwnershipCannotBecomeFreeOrPublishable() throws Exception {
        var unknown = new WclHybridLiveOverlay.Selected(CHUNK, CHICKEN, "wwc",
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.HOLD, null),
                Map.of("shulker", BigInteger.valueOf(10000)));
        var captured = PaperCapturedHybridSettlement.fromObservedWithWcl(
                observed(false), new HashSet<>(), List.of(unknown));
        assertThrows(IOException.class, () -> captured.stageCandidate(
                new WorldStorageDiscovery.Manifest(List.of(), Map.of()), SCOPE, Set.of(),
                (world, block) -> null, (world, chunk) -> Map.of(),
                4096, 4096, VANILLA));
    }
}
