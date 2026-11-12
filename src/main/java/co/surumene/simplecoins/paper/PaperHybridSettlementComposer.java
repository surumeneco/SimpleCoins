package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.HybridLiveSourceStage;
import co.surumene.simplecoins.domain.SavedLiveSourcePartition;
import co.surumene.simplecoins.domain.SettlementScanRunner;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Links a previously captured Paper live monetary source set to disk NBT
 * source-selection, staged owner classification and mandatory closure. This
 * class deliberately CANNOT call Paper API, publish results or open login.
 *
 * The caller must verify the Paper main-thread snapshot is coherent with
 * subsequent on-disk reads, that every source is in scope, and ownership/BAN
 * evidence is authoritative. A stage that succeeds is NOT an admission permit.
 */
public final class PaperHybridSettlementComposer {
    private PaperHybridSettlementComposer() {}

    /**
     * This is an offline compositional stage. The walletIds set MUST be the
     * same mutable set used by scanWithSources() to record live wallet UUIDs.
     * Source discovery/owner callbacks may throw; on ANY exception the caller
     * must discard this entire generation and its shared wallet set.
     */
    public static SettlementScanRunner.Staged compose(
            PaperLiveUniverseAssets.Observed captured,
            WorldStorageDiscovery.Manifest manifest,
            WorldScope scope,
            Set<UUID> currentlyBanned,
            SettlementScanRunner.BlockOwnerLookup blockOwners,
            SettlementScanRunner.EntityOwnerLookup entityOwners,
            HybridLiveSourceStage.Classifier liveOwners,
            int maxCompressedBytes, int maxInflatedBytes,
            Set<UUID> sharedWalletIds) throws IOException {
        Objects.requireNonNull(captured, "validated Paper live monetary capture");
        Objects.requireNonNull(manifest, "disk authority");
        Objects.requireNonNull(scope, "accounting world scope");
        Objects.requireNonNull(currentlyBanned, "BAN evidence");
        Objects.requireNonNull(blockOwners, "saved BlockLocker owner evidence");
        Objects.requireNonNull(entityOwners, "saved Entity owner evidence");
        Objects.requireNonNull(liveOwners, "live ownership evidence");
        Objects.requireNonNull(sharedWalletIds, "same wallet IDs from Paper scan");

        List<SavedLiveSourcePartition.Chunk> chunks = new ArrayList<>();
        List<UUID> players = new ArrayList<>();
        for (HybridLiveSourceStage.Source source : captured.sources()) {
            if (source instanceof HybridLiveSourceStage.ChunkSource chunk) {
                if (!scope.includes(chunk.chunk().world()))
                    throw new IOException("Loaded monetary Chunk outside configured world scope: "
                            + chunk.chunk().world());
                chunks.add(chunk.chunk());
            } else if (source instanceof HybridLiveSourceStage.PlayerSource player) {
                players.add(player.player());
            } else {
                throw new IOException("Unknown live monetary source type");
            }
        }
        final SavedLiveSourcePartition partition;
        try {
            partition = new SavedLiveSourcePartition(chunks, players);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid physical live accounting source selection", invalid);
        }
        // The stage only operates on the actual captured sources supplied
        // by scanWithSources. It does not guess unscanned sources or reload
        // Chunk state after the snapshot.
        for (HybridLiveSourceStage.Source source : captured.sources()) {
            if (source instanceof HybridLiveSourceStage.ChunkSource chunk)
                partition.liveChunk(chunk.chunk());
            else if (source instanceof HybridLiveSourceStage.PlayerSource player)
                partition.livePlayer(player.player());
        }

        // Read ALL saved files with drift/integrity verification. Only saved
        // copies of already-captured live Chunk and Player sources are
        // suppressed by the same partition, never skipped without parsing.
        SettlementScanRunner.Staged staged = SettlementScanRunner.stage(
                manifest, scope, currentlyBanned, blockOwners, entityOwners,
                maxCompressedBytes, maxInflatedBytes, sharedWalletIds, partition);

        // The same pending accumulator receives every live monetary amount.
        // The owner resolver must independently establish the current BAN,
        // playerdata existence, BlockLocker and Mob owner classification.
        // A failure here makes the pending Staged result unusable.
        HybridLiveSourceStage.stage(staged, partition, captured.sources(), liveOwners);
        return staged;
    }
}
