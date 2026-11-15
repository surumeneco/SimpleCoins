package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.GrantDeliveryBatchPlanner;
import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * Read-only physical storage placement stage for an authenticated offline grant.
 *
 * Paper PlayerInventory.getStorageContents() exposes 36 inventory/hotbar
 * slots; armor, crafting and offhand are never a placement destination.
 * The returned value does not mutate an ItemStack, PDC or playerdata, and is
 * NOT a durable inventory+receipt transaction. A later controlled main-thread
 * adapter must recheck the snapshot immediately before any mutation and must
 * persist inventory plus receipt in the same verified playerdata generation.
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

    private final ItemStack[] observedStorage;
    private final GrantReceiptState previousReceipt;
    private final GrantReceiptState nextReceipt;
    private final List<Slot> placements;

    private PaperGrantInventorySlotPlan(ItemStack[] storage,
                                        GrantReceiptState previousReceipt,
                                        GrantReceiptState nextReceipt,
                                        List<Slot> placements) {
        observedStorage = copyStorage(storage);
        this.previousReceipt = previousReceipt;
        this.nextReceipt = nextReceipt;
        this.placements = List.copyOf(placements);
    }

    public GrantReceiptState previousReceipt() { return previousReceipt; }
    public GrantReceiptState nextReceipt() { return nextReceipt; }
    public List<Slot> placements() { return placements; }

    /**
     * Builds the next whole-stack delivery and matching receipt without
     * writing or counting money. Empty means there is no physical space for
     * any *new* stack; the original durable reservation remains unchanged.
     */
    public static Optional<PaperGrantInventorySlotPlan> stage(
            GrantLedger ledger, UUID player, GrantLedger.Grant grant,
            GrantReceiptState savedReceipt, ItemStack[] storage) {
        Objects.requireNonNull(ledger, "issuance ledger");
        Objects.requireNonNull(player, "player UUID");
        Objects.requireNonNull(grant, "grant");
        Objects.requireNonNull(savedReceipt, "saved receipt");
        requireStorage(storage);

        savedReceipt.verify(ledger, player);
        GrantReceiptState current = savedReceipt.active()
                ? savedReceipt : savedReceipt.begin(ledger, player, grant);

        // Check the exact immutable journal reservation even for an empty
        // inventory: no caller-forged denomination/amount can become a plan.
        if (!ledger.pending(player).contains(grant))
            throw new IllegalStateException("Grant is not the authentic pending reservation");
        if (current.activeSequence() != grant.sequence()
                || !grant.id().equals(current.activeId()))
            throw new IllegalStateException("Active receipt identifies another grant");

        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            if (isEmpty(storage[i])) empty.add(i);
        }
        GrantDeliveryBatchPlanner.Batch batch =
                GrantDeliveryBatchPlanner.next(grant, current.partialValue(), empty.size());
        if (batch.stacks().isEmpty()) {
            if (batch.complete())
                throw new IllegalStateException("Complete amount still has an active partial receipt");
            return Optional.empty();
        }

        GrantReceiptState next = current.advance(ledger, player, grant, batch);
        if (batch.complete())
            next = next.complete(ledger, player, grant);
        List<Slot> slots = new ArrayList<>(batch.stacks().size());
        for (int i = 0; i < batch.stacks().size(); i++) {
            var coin = batch.stacks().get(i);
            slots.add(new Slot(empty.get(i), coin.denomination(), coin.amount()));
        }
        return Optional.of(new PaperGrantInventorySlotPlan(storage,
                savedReceipt, next, slots));
    }

    /**
     * Mandatory time-of-use precondition for a future Paper commit. A change
     * to ANY player storage slot or to the PDC receipt invalidates the plan,
     * even if the intended target slots happen to remain empty.
     */
    public void requireUnchanged(GrantReceiptState currentReceipt, ItemStack[] currentStorage) {
        if (!previousReceipt.equals(currentReceipt))
            throw new IllegalStateException("Player grant receipt changed after staging");
        requireStorage(currentStorage);
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            if (!Objects.equals(observedStorage[i], currentStorage[i]))
                throw new IllegalStateException(
                        "Player storage changed after grant preflight at slot " + i);
        }
    }

    private static ItemStack[] copyStorage(ItemStack[] storage) {
        ItemStack[] clone = new ItemStack[STORAGE_SLOTS];
        for (int i = 0; i < STORAGE_SLOTS; i++)
            clone[i] = storage[i] == null ? null : storage[i].clone();
        return clone;
    }

    private static void requireStorage(ItemStack[] storage) {
        Objects.requireNonNull(storage, "player storage contents");
        if (storage.length != STORAGE_SLOTS)
            throw new IllegalArgumentException("Expected exactly 36 Paper player storage slots");
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
