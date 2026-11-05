package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Characterizes existing text-only BlockLocker 1.14.1 localization matching.
 * These vectors do not establish sign attachment, trusted owners, or settlement.
 */
class BlockLockerLegacyFormattingMatrixTest {
    private static final Set<String> PRIVATE = Set.of("[Private]");
    private static final Set<String> MORE = Set.of("[More Users]");
    private static final String FORMATTING = "0123456789abcdefklmnorx";

    @Test void everySupportedFormattingCodeAndCasePreservesVisibleHeaderAndTag() throws Exception {
        int vectors = 0;
        for (char code : FORMATTING.toCharArray()) {
            for (char variant : new char[] {code, Character.toUpperCase(code)}) {
                String color = "§" + variant;
                assertEquals(Optional.of(BlockLockerHeaderMatcher.Type.PRIVATE),
                        BlockLockerHeaderMatcher.match(color + "[Private]§r", PRIVATE, MORE), color);
                assertEquals(Optional.of(BlockLockerHeaderMatcher.Type.MORE_USERS),
                        BlockLockerHeaderMatcher.match(color + "[More Users]§r", PRIVATE, MORE), color);
                assertTrue(BlockLockerEveryoneText.matches(color + "[Everyone]§r", Set.of("Everyone")), color);
                vectors += 3;
            }
        }
        assertEquals(138, vectors);
    }

    @Test void fullLegacyRgbSequenceOnVisibleTextHasNoSemanticEffect() throws Exception {
        String rgb = "§x§F§F§0§0§A§A";
        assertEquals(Optional.of(BlockLockerHeaderMatcher.Type.PRIVATE),
                BlockLockerHeaderMatcher.match(rgb + "[Private]", PRIVATE, MORE));
        assertTrue(BlockLockerEveryoneText.matches(rgb + "[Everyone]", Set.of("Everyone")));
        assertEquals(Optional.empty(),
                BlockLockerHeaderMatcher.match(rgb + "[Unknown]", PRIVATE, MORE));
    }

    @Test void unsupportedFormattingSequencesCannotTurnUnknownTextIntoKnownTag() throws Exception {
        for (String prefix : new String[] {"§g", "§z", "§?", "§"}) {
            assertEquals(Optional.empty(),
                    BlockLockerHeaderMatcher.match(prefix + "[Private]", PRIVATE, MORE), prefix);
            assertFalse(BlockLockerEveryoneText.matches(prefix + "[Everyone]", Set.of("Everyone")), prefix);
        }
    }

    @Test void aliasCollisionsAndFormattingOnlyAliasesFailClosed() {
        for (String color : new String[] {"§a", "§B", "§r", "§x"}) {
            assertThrows(IOException.class, () -> BlockLockerHeaderMatcher.match(
                    "[Private]", Set.of(color + "[Private]"), Set.of("[private]")), color);
            assertThrows(IOException.class, () -> BlockLockerHeaderMatcher.match(
                    "[Private]", Set.of("[Private]", color), MORE), color);
        }
    }
}
