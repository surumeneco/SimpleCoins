package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.BanSourceCoverage;
import co.surumene.simplecoins.domain.HybridLiveSourceStage;
import co.surumene.simplecoins.domain.SettlementAccumulator;
import co.surumene.simplecoins.domain.SavedLiveSourcePartition;
import co.surumene.simplecoins.domain.SettlementScanRunner;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import co.surumene.simplecoins.domain.WclHybridLiveOverlay;
import co.surumene.simplecoins.domain.WclOwnerEvidence;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * A deliberately ONE-SHOT bridge from the actual Paper-main-thread live scan
 * to saved playerdata/Anvil and live monetary source staging.
 *
 * This class owns the shared physical-wallet UUID identity generation and
 * constructs all loaded-Chunk/online-Player selections from source data
 * already physically observed. It does not invent missing holders or infer
 * monetary ownership. This is STILL ONLY AN UNPUBLISHED CANDIDATE:
 * Paper-world quiescence, BlockLocker/BAN/WWC/WWW coverage, journal replay,
 * atomic publication and player admission must be handled separately.
 */
public final class PaperCapturedHybridSettlement {
    private final PaperLiveUniverseAssets.Observed observed;
    private final Set<UUID> walletIds;
    private final AtomicBoolean consumed = new AtomicBoolean();
    /** Explicit main-thread LIVE WCL reads from this capture's wallet generation. */
    private final List<WclHybridLiveOverlay.Selected> selectedWcl = new ArrayList<>();


    private PaperCapturedHybridSettlement(PaperLiveUniverseAssets.Observed observed,
                                         Set<UUID> walletIds) {
        this.observed = Objects.requireNonNull(observed, "validated Paper source report");
        this.walletIds = new HashSet<>(Objects.requireNonNull(walletIds, "wallet UUIDs"));
    }

    /**
     * Must be invoked on Paper's primary thread. Source discovery is strict;
     * no Chunk is generated or loaded and all wallets share ONE UUID context.
     */
    public static PaperCapturedHybridSettlement capture(
            Collection<World> selectedWorlds,
            PaperLiveUniverseAssets.Budget budget) throws IOException {
        Set<UUID> liveWallets = new HashSet<>();
        var snapshot = PaperLiveUniverseAssets.scanWithSources(
                selectedWorlds, budget, liveWallets);
        return new PaperCapturedHybridSettlement(snapshot, liveWallets);
    }

    /** Test seam for genuine immutable `Observed` contracts without a mock server. */
    static PaperCapturedHybridSettlement fromObserved(
            PaperLiveUniverseAssets.Observed verifiedObserved,
            Set<UUID> liveWalletIds) {
        return new PaperCapturedHybridSettlement(verifiedObserved, liveWalletIds);
    }

    /**
     * Attach one actually selected loaded WCL Entity to THIS monetary
     * generation, before stageCandidate. The shared wallet UUID context is
     * mandatory: a second private WCL-only set could count the same wallet
     * twice. Neither this API nor an empty selection proves that every WCL
     * Entity was found, or that other writers are quiescent.
     */
    public synchronized void includeLoadedWcl(
            Plugin consumer, Entity entity, String providerId,
            int maxSerializedBytes, int maxInflatedBytes) throws IOException {
        if (consumed.get())
            throw new IllegalStateException("WCL selection after capture consumption");
        try {
            Objects.requireNonNull(entity, "selected LIVE WCL Entity");
            if (!Bukkit.isPrimaryThread())
                throw new IllegalStateException("LIVE WCL selection requires main thread");
            UUID id = entity.getUniqueId();
            if (!entity.isValid() || Bukkit.getEntity(id) != entity)
                throw new IOException("Selected WCL Entity is not authoritative");
            World world = entity.getWorld();
            var location = entity.getLocation();
            var chunk = new SavedLiveSourcePartition.Chunk(world.getName(),
                    Math.floorDiv(location.getBlockX(), 16),
                    Math.floorDiv(location.getBlockZ(), 16));
            boolean selected = observed.sources().stream().anyMatch(source ->
                    source instanceof HybridLiveSourceStage.ChunkSource captured
                            && captured.chunk().equals(chunk));
            if (!selected)
                throw new IOException("Selected WCL Entity has no captured LIVE Chunk");
            if (selectedWcl.stream().anyMatch(wcl -> wcl.entityId().equals(id)))
                throw new IOException("Repeated WCL Entity in one captured generation: " + id);
            var candidate = PaperWonderfulLiveCandidates.collect(
                    consumer, entity, providerId, walletIds,
                    maxSerializedBytes, maxInflatedBytes);
            // The provider can call external code. Recheck current paper
            // identity and Chunk membership after the read, not just before.
            var after = entity.getLocation();
            var actual = new SavedLiveSourcePartition.Chunk(entity.getWorld().getName(),
                    Math.floorDiv(after.getBlockX(), 16),
                    Math.floorDiv(after.getBlockZ(), 16));
            if (!entity.isValid() || Bukkit.getEntity(id) != entity
                    || !actual.equals(chunk)
                    || !candidate.worldId().equals(world.getUID())
                    || !candidate.worldName().equals(chunk.world())
                    || !candidate.entityId().equals(id))
                throw new IOException("Selected WCL Entity moved or changed during read");
            selectedWcl.add(new WclHybridLiveOverlay.Selected(chunk, candidate.entityId(),
                    candidate.providerId(), candidate.ownerEvidence(), candidate.slotValues()));
        } catch (IOException | RuntimeException invalid) {
            consumed.set(true); // discard the whole potentially mutated wallet generation
            throw invalid;
        }
    }

