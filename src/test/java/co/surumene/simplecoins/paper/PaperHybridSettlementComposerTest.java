package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.*;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure-JVM regression for the Paper -> saved-NBT -> live accounting boundary. */
class PaperHybridSettlementComposerTest {
    private static final UUID USER =
            UUID.fromString("00000000-0000-0000-0000-000000007111");
    private static final String BLOCK_SLOT = "world/world/1,64,1:0";
    private static final String PLAYER_SLOT = "player/" + USER + "/inventory/0";

    private static PaperLiveUniverseAssets.Observed capture() {
        var block = new PaperLoadedChunkAssets.Entry(
                BLOCK_SLOT, "world", BigInteger.valueOf(700));
        var reportedWorld = new PaperLoadedWorldAssets.Report(
                "world", 1, 1, 0, 0, 0, 27, List.of(block));
        var player = new PaperLivePlayerAssets.Entry(
                PLAYER_SLOT, USER, BigInteger.valueOf(500));
        var online = new PaperLivePlayerAssets.OnlineSet(1, 63,
                List.of(new PaperLivePlayerAssets.Snapshot(USER, 63, List.of(player))));
        var report = new PaperLiveUniverseAssets.Report(List.of(reportedWorld), online,
                1, 1, 0, 90);
        var chunk = new HybridLiveSourceStage.ChunkSource(
                new SavedLiveSourcePartition.Chunk("world", 0, 0),
                List.of(new HybridLiveSourceStage.Value(
                        BLOCK_SLOT, "world", BigInteger.valueOf(700))));
        var inventory = new HybridLiveSourceStage.PlayerSource(USER,
                List.of(new HybridLiveSourceStage.Value(
                        PLAYER_SLOT, null, BigInteger.valueOf(500))));
        return new PaperLiveUniverseAssets.Observed(report, List.of(chunk, inventory));
    }

    private static HybridLiveSourceStage.Classifier classifier() {
        return (source, value) -> {
            UUID owner = source instanceof HybridLiveSourceStage.PlayerSource ? USER : null;
            return new SettlementAccumulator.Asset(source.id(), value.physicalKey(),
                    value.world(), owner, false, false, true,
                    value.amount(), "accounting/" + source.id() + "/" + value.physicalKey());
        };
    }

    @Test void capturedPaperSourcesStageThroughOnePendingSavedAndLiveGeneration() throws Exception {
        var manifest = new WorldStorageDiscovery.Manifest(List.of(), Map.of());
        var staged = PaperHybridSettlementComposer.compose(capture(), manifest,
                new WorldScope(true, Set.of(), Set.of()),
                Set.of(), (world, location) -> null, (world, nbt) -> Map.of(),
                classifier(), 4096, 4096, new java.util.HashSet<>());
        assertEquals(2, staged.requiredSourceIds().size());
        var result = staged.pending().commit();
        assertEquals(BigInteger.valueOf(1200), result.statistics().worldTotal());
        assertEquals(BigInteger.valueOf(700), result.free());
        assertEquals(BigInteger.valueOf(500), result.personalBalances().get(USER));
    }

    @Test void scopeExclusionDoesNotSilentlyZeroTheLiveWorld() throws Exception {
        var manifest = new WorldStorageDiscovery.Manifest(List.of(), Map.of());
        assertThrows(IOException.class, () -> PaperHybridSettlementComposer.compose(
                capture(), manifest, new WorldScope(false, Set.of(), Set.of()),
                Set.of(), (world, location) -> null, (world, nbt) -> Map.of(),
                classifier(), 4096, 4096, new java.util.HashSet<>()));
    }

    @Test void malformedOrMissingClassificationFailsWithoutPublishing() throws Exception {
        var manifest = new WorldStorageDiscovery.Manifest(List.of(), Map.of());
        assertThrows(IOException.class, () -> PaperHybridSettlementComposer.compose(
                capture(), manifest, new WorldScope(true, Set.of(), Set.of()),
                Set.of(), (world, location) -> null, (world, nbt) -> Map.of(),
                (source, value) -> null, 4096, 4096, new java.util.HashSet<>()));
    }

    @Test void duplicateWalletContextProvidedByCallerIsNotReplaced() throws Exception {
        var shared = new java.util.HashSet<UUID>();
        UUID seen = UUID.fromString("00000000-0000-0000-0000-000000007211");
        shared.add(seen);
        PaperHybridSettlementComposer.compose(capture(),
                new WorldStorageDiscovery.Manifest(List.of(), Map.of()),
                new WorldScope(true, Set.of(), Set.of()), Set.of(),
                (world, location) -> null, (world, nbt) -> Map.of(),
                classifier(), 4096, 4096, shared);
        assertEquals(Set.of(seen), shared);
    }
}
