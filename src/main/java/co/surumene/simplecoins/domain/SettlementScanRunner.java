package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only, disk-only settlement staging. This does NOT establish a consistent
 * live-world snapshot or authorize publication. Loaded entities, plugin-specific
 * ownership, BAN state and all other sources must be reconciled separately.
 */
public final class SettlementScanRunner {
    private SettlementScanRunner() {}

    @FunctionalInterface
    public interface BlockOwnerLookup {
        BlockEntitySettlementImport.Ownership resolve(String world, String blockPosition)
                throws IOException;
    }

    @FunctionalInterface
    public interface EntityOwnerLookup {
        Map<String, ResolvedEntitySettlementImport.Ownership> resolve(
                String world, Map<String, Object> savedChunk) throws IOException;
    }

    /** Pending accounting state only; no commit or publication is performed. */
    public record Staged(SettlementAccumulator pending,
                         WorldSnapshotTraversal.Report report,
                         Set<String> requiredSourceIds) {
        public Staged {
            Objects.requireNonNull(pending);
            Objects.requireNonNull(report);
            requiredSourceIds = Set.copyOf(requiredSourceIds);
        }
    }

    private static String sourceId(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    public static Staged stage(WorldStorageDiscovery.Manifest manifest,
                               WorldScope scope,
                               Set<UUID> currentlyBanned,
                               BlockOwnerLookup blockOwners,
                               EntityOwnerLookup entityOwners,
                               int maximumCompressedBytes,
                               int maximumInflatedBytes) throws IOException {
        return stage(manifest, scope, currentlyBanned, blockOwners, entityOwners,
                maximumCompressedBytes, maximumInflatedBytes, new java.util.HashSet<>());
    }

    /**
     * Optional externally-owned identity context for a validated, single
     * saved/live settlement generation. The same mutable Set may be passed to
     * PaperLiveUniverseAssets.scan(..., walletIds); a duplicate physical wallet
     * UUID must reject either read order. On any failure ALL staged values and
     * the shared Set must be discarded. This method is still DISK-ONLY and
     * cannot publish an authoritative world settlement.
     */
    public static Staged stage(WorldStorageDiscovery.Manifest manifest,
                               WorldScope scope,
                               Set<UUID> currentlyBanned,
                               BlockOwnerLookup blockOwners,
                               EntityOwnerLookup entityOwners,
                               int maximumCompressedBytes,
                               int maximumInflatedBytes,
                               Set<UUID> sharedWalletIds) throws IOException {
        return stage(manifest, scope, currentlyBanned, blockOwners, entityOwners,
                maximumCompressedBytes, maximumInflatedBytes, sharedWalletIds, null);
    }

    /**
     * Hybrid routing of the saved file visits. Every saved source is still read
     * and checked for content/source drift; monetary callbacks for holders
     * shadowed by declared loaded Chunk and online Player sources are skipped.
     * The external caller must capture/verify the selected live monetary
     * sources in the same candidate generation before acknowledging them in
     * {@link SavedLiveSourcePartition}; this method DOES NOT stage or prove
     * those live values. Its return cannot authorize admission/publication.
     */
    public static Staged stage(WorldStorageDiscovery.Manifest manifest,
                               WorldScope scope,
                               Set<UUID> currentlyBanned,
                               BlockOwnerLookup blockOwners,
                               EntityOwnerLookup entityOwners,
                               int maximumCompressedBytes,
                               int maximumInflatedBytes,
                               Set<UUID> sharedWalletIds,
                               SavedLiveSourcePartition partition) throws IOException {
        Objects.requireNonNull(sharedWalletIds, "shared physical wallet identity generation");
        Objects.requireNonNull(manifest);
        Objects.requireNonNull(scope);
        Objects.requireNonNull(currentlyBanned);
        Objects.requireNonNull(blockOwners);
        Objects.requireNonNull(entityOwners);
        Set<UUID> banned = Set.copyOf(currentlyBanned);
        Set<String> required = manifest.requiredSourceIds();
        var pending = new SettlementAccumulator(scope, required);
        // One authoritative playerdata generation must not count a copied
        // wallet UUID as separate currency for two different players.
        Set<UUID> seenSavedWallets = sharedWalletIds;

        WorldSnapshotTraversal.Report report = WorldSnapshotTraversal.traverse(
                manifest, maximumCompressedBytes, maximumInflatedBytes,
                new WorldSnapshotTraversal.Visitor() {
                    @Override
                    public void chunk(WorldStorageDiscovery.SourceFile source,
                                      AnvilRegionReader.Chunk chunk) throws IOException {
                        // The discovery layer normally excludes disabled worlds.
                        // If a manually constructed manifest includes one, read
                        // it but do not attribute assets from that world.
                        if (!scope.includes(source.world())) return;
                        String id = sourceId(source.path());
                        if (source.kind() == WorldStorageDiscovery.Kind.REGION) {
                            BlockEntitySettlementImport.stageChunk(
                                    pending, id, source.world(), chunk.nbt(),
                                    position -> blockOwners.resolve(source.world(), position),
                                    seenSavedWallets);
                        } else {
                            FreeEntitySettlementImport.stageChunk(
                                    pending, id, source.world(), chunk.nbt(), seenSavedWallets);
                            Map<String, ResolvedEntitySettlementImport.Ownership> owners =
                                    entityOwners.resolve(source.world(), chunk.nbt());
                            if (owners == null)
                                throw new IOException("Saved Entity ownership resolver returned null");
                            ResolvedEntitySettlementImport.stageChunk(
                                    pending, id, source.world(), chunk.nbt(), owners,
                                    seenSavedWallets);
                        }
                    }

                    @Override
                    public void player(UUID player, Map<String, Object> data) throws IOException {
                        Path path = manifest.playerDataFiles().get(player);
                        if (path == null)
                            throw new IOException("Playerdata missing from manifest: " + player);
                        PlayerDataSettlementImport.stage(pending, sourceId(path), player,
                                banned.contains(player), data, seenSavedWallets);
                    }
                }, partition);

        // A world source may contain multiple chunks. Completion is valid only
        // after traversal of every file and playerdata has returned successfully.
        for (WorldStorageDiscovery.SourceFile source : manifest.worldFiles())
            pending.completeSource(sourceId(source.path()));
        // PlayerDataSettlementImport normally closes the playerdata source.
        // Online players' disk copies were deliberately not staged, so they
        // must instead be closed only after WorldSnapshotTraversal has read
        // and SHA-verified EVERY saved input and verified live-plan coverage.
        // Without this, a valid hybrid run would have an incomplete required
        // source even though no disk balance should be included for it.
        if (partition != null) {
            for (var entry : manifest.playerDataFiles().entrySet()) {
                if (partition.shadowsSavedPlayer(entry.getKey()))
                    pending.completeSource(sourceId(entry.getValue()));
            }
        }
        return new Staged(pending, report, required);
    }
}
