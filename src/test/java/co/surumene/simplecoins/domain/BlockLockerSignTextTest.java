package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Text matching is not proof of attachment, owner identity or settlement. */
class BlockLockerSignTextTest {
    private static final Set<String> PRIVATE = Set.of("[Private]");
    private static final Set<String> MORE = Set.of("[More Users]");

    @Test void privateAndMoreUsersUseOnlyExactLocalizedVisibleHeaders() throws Exception {
        assertEquals(Optional.of(BlockLockerHeaderMatcher.Type.PRIVATE),
                BlockLockerHeaderMatcher.match(" §a[Private]§r ", PRIVATE, MORE));
        assertEquals(Optional.of(BlockLockerHeaderMatcher.Type.MORE_USERS),
                BlockLockerHeaderMatcher.match("[More Users]", PRIVATE, MORE));
        assertEquals(Optional.empty(),
                BlockLockerHeaderMatcher.match("[Private] other", PRIVATE, MORE));
        assertEquals(Optional.empty(),
                BlockLockerHeaderMatcher.match("[Everyone]", PRIVATE, MORE));
    }

    @Test void overlappingOrMissingRuntimeHeaderAliasesFailClosed() {
        assertThrows(IOException.class, () -> BlockLockerHeaderMatcher.match(
                "[Private]", Set.of("[Private]"), Set.of(" §a[private] ")));
        assertThrows(IOException.class, () -> BlockLockerHeaderMatcher.match(
                "[Private]", Set.of(), MORE));
        assertThrows(IOException.class, () -> BlockLockerHeaderMatcher.match(
                null, PRIVATE, MORE));
    }

    @Test void everyoneRequiresExactBracketedRuntimeAlias() throws Exception {
        assertTrue(BlockLockerEveryoneText.matches("[Everyone]", Set.of("Everyone")));
        assertTrue(BlockLockerEveryoneText.matches(" §a[Everyone]§r ", Set.of("Everyone")));
        assertTrue(BlockLockerEveryoneText.matches("[Alle]", Set.of("Alle")));
        assertFalse(BlockLockerEveryoneText.matches("Everyone", Set.of("Everyone")));
        assertFalse(BlockLockerEveryoneText.matches("[Everyone] other", Set.of("Everyone")));
        assertFalse(BlockLockerEveryoneText.matches("[Everyone]", Set.of("Alle")));
    }

    @Test void everyoneMissingOrMalformedAliasesFailClosed() {
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches(
                "[Everyone]", Set.of()));
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches(
                "[Everyone]", Set.of("[Everyone]")));
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches(
                null, Set.of("Everyone")));
    }
}