    /** Verified source fixture only: never independent proof of Paper LIVE authority. */
    static PaperCapturedHybridSettlement fromObservedWithWcl(
            PaperLiveUniverseAssets.Observed verifiedObserved, Set<UUID> liveWalletIds,
            List<WclHybridLiveOverlay.Selected> selections) {
        var capture = fromObserved(verifiedObserved, liveWalletIds);
        capture.selectedWcl.addAll(List.copyOf(selections));
        return capture;
    }

    public PaperLiveUniverseAssets.Observed observed() {
        return observed;
    }

    /**
     * Consumes this capture exactly once, including on failure. Source
     * callbacks must supply real owner, BAN, BlockLocker, WWC/WWW and playerdata
     * evidence. Disk reads are bounded and content-stamped; the live sources
     * were captured previously and are never reread here.
     *
     * The returned Staged object is NOT a verified publishable settlement,
     * even if its accumulator can commit locally. Callers must independently
     * verify no source generation changed and complete ALL other product gates.
     */
    public SettlementScanRunner.Staged stageCandidate(
            WorldStorageDiscovery.Manifest manifest,
            WorldScope scope,
            Set<UUID> currentlyBanned,
            SettlementScanRunner.BlockOwnerLookup blockOwners,
            SettlementScanRunner.EntityOwnerLookup entityOwners,
            int maximumCompressedBytes,
            int maximumInflatedBytes,
            HybridLiveSourceStage.Classifier liveOwnership) throws IOException {
        return stageCandidateInternal(manifest, scope, currentlyBanned, null,
                blockOwners, entityOwners, maximumCompressedBytes,
                maximumInflatedBytes, liveOwnership);
    }

    /** Use one complete, immutable provider-reported BAN generation for both
     * saved and LIVE holders; this does not collect provider observations. */
    public SettlementScanRunner.Staged stageCandidateWithBanCoverage(
            WorldStorageDiscovery.Manifest manifest,
            WorldScope scope,
            BanSourceCoverage banSources,
            SettlementScanRunner.BlockOwnerLookup blockOwners,
            SettlementScanRunner.EntityOwnerLookup entityOwners,
            int maximumCompressedBytes,
            int maximumInflatedBytes,
            HybridLiveSourceStage.Classifier liveOwnership) throws IOException {
        Objects.requireNonNull(banSources, "BAN source coverage");
        return stageCandidateInternal(manifest, scope, null, banSources,
                blockOwners, entityOwners, maximumCompressedBytes,
                maximumInflatedBytes, liveOwnership);
    }

