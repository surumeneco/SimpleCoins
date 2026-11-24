package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.GrantDeliveryBatchPlanner;
import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * Read-only physical storage placement preflight for one authentic offline grant.
 * No player inventory/PDC mutation, playerdata saving or issuance takes place.
 * A later Paper main-thread commit must recapture every storage slot and PDC
 * immediately before use and durably save inventory + receipt in one generation.
 */
public final class PaperGrantInventorySlotPlan {
    public static final int STORAGE_SLOTS = 36;

    public record Slot(int index, CoinDenomination denomination, int count) {
        public Slot {
            if (index < 0 || index >= STORAGE_SLOTS)
                throw new IllegalArgumentException("Not a player storage slot: " + index);
            Objects.requireNonNull(denomination, "denomination");
            if (count < 1 || count > 64)
                throw new IllegalArgumentException("Coin count must fit one physical stack");
        }
    }

    /**
     * Immutable per-slot fingerprints; null means vacant. Production snapshots
     * must be captured from Paper's real getStorageContents(), not fabricated.
     * A pure value constructor permits JUnit preflight testing without a
     * running Bukkit Server/ItemFactory; its success is not a real Paper test.
     */
    public record StorageSnapshot(List<String> fingerprints) {
        public StorageSnapshot {
            Objects.requireNonNull(fingerprints, "storage fingerprints");
            if (fingerprints.size() != STORAGE_SLOTS)
                throw new IllegalArgumentException("Expected exactly 36 Paper storage slots");
            for (String fingerprint : fingerprints) {
                if (fingerprint != null && fingerprint.isBlank())
                    throw new IllegalArgumentException("Empty occupied-slot fingerprint");
            }
            fingerprints = Collections.unmodifiableList(new ArrayList<>(fingerprints));
        }

        /**
         * Paper-only: serialize full ItemStack bytes including item metadata,
         * PDC and count. Neither armor, offhand nor crafting slots are passed.
         */
        public static StorageSnapshot fromPaper(ItemStack[] storage) {
            Objects.requireNonNull(storage, "player storage contents");
            if (storage.length != STORAGE_SLOTS)
                throw new IllegalArgumentException("Expected exactly 36 Paper storage slots");
            List<String> fingerprints = new ArrayList<>(STORAGE_SLOTS);
            for (ItemStack item : storage) {
                if (item == null || item.getType().isAir()) {
                    fingerprints.add(null);
                } else {
                    try {
                        byte[] bytes = item.serializeAsBytes();
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        fingerprints.add(HexFormat.of().formatHex(digest.digest(bytes)));
                    } catch (NoSuchAlgorithmException impossible) {
                        throw new AssertionError(impossible);
                    }
                }
            }
            return new StorageSnapshot(fingerprints);
        }

        public boolean vacant(int index) {
            if (index < 0 || index >= STORAGE_SLOTS)
                throw new IllegalArgumentException("Not a storage slot");
            return fingerprints.get(index) == null;
        }
    }

    private final StorageSnapshot observedStorage;
    private final GrantReceiptState previousReceipt;
    private final GrantReceiptState nextReceipt;
    private final List<Slot> placements;

    private PaperGrantInventorySlotPlan(StorageSnapshot storage,
                                        GrantReceiptState previousReceipt,
                                        GrantReceiptState nextReceipt,
                                        List<Slot> placements) {
        this.observedStorage = storage;
        this.previousReceipt = previousReceipt;
        this.nextReceipt = nextReceipt;
        this.placements = List.copyOf(placements);
    }

    public GrantReceiptState previousReceipt() { return previousReceipt; }
    public GrantReceiptState nextReceipt() { return nextReceipt; }
    public List<Slot> placements() { return placements; }

    /** Physical Paper bridge; must be called on Paper's main thread. */
    public static Optional<PaperGrantInventorySlotPlan> stage(
            GrantLedger ledger, UUID player, GrantLedger.Grant grant,
            GrantReceiptState receipt, ItemStack[] storage) {
        return stage(ledger, player, grant, receipt, StorageSnapshot.fromPaper(storage));
    }

    /** Pure, non-mutating receipt+slot planning with a captured storage view. */
    public static Optional<PaperGrantInventorySlotPlan> stage(
            GrantLedger ledger, UUID player, GrantLedger.Grant grant,
            GrantReceiptState savedReceipt, StorageSnapshot storage) {
        Objects.requireNonNull(ledger, "issuance ledger");
        Objects.requireNonNull(player, "player UUID");
        Objects.requireNonNull(grant, "grant");
        Objects.requireNonNull(savedReceipt, "saved receipt");
        Objects.requireNonNull(storage, "storage snapshot");

        // Exactly one full durable journal scan for this read-only plan.
        // The saved playerdata/ACK path must independently verify current evidence.
        return stage(ledger.verifiedPlayerSnapshot(player), grant, savedReceipt, storage);
    }

    /** Pure planning using the very same independently verified saved receipt view. */
    static Optional<PaperGrantInventorySlotPlan> stage(
            GrantLedger.VerifiedPlayerSnapshot view, GrantLedger.Grant grant,
            GrantReceiptState savedReceipt, StorageSnapshot storage) {
        Objects.requireNonNull(view, "verified planning view");
        Objects.requireNonNull(grant, "grant");
        Objects.requireNonNull(savedReceipt, "saved receipt");
        Objects.requireNonNull(storage, "storage snapshot");
        savedReceipt.verify(view);
        GrantReceiptState current = savedReceipt.active()
                ? savedReceipt : savedReceipt.begin(view, grant);
        if (!view.pending().contains(grant))
            throw new IllegalStateException("Grant is not the authentic pending reservation");
        if (current.activeSequence() != grant.sequence()
                || !grant.id().equals(current.activeId()))
            throw new IllegalStateException("Active receipt identifies another grant");

        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < STORAGE_SLOTS; i++)
            if (storage.vacant(i)) empty.add(i);

        GrantDeliveryBatchPlanner.Batch batch =
                GrantDeliveryBatchPlanner.next(grant, current.partialValue(), empty.size());
        if (batch.stacks().isEmpty()) {
            if (batch.complete())
                throw new IllegalStateException("Complete amount still has an active partial receipt");
            return Optional.empty();
        }
        GrantReceiptState next = current.advance(view, grant, batch);
        if (batch.complete())
            next = next.complete(view, grant);
        List<Slot> slots = new ArrayList<>(batch.stacks().size());
        for (int i = 0; i < batch.stacks().size(); i++) {
            var coin = batch.stacks().get(i);
            slots.add(new Slot(empty.get(i), coin.denomination(), coin.amount()));
        }
        return Optional.of(new PaperGrantInventorySlotPlan(storage,
                savedReceipt, next, slots));
    }

    /** Reject any receipt or storage drift before a future physical commit. */
    public void requireUnchanged(GrantReceiptState current, StorageSnapshot storage) {
        if (!previousReceipt.equals(current))
            throw new IllegalStateException("Player grant receipt changed after staging");
        if (!observedStorage.equals(storage))
            throw new IllegalStateException("Player storage changed after grant preflight");
    }

    /** Main-thread helper for future commit implementations. */
    public void requireUnchanged(GrantReceiptState current, ItemStack[] actualStorage) {
        requireUnchanged(current, StorageSnapshot.fromPaper(actualStorage));
    }
}
