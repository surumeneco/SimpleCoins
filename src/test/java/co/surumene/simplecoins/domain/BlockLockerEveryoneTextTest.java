package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BlockLockerEveryoneTextTest {
    @Test void localizedEveryoneTagMatchesVisibleSignProfileText() throws Exception {
        assertTrue(BlockLockerEveryoneText.matches(" §a[ALLE] ",
                Set.of("Everyone", "Alle")));
        assertTrue(BlockLockerEveryoneText.matches("[everyone]",
                Set.of("Everyone", "Alle")));
    }

    @Test void UnrelatedGroupsAndSuffixesDoNotBecomeEveryone() throws Exception {
        assertFalse(BlockLockerEveryoneText.matches("[Builders]",
                Set.of("Everyone", "Alle")));
        assertFalse(BlockLockerEveryoneText.matches("[Everyone] extra",
                Set.of("Everyone", "Alle")));
        assertFalse(BlockLockerEveryoneText.matches("Everyone",
                Set.of("Everyone", "Alle")));
    }

    @Test void InvalidRuntimeLocalizationFailsClosed() {
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches(
                "[Everyone]", Set.of()));
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches(
                "[Everyone]", Set.of("  ")));
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches(
                "[Everyone]", Set.of("[Everyone]")));
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches(
                null, Set.of("Everyone")));
    }
}
