package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WalletUuidDuplicateAccountingTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000001001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000001002");

    private static Map<String,Object> wallet(UUID id, long amount) {
        return Map.of("id", "minecraft:bundle", "count", 1,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of(
                                "simplecoins:wallet_id", id.toString(),
                                "simplecoins:wallet_balance", amount))));
    }

    private static Map<String,Object> inventorySlot(int slot, UUID id, long amount) {
        return Map.of("Slot", (byte) slot, "id", "minecraft:bundle", "count", 1,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of(
                                "simplecoins:wallet_id", id.toString(),
                                "simplecoins:wallet_balance", amount))));
    }

    @Test void nestedBundleCannotCountSameWalletUuidTwice() {
        var bundle = Map.<String,Object>of(
                "id", "minecraft:bundle", "count", 1,
                "components", Map.of("minecraft:bundle_contents",
                        List.of(wallet(ID, 100), wallet(ID, 100))));
        assertThrows(IOException.class, () -> StoredItemValue.value(bundle));
    }

    @Test void nestedDifferentWalletIdsRemainIndependent() throws Exception {
        var bundle = Map.<String,Object>of(
                "id", "minecraft:bundle", "count", 1,
                "components", Map.of("minecraft:bundle_contents",
                        List.of(wallet(ID, 100), wallet(OTHER, 500))));
        assertEquals(BigInteger.valueOf(600), StoredItemValue.value(bundle));
    }

    @Test void onePlayerCannotDuplicateWalletAcrossInventoryAndEnderChest() {
        var player = Map.<String,Object>of(
                "Inventory", List.of(inventorySlot(0, ID, 100)),
                "EnderItems", List.of(inventorySlot(0, ID, 100)));
        assertThrows(IOException.class, () -> SavedPlayerAssets.extract(player));
    }

    @Test void onePlayerCannotDuplicateWalletAcrossEquipmentAndEnderChest() {
        var player = Map.<String,Object>of(
                "Inventory", List.of(),
                "EnderItems", List.of(inventorySlot(0, ID, 100)),
                "equipment", Map.of("offhand", wallet(ID, 100)));
        assertThrows(IOException.class, () -> SavedPlayerAssets.extract(player));
    }

    @Test void onePlayerWithSeparateWalletsKeepsBoth() throws Exception {
        var player = Map.<String,Object>of(
                "Inventory", List.of(inventorySlot(0, ID, 100)),
                "EnderItems", List.of(inventorySlot(0, OTHER, 500)));
        var assets = SavedPlayerAssets.extract(player);
        assertEquals(BigInteger.valueOf(100), assets.get("inventory:0"));
        assertEquals(BigInteger.valueOf(500), assets.get("ender:0"));
    }

    @Test void sharedWalletIdentityContextMustRejectDuplicatesAcrossIndependentItemCalls()
            throws Exception {
        Set<UUID> seen = new HashSet<>();
        assertEquals(BigInteger.valueOf(100), StoredItemValue.value(wallet(ID, 100), seen));
        assertThrows(IOException.class, () -> StoredItemValue.value(wallet(ID, 100), seen));
    }
}
