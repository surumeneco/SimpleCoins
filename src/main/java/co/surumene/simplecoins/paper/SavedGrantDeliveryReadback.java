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


    /**
     * Stronger offline-grant readback: compare two decoded playerdata roots
     * from the same authenticated UUID. Existing physical items must survive
     * unchanged; only previously vacant, staged coin slots may appear.
     *
     * This is a read-only logical transition proof. It does not prove that
     * either NBT root is authoritative/current, that Paper saveData finished,
     * or that a crash cannot replace the most recently observed bytes.
     */
    public static Proof verifyTransition(Map<String, Object> before,
                                         Map<String, Object> after,
                                         GrantLedger ledger, UUID expectedPlayer,
                                         PaperGrantInventorySlotPlan staged)
            throws IOException {
        if (before == null || after == null || ledger == null
                || expectedPlayer == null || staged == null)
            throw new IOException("Incomplete before/after saved grant transition");
        GrantReceiptState original = SavedGrantReceiptNbt.readAndVerify(
                before, ledger, expectedPlayer);
        if (!original.equals(staged.previousReceipt()))
            throw new IOException("Saved prior receipt differs from the staged precondition");

        // Extraction also verifies physical slot uniqueness and every nested
        // monetary marker. Neither the old inventory nor the new one is
        // allowed to conceal an uncountable counterfeit wallet/coin.
        SavedPlayerAssets.extract(before);
        Proof next = verify(after, ledger, expectedPlayer, staged);
        Map<Integer, Map<?, ?>> oldInventory = inventoryBySlot(before);
        Map<Integer, Map<?, ?>> newInventory = inventoryBySlot(after);
        java.util.Set<Integer> granted = new java.util.HashSet<>();
        for (PaperGrantInventorySlotPlan.Slot placement : staged.placements()) {
            if (!granted.add(placement.index()))
                throw new IOException("Repeated physical destination in grant placement");
            if (oldInventory.containsKey(placement.index()))
                throw new IOException("Grant destination was occupied before delivery: "
                        + placement.index());
        }
        java.util.Set<Integer> allSlots = new java.util.HashSet<>(oldInventory.keySet());
        allSlots.addAll(newInventory.keySet());
        for (Integer slot : allSlots) {
            if (granted.contains(slot)) continue;
            if (!sameNbt(oldInventory.get(slot), newInventory.get(slot)))
                throw new IOException("An unplanned saved inventory slot changed: " + slot);
        }
        if (!sameNbt(before.get("EnderItems"), after.get("EnderItems"))
                || !sameNbt(before.get("equipment"), after.get("equipment")))
            throw new IOException("An unplanned Ender Chest or equipment asset changed");
        return next;
    }

    /** Compare decoded NBT, including primitive payload arrays by value. */
    private static boolean sameNbt(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a instanceof byte[] ba && b instanceof byte[] bb)
            return java.util.Arrays.equals(ba, bb);
        if (a instanceof int[] ia && b instanceof int[] ib)
            return java.util.Arrays.equals(ia, ib);
        if (a instanceof long[] la && b instanceof long[] lb)
            return java.util.Arrays.equals(la, lb);
        if (a instanceof Map<?, ?> ma && b instanceof Map<?, ?> mb) {
            if (!ma.keySet().equals(mb.keySet())) return false;
            for (var entry : ma.entrySet()) {
                if (!sameNbt(entry.getValue(), mb.get(entry.getKey()))) return false;
            }
            return true;
        }
        if (a instanceof List<?> la && b instanceof List<?> lb) {
            if (la.size() != lb.size()) return false;
            for (int i = 0; i < la.size(); i++) {
                if (!sameNbt(la.get(i), lb.get(i))) return false;
            }
            return true;
        }
        return a.equals(b);
    }

    private static Map<Integer, Map<?, ?>> inventoryBySlot(Map<String, Object> root)
            throws IOException {
        if (!(root.get("Inventory") instanceof List<?> inventory))
            throw new IOException("Missing saved player storage for transition");
        Map<Integer, Map<?, ?>> slots = new HashMap<>();
        for (Object stack : inventory) {
            if (!(stack instanceof Map<?, ?> item)
                    || !(item.get("Slot") instanceof Number number))
                throw new IOException("Invalid saved physical inventory item");
            int index = number.intValue();
            if (slots.putIfAbsent(index, item) != null)
                throw new IOException("Duplicate saved inventory slot: " + index);
        }
        return slots;
    }

    private static void requireOriginalCoinIdentity(Map<?, ?> item,
                                                    PaperGrantInventorySlotPlan.Slot slot)
            throws IOException {
        if (!"minecraft:paper".equals(item.get("id")))
            throw new IOException("Saved grant output is not PAPER");
        Object rawCount = item.containsKey("count") ? item.get("count") : Integer.valueOf(1);
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
