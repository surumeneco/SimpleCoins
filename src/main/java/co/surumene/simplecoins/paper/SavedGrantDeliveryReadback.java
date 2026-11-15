package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.SavedPlayerAssets;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Read-only, same-root saved playerdata observation of one *already staged*
 * offline grant delivery. This requires both the receipt and the exact
 * physical coin representations in the same decoded player NBT generation.
 *
 * Neither a matched saved root nor a checksum proves that Paper.saveData()
 * was synchronous/atomic, that the source remained unchanged after read, or
 * that the owner could not move coins after saving. This is NOT an issuing,
 * acknowledgement, startup publication, or admission path.
 */
public final class SavedGrantDeliveryReadback {
    private SavedGrantDeliveryReadback() { }

    public record Proof(GrantReceiptState receipt, int matchedStacks,
                        BigInteger matchedMonetaryValue) {
        public Proof {
            Objects.requireNonNull(receipt, "saved receipt");
            Objects.requireNonNull(matchedMonetaryValue, "saved value");
            if (matchedStacks < 0 || matchedMonetaryValue.signum() < 0)
                throw new IllegalArgumentException("Invalid saved delivery proof");
        }
    }

    public static Proof verify(Map<String, Object> oneSavedPlayerdataRoot,
                               GrantLedger journal, UUID expectedPlayer,
                               PaperGrantInventorySlotPlan staged) throws IOException {
        if (oneSavedPlayerdataRoot == null || journal == null
                || expectedPlayer == null || staged == null)
            throw new IOException("Missing authoritative saved delivery proof input");

        // Receipt and physical items are read from this SAME NBT object.
        GrantReceiptState saved = SavedGrantReceiptNbt.readAndVerify(
                oneSavedPlayerdataRoot, journal, expectedPlayer);
        if (!saved.equals(staged.nextReceipt()))
            throw new IOException("Saved receipt differs from the staged delivery progress");

        // Full player + Ender Chest + equipment reading rejects invalid
        // physical slot data, duplicate slots, counterfeit currency markers,
        // cloned wallet IDs and malformed nested assets. It does not mutate.
        Map<String, BigInteger> monetarySlots =
                SavedPlayerAssets.extract(oneSavedPlayerdataRoot);
        Object rawInventory = oneSavedPlayerdataRoot.get("Inventory");
        if (!(rawInventory instanceof List<?> inventory))
            throw new IOException("Missing or malformed saved player Inventory");

        Map<Integer, Map<?, ?>> bySlot = new HashMap<>();
        for (Object raw : inventory) {
            if (!(raw instanceof Map<?, ?> item))
                throw new IOException("Invalid saved player inventory item");
            Object rawSlot = item.get("Slot");
            if (!(rawSlot instanceof Byte || rawSlot instanceof Short
                    || rawSlot instanceof Integer || rawSlot instanceof Long))
                throw new IOException("Missing saved player physical slot");
            long slotIndex = ((Number) rawSlot).longValue();
            if (slotIndex >= 0 && slotIndex < PaperGrantInventorySlotPlan.STORAGE_SLOTS) {
                if (bySlot.putIfAbsent((int) slotIndex, item) != null)
                    throw new IOException("Duplicate saved player storage slot");
            }
        }

        BigInteger matchedValue = BigInteger.ZERO;
        for (PaperGrantInventorySlotPlan.Slot slot : staged.placements()) {
            Map<?, ?> item = bySlot.get(slot.index());
            if (item == null)
                throw new IOException("Planned issued coin is absent at saved storage slot "
                        + slot.index());
            requireOriginalCoinIdentity(item, slot);
            BigInteger expectedValue = BigInteger.valueOf(slot.denomination().value())
                    .multiply(BigInteger.valueOf(slot.count()));
            if (!expectedValue.equals(monetarySlots.get("inventory:" + slot.index())))
                throw new IOException("Saved monetary value differs from planned coin at slot "
                        + slot.index());
            matchedValue = matchedValue.add(expectedValue);
        }
        return new Proof(saved, staged.placements().size(), matchedValue);
    }

    private static void requireOriginalCoinIdentity(Map<?, ?> item,
                                                    PaperGrantInventorySlotPlan.Slot slot)
            throws IOException {
        if (!"minecraft:paper".equals(item.get("id")))
            throw new IOException("Saved grant output is not PAPER");
        Object rawCount = item.getOrDefault("count", Integer.valueOf(1));
        if (!(rawCount instanceof Byte || rawCount instanceof Short
                || rawCount instanceof Integer || rawCount instanceof Long)
                || ((Number) rawCount).longValue() != slot.count())
            throw new IOException("Saved grant coin count differs from planned stack");
        if (!(item.get("components") instanceof Map<?, ?> components)
                || !("simplecoins:coin_" + slot.denomination().value()).equals(
                        components.get("minecraft:item_model")))
            throw new IOException("Saved grant coin item_model is inconsistent");
        if (!(components.get("minecraft:custom_data") instanceof Map<?, ?> customData)
                || !(customData.get("PublicBukkitValues") instanceof Map<?, ?> pdc)
                || !(pdc.get("simplecoins:coin_denomination") instanceof Integer denomination)
                || denomination.intValue() != slot.denomination().value())
            throw new IOException("Saved grant coin PDC differs from planned denomination");
    }
}
