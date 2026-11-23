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
     * Physically place one authentic pending grant batch, persist real Paper
     * playerdata, and perform the correct protected saved proof and ACK/no-ACK
     * using the SAME externally installed, previously OPEN writer generation.
     *
     * <p>Returns empty only when no storage slot is available, without touching
     * the player or advancing receipt/journal. Any exception faults this epoch:
     * the saved outcome may be indeterminate and the caller must cold-recover
     * from genuine saved NBT and journal, never repeat issuance blindly.</p>
     *
     * <p>This synchronous entrypoint is currently exercised only with an
     * isolated detached player and tiny disposable saved source. Large-world
     * runtime scheduling must split main-thread Bukkit save from bounded async
     * disk verification/drain; do not call it in normal player admission.</p>
     */
    static Optional<SavedGrantDeliveryReadback.Proof> saveAndFinalizeFirstBatch(
            Player detached, GrantLedger ledger, GrantLedger.Grant grant,
            CoinItemCodec coins, WriterEpochFence fence,
            WorldStorageDiscovery.Manifest beforeManifest,
            Callable<WorldStorageDiscovery.Manifest> discoverAfterSave,
            int maximumCompressedBytes, int maximumInflatedBytes) throws IOException {
        Objects.requireNonNull(fence, "externally audited writer fence");
        try {
            Objects.requireNonNull(detached, "real Paper detached player");
            Objects.requireNonNull(ledger, "durable grant ledger");
            Objects.requireNonNull(grant, "authentic reservation");
            Objects.requireNonNull(coins, "canonical coin codec");
            Objects.requireNonNull(beforeManifest, "original source discovery");
            Objects.requireNonNull(discoverAfterSave, "new authoritative discovery");
            if (!Bukkit.isPrimaryThread() || detached.isOnline()
                    || !Bukkit.getOnlinePlayers().isEmpty())
                throw new IllegalStateException(
                        "Saved grant staging requires primary thread, detached player and zero admissions");
            if (fence.state() != WriterEpochFence.State.OPEN)
                throw new IllegalStateException("Grant save requires already OPEN tracked writer epoch");
            if (maximumCompressedBytes < 1 || maximumInflatedBytes < 1)
                throw new IllegalArgumentException("Nonpositive saved playerdata read budget");

            UUID player = detached.getUniqueId();
            if (!player.equals(grant.player()) || grant.delivered()
                    || grant.denomination() == null
                    || !ledger.pending(player).contains(grant))
                throw new IllegalStateException("Not a genuine pending typed grant");
            // A full-world manifest made up by a caller is insufficient.
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
                // No new physical money, receipt or journal ACK on a full inventory.
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

            // The ONLY mutation in this coordinator, and the real Paper
            // persistence call. The void return does not mean durability.
            detached.getInventory().setStorageContents(candidate);
            PaperGrantReceiptPdc.write(
                    detached.getPersistentDataContainer(), plan.nextReceipt());
            detached.saveData();

            // No point-in-time file hash or event callback may authorize ACK.
            // From here on new roots must be excluded, all admitted work
            // completed, and file/dir fsync + physical readback guarded until
            // the actual durable ledger commit returns.
            WriterEpochFence.Seal seal = fence.seal();
            fence.requireDrained(seal);
            WorldStorageDiscovery.Manifest afterManifest;
            try {
                afterManifest = discoverAfterSave.call();
            } catch (Exception external) {
                throw new IOException("Authoritative discovery failed after Paper save", external);
            }
            if (afterManifest == null || afterManifest.origin() == null
                    || !file.equals(afterManifest.playerDataFiles().get(player)))
                throw new IOException("Paper saved playerdata source changed identity");

            SavedGrantDeliveryReadback.Proof proof = plan.nextReceipt().active()
                    ? SavedGrantProtectedPartialStage.verifyPartial(
                            fence, seal, afterManifest, ledger, player, grant,
                            plan, savedBefore, maximumCompressedBytes, maximumInflatedBytes)
                    : SavedGrantProtectedAckStage.finish(
                            fence, seal, afterManifest, ledger, player, grant,
                            plan, savedBefore, maximumCompressedBytes, maximumInflatedBytes);
            fence.requireDrained(seal);
            if (plan.nextReceipt().active() == ledger.pending(player).isEmpty())
                throw new IllegalStateException(
                        "Partial/complete grant acknowledgement differs from saved receipt");
            return Optional.of(proof);
        } catch (IOException | RuntimeException | Error failure) {
            // A failed save/readback/ACK is not a permission to roll back
            // only the in-memory half and then issue another batch.
            fence.invalidate();
            throw failure;
        }
    }
}
