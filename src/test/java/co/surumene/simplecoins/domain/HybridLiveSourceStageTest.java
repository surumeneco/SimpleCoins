package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Real settlement source IDs, not a zero-balance fallback when LIVE was omitted. */
class HybridLiveSourceStageTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000004001");
    private static final SavedLiveSourcePartition.Chunk CHUNK =
            new SavedLiveSourcePartition.Chunk("world", -1, 2);

    private record Setup(SavedLiveSourcePartition plan, SettlementScanRunner.Staged staged) {}
    private static Setup setup() {
        var plan = new SavedLiveSourcePartition(List.of(CHUNK), List.of(OWNER));
        var required = new java.util.HashSet<>(plan.requiredLiveSourceIds());
        required.add("disk");
        var accumulator = new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()), required);
        accumulator.completeSource("disk");
        return new Setup(plan, new SettlementScanRunner.Staged(
                accumulator, new WorldSnapshotTraversal.Report(0, 0, 0), required));
    }

    private static final HybridLiveSourceStage.Classifier CLASSIFIER = (source, value) -> {
        UUID owner = source instanceof HybridLiveSourceStage.PlayerSource
                ? OWNER : null;
        return new SettlementAccumulator.Asset(source.id(), value.physicalKey(),
                value.world(), owner, false, false, true,
                value.amount(), source.id() + "/" + value.physicalKey());
    };

    private static HybridLiveSourceStage.ChunkSource chunk(long amount, String key) {
        return new HybridLiveSourceStage.ChunkSource(CHUNK, List.of(
                new HybridLiveSourceStage.Value(key, "world", BigInteger.valueOf(amount))));
    }

    private static HybridLiveSourceStage.PlayerSource player(long amount, String key) {
        return new HybridLiveSourceStage.PlayerSource(OWNER, List.of(
                new HybridLiveSourceStage.Value(key, null, BigInteger.valueOf(amount))));
    }

    @Test void fullAndExplicitLiveSourcesCanCompleteCombinedSettlement() throws Exception {
        Setup s = setup();
        assertThrows(IllegalStateException.class, s.staged().pending()::commit);
        HybridLiveSourceStage.stage(s.staged(), s.plan(), List.of(
                chunk(700, "world/world/-1,64,32:0"),
                player(500, "player/" + OWNER + "/inventory/0")), CLASSIFIER);
        var result = s.staged().pending().commit();
        assertEquals(BigInteger.valueOf(1200), result.statistics().worldTotal());
        assertEquals(BigInteger.valueOf(500), result.personalBalances().get(OWNER));
        assertEquals(BigInteger.valueOf(700), result.free());
    }

    @Test void missingLiveSourceFailsInsteadOfCommittingZero() {
        Setup s = setup();
        assertThrows(IOException.class, () -> HybridLiveSourceStage.stage(
                s.staged(), s.plan(), List.of(chunk(700, "world/world/-1,64,32:0")), CLASSIFIER));
        assertThrows(IllegalStateException.class, s.staged().pending()::commit);
    }

    @Test void zeroCurrencyStillRequiresExplicitSourceObservation() throws Exception {
        Setup s = setup();
        var chunks = new HybridLiveSourceStage.ChunkSource(CHUNK, List.of());
        var online = new HybridLiveSourceStage.PlayerSource(OWNER, List.of());
        HybridLiveSourceStage.stage(s.staged(), s.plan(), List.of(chunks, online), CLASSIFIER);
        assertEquals(BigInteger.ZERO, s.staged().pending().commit().statistics().worldTotal());
    }

    @Test void duplicateOrUnrecognizedSourceFailsBeforeAnyAssetIsAccepted() {
        Setup s = setup();
        var chunk = chunk(50, "world/world/-1,64,32:0");
        assertThrows(IOException.class, () -> HybridLiveSourceStage.stage(s.staged(), s.plan(),
                List.of(chunk, chunk, player(20, "player/" + OWNER + "/inventory/0")),
                CLASSIFIER));
        assertThrows(IllegalStateException.class, s.staged().pending()::commit);
        Setup different = setup();
        assertThrows(IOException.class, () -> HybridLiveSourceStage.stage(
                different.staged(), different.plan(), List.of(
                        new HybridLiveSourceStage.ChunkSource(
                                new SavedLiveSourcePartition.Chunk("other", 0, 0), List.of()),
                        new HybridLiveSourceStage.PlayerSource(OWNER, List.of())), CLASSIFIER));
    }

    @Test void classifierCannotChangeSourceValuePhysicalSlotOrWorld() {
        for (int defect = 0; defect < 4; defect++) {
            Setup s = setup();
            final int code = defect;
            var resolver = (HybridLiveSourceStage.Classifier) (source, value) ->
                    new SettlementAccumulator.Asset(code == 0 ? "disk" : source.id(),
                            code == 1 ? "invented-key" : value.physicalKey(),
                            code == 2 ? "different-world" : value.world(),
                            null, false, false, true,
                            code == 3 ? BigInteger.valueOf(999) : value.amount(),
                            "invented/asset/location");
            assertThrows(IOException.class, () -> HybridLiveSourceStage.stage(
                    s.staged(), s.plan(), List.of(chunk(700, "world/world/-1,64,32:0"),
                            new HybridLiveSourceStage.PlayerSource(OWNER, List.of())), resolver));
            assertThrows(IllegalStateException.class, s.staged().pending()::commit);
        }
    }

    @Test void aPlayerSourceCannotBeClassifiedAsFreeOrWorldBound() {
        Setup s = setup();
        var bad = (HybridLiveSourceStage.Classifier) (source, value) ->
                new SettlementAccumulator.Asset(source.id(), value.physicalKey(),
                        value.world(), null, false, false, true, value.amount(),
                        "misclassified/player");
        assertThrows(IOException.class, () -> HybridLiveSourceStage.stage(
                s.staged(), s.plan(), List.of(chunk(100, "world/world/-1,64,32:0"),
                        player(100, "player/" + OWNER + "/inventory/0")), bad));
        assertThrows(IllegalStateException.class, s.staged().pending()::commit);
    }

    @Test void nullOwnershipEvidenceIsNeverSilentlyTreatedAsFree() {
        Setup s = setup();
        assertThrows(IOException.class, () -> HybridLiveSourceStage.stage(
                s.staged(), s.plan(), List.of(
                        chunk(100, "world/world/-1,64,32:0"),
                        new HybridLiveSourceStage.PlayerSource(OWNER, List.of())),
                (source, entry) -> null));
        assertThrows(IllegalStateException.class, s.staged().pending()::commit);
    }

    @Test void duplicatePhysicalSlotAcrossLiveSourcesAlwaysRejectsCommit() {
        Setup s = setup();
        // Different source IDs cannot claim one physical item twice.
        assertThrows(IOException.class, () -> HybridLiveSourceStage.stage(s.staged(), s.plan(),
                List.of(chunk(100, "duplicate-key"), player(100, "duplicate-key")), CLASSIFIER));
        assertThrows(IllegalStateException.class, s.staged().pending()::commit);
    }
}
