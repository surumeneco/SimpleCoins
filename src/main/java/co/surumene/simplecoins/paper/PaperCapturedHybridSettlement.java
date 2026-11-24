package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.BanSourceCoverage;
import co.surumene.simplecoins.domain.HybridLiveSourceStage;
import co.surumene.simplecoins.domain.SettlementAccumulator;
import co.surumene.simplecoins.domain.SavedLiveSourcePartition;
import co.surumene.simplecoins.domain.SavedBlockLockerSettlementOwnerLookup;
import co.surumene.simplecoins.domain.SavedBlockLockerTerrainObservations;
import co.surumene.simplecoins.domain.SavedTerrainBlockPalette;
import co.surumene.simplecoins.domain.SettlementScanRunner;
import co.surumene.simplecoins.domain.SettlementReadDispatch;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import co.surumene.simplecoins.domain.WclHybridLiveOverlay;
import co.surumene.simplecoins.domain.WclOwnerEvidence;
import co.surumene.simplecoins.domain.WclLiveEquipmentMirrorProof;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Chicken;
import org.bukkit.inventory.ItemStack;
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
    private final Map<String, World> capturedWorlds;
    /** Bytes from EXACT same initial scan as original positive equipment money. */
    private final Map<String, byte[]> originalLiveEquipment;
    private final PaperLiveUniverseAssets.Budget captureBudget;
    private final AtomicBoolean consumed = new AtomicBoolean();
    private boolean autoEnumerationCompleted;
    /** Explicit main-thread LIVE WCL reads from this capture's wallet generation. */
    private final List<WclHybridLiveOverlay.Selected> selectedWcl = new ArrayList<>();
    /** Immutable first one-read WCL provenance for actual Paper Entity rereads. */
    private final List<PaperWonderfulLiveCandidates.Candidate> selectedWclOriginals =
            new ArrayList<>();
    private Plugin selectedWclReader;


    private PaperCapturedHybridSettlement(PaperLiveUniverseAssets.Observed observed,
                                         Set<UUID> walletIds,
                                         Map<String, World> capturedWorlds,
                                         PaperLiveUniverseAssets.Budget captureBudget,
                                         Map<String, byte[]> scannedEquipment) {
        this.observed = Objects.requireNonNull(observed, "validated Paper source report");
        this.walletIds = new HashSet<>(Objects.requireNonNull(walletIds, "wallet UUIDs"));
        this.capturedWorlds = Map.copyOf(Objects.requireNonNull(capturedWorlds));
        Map<String, byte[]> evidence = new java.util.HashMap<>();
        Objects.requireNonNull(scannedEquipment, "same-source equipment bytes")
                .forEach((key, bytes) -> evidence.put(key, bytes.clone()));
        this.originalLiveEquipment = Map.copyOf(evidence);
        this.captureBudget = captureBudget; // null only for the test-only fromObserved seam
    }

    /**
     * Must be invoked on Paper's primary thread. Source discovery is strict;
     * no Chunk is generated or loaded and all wallets share ONE UUID context.
     */
    public static PaperCapturedHybridSettlement capture(
            Collection<World> selectedWorlds,
            PaperLiveUniverseAssets.Budget budget) throws IOException {
        List<World> selection = List.copyOf(Objects.requireNonNull(selectedWorlds));
        Set<UUID> liveWallets = new HashSet<>();
        Map<String, byte[]> nativeEquipment = new java.util.HashMap<>();
        var snapshot = PaperLiveUniverseAssets.scanWithSources(
                selection, budget, liveWallets, nativeEquipment);
        Map<String, World> selected = new java.util.LinkedHashMap<>();
        for (World world : selection) {
            if (selected.putIfAbsent(world.getName(), world) != null)
                throw new IOException("Duplicate Paper World identity during LIVE capture");
        }
        return new PaperCapturedHybridSettlement(
                snapshot, liveWallets, selected, budget, nativeEquipment);
    }

    /** Test seam for genuine immutable `Observed` contracts without a mock server. */
    static PaperCapturedHybridSettlement fromObserved(
            PaperLiveUniverseAssets.Observed verifiedObserved,
            Set<UUID> liveWalletIds) {
        return new PaperCapturedHybridSettlement(
                verifiedObserved, liveWalletIds, Map.of(), null, Map.of());
    }

    /**
     * Automatically enumerate all PDC-marked WWC/WWW Entities within the
     * exact currently loaded Chunk set captured from these Paper World
     * instances, and route them through the existing one-read wallet-shared
     * candidate collector. This is only a bounded LIVE read; no writer epoch,
     * all-world completeness or settlement publication is implied.
     */
    public synchronized PaperWonderfulLiveEntityDiscovery.Report includeDiscoveredLoadedWcl(
            Plugin consumer) throws IOException {
        if (consumed.get())
            throw new IllegalStateException("WCL enumeration after capture consumption");
        try {
            Objects.requireNonNull(consumer, "WCL consumer");
            if (captureBudget == null)
                throw new IOException("WCL discovery requires an actual Paper World capture");
            if (autoEnumerationCompleted || !selectedWcl.isEmpty())
                throw new IOException("WCL automatic and explicit selection cannot be mixed");
            var first = PaperWonderfulLiveEntityDiscovery.discover(
                    capturedWorlds, observed.sources(),
                    captureBudget.maxChunks(), captureBudget.maxEntities());
            for (var holder : first.holders()) {
                includeLoadedWcl(consumer, holder.entity(), holder.providerId(),
                        captureBudget.maxSerializedBytes(), captureBudget.maxInflatedBytes());
            }
            // A provider call can change loaded entity markers or move holders.
            // Rescan identities and ALL entity UUIDs, not just the WCL count.
            var second = PaperWonderfulLiveEntityDiscovery.discover(
                    capturedWorlds, observed.sources(),
                    captureBudget.maxChunks(), captureBudget.maxEntities());
            if (first.scannedChunks() != second.scannedChunks()
                    || first.scannedEntities() != second.scannedEntities()
                    || !first.observedEntityIds().equals(second.observedEntityIds())
                    || !first.identities().equals(second.identities()))
                throw new IOException("LIVE WCL Entity population changed during provider reads");
            Map<UUID, Entity> selectedObjects = new java.util.HashMap<>();
            for (var holder : first.holders())
                selectedObjects.put(holder.entity().getUniqueId(), holder.entity());
            for (var holder : second.holders())
                if (selectedObjects.get(holder.entity().getUniqueId()) != holder.entity())
                    throw new IOException("LIVE WCL Entity object changed during provider reads");
            autoEnumerationCompleted = true;
            return first;
        } catch (IOException | RuntimeException failed) {
            consumed.set(true); // discard every partial candidate and shared wallet UUID
            throw failed;
        }
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
        if (autoEnumerationCompleted) {
            consumed.set(true);
            throw new IllegalStateException("Cannot add explicit WCL after full LIVE enumeration");
        }
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
            if (selectedWclReader != null && selectedWclReader != consumer)
                throw new IOException("Different WCL consumer in one LIVE capture");
            var candidate = PaperWonderfulLiveCandidates.collectForHybrid(
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
            Set<String> sameItemMirrors = new HashSet<>();
            for (var slot : candidate.slotValues().entrySet()) {
                if (slot.getValue().signum() <= 0)
                    continue;
                String nativeSlot;
                if ("www".equals(providerId) && "weapon".equals(slot.getKey())) {
                    if (!(entity instanceof Wolf))
                        throw new IOException("WWW mainhand mirror requires verified Wolf");
                    nativeSlot = "mainhand";
                } else if ("wwc".equals(providerId) && "head".equals(slot.getKey())) {
                    if (!(entity instanceof Chicken))
                        throw new IOException("WWC head mirror requires verified Chicken");
                    nativeSlot = "head";
                } else continue;
                String physical = "world/" + chunk.world() + "/entity/" + id
                        + "/equipment/" + nativeSlot;
                byte[] originally = originalLiveEquipment.get(physical);
                var stored = candidate.sourceItems().get(slot.getKey());
                if (originally == null || stored == null
                        || !(entity instanceof LivingEntity living)
                        || living.getEquipment() == null)
                    throw new IOException("WCL mirrored gear lacks original captured native source");
                ItemStack current = "mainhand".equals(nativeSlot)
                        ? living.getEquipment().getItemInMainHand()
                        : living.getEquipment().getHelmet();
                if (current == null || current.getType().isAir())
                    throw new IOException("WCL mirrored gear disappeared since LIVE scan");
                final byte[] now;
                try {
                    now = current.serializeAsBytes();
                } catch (RuntimeException invalid) {
                    throw new IOException("WCL current LIVE gear could not be serialized", invalid);
                }
                WclLiveEquipmentMirrorProof.prove(originally, now, stored.bytes(),
                        maxSerializedBytes, maxInflatedBytes);
                sameItemMirrors.add(slot.getKey());
            }
            selectedWcl.add(new WclHybridLiveOverlay.Selected(chunk, candidate.entityId(),
                    candidate.providerId(), candidate.ownerEvidence(), candidate.slotValues(),
                    sameItemMirrors));
            selectedWclOriginals.add(candidate);
            selectedWclReader = consumer;
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

    /** Authoritative WCL owner from the same provider read as the captured LIVE money. */
    public synchronized java.util.Optional<WclOwnerEvidence.Decision> capturedWclOwner(UUID entity) {
        return selectedWcl.stream().filter(wcl -> wcl.entityId().equals(entity))
                .map(WclHybridLiveOverlay.Selected::ownerEvidence).findFirst();
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
                maximumInflatedBytes, liveOwnership, null, null);
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
                maximumInflatedBytes, liveOwnership, null, null);
    }

    /**
     * Optional SAVED WCL provider read, executed within the SAME selected
     * SAVED Anvil Entity callback as normal Entity monetary staging.
     * Caller must independently establish world UUID provenance, writer
     * exclusion and complete LIVE/SAVED source selection. Result unpublished.
     */
    public SettlementScanRunner.Staged stageCandidateWithSavedWcl(
            Plugin consumer, Map<String, UUID> verifiedWorldIds,
            WorldStorageDiscovery.Manifest manifest, WorldScope scope,
            Set<UUID> currentlyBanned,
            SettlementScanRunner.BlockOwnerLookup blockOwners,
            SettlementScanRunner.EntityOwnerLookup entityOwners,
            int maximumCompressedBytes, int maximumInflatedBytes,
            HybridLiveSourceStage.Classifier liveOwnership) throws IOException {
        return stageCandidateInternal(manifest, scope, currentlyBanned, null,
                blockOwners, entityOwners, maximumCompressedBytes,
                maximumInflatedBytes, liveOwnership,
                Objects.requireNonNull(consumer), Map.copyOf(verifiedWorldIds));
    }

    /** Variant where the same BAN provider generation covers saved and LIVE. */
    public SettlementScanRunner.Staged stageCandidateWithSavedWclAndBanCoverage(
            Plugin consumer, Map<String, UUID> verifiedWorldIds,
            WorldStorageDiscovery.Manifest manifest, WorldScope scope,
            BanSourceCoverage banSources,
            SettlementScanRunner.BlockOwnerLookup blockOwners,
            SettlementScanRunner.EntityOwnerLookup entityOwners,
            int maximumCompressedBytes, int maximumInflatedBytes,
            HybridLiveSourceStage.Classifier liveOwnership) throws IOException {
        return stageCandidateInternal(manifest, scope, null,
                Objects.requireNonNull(banSources), blockOwners, entityOwners,
                maximumCompressedBytes, maximumInflatedBytes, liveOwnership,
                Objects.requireNonNull(consumer), Map.copyOf(verifiedWorldIds));
    }

    /**
     * One-shot, source-scoped and still UNPUBLISHED composition of the
     * independently captured SAVED terrain owner observation with the
     * existing genuine WCL, BAN and LIVE/SAVED hybrid stage.
     *
     * Only the caller can supply independent per-chest protection proofs.
     * A saved-content witness is not evidence that Paper/other writers are
     * stopped. Never grant G0 publication, G3 ACK or player admission here.
     */
    public SettlementScanRunner.Staged
            stageCandidateWithCapturedBlockLockerAndSavedWclAndBanCoverage(
            Plugin consumer, Map<String, UUID> verifiedWorldIds,
            WorldStorageDiscovery.Manifest manifest, WorldScope scope,
            BanSourceCoverage banSources,
            SavedBlockLockerTerrainObservations.Snapshot savedTerrain,
            SavedBlockLockerTerrainObservations.Snapshot.IndependentProofs independentProofs,
            SettlementScanRunner.SourceBlockOwnerLookup otherBlockOwners,
            SettlementScanRunner.EntityOwnerLookup entityOwners,
            int maximumCompressedBytes, int maximumInflatedBytes,
            HybridLiveSourceStage.Classifier liveOwnership) throws IOException {
        return stageCombined(consumer, verifiedWorldIds, manifest, scope, banSources, savedTerrain,
                independentProofs, otherBlockOwners, entityOwners, maximumCompressedBytes,
                maximumInflatedBytes, liveOwnership, SettlementReadDispatch.direct());
    }

    /** Actual worker path for a previously captured protected LIVE generation.
     * SAVED lookups and independent proofs must use immutable source observations;
     * only the provider and LIVE Paper callbacks are dispatched to primary.
     * The enclosing runtime must retain its drained writer lease throughout. */
    public SettlementScanRunner.Staged stageCombinedOffMain(
            Plugin consumer, Map<String, UUID> verifiedWorldIds,
            WorldStorageDiscovery.Manifest manifest, WorldScope scope, BanSourceCoverage banSources,
            SavedBlockLockerTerrainObservations.Snapshot savedTerrain,
            SavedBlockLockerTerrainObservations.Snapshot.IndependentProofs independentProofs,
            SettlementScanRunner.SourceBlockOwnerLookup otherBlockOwners,
            SettlementScanRunner.EntityOwnerLookup entityOwners, int maximumCompressedBytes,
            int maximumInflatedBytes, HybridLiveSourceStage.Classifier liveOwnership,
            long timeoutMillis) throws IOException {
        Objects.requireNonNull(consumer);
        if (Bukkit.isPrimaryThread()) throw new IOException("Combined worker stage on primary");
        try (var reads = SettlementReadDispatch.bounded(
                task -> Bukkit.getScheduler().runTask(consumer, task), Bukkit::isPrimaryThread, timeoutMillis)) {
            return stageCombined(consumer, verifiedWorldIds, manifest, scope, banSources, savedTerrain,
                    independentProofs, otherBlockOwners, entityOwners, maximumCompressedBytes,
                    maximumInflatedBytes, liveOwnership, reads);
        }
    }

    private SettlementScanRunner.Staged stageCombined(
            Plugin consumer, Map<String, UUID> verifiedWorldIds,
            WorldStorageDiscovery.Manifest manifest, WorldScope scope, BanSourceCoverage banSources,
            SavedBlockLockerTerrainObservations.Snapshot savedTerrain,
            SavedBlockLockerTerrainObservations.Snapshot.IndependentProofs independentProofs,
            SettlementScanRunner.SourceBlockOwnerLookup otherBlockOwners,
            SettlementScanRunner.EntityOwnerLookup entityOwners, int maximumCompressedBytes,
            int maximumInflatedBytes, HybridLiveSourceStage.Classifier liveOwnership,
            SettlementReadDispatch reads) throws IOException {
        boolean finished = false;
        try {
            reads.requireCurrent();
            if (consumed.get())
                throw new IllegalStateException(
                        "Paper monetary capture was already consumed; acquire a new generation");
            // fromCaptured validates the original Manifest and both source
            // witnesses and refuses missing/contradictory independently
            // supplied BlockLocker proof. No coordinate-only fallback.
            var physical = SavedBlockLockerSettlementOwnerLookup.fromCaptured(
                    savedTerrain, manifest, banSources, independentProofs);
            var chestOwners = physical.sourceScoped();
            var otherOwners = Objects.requireNonNull(
                    otherBlockOwners, "independently verified non-chest ownership");
            // The A04 confirmed classification is chest-only. The complete
            // SAVED source universe also includes e.g. furnaces/barrels.
            // Never classify those unknown owners as FREE or reject them as
            // if they were malformed chests: require a separately provided
            // SOURCE-SCOPED ownership resolver. Identify every tile from
            // the selected Chunk and its actual terrain palette first.
            SettlementScanRunner.SourceBlockOwnerLookup allBlocks =
                    (source, chunk, position) -> {
                        String type = verifiedSavedBlockType(manifest, source, chunk, position);
                        if (savedTerrain.configuredContainers().contains(type))
                            return chestOwners.resolve(source, chunk, position);
                        return otherOwners.resolve(source, chunk, position);
                    };
            var staged = stageCandidateInternal(manifest, scope, null, Objects.requireNonNull(banSources),
                    allBlocks, entityOwners, maximumCompressedBytes, maximumInflatedBytes, liveOwnership,
                    Objects.requireNonNull(consumer), Map.copyOf(verifiedWorldIds), reads);
            // Independent local source checks only, not whole-world quiescence.
            savedTerrain.witness().requireUnchanged();
            if (staged.savedContentWitness() == null)
                throw new IOException("Missing completed SAVED generation witness");
            staged.savedContentWitness().requireUnchanged();
            manifest.requireUnchangedDiscovery();
            reads.requireCurrent();
            finished = true;
            return staged;
        } finally {
            // Even an exception before stageCandidateInternal starts must
            // invalidate this capture. Never retry UNKNOWN/changed owner
            // evidence by substituting weaker ownership into the same scan.
            if (!finished) consumed.set(true);
        }
    }

    /**
     * One physical saved BlockEntity identity from the exact selected REGION
     * and Chunk; a caller cannot route an orphan tile or a hand-authored type
     * assertion around the confirmed A04 chest source-scoped protection check.
     * This local check is not an immutable writer epoch proof.
     */
    private static String verifiedSavedBlockType(
            WorldStorageDiscovery.Manifest selected,
            WorldStorageDiscovery.SourceFile source,
            co.surumene.simplecoins.domain.AnvilRegionReader.Chunk chunk,
            String position) throws IOException {
        if (selected == null || source == null || chunk == null
                || source.kind() != WorldStorageDiscovery.Kind.REGION
                || !selected.worldFiles().contains(source) || position == null)
            throw new IOException("Unselected or untyped SAVED BlockEntity source");
        String[] parts = position.split(",", -1);
        if (parts.length != 3)
            throw new IOException("Noncanonical SAVED BlockEntity coordinate");
        int[] xyz = new int[3];
        for (int i = 0; i < 3; i++) {
            try {
                xyz[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException malformed) {
                throw new IOException("Invalid SAVED BlockEntity coordinate", malformed);
            }
            if (!Integer.toString(xyz[i]).equals(parts[i]))
                throw new IOException("Noncanonical SAVED BlockEntity coordinate");
        }
        if (Math.floorDiv(xyz[0], 16) != chunk.x()
                || Math.floorDiv(xyz[2], 16) != chunk.z())
            throw new IOException("SAVED BlockEntity is outside its selected Chunk");
        if (chunk.nbt() == null
                || !(chunk.nbt().get("block_entities") instanceof List<?> tiles))
            throw new IOException("Selected SAVED Chunk has no BlockEntity source");
        String type = null;
        for (Object raw : tiles) {
            if (!(raw instanceof Map<?, ?> tile)
                    || !(tile.get("x") instanceof Integer x)
                    || !(tile.get("y") instanceof Integer y)
                    || !(tile.get("z") instanceof Integer z)
                    || !(tile.get("id") instanceof String id))
                throw new IOException("Malformed SAVED BlockEntity");
            if (x == xyz[0] && y == xyz[1] && z == xyz[2]) {
                if (type != null)
                    throw new IOException("Duplicate selected SAVED physical BlockEntity");
                type = id;
            }
        }
        if (type == null)
            throw new IOException("No selected SAVED BlockEntity at requested coordinate");
        String physical=SavedTerrainBlockPalette.read(chunk.nbt(),xyz[0],xyz[1],xyz[2]).name();
        if (!co.surumene.simplecoins.domain.SavedBlockLockerChestSigns.matchesStorage(type,physical))
            throw new IOException("SAVED BlockEntity differs from physical terrain palette");
        return physical;
    }

    /**
     * Necessary (not sufficient for G0) locally bounded WCL freshness proof:
     * reject a changed, moved or missing *actually selected* LIVE provider
     * source rather than returning a stale owner with an unchanged money total.
     *
     * This does not freeze a writer after return. Only the real Paper capture
     * stores a source-backed initial candidate; fromObservedWithWcl is a test
     * seam and must not masquerade as a successful Paper source recheck.
     */
    private void requireSelectedWclStillCurrent(
            int maxSerializedBytes, int maxInflatedBytes, SettlementReadDispatch reads) throws IOException {
        if (selectedWcl.isEmpty() || capturedWorlds.isEmpty()) return;
        for (int offset = 0; offset < selectedWcl.size(); offset += 32) {
            final int start = offset, end = Math.min(offset + 32, selectedWcl.size());
            reads.call(() -> { requireSelectedWclRangeStillCurrent(maxSerializedBytes, maxInflatedBytes, start, end); return null; });
        }
    }

    private void requireSelectedWclRangeStillCurrent(
            int maxSerializedBytes, int maxInflatedBytes, int start, int end) throws IOException {
        if (!Bukkit.isPrimaryThread() || selectedWclReader == null
                || selectedWclOriginals.size() != selectedWcl.size())
            throw new IOException("Real LIVE WCL freshness proof is missing or off Paper thread");
        for (int index = start; index < end; index++) {
            var first = selectedWclOriginals.get(index);
            var originalSelection = selectedWcl.get(index);
            Entity entity = Bukkit.getEntity(first.entityId());
            World selectedWorld = capturedWorlds.get(first.worldName());
            if (entity == null || !entity.isValid() || selectedWorld == null
                    || entity.getWorld() != selectedWorld
                    || !entity.getWorld().getUID().equals(first.worldId())
                    || !originalSelection.entityId().equals(first.entityId())
                    || !originalSelection.providerId().equals(first.providerId())) {
                throw new IOException("LIVE WCL holder changed during saved stage: "
                        + first.entityId() + " (missing, moved or changed source)");
            }
            var location = entity.getLocation();
            var currentChunk = new SavedLiveSourcePartition.Chunk(
                    selectedWorld.getName(),
                    Math.floorDiv(location.getBlockX(), 16),
                    Math.floorDiv(location.getBlockZ(), 16));
            if (!originalSelection.chunk().equals(currentChunk))
                throw new IOException("LIVE WCL holder changed during saved stage: "
                        + first.entityId() + " (chunk relocated)");
            final PaperWonderfulLiveCandidates.Candidate fresh;
            try {
                // Separate identity set is correct for a value-preserving
                // readback. Original shared-wallet uniqueness was verified
                // during the first capture; raw serialized item bytes MUST
                // match here, including wallet UUID and its monetary balance.
                fresh = PaperWonderfulLiveCandidates.collect(
                        selectedWclReader, entity, first.providerId(),
                        new HashSet<>(), maxSerializedBytes, maxInflatedBytes);
            } catch (IOException | RuntimeException failedRead) {
                throw new IOException("LIVE WCL holder changed during saved stage: "
                        + first.entityId() + " (unreadable reread)", failedRead);
            }
            boolean equal = first.entityId().equals(fresh.entityId())
                    && first.worldId().equals(fresh.worldId())
                    && first.worldName().equals(fresh.worldName())
                    && first.providerId().equals(fresh.providerId())
                    && first.ownerEvidence().equals(fresh.ownerEvidence())
                    && first.relationships().equals(fresh.relationships())
                    && first.slotValues().equals(fresh.slotValues())
                    && first.physicalSlotKeys().equals(fresh.physicalSlotKeys())
                    && first.sourceItems().keySet().equals(fresh.sourceItems().keySet());
            if (equal) {
                for (var entry : first.sourceItems().entrySet()) {
                    var now = fresh.sourceItems().get(entry.getKey());
                    if (now == null || !entry.getValue().slot().equals(now.slot())
                            || !java.util.Arrays.equals(
                                    entry.getValue().bytes(), now.bytes())) {
                        equal = false;
                        break;
                    }
                }
            }
            if (equal) {
                // An original positive WWW weapon/WWC head was also physically
                // counted from vanilla equipment. The provider's snapshot can
                // remain byte-identical while a separate Paper equipment writer
                // replaces that real native item. Recheck BOTH physical views
                // at this last local staging boundary, never accept an old
                // native 10,000C just because the WCL weapon still says 10,000C.
                for (String slot : originalSelection.verifiedEquipmentMirrorSlots()) {
                    try {
                        String nativeSlot = switch (first.providerId() + "/" + slot) {
                            case "www/weapon" -> "mainhand";
                            case "wwc/head" -> "head";
                            default -> throw new IOException(
                                    "Unknown verified WCL/native equipment mirror slot");
                        };
                        if (!(entity instanceof LivingEntity living)
                                || living.getEquipment() == null)
                            throw new IOException("Selected LIVE WCL entity has no equipment");
                        ItemStack now = "mainhand".equals(nativeSlot)
                                ? living.getEquipment().getItemInMainHand()
                                : living.getEquipment().getHelmet();
                        if (now == null || now.getType().isAir())
                            throw new IOException("Mirrored native equipment is missing");
                        String nativePhysical = "world/" + originalSelection.chunk().world()
                                + "/entity/" + first.entityId()
                                + "/equipment/" + nativeSlot;
                        byte[] original = originalLiveEquipment.get(nativePhysical);
                        var stored = first.sourceItems().get(slot);
                        if (original == null || stored == null)
                            throw new IOException("Missing original native/WCL mirror evidence");
                        WclLiveEquipmentMirrorProof.prove(original, now.serializeAsBytes(),
                                stored.bytes(), maxSerializedBytes, maxInflatedBytes);
                    } catch (IOException | RuntimeException changedNative) {
                        throw new IOException("LIVE WCL holder changed during saved stage: "
                                + first.entityId() + " (native equipment mirror drift)",
                                changedNative);
                    }
                }
            }

            if (!equal)
                throw new IOException("LIVE WCL holder changed during saved stage: "
                        + first.entityId() + " (owner, values or ItemStack bytes)");
        }
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
            HybridLiveSourceStage.Classifier liveOwnership,
            Plugin savedWclConsumer, Map<String, UUID> savedWorldIds) throws IOException {
        return stageCandidateInternal(manifest, scope, currentlyBanned, banSources, blockOwners, entityOwners,
                maximumCompressedBytes, maximumInflatedBytes, liveOwnership, savedWclConsumer, savedWorldIds,
                SettlementReadDispatch.direct());
    }

    private SettlementScanRunner.Staged stageCandidateInternal(
            WorldStorageDiscovery.Manifest manifest, WorldScope scope, Set<UUID> currentlyBanned,
            BanSourceCoverage banSources, SettlementScanRunner.BlockOwnerLookup blockOwners,
            SettlementScanRunner.EntityOwnerLookup entityOwners, int maximumCompressedBytes,
            int maximumInflatedBytes, HybridLiveSourceStage.Classifier liveOwnership,
            Plugin savedWclConsumer, Map<String, UUID> savedWorldIds, SettlementReadDispatch reads) throws IOException {
        reads.requireCurrent();
        List<WclHybridLiveOverlay.Selected> wclCandidates;
        synchronized (this) {
            if (!consumed.compareAndSet(false, true))
                throw new IllegalStateException(
                        "Paper monetary capture was already consumed; acquire a new generation");
            wclCandidates = List.copyOf(selectedWcl);
        }
        // Attach WCL independent slots to the selected native LIVE Chunk.
        // Positive equipment aliases may reuse the captured native Value only
        // when full source-time/current/provider ItemStack equality was proven.
        // No new source or global writer/settlement guarantee is introduced.
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

        // Select SAVED WCL only for chunks that the partition routed to disk.
        // The callback executes within the normal saved Anvil visitor BEFORE
        // its source is completed and shares the same pending transaction and
        // wallet identities. Failure discards this one-shot candidate.
        PaperWonderfulSavedSettlementSource savedWclSupplement =
                savedWclConsumer == null ? null : new PaperWonderfulSavedSettlementSource(
                        savedWclConsumer, savedWorldIds, manifest, currentlyBanned,
                        banSources, wclCandidates.stream()
                                .map(WclHybridLiveOverlay.Selected::entityId)
                                .collect(java.util.stream.Collectors.toSet()),
                        maximumCompressedBytes, maximumInflatedBytes, reads);
        SettlementScanRunner.Staged staged = banSources == null
                ? SettlementScanRunner.stage(
                        manifest, scope, currentlyBanned, blockOwners, entityOwners,
                        maximumCompressedBytes, maximumInflatedBytes, walletIds,
                        selection, savedWclSupplement)
                : SettlementScanRunner.stageWithBanCoverage(
                        manifest, scope, banSources, blockOwners, entityOwners,
                        maximumCompressedBytes, maximumInflatedBytes, walletIds,
                        selection, savedWclSupplement);

        // Same physical source keys and balances; explicit ownership MUST be
        // provided. A failure never returns the staged result to the caller.
        HybridLiveSourceStage.Classifier capturedOwnership = reads.isDispatched()
                ? capturePrimaryOwnership(overlay, liveOwnership, reads) : liveOwnership;
        HybridLiveSourceStage.Classifier verifiedLive = (source, value) -> {
            WclOwnerEvidence.Decision wcl = overlay.wclOwners().get(value.physicalKey());
            if (wcl != null) {
                UUID owner = wcl.owner();
                boolean banned = owner != null && (banSources != null
                        ? banSources.currentlyBanned(owner)
                        : currentlyBanned.contains(owner));
                boolean playerdata = owner != null
                        && manifest.playerDataFiles().containsKey(owner);
                if (overlay.mirroredPhysicalKeys().contains(value.physicalKey())) {
                    // Native asset is authoritative and already present in
                    // this source. WCL may not silently overwrite the separate
                    // LIVE ownership classification, even for equal money.
                    SettlementAccumulator.Asset nativeAsset =
                            capturedOwnership.classify(source, value);
                    if (nativeAsset == null
                            || !Objects.equals(owner, nativeAsset.owner())
                            || nativeAsset.everyoneProtected()
                            || nativeAsset.currentlyBanned() != banned
                            || nativeAsset.hasPlayerdata() != playerdata)
                        throw new IOException("Native/WCL LIVE equipment owner evidence differs: "
                                + value.physicalKey());
                    return nativeAsset; // checked by HybridLiveSourceStage
                }
                // WCL-independent cargo/shulker/slot ownership.
                return new SettlementAccumulator.Asset(
                        source.id(), value.physicalKey(), value.world(), owner,
                        false, banned, playerdata,
                        value.amount(), value.physicalKey());
            }
            SettlementAccumulator.Asset raw = capturedOwnership.classify(source, value);
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
        if (savedWclSupplement != null)
            savedWclSupplement.requireWorldIdentityUnchanged();
        // Read actual enrolled LIVE WWC/WWW once more after the saved phase.
        // A changed owner with identical currency bytes must HOLD. This
        // closes a *known local read-to-stage gap only*: it does not establish
        // the global writer epoch through the caller's eventual publication.
        requireSelectedWclStillCurrent(maximumCompressedBytes, maximumInflatedBytes, reads);
        reads.requireCurrent();
        return staged;
    }

    /** Bound the number of actual LIVE classifications in one primary callback. */
    private static HybridLiveSourceStage.Classifier capturePrimaryOwnership(
            WclHybridLiveOverlay.Overlay overlay, HybridLiveSourceStage.Classifier resolver,
            SettlementReadDispatch reads) throws IOException {
        // Key by physical key and source id rather than hashing large Source lists repeatedly.
        record Key(String source, String physical) {}
        record Request(HybridLiveSourceStage.Source source, HybridLiveSourceStage.Value value) {
            Key key() { return new Key(source.id(), value.physicalKey()); }
        }
        List<Request> requests = new ArrayList<>();
        for (var source : overlay.sources()) for (var value : source.values()) {
            if (!overlay.wclOwners().containsKey(value.physicalKey()) || overlay.mirroredPhysicalKeys().contains(value.physicalKey()))
                requests.add(new Request(source, value));
        }
        Map<Key, SettlementAccumulator.Asset> resolved = new java.util.HashMap<>();
        for (int offset=0; offset<requests.size(); offset+=128) {
            var batch=List.copyOf(requests.subList(offset, Math.min(offset+128, requests.size())));
            var result=reads.call(() -> {
                Map<Key, SettlementAccumulator.Asset> observations = new java.util.HashMap<>();
                for (var request : batch) {
                    var asset=resolver.classify(request.source(), request.value());
                    if (asset==null || observations.putIfAbsent(request.key(),asset)!=null)
                        throw new IOException("Missing or duplicate LIVE owner observation");
                }
                return Map.copyOf(observations);
            });
            for (var entry : result.entrySet()) if (resolved.putIfAbsent(entry.getKey(),entry.getValue())!=null)
                throw new IOException("Repeated LIVE owner request");
        }
        var immutable=Map.copyOf(resolved);
        return (source,value) -> immutable.get(new Key(source.id(),value.physicalKey()));
    }
}
