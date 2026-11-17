package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * CAT code-to-spec Candidate: characterize the exact six saved BlockLocker
 * profile slot keys before optimizing the saved-sign PDC scan.
 * These checks are observed implementation behavior, not owner authority.
 */
class BlockLockerSavedHintsProfileSlotCandidateTest {
    private static Map<String, Object> sign(String profileKey) {
        var pdc = new HashMap<String, Object>();
        pdc.put("blocklocker:header", "PRIVATE");
        pdc.put(profileKey, Map.of("blocklocker:e", (byte) 0));
        return pdc;
    }

    @Test void eachOfTheSixSupportedSlotsPreservesPresenceOfEveryoneMarker() throws Exception {
        for (int slot = 1; slot <= 6; slot++) {
            String key = "blocklocker:profile_" + slot;
            var hints = BlockLockerSavedHints.read(sign(key)).orElseThrow();
            assertTrue(hints.everyoneMarkerPresent(), key);
            assertTrue(hints.firstProfileUuid().isEmpty(), key);
        }
    }

    @Test void unsupportedOrNoncanonicalProfileSlotSuffixesFailClosed() {
        String[] malformed = {
                "blocklocker:profile_0",
                "blocklocker:profile_7",
                "blocklocker:profile_01",
                "blocklocker:profile_10",
                "blocklocker:profile_6_extra",
                "blocklocker:profile_1\n",
                "blocklocker:profile_１",
                "blocklocker:profile_١"
        };
        for (String key : malformed)
            assertThrows(IOException.class, () -> BlockLockerSavedHints.read(sign(key)), key);
    }

    @Test void unrelatedPdcExtensionDoesNotCreateAProfileSlot() throws Exception {
        var pdc = new HashMap<String, Object>();
        pdc.put("blocklocker:header", "PRIVATE");
        pdc.put("unrelated:profile_7", Map.of("blocklocker:e", (byte) 0));
        pdc.put("blocklocker:profile", Map.of("blocklocker:e", (byte) 0));
        var hints = BlockLockerSavedHints.read(pdc).orElseThrow();
        assertFalse(hints.everyoneMarkerPresent());
        assertTrue(hints.firstProfileUuid().isEmpty());
    }

    @Test void validSlotWithoutHeaderRemainsOrphanEvidenceNotAnOwner() {
        var pdc = new HashMap<String, Object>();
        pdc.put("blocklocker:profile_6", Map.of("blocklocker:e", (byte) 0));
        assertThrows(IOException.class, () -> BlockLockerSavedHints.read(pdc));
    }
}
