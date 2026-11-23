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
    // Represents completed, bounded discovery with no disk files to account.
    // It is never synthesized for a manual Manifest without trusted roots.
    private static final String EMPTY_DISCOVERY_SOURCE = "saved:verified-empty-discovery";

    private SettlementScanRunner() {}

    @FunctionalInterface
    public interface BlockOwnerLookup {
        BlockEntitySettlementImport.Ownership resolve(String world, String blockPosition)
                throws IOException;
    }

    /**
     * Optional physical SAVED REGION ownership interface. Unlike the legacy
     * world/position callback, this receives the exact selected file and
     * decoded Chunk from the verified traversal, before value staging.
     * It does not imply global source/writer-epoch completeness.
     */
    @FunctionalInterface
    public interface SourceBlockOwnerLookup extends BlockOwnerLookup {
        BlockEntitySettlementImport.Ownership resolve(
                WorldStorageDiscovery.SourceFile source,
                AnvilRegionReader.Chunk chunk,
                String blockPosition) throws IOException;

        @Override
        default BlockEntitySettlementImport.Ownership resolve(
                String world, String blockPosition) throws IOException {
            throw new IOException("Source-scoped SAVED owner lookup requires actual file and Chunk");
        }
    }

    @FunctionalInterface
    public interface EntityOwnerLookup {
        Map<String, ResolvedEntitySettlementImport.Ownership> resolve(
                String world, Map<String, Object> savedChunk) throws IOException;
    }

    /**
     * Optional source-local supplement. Invoked ONLY for the authoritative
     * SAVED Entity chunk, after the regular item imports, before source
     * completion. It shares the pending transaction and physical wallet set.
     * If an exception escapes, discard the entire pending generation.
     */
    @FunctionalInterface
    public interface SavedEntitySupplement {
        void stage(WorldStorageDiscovery.SourceFile source,
                   AnvilRegionReader.Chunk chunk,
                   SettlementAccumulator pending, Set<UUID> sharedWalletIds)
                throws IOException;
    }



    /**
     * Pending accounting state only; no commit or publication is performed.
     *
     * A caller-constructed Staged is permitted for legacy domain fixtures,
     * but never inherits the producer-origin authority of the actual
     * SettlementScanRunner traversal. In particular, a copied valid
     * ContentWitness cannot authenticate an unrelated accumulator.
     */
    public static final class Staged {
        private final SettlementAccumulator pending;
        private final WorldSnapshotTraversal.Report report;
        private final Set<String> requiredSourceIds;
        private final WorldSnapshotTraversal.ContentWitness savedContentWitness;
        // Only SettlementScanRunner's successful complete source traversal may
        // set this immutable flag; no public constructor/copy method can do so.
        private final boolean issuedByVerifiedTraversal;

        /** Legacy isolated fixture; NOT eligible for source-attested publish. */
        public Staged(SettlementAccumulator pending,
                      WorldSnapshotTraversal.Report report,
                      Set<String> requiredSourceIds) {
            this(pending, report, requiredSourceIds, null, false);
        }

        /** Public saved-witness fixture; NOT an authenticated source stage. */
        public Staged(SettlementAccumulator pending,
                      WorldSnapshotTraversal.Report report,
                      Set<String> requiredSourceIds,
                      WorldSnapshotTraversal.ContentWitness savedContentWitness) {
            this(pending, report, requiredSourceIds, savedContentWitness, false);
        }

        private Staged(SettlementAccumulator pending,
                       WorldSnapshotTraversal.Report report,
                       Set<String> requiredSourceIds,
                       WorldSnapshotTraversal.ContentWitness savedContentWitness,
                       boolean issuedByVerifiedTraversal) {
            this.pending = Objects.requireNonNull(pending, "pending settlement");
            this.report = Objects.requireNonNull(report, "source traversal report");
            this.requiredSourceIds = Set.copyOf(requiredSourceIds);
            this.savedContentWitness = savedContentWitness;
            this.issuedByVerifiedTraversal = issuedByVerifiedTraversal;
        }

        /** Package-private origin check for the internal protected publisher. */
        boolean issuedByVerifiedTraversal() {
            return issuedByVerifiedTraversal;
        }

        public SettlementAccumulator pending() { return pending; }
        public WorldSnapshotTraversal.Report report() { return report; }
        public Set<String> requiredSourceIds() { return requiredSourceIds; }
        public WorldSnapshotTraversal.ContentWitness savedContentWitness() {
            return savedContentWitness;
        }

        /** Preserve the four original record components' equality semantics. */
        @Override public boolean equals(Object other) {
            if (!(other instanceof Staged that)) return false;
            return pending.equals(that.pending)
                    && report.equals(that.report)
                    && requiredSourceIds.equals(that.requiredSourceIds)
                    && Objects.equals(savedContentWitness, that.savedContentWitness);
        }

        @Override public int hashCode() {
            return Objects.hash(pending, report, requiredSourceIds, savedContentWitness);
        }

        @Override public String toString() {
            return "Staged[pending=" + pending + ", report=" + report
                    + ", requiredSourceIds=" + requiredSourceIds
                    + ", savedContentWitness=" + savedContentWitness + "]";
        }
    }

    private static String sourceId(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    /**
     * Source-aware saved NBT staging. Resolve BAN and present playerdata from
     * the SAME captured evidence generation, never from booleans supplied by
     * the BlockLocker/Entity owner callbacks. Neither callback is itself
     * evidence of correct owner identification or external plugin coverage.
     *
     * An unobserved owner UUID aborts the entire candidate (not treated as
     * a non-banned player). This does not authorize publication or login.
     */
    public static Staged stageWithBanCoverage(
            WorldStorageDiscovery.Manifest manifest,
            WorldScope scope,
            BanSourceCoverage banSources,
            BlockOwnerLookup blockOwners,
            EntityOwnerLookup entityOwners,
            int maximumCompressedBytes,
            int maximumInflatedBytes) throws IOException {
        return stageWithBanCoverage(manifest, scope, banSources, blockOwners,
                entityOwners, maximumCompressedBytes, maximumInflatedBytes,
                new java.util.HashSet<>(), null);
    }

    /** Same verified BAN generation across selected LIVE sources and saved NBT. */
    public static Staged stageWithBanCoverage(
            WorldStorageDiscovery.Manifest manifest,
            WorldScope scope,
            BanSourceCoverage banSources,
            BlockOwnerLookup blockOwners,
            EntityOwnerLookup entityOwners,
            int maximumCompressedBytes,
            int maximumInflatedBytes,
            Set<UUID> sharedWalletIds,
            SavedLiveSourcePartition partition) throws IOException {
        return stageWithBanCoverage(manifest, scope, banSources, blockOwners,
                entityOwners, maximumCompressedBytes, maximumInflatedBytes,
                sharedWalletIds, partition, null);
    }

    /** Same BAN generation with optional source-local SAVED Entity supplement. */
    public static Staged stageWithBanCoverage(
            WorldStorageDiscovery.Manifest manifest,
            WorldScope scope,
            BanSourceCoverage banSources,
            BlockOwnerLookup blockOwners,
            EntityOwnerLookup entityOwners,
            int maximumCompressedBytes,
            int maximumInflatedBytes,
            Set<UUID> sharedWalletIds,
            SavedLiveSourcePartition partition,
            SavedEntitySupplement savedSupplement) throws IOException {
        Objects.requireNonNull(manifest, "saved manifest");
        Objects.requireNonNull(banSources, "complete BAN source observations");
        Objects.requireNonNull(blockOwners, "external block owner resolver");
        // The saved BlockLocker adapter has a legacy coordinate-only method
        // for local diagnostics. It is NOT an authenticated SAVED source
        // resolver; settlement must use its explicit sourceScoped() adapter.
        if (blockOwners instanceof SavedBlockLockerSettlementOwnerLookup)
            throw new IOException("SAVED BlockLocker settlement needs sourceScoped() and selected Chunk");
        Objects.requireNonNull(entityOwners, "external entity owner resolver");
        // All discovered saved players must have complete BAN observations
        // before any world data is read or any asset staged.
        Set<UUID> banned = banSources.bannedAmong(manifest.playerDataFiles().keySet());
        Set<UUID> existingPlayers = manifest.playerDataFiles().keySet();
        // Preserve the original source-scoped capability through BAN validation.
        // Wrapping it as an unqualified lambda would drop the actual saved
        // Chunk and incorrectly invoke the fail-stop legacy overload.
        BlockOwnerLookup validatedBlocks = blockOwners instanceof SourceBlockOwnerLookup scoped
                ? (SourceBlockOwnerLookup) (source, chunk, position) ->
                        validatedBlockOwner(scoped.resolve(source, chunk, position),
                                banSources, existingPlayers)
                : (world, position) -> validatedBlockOwner(
                        blockOwners.resolve(world, position), banSources, existingPlayers);
        EntityOwnerLookup validatedEntities = (world, chunk) -> {
            var owners = entityOwners.resolve(world, chunk);
            if (owners == null)
                throw new IOException("Missing Entity ownership evidence");
            Map<String, ResolvedEntitySettlementImport.Ownership> validated =
                    new java.util.HashMap<>();
            for (var entry : owners.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null)
                    throw new IOException("Invalid Entity ownership decision");
                UUID owner = entry.getValue().owner();
                var updated = owner == null
                        ? new ResolvedEntitySettlementImport.Ownership(null, false, false)
                        : new ResolvedEntitySettlementImport.Ownership(
                                owner, banSources.currentlyBanned(owner),
                                existingPlayers.contains(owner));
                validated.put(entry.getKey(), updated);
            }
            return Map.copyOf(validated);
        };
        return stage(manifest, scope, banned, validatedBlocks, validatedEntities,
                maximumCompressedBytes, maximumInflatedBytes, sharedWalletIds, partition,
                savedSupplement);
    }

    private static BlockEntitySettlementImport.Ownership validatedBlockOwner(
            BlockEntitySettlementImport.Ownership owner,
            BanSourceCoverage banSources, Set<UUID> existingPlayers) throws IOException {
        if (owner == null) return null;
        if (owner.owner() == null || owner.everyoneProtected())
            return new BlockEntitySettlementImport.Ownership(
                    owner.owner(), owner.everyoneProtected(), false, false);
        return new BlockEntitySettlementImport.Ownership(
                owner.owner(), false, banSources.currentlyBanned(owner.owner()),
                existingPlayers.contains(owner.owner()));
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
        return stage(manifest, scope, currentlyBanned, blockOwners, entityOwners,
                maximumCompressedBytes, maximumInflatedBytes, sharedWalletIds,
                partition, null);
    }

    /**
     * Source-local SAVED supplements use exactly the same Anvil chunk decode,
     * source ID, wallet generation and partition routing as regular imports.
     * A supplement never runs on shadowed LIVE Entity chunks.
     */
    public static Staged stage(WorldStorageDiscovery.Manifest manifest,
                               WorldScope scope,
                               Set<UUID> currentlyBanned,
                               BlockOwnerLookup blockOwners,
                               EntityOwnerLookup entityOwners,
                               int maximumCompressedBytes,
                               int maximumInflatedBytes,
                               Set<UUID> sharedWalletIds,
                               SavedLiveSourcePartition partition,
                               SavedEntitySupplement savedSupplement) throws IOException {
        Objects.requireNonNull(sharedWalletIds, "shared physical wallet identity generation");
        Objects.requireNonNull(manifest);
        Objects.requireNonNull(scope);
        // A discovered Manifest's file set was selected using that exact
        // world scope. Reinterpreting it under a different filter could
        // silently omit authoritative regions that discovery excluded.
        if (manifest.origin() != null && !manifest.origin().scope().equals(scope))
            throw new IOException("Settlement WorldScope differs from saved discovery");
        Objects.requireNonNull(currentlyBanned);
        Objects.requireNonNull(blockOwners);
        // The saved BlockLocker adapter has a legacy coordinate-only method
        // for local diagnostics. It is NOT an authenticated SAVED source
        // resolver; settlement must use its explicit sourceScoped() adapter.
        if (blockOwners instanceof SavedBlockLockerSettlementOwnerLookup)
            throw new IOException("SAVED BlockLocker settlement needs sourceScoped() and selected Chunk");
        Objects.requireNonNull(entityOwners);
        Set<UUID> banned = Set.copyOf(currentlyBanned);
        Set<String> savedSources = manifest.requiredSourceIds();
        boolean verifiedEmptyDiscovery =
                savedSources.isEmpty() && manifest.origin() != null;
        if (verifiedEmptyDiscovery)
            savedSources = Set.of(EMPTY_DISCOVERY_SOURCE);
        Set<String> required = savedSources;
        if (partition != null) {
            // A hybrid plan MUST NOT make the on-disk copy disappear and
            // then allow a zero-valued commit without the live replacement.
            // Every declared loaded Chunk and online Player is an additional
            // mandatory accounting source, even if it currently holds 0C.
            Set<String> allSources = new java.util.HashSet<>(savedSources);
            Set<String> liveSources = partition.requiredLiveSourceIds();
            if (!allSources.addAll(liveSources) && !liveSources.isEmpty())
                throw new IOException("Overlapping saved/live accounting source IDs");
            required = Set.copyOf(allSources);
        }
        var pending = new SettlementAccumulator(scope, required);
        // One authoritative playerdata generation must not count a copied
        // wallet UUID as separate currency for two different players.
        Set<UUID> seenSavedWallets = sharedWalletIds;

        WorldSnapshotTraversal.Verified verified = WorldSnapshotTraversal.traverseVerified(
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
                                    position -> blockOwners instanceof SourceBlockOwnerLookup scoped
                                            ? scoped.resolve(source, chunk, position)
                                            : blockOwners.resolve(source.world(), position),
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
                            if (savedSupplement != null)
                                savedSupplement.stage(source, chunk, pending, seenSavedWallets);
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

        // A verified empty discovery is a real zero-input generation. Close
        // its synthetic source only after the full bounded manifest recheck.
        if (verifiedEmptyDiscovery)
            pending.completeSource(EMPTY_DISCOVERY_SOURCE);
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
        return new Staged(pending, verified.report(), required, verified.witness(), true);
    }
}
