package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoredBlockEntityAssetsTest {
    private static Map<String,Object> coin(int slot, int count) {
        return Map.of("Slot", (byte) slot, "id", "minecraft:paper", "count", count,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
    }

    @Test void extractsChestInventoryFromModernChunkCoordinatesAndSlots() throws Exception {
        Map<String,Object> chest = Map.of(
                "id", "minecraft:chest", "x", 24, "y", 70, "z", -9,
                "Items", List.of(coin(0, 2), coin(1, 3)));
        Map<String,BigInteger> assets = StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(chest)));
        assertEquals(2, assets.size());
        assertEquals(BigInteger.valueOf(200), assets.get("24,70,-9:0"));
        assertEquals(BigInteger.valueOf(300), assets.get("24,70,-9:1"));
    }

    @Test void chestCannotAccountForCurrencyInNonexistentSlotTwentySeven() {
        Map<String,Object> chest = Map.of(
                "id", "minecraft:chest", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(27, 1)));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(chest))));
    }

    @Test void lastPhysicalChestSlotRemainsCountable() throws Exception {
        Map<String,Object> chest = Map.of(
                "id", "minecraft:chest", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(26, 1)));
        assertEquals(Map.of("0,64,0:26", BigInteger.valueOf(100)),
                StoredBlockEntityAssets.extract(Map.of("block_entities", List.of(chest))));
    }

    @Test void decoratedPotSingleItemStackIsIncludedInPhysicalAssets() throws Exception {
        Map<String,Object> pot = Map.of(
                "id", "minecraft:decorated_pot", "x", 5, "y", 70, "z", -2,
                "item", coin(0, 3));
        Map<String,BigInteger> assets = StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(pot)));
        assertEquals(Map.of("5,70,-2:0", BigInteger.valueOf(300)), assets);
    }

    @Test void unknownSingleItemStorageFailsClosedInsteadOfBeingCountedAsEmpty() {
        Map<String,Object> unknown = Map.of(
                "id", "minecraft:unknown_storage", "x", 5, "y", 70, "z", -2,
                "item", coin(0, 3));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(unknown))));
    }

    @Test void decoratedPotWithConflictingInventoryFormatsFailsClosed() {
        Map<String,Object> pot = Map.of(
                "id", "minecraft:decorated_pot", "x", 5, "y", 70, "z", -2,
                "item", coin(0, 3), "Items", List.of(coin(0, 2)));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(pot))));
    }

    @Test void unknownOrDuplicatedBlockEntityInventoryCannotBeSilentlyCountedAsZero() {
        Map<String,Object> chest = Map.of(
                "id", "minecraft:chest", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(0, 1)));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(Map.of()));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(chest, chest))));
    }
    @Test void blockEntityMustBelongToTheDeclaringTerrainChunk() {
        Map<String,Object> outOfChunk = Map.of(
                "id", "minecraft:chest", "x", 16, "y", 64, "z", 0,
                "Items", List.of(coin(0, 1)));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("xPos", 0, "zPos", 0, "block_entities", List.of(outOfChunk))));
    }


    @Test void overflowingNonNbtSlotNumberCannotAliasChestSlotZero() {
        Map<String, Object> malformed = Map.of("Slot", BigInteger.ONE.shiftLeft(64),
                "id", "minecraft:paper", "count", 1,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        Map<String, Object> chest = Map.of("id", "minecraft:chest",
                "x", 0, "y", 64, "z", 0, "Items", List.of(malformed));
        assertThrows(IOException.class, () ->
                StoredBlockEntityAssets.extract(Map.of("block_entities", List.of(chest))));
    }

    @Test void hopperBlockSlotFiveMustFailClosed() {
        Map<String,Object> hopper = Map.of(
                "id", "minecraft:hopper", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(5, 1)));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(hopper))));
    }

    @Test void hopperBlockLastPhysicalSlotRemainsCountable() throws Exception {
        Map<String,Object> hopper = Map.of(
                "id", "minecraft:hopper", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(4, 1)));
        assertEquals(Map.of("0,64,0:4", BigInteger.valueOf(100)),
                StoredBlockEntityAssets.extract(Map.of("block_entities", List.of(hopper))));
    }

    @Test void barrelSlotTwentySevenMustFailClosed() {
        Map<String,Object> barrel = Map.of(
                "id", "minecraft:barrel", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(27, 1)));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(barrel))));
    }

    @Test void barrelLastPhysicalSlotRemainsCountable() throws Exception {
        Map<String,Object> barrel = Map.of(
                "id", "minecraft:barrel", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(26, 1)));
        assertEquals(Map.of("0,64,0:26", BigInteger.valueOf(100)),
                StoredBlockEntityAssets.extract(Map.of("block_entities", List.of(barrel))));
    }

    @Test void dispenserSlotNineMustFailClosed() {
        Map<String,Object> dispenser = Map.of(
                "id", "minecraft:dispenser", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(9, 1)));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(dispenser))));
    }

    @Test void dropperSlotNineMustFailClosed() {
        Map<String,Object> dropper = Map.of(
                "id", "minecraft:dropper", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(9, 1)));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(dropper))));
    }

    @Test void dispenserLastPhysicalSlotRemainsCountable() throws Exception {
        Map<String,Object> dispenser = Map.of(
                "id", "minecraft:dispenser", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(8, 1)));
        assertEquals(Map.of("0,64,0:8", BigInteger.valueOf(100)),
                StoredBlockEntityAssets.extract(Map.of("block_entities", List.of(dispenser))));
    }

    @Test void furnaceFamilyCannotCountNonexistentFourthSlot() {
        for (String id : List.of("minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker")) {
            Map<String,Object> furnace = Map.of(
                    "id", id, "x", 0, "y", 64, "z", 0,
                    "Items", List.of(coin(3, 1)));
            assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                    Map.of("block_entities", List.of(furnace))), id);
        }
    }

    @Test void furnaceOutputSlotRemainsCountable() throws Exception {
        Map<String,Object> furnace = Map.of(
                "id", "minecraft:furnace", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(2, 1)));
        assertEquals(Map.of("0,64,0:2", BigInteger.valueOf(100)),
                StoredBlockEntityAssets.extract(Map.of("block_entities", List.of(furnace))));
    }

    @Test void brewingStandSlotFiveMustFailClosed() {
        Map<String,Object> stand = Map.of(
                "id", "minecraft:brewing_stand", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(5, 1)));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(stand))));
    }

    @Test void brewingStandFuelSlotRemainsCountable() throws Exception {
        Map<String,Object> stand = Map.of(
                "id", "minecraft:brewing_stand", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(4, 1)));
        assertEquals(Map.of("0,64,0:4", BigInteger.valueOf(100)),
                StoredBlockEntityAssets.extract(Map.of("block_entities", List.of(stand))));
    }

    @Test void crafterSlotNineMustFailClosed() {
        Map<String,Object> crafter = Map.of(
                "id", "minecraft:crafter", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(9, 1)));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(crafter))));
    }

    @Test void crafterLastPhysicalSlotRemainsCountable() throws Exception {
        Map<String,Object> crafter = Map.of(
                "id", "minecraft:crafter", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(8, 1)));
        assertEquals(Map.of("0,64,0:8", BigInteger.valueOf(100)),
                StoredBlockEntityAssets.extract(Map.of("block_entities", List.of(crafter))));
    }

    @Test void shulkerBlockSlotTwentySevenMustFailClosed() {
        for (String id : List.of("minecraft:shulker_box", "minecraft:blue_shulker_box")) {
            Map<String,Object> shulker = Map.of(
                    "id", id, "x", 0, "y", 64, "z", 0,
                    "Items", List.of(coin(27, 1)));
            assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                    Map.of("block_entities", List.of(shulker))), id);
        }
    }

    @Test void shulkerBlockLastPhysicalSlotRemainsCountable() throws Exception {
        Map<String,Object> shulker = Map.of(
                "id", "minecraft:blue_shulker_box", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(26, 1)));
        assertEquals(Map.of("0,64,0:26", BigInteger.valueOf(100)),
                StoredBlockEntityAssets.extract(Map.of("block_entities", List.of(shulker))));
    }

    @Test void chiseledBookshelfSlotSixMustFailClosed() {
        Map<String,Object> shelf = Map.of(
                "id", "minecraft:chiseled_bookshelf", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(6, 1)));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(shelf))));
    }

    @Test void chiseledBookshelfLastPhysicalSlotRemainsCountable() throws Exception {
        Map<String,Object> shelf = Map.of(
                "id", "minecraft:chiseled_bookshelf", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(5, 1)));
        assertEquals(Map.of("0,64,0:5", BigInteger.valueOf(100)),
                StoredBlockEntityAssets.extract(Map.of("block_entities", List.of(shelf))));
    }

    @Test void explicitlyNullItemsCannotMasqueradeAsEmptyChest() {
        Map<String,Object> chest = new java.util.HashMap<>();
        chest.put("id", "minecraft:chest");
        chest.put("x", 0);
        chest.put("y", 64);
        chest.put("z", 0);
        chest.put("Items", null);
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(chest))));
    }
    @Test void unknownItemsHolderMustNotCreatePhysicalCurrency() {
        for (String id : List.of("minecraft:sign", "minecraft:future_storage",
                "otherplugin:custom_container", "minecraft:ultraviolet_shulker_box")) {
            Map<String,Object> holder = Map.of("id", id,
                    "x", 0, "y", 64, "z", 0, "Items", List.of(coin(0, 1)));
            assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                    Map.of("block_entities", List.of(holder))), id);
        }
    }

    @Test void knownColoredAndUncoloredShulkerItemsRemainCountable() throws Exception {
        for (String id : List.of("minecraft:shulker_box", "minecraft:red_shulker_box",
                "minecraft:light_blue_shulker_box")) {
            Map<String,Object> holder = Map.of("id", id,
                    "x", 0, "y", 64, "z", 0, "Items", List.of(coin(0, 2)));
            assertEquals(Map.of("0,64,0:0", BigInteger.valueOf(200)),
                    StoredBlockEntityAssets.extract(
                            Map.of("block_entities", List.of(holder))), id);
        }
    }

}
