package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * TASK-20261101-004 / SC-EVERYONE-ALIAS-01..03.
 * CAT Draft regression only; a visible text match is not ownership evidence.
 */
class BlockLockerEveryoneColorOnlyAliasTest {
    @Test void colorOnlyAliasCannotBeIgnoredAfterPositiveMatch() {
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches(
                "[Everyone]", Set.of("Everyone", "§a")));
    }

    @Test void whitespaceAndFormattingOnlyAliasFailsBeforeNegativeMatch() {
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches(
                "[Other]", Set.of("Everyone", " §r ")));
    }

    @Test void validAliasAndColoredVisibleProfileStillMatch() throws Exception {
        assertTrue(BlockLockerEveryoneText.matches(
                "§a[Everyone]§r", Set.of("Everyone")));
    }
}