    private SettlementScanRunner.Staged stageCandidateInternal(
            WorldStorageDiscovery.Manifest manifest,
            WorldScope scope,
            Set<UUID> currentlyBanned,
            BanSourceCoverage banSources,
            SettlementScanRunner.BlockOwnerLookup blockOwners,
            SettlementScanRunner.EntityOwnerLookup entityOwners,
            int maximumCompressedBytes,
            int maximumInflatedBytes,
            HybridLiveSourceStage.Classifier liveOwnership) throws IOException {
        List<WclHybridLiveOverlay.Selected> wclCandidates;
        synchronized (this) {
            if (!consumed.compareAndSet(false, true))
                throw new IllegalStateException(
                        "Paper monetary capture was already consumed; acquire a new generation");
            wclCandidates = List.copyOf(selectedWcl);
        }
        // Add independently stored WCL slots into their already mandatory
        // selected LIVE Chunk. Alias collisions remain HOLD; never double or
        // silently discard mirrored vanilla equipment/display value.
        WclHybridLiveOverlay.Overlay overlay =
                WclHybridLiveOverlay.attach(observed.sources(), wclCandidates);
        Objects.requireNonNull(manifest, "saved manifest");
        Objects.requireNonNull(scope, "World inclusion");
        if (banSources == null)
            Objects.requireNonNull(currentlyBanned, "verified current BAN set");
        Objects.requireNonNull(blockOwners, "saved Block ownership");
        Objects.requireNonNull(entityOwners, "saved Entity ownership");
        Objects.requireNonNull(liveOwnership, "authoritative LIVE owner classification");

        List<SavedLiveSourcePartition.Chunk> loaded = new ArrayList<>();
        List<UUID> online = new ArrayList<>();
        for (HybridLiveSourceStage.Source source : observed.sources()) {
            if (source instanceof HybridLiveSourceStage.ChunkSource chunk)
                loaded.add(chunk.chunk());
            else if (source instanceof HybridLiveSourceStage.PlayerSource player) {
                // Mandatory even when a live Player holds 0C.
                if (banSources != null) banSources.currentlyBanned(player.player());
                online.add(player.player());
            }
            else
                throw new IOException("Unsupported captured Paper LIVE source");
        }

        // The selection is not an administrator-supplied claim: it is derived
        // from the exact observed source list validated against the same
        // physical Paper inventory scan. All required LIVE holders are included
        // even if their currency total was zero.
        SavedLiveSourcePartition selection;
        try {
            selection = new SavedLiveSourcePartition(loaded, online);
            for (var chunk : loaded) selection.liveChunk(chunk);
            for (var player : online) selection.livePlayer(player);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Duplicate/invalid observed LIVE source selection", invalid);
        }

        // This traverses and validates ALL saved files but excludes disk
        // currency shadowed by those previously observed live sources.
        SettlementScanRunner.Staged staged = banSources == null
                ? SettlementScanRunner.stage(
                        manifest, scope, currentlyBanned, blockOwners, entityOwners,
                        maximumCompressedBytes, maximumInflatedBytes, walletIds,
                        selection)
                : SettlementScanRunner.stageWithBanCoverage(
                        manifest, scope, banSources, blockOwners, entityOwners,
                        maximumCompressedBytes, maximumInflatedBytes, walletIds,
                        selection);

        // Same physical source keys and balances; explicit ownership MUST be
        // provided. A failure never returns the staged result to the caller.
        HybridLiveSourceStage.Classifier verifiedLive = (source, value) -> {
            WclOwnerEvidence.Decision wcl = overlay.wclOwners().get(value.physicalKey());
            if (wcl != null) {
                // WCL OWNER and FREE are local verdicts. Resolve current BAN
                // and real playerdata against the SAME saved selection.
                UUID owner = wcl.owner();
                boolean banned = owner != null && (banSources != null
                        ? banSources.currentlyBanned(owner)
                        : currentlyBanned.contains(owner));
                return new SettlementAccumulator.Asset(
                        source.id(), value.physicalKey(), value.world(), owner,
                        false, banned,
                        owner != null && manifest.playerDataFiles().containsKey(owner),
                        value.amount(), value.physicalKey());
            }
            SettlementAccumulator.Asset raw = liveOwnership.classify(source, value);
            if (raw == null || banSources == null) return raw;
            UUID owner = raw.owner();
            boolean personalOwner = owner != null && !raw.everyoneProtected();
            return new SettlementAccumulator.Asset(
                    raw.source(), raw.physicalKey(), raw.world(), owner,
                    raw.everyoneProtected(),
                    personalOwner && banSources.currentlyBanned(owner),
                    personalOwner && manifest.playerDataFiles().containsKey(owner),
                    raw.value(), raw.location());
        };
        HybridLiveSourceStage.stage(staged, selection,
                overlay.sources(), verifiedLive);
        // LIVE owner callbacks execute after the saved files were parsed.
        // Verify both the directory set and the SAME pre-parse file fingerprints
        // again: same-size / same-mtime byte rewrites must invalidate this
        // candidate instead of returning stale monetary amounts.
        // This is a boundary detector, not an atomic memory/disk snapshot.
        if (staged.savedContentWitness() == null)
            throw new IOException("Saved monetary source fingerprint evidence unavailable");
        staged.savedContentWitness().requireUnchanged();
        return staged;
    }
}
