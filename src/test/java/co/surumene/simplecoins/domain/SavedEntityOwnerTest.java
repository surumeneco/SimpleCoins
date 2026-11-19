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

    @Test void savedWclWolfOwnerIsVerifiedFromSelectedUuid() throws Exception {
        UUID wolf = new UUID(0, 84);
        var chunk = Map.<String,Object>of("Entities", List.of(
                Map.of("id", "minecraft:wolf", "UUID", new int[]{0,0,0,84},
                       "Owner", new int[]{0,0,0,42})));
        var evidence = SavedEntityOwner.wclTameObservation(chunk, wolf, "www");
        assertTrue(evidence.verified());
        assertEquals(new UUID(0, 42), evidence.owner());
    }

    @Test void normalChickenHasVerifiedNoVanillaTameOwner() throws Exception {
        var chunk = Map.<String,Object>of("Entities", List.of(
                Map.of("id", "minecraft:chicken", "UUID", new int[]{0,0,0,85})));
        var evidence = SavedEntityOwner.wclTameObservation(chunk, new UUID(0,85), "wwc");
        assertTrue(evidence.verified());
        assertNull(evidence.owner());
    }

    @Test void missingOrWrongSpeciesIsNotEvidenceOfFree() {
        var chunk = Map.<String,Object>of("Entities", List.of(
                Map.of("id", "minecraft:chicken", "UUID", new int[]{0,0,0,85})));
        assertThrows(java.io.IOException.class, () ->
                SavedEntityOwner.wclTameObservation(chunk, new UUID(0,84), "wwc"));
        assertThrows(java.io.IOException.class, () ->
                SavedEntityOwner.wclTameObservation(chunk, new UUID(0,85), "www"));
    }

    @Test void malformedOrDuplicatedSavedWolfUuidMustNotProveAbsentOwner() {
        var duplicate = Map.<String,Object>of("Entities", List.of(
                Map.of("id", "minecraft:wolf", "UUID", new int[]{0,0,0,84}),
                Map.of("id", "minecraft:wolf", "UUID", new int[]{0,0,0,84})));
        var malformed = Map.<String,Object>of("Entities", List.of(
                Map.of("id", "minecraft:wolf", "UUID", new int[]{0,0,0,84},
                       "Owner", "bad-owner")));
        assertThrows(java.io.IOException.class, () ->
                SavedEntityOwner.wclTameObservation(duplicate, new UUID(0,84), "www"));
        assertThrows(java.io.IOException.class, () ->
                SavedEntityOwner.wclTameObservation(malformed, new UUID(0,84), "www"));
    }
}
