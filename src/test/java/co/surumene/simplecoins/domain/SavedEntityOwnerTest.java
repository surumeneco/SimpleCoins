package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SavedEntityOwnerTest {
    @Test void extractsVanillaTameOwnerUuidWithoutAssumingPlayerRegistration() throws Exception {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-00000000002a");
        Map<String, Object> wolf = Map.of("id", "minecraft:wolf",
                "UUID", new int[]{0, 0, 0, 70}, "Owner", new int[]{0, 0, 0, 42});
        Map<String, Object> cat = Map.of("id", "minecraft:cat",
                "UUID", new int[]{0, 0, 0, 71});
        assertEquals(Map.of("00000000-0000-0000-0000-000000000046", owner),
                SavedEntityOwner.extract(Map.of("Entities", List.of(wolf, cat))));
    }

    @Test void droppedItemOwnerTargetIsNotTameOwnership() throws Exception {
        Map<String, Object> dropped = Map.of("id", "minecraft:item",
                "UUID", new int[]{0, 0, 0, 72}, "Owner", new int[]{0, 0, 0, 42});
        assertEquals(Map.of(),
                SavedEntityOwner.extract(Map.of("Entities", List.of(dropped))));
    }

    @Test void malformedTameOwnerCannotBeSilentlyAssignedToFree() {
        Map<String, Object> wolf = Map.of("id", "minecraft:wolf",
                "UUID", new int[]{0, 0, 0, 70}, "Owner", "invalid");
        assertThrows(java.io.IOException.class, () ->
                SavedEntityOwner.extract(Map.of("Entities", List.of(wolf))));
    }

    @Test void malformedChunkOrEntityFailsClosedWithoutPartialOwnerMap() {
        assertThrows(java.io.IOException.class, () -> SavedEntityOwner.extract(null));
        assertThrows(java.io.IOException.class, () -> SavedEntityOwner.extract(Map.of()));
        assertThrows(java.io.IOException.class, () -> SavedEntityOwner.extract(Map.of("Entities", "not-a-list")));
        Map<String, Object> wolf = Map.of("id", "minecraft:wolf",
                "UUID", new int[]{0, 0, 0, 70}, "Owner", new int[]{0, 0, 0, 42});
        assertThrows(java.io.IOException.class, () -> SavedEntityOwner.extract(
                Map.of("Entities", List.of(wolf, Map.of("id", "")))));
        assertThrows(java.io.IOException.class, () -> SavedEntityOwner.extract(
                Map.of("Entities", List.of(wolf, "malformed-entity"))));
    }

    @Test void duplicateTameableUuidIsRejectedEvenWhenFirstHasNoOwner() {
        Map<String, Object> unowned = Map.of("id", "minecraft:wolf",
                "UUID", new int[]{0, 0, 0, 70});
        Map<String, Object> owned = Map.of("id", "minecraft:cat",
                "UUID", new int[]{0, 0, 0, 70}, "Owner", new int[]{0, 0, 0, 42});
        assertThrows(java.io.IOException.class, () -> SavedEntityOwner.extract(
                Map.of("Entities", List.of(unowned, owned))));
        assertThrows(java.io.IOException.class, () -> SavedEntityOwner.extract(
                Map.of("Entities", List.of(owned, unowned))));
    }

    @Test void invalidTameableUuidAndOwnerArraysFailClosed() {
        Map<String, Object> missingUuid = Map.of("id", "minecraft:wolf",
                "Owner", new int[]{0, 0, 0, 42});
        Map<String, Object> shortUuid = Map.of("id", "minecraft:wolf",
                "UUID", new int[]{0, 0, 70});
        Map<String, Object> shortOwner = Map.of("id", "minecraft:wolf",
                "UUID", new int[]{0, 0, 0, 70}, "Owner", new int[]{0, 0, 42});
        for (Map<String, Object> entity : List.of(missingUuid, shortUuid, shortOwner)) {
            assertThrows(java.io.IOException.class, () -> SavedEntityOwner.extract(
                    Map.of("Entities", List.of(entity))));
        }
    }

    @Test void signedUuidComponentsPreserveAll128Bits() throws Exception {
        UUID entityId = UUID.fromString("ffffffff-8000-0000-7fff-ffff00000001");
        UUID ownerId = UUID.fromString("80000000-ffffffff-ffffffff-00000002");
        Map<String, Object> wolf = Map.of("id", "minecraft:wolf",
                "UUID", new int[]{-1, Integer.MIN_VALUE, Integer.MAX_VALUE, 1},
                "Owner", new int[]{Integer.MIN_VALUE, -1, -1, 2});
        assertEquals(Map.of(entityId.toString(), ownerId),
                SavedEntityOwner.extract(Map.of("Entities", List.of(wolf))));
    }

    @Test void ownerHintSnapshotIsImmutableAndIndependentOfInputMutation() throws Exception {
        int[] ownerParts = new int[]{0, 0, 0, 42};
        Map<String, Object> wolf = Map.of("id", "minecraft:wolf",
                "UUID", new int[]{0, 0, 0, 70}, "Owner", ownerParts);
        Map<String, UUID> snapshot = SavedEntityOwner.extract(
                Map.of("Entities", List.of(wolf)));
        assertThrows(UnsupportedOperationException.class, () ->
                snapshot.put("other", UUID.randomUUID()));
        ownerParts[3] = 99;
        assertEquals(UUID.fromString("00000000-0000-0000-0000-00000000002a"),
                snapshot.get("00000000-0000-0000-0000-000000000046"));
    }

    @Test void vanillaTameableWhitelistDoesNotPromoteOtherOwnerTags() throws Exception {
        List<String> tameableIds = List.of("minecraft:wolf", "minecraft:cat",
                "minecraft:parrot", "minecraft:horse", "minecraft:donkey",
                "minecraft:mule", "minecraft:llama", "minecraft:trader_llama",
                "minecraft:skeleton_horse", "minecraft:zombie_horse");
        java.util.ArrayList<Map<String, Object>> entities = new java.util.ArrayList<>();
        for (int i = 0; i < tameableIds.size(); i++) {
            entities.add(Map.of("id", tameableIds.get(i),
                    "UUID", new int[]{0, 0, 0, i + 1},
                    "Owner", new int[]{0, 0, 0, 42}));
        }
        entities.add(Map.of("id", "minecraft:item", "Owner", new int[]{0, 0, 0, 42}));
        entities.add(Map.of("id", "minecraft:arrow", "Owner", new int[]{0, 0, 0, 42}));
        entities.add(Map.of("id", "minecraft:allay", "Owner", new int[]{0, 0, 0, 42}));
        Map<String, UUID> hints = SavedEntityOwner.extract(Map.of("Entities", entities));
        assertEquals(tameableIds.size(), hints.size());
        assertTrue(hints.values().stream().allMatch(
                id -> id.equals(UUID.fromString("00000000-0000-0000-0000-00000000002a"))));
    }
}
