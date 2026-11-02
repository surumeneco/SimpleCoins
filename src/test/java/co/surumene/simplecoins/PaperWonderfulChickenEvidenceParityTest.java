package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.CompanionOwnerResolution;
import co.surumene.simplecoins.domain.SavedWonderfulChickenOwnerEvidence;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** TASK-20261101-004: WWC loaded PDC and saved NBT must not disagree on the same evidence. */
class PaperWonderfulChickenEvidenceParityTest {
    private static final UUID COMMANDER =
            UUID.fromString("00000000-0000-0000-0000-0000000000af");
    private static final String NS = "whatawonderfulchicken:";

    private static Map<String, Object> saved(Object marker, Object version, Object target) {
        Map<String, Object> pdc = new HashMap<>();
        if (marker != null) pdc.put(NS + "type", marker);
        if (version != null) pdc.put(NS + "data_version", version);
        if (target != null) pdc.put(NS + "follow_target", target);
        return Map.of("id", "minecraft:chicken", "BukkitValues", pdc);
    }

    private static CompanionOwnerResolution.Identity assertParity(
            String marker, Integer version, String target) {
        var loaded = PaperWonderfulChickenOwnerAdapter.read(marker, target, version);
        var persisted = SavedWonderfulChickenOwnerEvidence.read(saved(marker, version, target));
        assertEquals(loaded, persisted,
                "WWC loaded/saved evidence diverged for marker=" + marker + ", version=" + version);
        return loaded;
    }

    @Test void allSupportedVersionsYieldTheSamePositiveCommander() {
        for (int version : new int[]{2, 3, 4})
            assertEquals(CompanionOwnerResolution.known(COMMANDER),
                    assertParity("wonderful_chicken", version, COMMANDER.toString()));
    }

    @Test void eachInvalidTargetIsUnknownInBothRepresentations() {
        for (String target : new String[]{null, "", "not-a-uuid", COMMANDER.toString().toUpperCase(),
                " " + COMMANDER, COMMANDER + " "})
            assertEquals(CompanionOwnerResolution.unknown(),
                    assertParity("wonderful_chicken", 4, target));
    }

    @Test void eachInvalidMarkerOrVersionIsUnknownInBothRepresentations() {
        assertEquals(CompanionOwnerResolution.unknown(), assertParity(null, 4, COMMANDER.toString()));
        assertEquals(CompanionOwnerResolution.unknown(), assertParity("other", 4, COMMANDER.toString()));
        for (Integer version : new Integer[]{null, 1, 5})
            assertEquals(CompanionOwnerResolution.unknown(),
                    assertParity("wonderful_chicken", version, COMMANDER.toString()));
    }

    @Test void corruptSavedTypesOrEntityTypeCannotProveOwnership() {
        assertEquals(CompanionOwnerResolution.unknown(),
                SavedWonderfulChickenOwnerEvidence.read(saved("wonderful_chicken", "4", COMMANDER.toString())));
        assertEquals(CompanionOwnerResolution.unknown(),
                SavedWonderfulChickenOwnerEvidence.read(saved("wonderful_chicken", 4, COMMANDER)));
        assertEquals(CompanionOwnerResolution.unknown(),
                SavedWonderfulChickenOwnerEvidence.read(Map.of("id", "minecraft:wolf",
                        "BukkitValues", saved("wonderful_chicken", 4, COMMANDER.toString()).get("BukkitValues"))));
    }
}
