package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.HybridLiveSourceStage;
import co.surumene.simplecoins.domain.SavedLiveSourcePartition;
import co.surumene.simplecoins.domain.SettlementScanRunner;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
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
        if (!consumed.compareAndSet(false, true))
            throw new IllegalStateException(
                    "Paper monetary capture was already consumed; acquire a new generation");
        Objects.requireNonNull(manifest, "saved manifest");
        Objects.requireNonNull(scope, "World inclusion");
        Objects.requireNonNull(currentlyBanned, "verified current BAN set");
        Objects.requireNonNull(blockOwners, "saved Block ownership");
        Objects.requireNonNull(entityOwners, "saved Entity ownership");
        Objects.requireNonNull(liveOwnership, "authoritative LIVE owner classification");

        List<SavedLiveSourcePartition.Chunk> loaded = new ArrayList<>();
        List<UUID> online = new ArrayList<>();
        for (HybridLiveSourceStage.Source source : observed.sources()) {
            if (source instanceof HybridLiveSourceStage.ChunkSource chunk)
                loaded.add(chunk.chunk());
            else if (source instanceof HybridLiveSourceStage.PlayerSource player)
                online.add(player.player());
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
        SettlementScanRunner.Staged staged = SettlementScanRunner.stage(
                manifest, scope, currentlyBanned, blockOwners, entityOwners,
                maximumCompressedBytes, maximumInflatedBytes, walletIds,
                selection);

        // Same physical source keys and balances; explicit ownership MUST be
        // provided. A failure never returns the staged result to the caller.
        HybridLiveSourceStage.stage(staged, selection,
                observed.sources(), liveOwnership);
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
