package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Normal Paper delivery: one target, one main-thread inventory/save/readback
 * transaction. Receipt and physical items share the same native playerdata.
 * The saved receipt is the recovery cursor; only new confirmed batches are
 * passed to normal accounting. No native writer Agent or global admission seal.
 */
public final class PaperPlayerGrantDelivery {
    private static final int COMPRESSED = 16 * 1024 * 1024;
    private static final int INFLATED = 64 * 1024 * 1024;
    private final GrantLedger ledger;
    private final CoinItemCodec coins;
    private final BiConsumer<UUID, Long> issued;
    private final Set<UUID> blocked = new HashSet<>();

    public record Result(long deliveredNow, int pendingCount) {}

    public PaperPlayerGrantDelivery(GrantLedger ledger, CoinItemCodec coins,
            BiConsumer<UUID, Long> issued) {
        this.ledger = Objects.requireNonNull(ledger);
        this.coins = Objects.requireNonNull(coins);
        this.issued = Objects.requireNonNull(issued);
    }

    public boolean isBlocked(UUID id) { return blocked.contains(id); }

    public Result deliver(Player player) throws IOException {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Paper main thread required");
        UUID id = player.getUniqueId();
        if (blocked.contains(id)) throw new IOException("Automatic delivery held for " + id);
        try {
            return deliverPending(player, id);
        } catch (IOException | RuntimeException failure) {
            blocked.add(id);
            throw failure;
        }
    }

    private Result deliverPending(Player player, UUID id) throws IOException {
        var pending = ledger.pending(id);
        if (pending.isEmpty()) return new Result(0, 0);
        GrantReceiptState receipt = PaperGrantReceiptPdc.read(player.getPersistentDataContainer());
        receipt.verify(ledger, id);
        Path file = playerdata(id);
        if (file == null) {
            if (!receipt.equals(GrantReceiptState.empty()))
                throw new IOException("Existing receipt has no saved playerdata");
            player.saveData();
            file = playerdata(id);
            if (file == null) throw new IOException("Paper did not create playerdata");
        }
        var saved = SavedGrantReceiptNbt.readAndVerify(PlayerDataReader.read(file, COMPRESSED, INFLATED),
                ledger, id);
        if (!saved.equals(receipt)) throw new IOException("Live and saved grant receipts disagree");
        long total = 0;
        while (!pending.isEmpty()) {
            var grant = pending.getFirst();
            if (receipt.completedSequence() == grant.sequence()) {
                if (GrantDeliveryDecision.recover(new GrantDeliveryDecision.Evidence(true, true, false))
                        != GrantDeliveryDecision.Verdict.ALLOW_ACK)
                    throw new IllegalStateException("Recovered receipt not eligible");
                // These coins were already present in the initial settlement
                // or previously accounted for; recovery is NOT new issuance.
                ledger.confirmSavedProgress(grant, receipt);
                pending = ledger.pending(id);
                continue;
            }
            if (receipt.active() && receipt.partialValue() > grant.confirmedPartial()) {
                ledger.confirmSavedProgress(grant, receipt);
                pending = ledger.pending(id);
                grant = pending.getFirst();
            }
            var before = player.getInventory().getStorageContents();
            var plan = PaperGrantInventorySlotPlan.stage(ledger, id, grant, receipt, before);
            if (plan.isEmpty()) break;
            var staged = plan.orElseThrow();
            var next = PaperGrantInventoryMaterializer.buildCopy(staged, receipt, before, coins);
            SavedGrantDeliveryReadback.Proof proof;
            try {
                player.getInventory().setStorageContents(next);
                PaperGrantReceiptPdc.write(player.getPersistentDataContainer(), staged.nextReceipt());
                player.saveData();
                // SaveData returns void, including on a failed native save.
                // Read this UUID only, in the same main-thread transaction.
                if (!file.equals(playerdata(id))) throw new IOException("Playerdata source changed");
                proof = SavedGrantDeliveryReadback.verify(
                        PlayerDataReader.read(file, COMPRESSED, INFLATED), ledger, id, staged);
            } catch (IOException | RuntimeException failure) {
                // No player can use this batch before the synchronous callback
                // returns. Keep the reservation and restore the live prior view;
                // the next startup reconciles whichever receipt was actually saved.
                player.getInventory().setStorageContents(before);
                PaperGrantReceiptPdc.write(player.getPersistentDataContainer(), receipt);
                throw failure;
            }
            long value = proof.matchedMonetaryValue().longValueExact();
            // After physical proof, never roll back a batch that may be visible
            // or already journaled. A later error holds only this target.
            issued.accept(id, value);
            total = Math.addExact(total, value);
            receipt = proof.receipt();
            boolean complete = receipt.completedSequence() == grant.sequence();
            var verdict = GrantDeliveryDecision.verify(new GrantDeliveryDecision.Evidence(true, complete, false));
            if (complete ? verdict != GrantDeliveryDecision.Verdict.ALLOW_ACK
                    : verdict != GrantDeliveryDecision.Verdict.KEEP_PENDING)
                throw new IllegalStateException("Unexpected saved delivery decision");
            ledger.confirmSavedProgress(grant, receipt);
            pending = ledger.pending(id);
            if (!complete) break;
        }
        return new Result(total, pending.size());
    }

    /** Resolve only the selected UUID, using Paper's actual registered world paths.
     * Paper 26.2 dimensions are under level/dimensions/namespace/dimension.
     */
    public static Path playerdata(UUID id) throws IOException {
        Set<Path> found = new HashSet<>();
        Set<Path> roots = new HashSet<>();
        for (World world : Bukkit.getWorlds()) {
            Path path = world.getWorldPath().toAbsolutePath().normalize();
            roots.add(path);
            Path namespace = path.getParent();
            Path dimensions = namespace == null ? null : namespace.getParent();
            if (dimensions != null && dimensions.getFileName().toString().equals("dimensions")
                    && dimensions.getParent() != null) roots.add(dimensions.getParent());
        }
        for (Path root : roots) {
            for (Path directory : List.of(root.resolve("players/data"), root.resolve("playerdata"))) {
                Path file = directory.resolve(id + ".dat");
                if (Files.exists(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                    if (!Files.isRegularFile(file, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                        throw new IOException("Invalid playerdata source: " + file);
                    found.add(file);
                }
            }
        }
        if (found.size() > 1) throw new IOException("Ambiguous saved playerdata for " + id);
        return found.stream().findFirst().orElse(null);
    }
}
