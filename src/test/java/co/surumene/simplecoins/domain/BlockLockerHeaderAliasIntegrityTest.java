package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * TASK-20261101-004: source-derived Draft Red for uncolored runtime
 * BlockLocker 1.14.1 header aliases. Not physical sign ownership evidence.
 */
class BlockLockerHeaderAliasIntegrityTest {
    @Test void coloredPrivateRuntimeAliasInvalidatesEntireSet() {
        assertThrows(IOException.class, () -> BlockLockerHeaderMatcher.match(
                "[Private]",
                Set.of("[Private]", "§a[Privat]"),
                Set.of("[More Users]")));
    }

    @Test void coloredMoreUsersRuntimeAliasInvalidatesEntireSet() {
        assertThrows(IOException.class, () -> BlockLockerHeaderMatcher.match(
                "[Private]",
                Set.of("[Private]"),
                Set.of("[More Users]", "§b[More Users]")));
    }

    @Test void formattedVisibleHeaderStillMatchesUncoloredRuntimeAlias() throws Exception {
        assertEquals(Optional.of(BlockLockerHeaderMatcher.Type.PRIVATE),
                BlockLockerHeaderMatcher.match(
                        " §b[PRIVATE]§r ",
                        Set.of("[Private]"),
                        Set.of("[More Users]")));
    }

    @Test void unknownOrTrailingVisibleHeaderDoesNotMatch() throws Exception {
        assertTrue(BlockLockerHeaderMatcher.match(
                "[Private] extra",
                Set.of("[Private]"),
                Set.of("[More Users]")).isEmpty());
    }
}
