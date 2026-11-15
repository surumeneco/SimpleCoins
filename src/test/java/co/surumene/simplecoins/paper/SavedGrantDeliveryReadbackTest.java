package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.PlayerDataReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SavedGrantDeliveryReadbackTest {
    @TempDir Path temp;

    private static PaperGrantInventorySlotPlan.StorageSnapshot freeSlots(int count) {
        List<String> names = new ArrayList<>(Collections.nCopies(36, "ordinary-item"));
        for (int i = 0; i < count; i++) names.set(i, null);
        return new PaperGrantInventorySlotPlan.StorageSnapshot(names);
    }

    private static Map<String, Object> coin(int slot, CoinDenomination denom, int count) {
        return Map.of(
                "Slot", Byte.valueOf((byte) slot),
                "id", "minecraft:paper",
                "count", Integer.valueOf(count),
                "components", Map.of(
                        "minecraft:item_model", "simplecoins:coin_" + denom.value(),
                        "minecraft:custom_data", Map.of(
                                "PublicBukkitValues", Map.of(
                                        "simplecoins:coin_denomination", Integer.valueOf(denom.value())))));
    }

    private static Map<String, Object> saved(
            GrantReceiptState receipt, List<Map<String, Object>> slots) {
        return Map.of(
                "BukkitValues", Map.of("simplecoins:grant_receipt_v1",
                        PaperGrantReceiptPdc.toSavedBytes(receipt)),
                "Inventory", slots,
                "EnderItems", List.of());
    }

    @Test void full5200CRequiresExactPhysicalCoinCountAndReceiptFromSameRoot() throws Exception {
        var ledger = new GrantLedger(temp.resolve("full"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5200, "console");
        var plan = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), freeSlots(2)).orElseThrow();
        var items = List.of(coin(0, CoinDenomination.FIVE_THOUSAND, 1),
                coin(1, CoinDenomination.HUNDRED, 2));
        var proof = SavedGrantDeliveryReadback.verify(saved(plan.nextReceipt(), items),
                ledger, player, plan);
        assertEquals(plan.nextReceipt(), proof.receipt());
        assertEquals(2, proof.matchedStacks());
        assertEquals(BigInteger.valueOf(5200), proof.matchedMonetaryValue());
        assertEquals(1, ledger.pending(player).size(),
                "Saved readback must not acknowledge the durable journal");
    }

    @Test void partial5000CAndUnrelatedStoredAssetsAreReadOnly() throws Exception {
        var ledger = new GrantLedger(temp.resolve("partial"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5200, "console");
        var plan = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), freeSlots(1)).orElseThrow();
        List<Map<String, Object>> items = List.of(
                coin(0, CoinDenomination.FIVE_THOUSAND, 1),
                coin(3, CoinDenomination.FIVE, 1));
        var proof = SavedGrantDeliveryReadback.verify(saved(plan.nextReceipt(), items),
                ledger, player, plan);
        assertEquals(1, proof.matchedStacks());
        assertEquals(BigInteger.valueOf(5000), proof.matchedMonetaryValue());
        assertEquals(5000, proof.receipt().partialValue());
        assertEquals(2, items.size());
        assertEquals(1, ledger.pending(player).size());
    }

    @Test void equivalentValueAtWrongDenominationOrCountIsNotProof() throws Exception {
        var ledger = new GrantLedger(temp.resolve("identity"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 200, "console");
        var plan = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), freeSlots(1)).orElseThrow();
        // Both are worth 200C, but only 100C x2 is the original grant.
        assertThrows(IOException.class, () -> SavedGrantDeliveryReadback.verify(
                saved(plan.nextReceipt(), List.of(coin(0, CoinDenomination.FIFTY, 4))),
                ledger, player, plan));
        assertThrows(IOException.class, () -> SavedGrantDeliveryReadback.verify(
                saved(plan.nextReceipt(), List.of(coin(0, CoinDenomination.HUNDRED, 1))),
                ledger, player, plan));
        assertThrows(IOException.class, () -> SavedGrantDeliveryReadback.verify(
                saved(plan.nextReceipt(), List.of()), ledger, player, plan));
    }

    @Test void incorrectModelPdcAndDuplicateSlotsAreRejected() throws Exception {
        var ledger = new GrantLedger(temp.resolve("tamper"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5000, "console");
        var plan = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), freeSlots(1)).orElseThrow();

        Map<String, Object> mismatchedModel = new HashMap<>(
                coin(0, CoinDenomination.FIVE_THOUSAND, 1));
        mismatchedModel.put("components", Map.of(
                "minecraft:item_model", "simplecoins:coin_1",
                "minecraft:custom_data", Map.of(
                        "PublicBukkitValues", Map.of("simplecoins:coin_denomination", 5000))));
        assertThrows(IOException.class, () -> SavedGrantDeliveryReadback.verify(
                saved(plan.nextReceipt(), List.of(mismatchedModel)), ledger, player, plan));

        Map<String, Object> wrongPdc = new HashMap<>(
                coin(0, CoinDenomination.FIVE_THOUSAND, 1));
        wrongPdc.put("components", Map.of(
                "minecraft:item_model", "simplecoins:coin_5000",
                "minecraft:custom_data", Map.of(
                        "PublicBukkitValues", Map.of("simplecoins:coin_denomination", 1))));
        assertThrows(IOException.class, () -> SavedGrantDeliveryReadback.verify(
                saved(plan.nextReceipt(), List.of(wrongPdc)), ledger, player, plan));

        var same = coin(0, CoinDenomination.FIVE_THOUSAND, 1);
        assertThrows(IOException.class, () -> SavedGrantDeliveryReadback.verify(
                saved(plan.nextReceipt(), List.of(same, same)), ledger, player, plan));
    }

    @Test void forgedOrMissingReceiptAndForeignOwnerFailClosed() throws Exception {
        var ledger = new GrantLedger(temp.resolve("receipt"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5000, "console");
        var plan = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), freeSlots(1)).orElseThrow();
        var coin = List.of(coin(0, CoinDenomination.FIVE_THOUSAND, 1));
        assertThrows(IOException.class, () -> SavedGrantDeliveryReadback.verify(
                saved(GrantReceiptState.empty(), coin), ledger, player, plan));
        assertThrows(IOException.class, () -> SavedGrantDeliveryReadback.verify(
                Map.of("Inventory", coin, "EnderItems", List.of()), ledger, player, plan));
        assertThrows(IOException.class, () -> SavedGrantDeliveryReadback.verify(
                saved(plan.nextReceipt(), coin), ledger, UUID.randomUUID(), plan));
    }


    private static Map<String, Object> nonCurrency(int slot, int amount, byte[] data) {
        return Map.of(
                "Slot", Byte.valueOf((byte) slot),
                "id", "minecraft:diamond",
                "count", Integer.valueOf(amount),
                "components", Map.of("minecraft:custom_data", Map.of("marker", data)));
    }

    private static Map<String, Object> initialSaved(List<Map<String, Object>> inventory) {
        return saved(GrantReceiptState.empty(), inventory);
    }

    @Test void transitionPreservesExistingSlotsAndUnrelatedCustomData() throws Exception {
        var ledger = new GrantLedger(temp.resolve("preserved"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5000, "console");
        var plan = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), freeSlots(1)).orElseThrow();
        var original = nonCurrency(9, 2, new byte[] {7, 8, 9});
        var before = initialSaved(List.of(original));
        var after = saved(plan.nextReceipt(), List.of(
                coin(0, CoinDenomination.FIVE_THOUSAND, 1),
                nonCurrency(9, 2, new byte[] {7, 8, 9})));
        var proof = SavedGrantDeliveryReadback.verifyTransition(
                before, after, ledger, player, plan);
        assertEquals(BigInteger.valueOf(5000), proof.matchedMonetaryValue());
        assertEquals(1, ledger.pending(player).size());
    }

    @Test void transitionRejectsLossMutationAndUnexpectedNewAssets() throws Exception {
        var ledger = new GrantLedger(temp.resolve("preservation-failure"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5000, "console");
        var plan = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), freeSlots(1)).orElseThrow();
        var original = nonCurrency(9, 2, new byte[] {7, 8, 9});
        var before = initialSaved(List.of(original));
        var planned = coin(0, CoinDenomination.FIVE_THOUSAND, 1);
        // The prior verify() accepts these despite loss/alteration elsewhere.
        assertEquals(1, SavedGrantDeliveryReadback.verify(
                saved(plan.nextReceipt(), List.of(planned)), ledger, player, plan).matchedStacks());

        var invalid = List.of(
                List.of(planned), // lost unrelated diamond
                List.of(planned, nonCurrency(9, 3, new byte[] {7, 8, 9})),
                List.of(planned, nonCurrency(9, 2, new byte[] {7, 8, 0})),
                List.of(planned, original, nonCurrency(18, 1, new byte[] {1}))
        );
        for (var after : invalid) {
            assertThrows(IOException.class, () -> SavedGrantDeliveryReadback.verifyTransition(
                    before, saved(plan.nextReceipt(), after), ledger, player, plan));
        }
    }

    @Test void transitionRejectsOverwritePreexistingTargetAndForgedPriorReceipt()
            throws Exception {
        var ledger = new GrantLedger(temp.resolve("target-overwrite"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5000, "console");
        var plan = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), freeSlots(1)).orElseThrow();
        var newCoin = coin(0, CoinDenomination.FIVE_THOUSAND, 1);
        assertThrows(IOException.class, () -> SavedGrantDeliveryReadback.verifyTransition(
                initialSaved(List.of(nonCurrency(0, 1, new byte[] {3}))),
                saved(plan.nextReceipt(), List.of(newCoin)), ledger, player, plan));
        assertThrows(IOException.class, () -> SavedGrantDeliveryReadback.verifyTransition(
                saved(plan.nextReceipt(), List.of()),
                saved(plan.nextReceipt(), List.of(newCoin)), ledger, player, plan));
    }

    @Test void transitionRejectsEnderChestAndEquipmentDrift() throws Exception {
        var ledger = new GrantLedger(temp.resolve("other-storage"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5000, "console");
        var plan = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), freeSlots(1)).orElseThrow();
        var before = initialSaved(List.of());
        var after = new HashMap<String, Object>(saved(plan.nextReceipt(),
                List.of(coin(0, CoinDenomination.FIVE_THOUSAND, 1))));
        after.put("EnderItems", List.of(nonCurrency(4, 1, new byte[] {1})));
        assertThrows(IOException.class, () -> SavedGrantDeliveryReadback.verifyTransition(
                before, after, ledger, player, plan));
        after.remove("EnderItems");
        after.put("equipment", Map.of("head", Map.of("id", "minecraft:diamond", "count", 1)));
        assertThrows(IOException.class, () -> SavedGrantDeliveryReadback.verifyTransition(
                before, after, ledger, player, plan));
    }

    @Test void realGzipPlayerdataParserYieldsSameReceiptAndPhysicalItemGeneration()
            throws Exception {
        var ledger = new GrantLedger(temp.resolve("compressed"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5200, "console");
        var plan = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), freeSlots(2)).orElseThrow();
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(10); out.writeUTF("BukkitValues");
            byte[] bytes = PaperGrantReceiptPdc.toSavedBytes(plan.nextReceipt());
            out.writeByte(7); out.writeUTF("simplecoins:grant_receipt_v1");
            out.writeInt(bytes.length); out.write(bytes);
            out.writeByte(0);
            out.writeByte(9); out.writeUTF("Inventory");
            out.writeByte(10); out.writeInt(plan.placements().size());
            for (var placement : plan.placements()) {
                out.writeByte(1); out.writeUTF("Slot"); out.writeByte(placement.index());
                out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:paper");
                out.writeByte(3); out.writeUTF("count"); out.writeInt(placement.count());
                out.writeByte(10); out.writeUTF("components");
                out.writeByte(8); out.writeUTF("minecraft:item_model");
                out.writeUTF("simplecoins:coin_" + placement.denomination().value());
                out.writeByte(10); out.writeUTF("minecraft:custom_data");
                out.writeByte(10); out.writeUTF("PublicBukkitValues");
                out.writeByte(3); out.writeUTF("simplecoins:coin_denomination");
                out.writeInt(placement.denomination().value());
                out.writeByte(0); out.writeByte(0); out.writeByte(0); out.writeByte(0);
            }
            out.writeByte(9); out.writeUTF("EnderItems");
            out.writeByte(10); out.writeInt(0);
            out.writeByte(0);
        }
        Path file = temp.resolve("playerdata/" + player + ".dat");
        Files.createDirectories(file.getParent());
        try (var out = new GZIPOutputStream(Files.newOutputStream(file))) {
            out.write(nbt.toByteArray());
        }
        var root = PlayerDataReader.read(file, 65536, 65536);
        var proof = SavedGrantDeliveryReadback.verify(root, ledger, player, plan);
        assertEquals(BigInteger.valueOf(5200), proof.matchedMonetaryValue());
        assertEquals(2, proof.matchedStacks());
    }
}
