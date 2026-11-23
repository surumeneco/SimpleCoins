package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Code-to-spec Candidate: characterize the current alias comparison boundary.
 * These observations are not a normative approval of control characters in
 * localized BlockLocker aliases, and do not prove attachment or ownership.
 */
class BlockLockerEveryoneAliasControlCharacterCandidateTest {
    @Test
    void controlCharactersRemainLiteralWithinTheRuntimeAlias() throws Exception {
        assertTrue(BlockLockerEveryoneText.matches("[Every\none]", Set.of("Every\none")));
        assertFalse(BlockLockerEveryoneText.matches("[Everyone]", Set.of("Every\none")));
        assertFalse(BlockLockerEveryoneText.matches("[Every\none]", Set.of("Everyone")));
    }

    @Test
    void invisibleUnicodeFormatCharacterIsNotNormalizedAway() throws Exception {
        String alias = "Every\u200Bone";
        assertTrue(BlockLockerEveryoneText.matches("[" + alias + "]", Set.of(alias)));
        assertFalse(BlockLockerEveryoneText.matches("[Everyone]", Set.of(alias)));
    }
}
