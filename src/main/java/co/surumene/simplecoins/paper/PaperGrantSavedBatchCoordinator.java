package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.PlayerDataReader;
import co.surumene.simplecoins.domain.WriterEpochFence;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Product-owned real Paper saved-batch orchestration for one reserved grant.
 *
 * <p>Deliberately NOT registered as a join listener, command or runtime service.
 * The supplied writer epoch MUST already cover the entire installed native,
 * plugin, asynchronous and direct-file writer universe before this code can
 * authorize live use. The current PASSIVE startup Agent does NOT meet that
 * prerequisite. A test-only armed fence is not sufficient for deployment.</p>
 *
 * <p>For now this entrypoint covers a fresh, not-yet-active grant's first
 * batch, including full single-batch ACK and incomplete first-batch progress.
 * Resumed multi-batch delivery requires independently durable verification
 * of earlier physical custody; the final-batch proof helper exists separately
 * but is not invoked from this first-batch coordinator.</p>
 */
final class PaperGrantSavedBatchCoordinator {
    private PaperGrantSavedBatchCoordinator() {}

    /**
     * Product pre-save handoff. Capture only Paper-owned memory on primary;
     * the complete filesystem and ledger preflight runs on the bounded worker.
     * The enclosing runtime must supply actual captured roots and complete
     * writer coverage (including all grant journal writers) before use.
     */
    static PendingPreflight schedulePreflightOnMain(Player detached, GrantLedger ledger,
            GrantLedger.Grant grant, WriterEpochFence fence,
            WorldStorageDiscovery.DiscoveryOrigin origin, SavedGrantCommitWorker worker,
            long timeoutMillis, int compressedLimit, int inflatedLimit) {
        Objects.requireNonNull(fence, "externally audited writer fence");
        try {
            requireDetachedPrimary(detached);
            Objects.requireNonNull(ledger, "durable grant ledger");
            Objects.requireNonNull(origin, "captured saved source roots");
            Objects.requireNonNull(grant, "authentic reservation");
            Objects.requireNonNull(worker, "bounded saved grant worker");
            if (compressedLimit < 1 || inflatedLimit < 1)
                throw new IllegalArgumentException("Nonpositive saved playerdata read budget");
            UUID player = detached.getUniqueId();
            if (!player.equals(grant.player()) || grant.delivered())
                throw new IllegalStateException("Not a genuine pending grant");
            WriterEpochFence.Seal seal = fence.seal();
            fence.requireDrained(seal); // never await writer drain on primary
            var receipt = PaperGrantReceiptPdc.read(detached.getPersistentDataContainer());
            var storage = PaperGrantInventorySlotPlan.StorageSnapshot.fromPaper(
                    detached.getInventory().getStorageContents());
            var ticket = worker.submit(timeoutMillis, live -> {
                if (Bukkit.isPrimaryThread())
                    throw new IllegalStateException("Pre-save disk verification on primary");
                return fence.withDrainedLease(seal, () -> {
                    requireLivePreflight(live);
                    var manifest = WorldStorageDiscovery.discover(origin.worldFolders(), origin.scope(),
                            origin.maximumEntries(), origin.sharedPlayerRoots());
                    Path file = manifest.playerDataFiles().get(player);
                    if (file == null || !file.getFileName().toString().equals(player + ".dat")
                            || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                        throw new IOException("Missing genuine original saved playerdata file");
                    var witness = SavedPlayerdataContentWitness.capturePlayer(manifest, player, compressedLimit);
                    byte[] captured = PlayerDataReader.readCompressed(file, compressedLimit);
                    var current = PlayerDataReader.decodeCompressed(captured, compressedLimit, inflatedLimit);
                    var view = ledger.verifiedPlayerSnapshot(player);
                    var saved = SavedGrantReceiptNbt.readAndVerify(current, view);
                    if (!saved.equals(receipt))
                        throw new IllegalStateException("Saved/memory receipt drift");
                    var intent = ledger.readFirstBatchIntent(view, grant, file, compressedLimit);
                    Map<String, Object> before;
                    Optional<PaperGrantInventorySlotPlan> plan;
                    boolean recoveryOnly = false;
                    if (intent.isPresent()) {
                        before = PlayerDataReader.decodeCompressed(intent.orElseThrow(), compressedLimit, inflatedLimit);
                        var prior = SavedGrantReceiptNbt.readAndVerify(before, view);
                        if (prior.active()) throw new IOException("First-batch intent already has a partial prior receipt");
                        var intended = PaperGrantInventorySlotPlan.stage(view, grant, prior,
                                SavedGrantDeliveryReadback.savedStorageForIntent(before)).orElseThrow(
                                        () -> new IOException("Durable intent has no physical placement"));
                        if (saved.equals(intended.nextReceipt())) {
                            // Crash after real save but before ACK: never mint again.
                            SavedGrantDeliveryReadback.verifyTransition(before, current, ledger, player, intended);
                            plan = Optional.of(intended);
                            recoveryOnly = true;
                        } else if (saved.equals(prior)) {
                            SavedGrantDeliveryReadback.requireSameIntentStorage(before, current);
                            plan = PaperGrantInventorySlotPlan.stage(view, grant, saved, storage);
                            requireSameIntentPlan(intended, plan);
                        } else throw new IOException("Saved receipt is not either durable intent boundary");
                    } else {
                        if (saved.active() || saved.completedSequence() >= grant.sequence())
                            throw new IOException("Saved progress has no durable first-batch intent");
                        before = current;
                        plan = PaperGrantInventorySlotPlan.stage(view, grant, saved, storage);
                        var diskPlan = PaperGrantInventorySlotPlan.stage(view, grant, saved,
                                SavedGrantDeliveryReadback.savedStorageForIntent(before));
                        if (plan.isPresent() != diskPlan.isPresent())
                            throw new IOException("Saved and live grant capacity disagree");
                        if (diskPlan.isPresent()) requireSameIntentPlan(diskPlan.orElseThrow(), plan);
                        witness.requireUnchanged();
                        requireLivePreflight(live);
                        if (plan.isPresent()) ledger.writeFirstBatchIntent(view, grant, file, captured, compressedLimit);
                    }
                    witness.requireUnchanged();
                    ledger.requireCurrentPlanningView(view);
                    requireLivePreflight(live);
                    return new PreparedPreflight(manifest, file, before, view, plan, recoveryOnly);
                });
            }, fence::invalidate);
            return new PendingPreflight(fence, seal, ledger, player, grant, receipt, storage,
                    compressedLimit, inflatedLimit, ticket);
        } catch (RuntimeException | Error failure) {
            fence.invalidate();
            throw failure;
        }
    }

    private static void requireSameIntentPlan(PaperGrantInventorySlotPlan intended,
            Optional<PaperGrantInventorySlotPlan> actual) throws IOException {
        if (actual.isEmpty() || !intended.previousReceipt().equals(actual.orElseThrow().previousReceipt())
                || !intended.nextReceipt().equals(actual.orElseThrow().nextReceipt())
                || !intended.placements().equals(actual.orElseThrow().placements()))
            throw new IOException("Paper placement differs from durable prior-state plan");
    }

    private static void requireDetachedPrimary(Player detached) {
        Objects.requireNonNull(detached, "real Paper detached player");
        if (!Bukkit.isPrimaryThread() || detached.isOnline() || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IllegalStateException("Grant save requires primary thread, detached player and zero admissions");
    }

    private static void requireLivePreflight(BooleanSupplier live) {
        if (!live.getAsBoolean()) throw new IllegalStateException("Saved preflight custody expired or abandoned");
    }

    private record PreparedPreflight(WorldStorageDiscovery.Manifest manifest, Path file,
            Map<String, Object> before, GrantLedger.VerifiedPlayerSnapshot view,
            Optional<PaperGrantInventorySlotPlan> plan, boolean recoveryOnly) {}

    /** One-shot pre-save custody. No live Player, ItemStack or World crosses threads. */
    static final class PendingPreflight {
        private final WriterEpochFence fence;
        private final WriterEpochFence.Seal seal;
        private final GrantLedger ledger;
        private final UUID player;
        private final GrantLedger.Grant grant;
        private final GrantReceiptState receipt;
        private final PaperGrantInventorySlotPlan.StorageSnapshot storage;
        private final int compressedLimit, inflatedLimit;
        private final SavedGrantCommitWorker.Ticket<PreparedPreflight> ticket;

        private PendingPreflight(WriterEpochFence fence, WriterEpochFence.Seal seal,
                GrantLedger ledger, UUID player, GrantLedger.Grant grant, GrantReceiptState receipt,
                PaperGrantInventorySlotPlan.StorageSnapshot storage, int compressedLimit,
                int inflatedLimit, SavedGrantCommitWorker.Ticket<PreparedPreflight> ticket) {
            this.fence = fence; this.seal = seal; this.ledger = ledger; this.player = player;
            this.grant = grant; this.receipt = receipt; this.storage = storage;
            this.compressedLimit = compressedLimit; this.inflatedLimit = inflatedLimit; this.ticket = ticket;
        }

        SavedGrantCommitWorker.Status status() { return ticket.status(); }

        /** No discovery, NBT parse, journal scan, wait or ACK on main. */
        Optional<SealedFirstBatch> saveOnMain(Player detached, CoinItemCodec coins) throws IOException {
            try {
                requireDetachedPrimary(detached);
                if (!player.equals(detached.getUniqueId())) throw new IllegalStateException("Foreign player preflight");
                Objects.requireNonNull(coins, "canonical coin codec");
                PreparedPreflight prepared = ticket.result();
                ticket.claimSuccess(); // expiry, close and duplicate consumption are rejected
                var result = fence.withDrainedWriter(seal, () -> {
                    ledger.requireCurrentPlanningView(prepared.view());
                    var currentReceipt = PaperGrantReceiptPdc.read(detached.getPersistentDataContainer());
                    var original = detached.getInventory().getStorageContents();
                    if (!receipt.equals(currentReceipt)
                            || !storage.equals(PaperGrantInventorySlotPlan.StorageSnapshot.fromPaper(original)))
                        throw new IllegalStateException("Player state changed after protected preflight");
                    if (prepared.plan().isEmpty()) return Optional.<SealedFirstBatch>empty();
                    var plan = prepared.plan().orElseThrow();
                    if (prepared.recoveryOnly())
                        return Optional.of(new SealedFirstBatch(fence, seal, prepared.manifest(), prepared.file(),
                                ledger, player, grant, plan, prepared.before(), compressedLimit, inflatedLimit));
                    var candidate = PaperGrantInventoryMaterializer.buildCopy(plan, currentReceipt, original, coins);
                    plan.requireUnchanged(PaperGrantReceiptPdc.read(detached.getPersistentDataContainer()),
                            detached.getInventory().getStorageContents());
                    ledger.requireCurrentPlanningView(prepared.view());
                    detached.getInventory().setStorageContents(candidate);
                    PaperGrantReceiptPdc.write(detached.getPersistentDataContainer(), plan.nextReceipt());
                    detached.saveData(); // no success claim until the post-save worker proves durable state
                    ledger.requireCurrentPlanningView(prepared.view());
                    return Optional.of(new SealedFirstBatch(fence, seal, prepared.manifest(), prepared.file(),
                            ledger, player, grant, plan, prepared.before(), compressedLimit, inflatedLimit));
                });
                if (result.isEmpty()) fence.resume(seal); // no physical progress or ACK
                return result;
            } catch (IOException | RuntimeException | Error failure) {
                ticket.abandon();
                fence.invalidate();
                throw failure;
            }
        }
    }

    /**
     * Existing synchronous internal integration contract. Real runtime code
     * MUST use schedulePreflightOnMain(), PendingPreflight.saveOnMain() and scheduleOffMain(): discovery,
     * forcing saved files and ACK are too expensive for the Paper main thread.
     *
     * This legacy path remains unregistered and exists only to preserve the
     * independently exercised historical Paper regression.
     */
    static Optional<SavedGrantDeliveryReadback.Proof> saveAndFinalizeFirstBatch(
            Player detached, GrantLedger ledger, GrantLedger.Grant grant,
            CoinItemCodec coins, WriterEpochFence fence,
            WorldStorageDiscovery.Manifest beforeManifest,
            Callable<WorldStorageDiscovery.Manifest> discoverAfterSave,
            int maximumCompressedBytes, int maximumInflatedBytes) throws IOException {
        Objects.requireNonNull(discoverAfterSave, "real saved source discovery");
        Optional<SealedFirstBatch> saved = saveAndSealFirstBatchOnMain(
                detached, ledger, grant, coins, fence, beforeManifest,
                maximumCompressedBytes, maximumInflatedBytes);
        return saved.isEmpty() ? Optional.empty()
                : Optional.of(saved.orElseThrow().finishLegacyMainOnly(discoverAfterSave));
    }

    /**
     * Legacy unregistered integration probe: performs synchronous preflight,
     * PDC/Inventory changes and Paper saveData on the primary thread. This
     * stage never ACKs a grant; an asynchronous bounded verifier MUST finish
     * the returned sealed result before this generation may be resumed.
     *
     * A full inventory returns Optional.empty() with no money/PDC save.
     * The already-open installed writer epoch is supplied, never armed here.
     */
    static Optional<SealedFirstBatch> saveAndSealFirstBatchOnMain(
            Player detached, GrantLedger ledger, GrantLedger.Grant grant,
            CoinItemCodec coins, WriterEpochFence fence,
            WorldStorageDiscovery.Manifest beforeManifest,
            int maximumCompressedBytes, int maximumInflatedBytes) throws IOException {
        Objects.requireNonNull(fence, "externally audited writer fence");
        try {
            Objects.requireNonNull(detached, "real Paper detached player");
            Objects.requireNonNull(ledger, "durable grant ledger");
            Objects.requireNonNull(grant, "authentic reservation");
            Objects.requireNonNull(coins, "canonical coin codec");
            Objects.requireNonNull(beforeManifest, "origin-backed source discovery");
            if (!Bukkit.isPrimaryThread() || detached.isOnline()
                    || !Bukkit.getOnlinePlayers().isEmpty())
                throw new IllegalStateException(
                        "Saved grant staging requires primary thread, detached player and zero admissions");
            if (fence.state() != WriterEpochFence.State.OPEN)
                throw new IllegalStateException("Grant save requires already OPEN tracked writer epoch");
            if (maximumCompressedBytes < 1 || maximumInflatedBytes < 1)
                throw new IllegalArgumentException("Nonpositive saved playerdata read budget");

            UUID player = detached.getUniqueId();
            // Both exact-denomination and amount-form reservations are authentic
            // grant kinds. The latter is the required offline give form and
            // uses the existing canonical minimum-coin batch planner.
            if (!player.equals(grant.player()) || grant.delivered()
                    || !ledger.pending(player).contains(grant))
                throw new IllegalStateException("Not a genuine pending grant");
            if (beforeManifest.origin() == null)
                throw new IOException("Saved playerdata discovery is not origin-backed");
            beforeManifest.requireUnchangedDiscovery();
            Path file = beforeManifest.playerDataFiles().get(player);
            if (file == null || !file.getFileName().toString().equals(player + ".dat")
                    || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Missing genuine original saved playerdata file");
            SavedPlayerdataContentWitness beforeWitness =
                    SavedPlayerdataContentWitness.capturePlayer(
                            beforeManifest, player, maximumCompressedBytes);
            Map<String, Object> savedBefore = PlayerDataReader.read(
                    file, maximumCompressedBytes, maximumInflatedBytes);
            GrantReceiptState savedReceipt = SavedGrantReceiptNbt.readAndVerify(
                    savedBefore, ledger, player);
            GrantReceiptState memoryReceipt =
                    PaperGrantReceiptPdc.read(detached.getPersistentDataContainer());
            if (!savedReceipt.equals(memoryReceipt) || memoryReceipt.active())
                throw new IllegalStateException(
                        "Saved/memory grant receipt drift or unproved earlier partial batch");

            ItemStack[] originalStorage = detached.getInventory().getStorageContents();
            Optional<PaperGrantInventorySlotPlan> planned =
                    PaperGrantInventorySlotPlan.stage(
                            ledger, player, grant, savedReceipt, originalStorage);
            if (planned.isEmpty()) {
                beforeWitness.requireUnchanged();
                return Optional.empty();
            }
            PaperGrantInventorySlotPlan plan = planned.orElseThrow();
            ItemStack[] candidate = PaperGrantInventoryMaterializer.buildCopy(
                    plan, memoryReceipt, originalStorage, coins);
            plan.requireUnchanged(
                    PaperGrantReceiptPdc.read(detached.getPersistentDataContainer()),
                    detached.getInventory().getStorageContents());
            beforeWitness.requireUnchanged();

            detached.getInventory().setStorageContents(candidate);
            PaperGrantReceiptPdc.write(
                    detached.getPersistentDataContainer(), plan.nextReceipt());
            detached.saveData(); // void return MUST NOT be interpreted as durable success

            // The transition to SEALED occurs immediately after real Paper
            // saveData. No subsequent filesystem traversal or ACK on main.
            WriterEpochFence.Seal seal = fence.seal();
            fence.requireDrained(seal); // never await other writers on main
            return Optional.of(new SealedFirstBatch(fence, seal, beforeManifest,
                    file, ledger, player, grant, plan, savedBefore,
                    maximumCompressedBytes, maximumInflatedBytes));
        } catch (IOException | RuntimeException | Error failure) {
            // A physical/PDC save may have partially persisted. No in-memory
            // rewind or blind reissue is allowed after an exception.
            fence.invalidate();
            throw failure;
        }
    }

    /**
     * Opaque one-shot custody of a post-save SEALED writer generation.
     * The coordinator never retains a Bukkit Player or World object here;
     * only immutable discovery inputs, the staged saved NBT transition and
     * the authentic ledger/epoch can cross the main/worker boundary.
     */
    static final class SealedFirstBatch {
        private final WriterEpochFence fence;
        private final WriterEpochFence.Seal seal;
        private final WorldStorageDiscovery.Manifest original;
        private final Path playerdataFile;
        private final GrantLedger ledger;
        private final UUID player;
        private final GrantLedger.Grant grant;
        private final PaperGrantInventorySlotPlan plan;
        private final Map<String, Object> before;
        private final int compressedLimit;
        private final int inflatedLimit;
        private boolean attempted; // short custody monitor only; never held across IO
        private volatile SavedGrantCommitWorker.Ticket<SavedGrantDeliveryReadback.Proof> scheduled;
        private final java.util.concurrent.atomic.AtomicBoolean verifiedComplete =
                new java.util.concurrent.atomic.AtomicBoolean();
        private final java.util.concurrent.atomic.AtomicBoolean handedBack =
                new java.util.concurrent.atomic.AtomicBoolean();

        private SealedFirstBatch(WriterEpochFence fence, WriterEpochFence.Seal seal,
                WorldStorageDiscovery.Manifest original, Path playerdataFile,
                GrantLedger ledger, UUID player, GrantLedger.Grant grant,
                PaperGrantInventorySlotPlan plan, Map<String, Object> before,
                int compressedLimit, int inflatedLimit) {
            this.fence = fence;
            this.seal = seal;
            this.original = original;
            this.playerdataFile = playerdataFile;
            this.ledger = ledger;
            this.player = player;
            this.grant = grant;
            this.plan = plan;
            this.before = before;
            this.compressedLimit = compressedLimit;
            this.inflatedLimit = inflatedLimit;
        }

        /**
         * Phase two: must run off Paper's primary thread. Rediscovers the
         * authoritative source set strictly from already captured path/scope
         * inputs, not from off-thread Bukkit access or a caller-supplied path.
         * No source contains a mutable Paper object or Player reference.
         *
         * On success the caller still owns a SEALED epoch. A fully verified
         * complete grant can return the same fence for the next operation
         * on main, but partial history and full writer coverage are not
         * established here.
         */
        SavedGrantDeliveryReadback.Proof finishOffMain() throws IOException {
            return finish(false, null);
        }

        /** Product-owned, bounded worker path; must be scheduled on the primary thread. */
        synchronized SavedGrantCommitWorker.Ticket<SavedGrantDeliveryReadback.Proof> scheduleOffMain(
                SavedGrantCommitWorker worker, long timeoutMillis) {
            if (!Bukkit.isPrimaryThread())
                throw new IllegalStateException("Schedule saved commit on Paper primary thread");
            if (attempted) throw new IllegalStateException("Saved grant batch already attempted");
            attempted = true;
            // If submission fails, this ticket remains sealed and consumed. Never retry issuance.
            scheduled = Objects.requireNonNull(worker, "saved grant worker").submit(timeoutMillis,
                    live -> finishOwned(false, null, live), fence::invalidate);
            return scheduled;
        }

        /**
         * Return the same fence for subsequent work, once, after a verified
         * COMPLETE durable ACK. Never reopen before completion, while partial
         * history is unresolved, or while players have been admitted. Resuming
         * the writer generation is not an Admission permit: the enclosing
         * runtime still owns the external writer audit and normal journal.
         */
        void resumeAfterVerifiedCompleteOnMain() {
            var owned = scheduled;
            boolean claimed = false;
            try {
                if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty()
                        || !verifiedComplete.get()
                        || !handedBack.compareAndSet(false, true))
                    throw new IllegalStateException(
                            "Only one fully verified complete grant may return its sealed epoch on main");
                if (owned != null) {
                    owned.claimSuccess();
                    claimed = true;
                }
                fence.resume(seal);
            } catch (RuntimeException | Error failure) {
                // An in-flight ACK can hold the fence monitor. Do not wait for
                // it on primary: abandon custody, leave sealed, recover cold.
                if (owned != null && !claimed) owned.abandon();
                else fence.invalidate();
                throw failure;
            }
        }

        /** Unregistered legacy test-only synchronous callback compatibility. */
        private SavedGrantDeliveryReadback.Proof finishLegacyMainOnly(
                Callable<WorldStorageDiscovery.Manifest> discoverAfterSave)
                throws IOException {
            return finish(true, discoverAfterSave);
        }

        private SavedGrantDeliveryReadback.Proof finish(
                boolean legacyMainThread,
                Callable<WorldStorageDiscovery.Manifest> callback) throws IOException {
            synchronized (this) {
                if (attempted) {
                    if (scheduled == null) fence.invalidate();
                    else scheduled.abandon();
                    throw new IllegalStateException("Saved grant batch already attempted");
                }
                attempted = true;
            }
            return finishOwned(legacyMainThread, callback, () -> true);
        }

        private SavedGrantDeliveryReadback.Proof finishOwned(boolean legacyMainThread,
                Callable<WorldStorageDiscovery.Manifest> callback, BooleanSupplier live) throws IOException {
            try {
                if (Bukkit.isPrimaryThread() != legacyMainThread)
                    throw new IllegalStateException(
                            "Wrong execution thread for protected saved commit");
                requireLive(live);
                fence.requireDrained(seal);
                WorldStorageDiscovery.Manifest fresh;
                try {
                    if (legacyMainThread) {
                        fresh = Objects.requireNonNull(callback, "legacy test discovery").call();
                    } else {
                        WorldStorageDiscovery.DiscoveryOrigin source =
                                original.origin();
                        if (source == null)
                            throw new IOException("Saved grant lacks immutable origin");
                        fresh = WorldStorageDiscovery.discover(
                                source.worldFolders(), source.scope(),
                                source.maximumEntries(), source.sharedPlayerRoots());
                    }
                } catch (IOException failure) {
                    throw failure;
                } catch (Exception failure) {
                    throw new IOException("Fresh playerdata discovery failed after Paper save", failure);
                }
                if (fresh == null || !Objects.equals(original.origin(), fresh.origin())
                        || !playerdataFile.equals(fresh.playerDataFiles().get(player)))
                    throw new IOException("Protected grant saved source changed identity or origin");

                requireLive(live);
                SavedGrantDeliveryReadback.Proof proof = plan.nextReceipt().active()
                        ? SavedGrantProtectedPartialStage.verifyPartial(
                                fence, seal, fresh, ledger, player, grant, plan, before,
                                compressedLimit, inflatedLimit)
                        : SavedGrantProtectedAckStage.finish(
                                fence, seal, fresh, ledger, player, grant, plan, before,
                                compressedLimit, inflatedLimit, live);
                requireLive(live);
                fence.requireDrained(seal);
                // A receipt describes THIS grant, not every reservation for
                // its owner. Later authentic pending reservations must remain
                // pending and must not fault an earlier successful ACK.
                boolean targetStillPending = ledger.pending(player).contains(grant);
                if (plan.nextReceipt().active() != targetStillPending)
                    throw new IllegalStateException(
                            "Target grant acknowledgement differs from saved receipt");
                if (!plan.nextReceipt().active()) {
                    // The actual durable ACK was independently checked above.
                    ledger.removeAcknowledgedFirstBatchIntent(grant);
                    requireLive(live);
                    fence.requireDrained(seal);
                    verifiedComplete.set(true);
                }
                return proof;
            } catch (IOException | RuntimeException | Error failure) {
                fence.invalidate();
                throw failure;
            }
        }

        private static void requireLive(BooleanSupplier live) {
            if (!live.getAsBoolean()) throw new IllegalStateException("Saved grant custody expired or abandoned");
        }
    }
}
