package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** TASK-20261101-004: saved WWC PDC is positive-only commander evidence. */
class SavedWonderfulChickenOwnerEvidenceTest {
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000047");
    private static final String NS = "whatawonderfulchicken:";

    private static Map<String, Object> chicken(Object marker, Object version, Object target) {
        Map<String, Object> pdc = new HashMap<>();
        if (marker != null) pdc.put(NS + "type", marker);
        if (version != null) pdc.put(NS + "data_version", version);
        if (target != null) pdc.put(NS + "follow_target", target);
        return Map.of("id", "minecraft:chicken", "BukkitValues", Map.copyOf(pdc));
    }

    @Test void canonicalSavedWwcTargetIsPositiveCommanderEvidence() {
        assertEquals(CompanionOwnerResolution.known(OWNER),
                SavedWonderfulChickenOwnerEvidence.read(
                        chicken("wonderful_chicken", 4, OWNER.toString())));
    }

    @Test void legacySupportedVersionsRetainPositiveEvidence() {
        for (int version : new int[]{2, 3, 4})
            assertEquals(CompanionOwnerResolution.known(OWNER),
                    SavedWonderfulChickenOwnerEvidence.read(
                            chicken("wonderful_chicken", version, OWNER.toString())));
    }

    @Test void serializedSavedEntityNbtRetainsTypedWwcEvidence() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(10); out.writeUTF(""); // root
            out.writeByte(9); out.writeUTF("Entities");
            out.writeByte(10); out.writeInt(1); // one compound
            out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:chicken");
            out.writeByte(10); out.writeUTF("BukkitValues");
            out.writeByte(8); out.writeUTF(NS + "type"); out.writeUTF("wonderful_chicken");
            out.writeByte(3); out.writeUTF(NS + "data_version"); out.writeInt(4);
            out.writeByte(8); out.writeUTF(NS + "follow_target");
            out.writeUTF(OWNER.toString());
            out.writeByte(0); // BukkitValues
            out.writeByte(0); // chicken
            out.writeByte(0); // root
        }
        Map<String, Object> root = NbtReader.read(
                new ByteArrayInputStream(bytes.toByteArray()), 4096);
        Map<String, Object> saved = (Map<String, Object>)
                ((List<?>) root.get("Entities")).get(0);
        assertEquals(CompanionOwnerResolution.known(OWNER),
                SavedWonderfulChickenOwnerEvidence.read(saved));
    }

    @Test void missingOrUnsupportedEvidenceNeverProvesFree() {
        assertEquals(CompanionOwnerResolution.unknown(),
                SavedWonderfulChickenOwnerEvidence.read(
                        Map.of("id", "minecraft:chicken")));
        assertEquals(CompanionOwnerResolution.unknown(),
                SavedWonderfulChickenOwnerEvidence.read(chicken(null, 4, OWNER.toString())));
        assertEquals(CompanionOwnerResolution.unknown(),
                SavedWonderfulChickenOwnerEvidence.read(chicken("wonderful_chicken", 4, null)));
        assertEquals(CompanionOwnerResolution.unknown(),
                SavedWonderfulChickenOwnerEvidence.read(chicken("wonderful_chicken", 5, OWNER.toString())));
    }

    @Test void malformedOrWrongEntityTypeRemainsUnknown() {
        assertEquals(CompanionOwnerResolution.unknown(),
                SavedWonderfulChickenOwnerEvidence.read(Map.of(
                        "id", "minecraft:wolf", "BukkitValues", Map.of(
                                NS + "type", "wonderful_chicken",
                                NS + "data_version", 4,
                                NS + "follow_target", OWNER.toString()))));
        assertEquals(CompanionOwnerResolution.unknown(),
                SavedWonderfulChickenOwnerEvidence.read(Map.of(
                        "id", "minecraft:chicken", "BukkitValues", "malformed")));
        assertEquals(CompanionOwnerResolution.unknown(),
                SavedWonderfulChickenOwnerEvidence.read(
                        chicken("wonderful_chicken", 4, 42)));
        assertEquals(CompanionOwnerResolution.unknown(),
                SavedWonderfulChickenOwnerEvidence.read(
                        chicken("wonderful_chicken", 4, OWNER.toString().toUpperCase())));
        assertEquals(CompanionOwnerResolution.unknown(),
                SavedWonderfulChickenOwnerEvidence.read(null));
    }
}
