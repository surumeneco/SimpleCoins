package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BlockLockerSavedSignTextTest {
    private static final List<String> FRONT = List.of(
            "{\"text\":\"[Private]\"}", "{\"text\":\"Alice\"}",
            "{\"text\":\"[Everyone]\"}", "{\"text\":\"\"}");
    private static final List<String> BACK = List.of(
            "{\"text\":\"[More Users]\"}", "{\"text\":\"Bob\"}",
            "{\"text\":\"\"}", "{\"text\":\"\"}");

    @Test void modernFrontAndBackArePreservedAsRawTextEvidence() throws Exception {
        var evidence = BlockLockerSavedSignText.read(Map.of(
                "front_text", Map.of("messages", FRONT),
                "back_text", Map.of("messages", BACK))).orElseThrow();
        assertEquals(FRONT, evidence.front());
        assertEquals(BACK, evidence.back().orElseThrow());
        assertFalse(evidence.legacy());
        assertThrows(UnsupportedOperationException.class, () -> evidence.front().add("x"));
    }

    @Test void legacyTextOnlySignIsNotDiscarded() throws Exception {
        var evidence = BlockLockerSavedSignText.read(Map.of(
                "Text1", "[Private]", "Text2", "Alice",
                "Text3", "[Everyone]", "Text4", "")).orElseThrow();
        assertEquals(List.of("[Private]", "Alice", "[Everyone]", ""), evidence.front());
        assertTrue(evidence.back().isEmpty());
        assertTrue(evidence.legacy());
    }

    @Test void missingBacksideIsUnknownNotEmptyAndNoTextYieldsNoEvidence() throws Exception {
        var evidence = BlockLockerSavedSignText.read(Map.of(
                "front_text", Map.of("messages", FRONT))).orElseThrow();
        assertTrue(evidence.back().isEmpty());
        assertFalse(evidence.legacy());
        assertTrue(BlockLockerSavedSignText.read(Map.of()).isEmpty());
    }

    @Test void malformedOrAmbiguousSavedSignTextFailsClosed() {
        assertThrows(IOException.class, () -> BlockLockerSavedSignText.read(
                Map.of("front_text", Map.of("messages", List.of("a", "b")))));
        assertThrows(IOException.class, () -> BlockLockerSavedSignText.read(
                Map.of("front_text", Map.of("messages", List.of("a", "b", 7, "d")))));
        assertThrows(IOException.class, () -> BlockLockerSavedSignText.read(
                Map.of("back_text", Map.of("messages", BACK))));
        assertThrows(IOException.class, () -> BlockLockerSavedSignText.read(
                Map.of("Text1", "[Private]")));
        assertThrows(IOException.class, () -> BlockLockerSavedSignText.read(
                Map.of("Text1", "[Private]", "Text2", "Alice",
                        "Text3", "", "Text4", "",
                        "front_text", Map.of("messages", FRONT))));
    }
}
