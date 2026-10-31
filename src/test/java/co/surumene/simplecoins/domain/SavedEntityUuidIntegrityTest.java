package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SavedEntityUuidIntegrityTest {
    @Test void duplicateUuidAcrossDifferentEntityTypesMustFailBeforeAssetStaging() {
        Map<String, Object> dropped = Map.of("id", "minecraft:item",
                "UUID", new int[]{0, 0, 0, 80});
        Map<String, Object> frame = Map.of("id", "minecraft:item_frame",
                "UUID", new int[]{0, 0, 0, 80});
        assertThrows(java.io.IOException.class, () ->
                SavedEntityUuidIntegrity.validate(Map.of("Entities", List.of(dropped, frame))));
    }

    @Test void distinctUuidsAcrossEntityTypesAreAccepted() {
        Map<String, Object> dropped = Map.of("id", "minecraft:item",
                "UUID", new int[]{0, 0, 0, 80});
        Map<String, Object> frame = Map.of("id", "minecraft:item_frame",
                "UUID", new int[]{0, 0, 0, 81});
        assertDoesNotThrow(() ->
                SavedEntityUuidIntegrity.validate(Map.of("Entities", List.of(dropped, frame))));
    }

    @Test void malformedUuidOnOtherwiseUnvaluedEntityIsRejected() {
        Map<String, Object> frame = Map.of("id", "minecraft:item_frame",
                "UUID", new int[]{0, 0, 80});
        assertThrows(java.io.IOException.class, () ->
                SavedEntityUuidIntegrity.validate(Map.of("Entities", List.of(frame))));
    }
}
