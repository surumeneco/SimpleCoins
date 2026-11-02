package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * TASK-20261101-004: Draft Red vectors for runtime Everyone alias integrity.
 * These are text-boundary tests, not evidence of attached BlockLocker protection.
 */
class BlockLockerEveryoneAliasIntegrityTest {
    @Test void colorFormattedRuntimeAliasUsesSameNormalizationAsVisibleLine() throws Exception {
        assertTrue(BlockLockerEveryoneText.matches(
                " §b[EVERYONE]§r ", Set.of("§aEveryone")));
    }

    @Test void formattingOnlyRuntimeAliasInvalidatesEntireAliasSet() {
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches(
                "[Everyone]", Set.of("Everyone", "§r")));
    }

    @Test void normalizedUnknownOrTrailingTextDoesNotGrantEveryone() throws Exception {
        assertFalse(BlockLockerEveryoneText.matches(
                "§b[Everyone] other§r", Set.of("Everyone")));
        assertFalse(BlockLockerEveryoneText.matches(
                "§b[Unknown]§r", Set.of("Everyone")));
    }
}
