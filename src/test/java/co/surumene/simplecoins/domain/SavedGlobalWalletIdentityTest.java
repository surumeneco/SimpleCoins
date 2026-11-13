package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** All saved source kinds must share one wallet-identity context per staging generation. */
class SavedGlobalWalletIdentityTest {
    private static final UUID WALLET = UUID.fromString("00000000-0000-0000-0000-000000008001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000008002");

    private static Map<String,Object> wallet(UUID id, long balance) {
        return Map.of("id", "minecraft:bundle", "count", 1,
                "components", Map.of("minecraft:custom_data", Map.of("PublicBukkitValues",
                        Map.of("simplecoins:wallet_id", id.toString(),
                               "simplecoins:wallet_balance", balance))));
    }

    private static Map<String,Object> player(UUID id) {
        return Map.of("Inventory", List.of(withSlot(wallet(id, 100), 0)),
                "EnderItems", List.of());
    }

    private static Map<String,Object> withSlot(Map<String,Object> item, int slot) {
        var copy = new java.util.HashMap<String,Object>(item);
        copy.put("Slot", slot);
        return copy;
    }

    private static Map<String,Object> block(UUID id) {
        return Map.of("xPos", 0, "zPos", 0,
                "block_entities", List.of(Map.of("id", "minecraft:chest",
                        "x", 1, "y", 64, "z", 1,
                        "Items", List.of(withSlot(wallet(id, 200), 0)))));
    }

    private static Map<String,Object> entity(String type, String storage, UUID walletId, int uuidPart) {
        var data = new java.util.HashMap<String,Object>();
        data.put("id", type);
        data.put("UUID", new int[] {0, 0, 0, uuidPart});
        if (storage.equals("Item")) data.put("Item", wallet(walletId, 300));
        if (storage.equals("item")) data.put("item", wallet(walletId, 300));
        if (storage.equals("equipment")) data.put("equipment", Map.of("mainhand", wallet(walletId, 300)));
        if (storage.equals("Items")) data.put("Items", List.of(withSlot(wallet(walletId, 300), 0)));
        return Map.of("Entities", List.of(data));
    }

    @Test void duplicatedPlayerAndPlacedChestWalletMustBeRejected() throws Exception {
        Set<UUID> identities = new HashSet<>();
        assertEquals(Map.of("inventory:0", BigInteger.valueOf(100)),
                SavedPlayerAssets.extract(player(WALLET), identities));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(block(WALLET), identities));
    }

    @Test void aDifferentWalletCanBeCountedAcrossPlayerAndChest() throws Exception {
        Set<UUID> identities = new HashSet<>();
        SavedPlayerAssets.extract(player(WALLET), identities);
        assertEquals(Map.of("1,64,1:0", BigInteger.valueOf(200)),
                StoredBlockEntityAssets.extract(block(OTHER), identities));
        assertEquals(Set.of(WALLET, OTHER), identities);
    }

    @Test void allSavedEntityExtractorsRejectCrossSourceClones() throws Exception {
        Set<UUID> identities = new HashSet<>();
        var dropped = entity("minecraft:item", "Item", WALLET, 1);
        assertEquals(BigInteger.valueOf(300),
                StoredDroppedItemAssets.extract(dropped, identities).get(
                        "00000000-0000-0000-0000-000000000001"));
        assertThrows(IOException.class, () -> StoredItemFrameAssets.extract(
                entity("minecraft:item_frame", "Item", WALLET, 2), identities));
        assertThrows(IOException.class, () -> StoredItemDisplayAssets.extract(
                entity("minecraft:item_display", "item", WALLET, 3), identities));
        assertThrows(IOException.class, () -> StoredEntityEquipmentAssets.extract(
                entity("minecraft:zombie", "equipment", WALLET, 4), identities));
        assertThrows(IOException.class, () -> StoredEntityInventoryAssets.extract(
                entity("minecraft:chest_minecart", "Items", WALLET, 5), identities));
    }

    @Test void clonedWalletInsideOnePlacedContainerAlsoFails() {
        var filled = new java.util.HashMap<String,Object>();
        filled.put("xPos", 0);
        filled.put("zPos", 0);
        filled.put("block_entities", List.of(Map.of("id","minecraft:chest",
                "x", 1, "y", 64, "z", 1, "Items",
                List.of(withSlot(wallet(WALLET, 20), 0), withSlot(wallet(WALLET, 20), 1)))));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(filled, new HashSet<>()));
    }
}
