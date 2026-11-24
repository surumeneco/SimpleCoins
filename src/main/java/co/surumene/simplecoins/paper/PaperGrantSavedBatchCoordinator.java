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
     * Existing synchronous internal integration contract. Real runtime code
     * MUST use saveAndSealFirstBatchOnMain() and finishOffMain(): discovery,
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
     * Phase one: only the real detached CraftPlayer's PDC/Inventory changes
     * and Paper's own saveData() require the Paper primary thread. This
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
        private final java.util.concurrent.atomic.AtomicBoolean attempted =
                new java.util.concurrent.atomic.AtomicBoolean();
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

        /**
         * Return the same fence for subsequent work, once, after a verified
         * COMPLETE durable ACK. Never reopen before completion, while partial
         * history is unresolved, or while players have been admitted. Resuming
         * the writer generation is not an Admission permit: the enclosing
         * runtime still owns the external writer audit and normal journal.
         */
        void resumeAfterVerifiedCompleteOnMain() {
            try {
                if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty()
                        || !verifiedComplete.get()
                        || !handedBack.compareAndSet(false, true))
                    throw new IllegalStateException(
                            "Only one fully verified complete grant may return its sealed epoch on main");
                fence.resume(seal);
            } catch (RuntimeException | Error failure) {
                fence.invalidate();
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
            // Both successful completion and ambiguous failure are terminal.
            // An accidental second caller may not redo the ACK from old NBT.
            if (!attempted.compareAndSet(false, true)) {
                fence.invalidate();
                throw new IllegalStateException("Saved grant batch already attempted");
            }
            try {
                if (Bukkit.isPrimaryThread() != legacyMainThread)
                    throw new IllegalStateException(
                            "Wrong execution thread for protected saved commit");
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

                SavedGrantDeliveryReadback.Proof proof = plan.nextReceipt().active()
                        ? SavedGrantProtectedPartialStage.verifyPartial(
                                fence, seal, fresh, ledger, player, grant, plan, before,
                                compressedLimit, inflatedLimit)
                        : SavedGrantProtectedAckStage.finish(
                                fence, seal, fresh, ledger, player, grant, plan, before,
                                compressedLimit, inflatedLimit);
                fence.requireDrained(seal);
                // A receipt describes THIS grant, not every reservation for
                // its owner. Later authentic pending reservations must remain
                // pending and must not fault an earlier successful ACK.
                boolean targetStillPending = ledger.pending(player).contains(grant);
                if (plan.nextReceipt().active() != targetStillPending)
                    throw new IllegalStateException(
                            "Target grant acknowledgement differs from saved receipt");
                if (!plan.nextReceipt().active()) verifiedComplete.set(true);
                return proof;
            } catch (IOException | RuntimeException | Error failure) {
                fence.invalidate();
                throw failure;
            }
        }
    }
}
