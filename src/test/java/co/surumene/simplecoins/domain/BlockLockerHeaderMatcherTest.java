package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BlockLockerHeaderMatcherTest {
    @Test void privateHeaderMatchesConfiguredLocalizedAliasAndColorCodes() throws Exception {
        var privateAliases = Set.of("[Private]", "[Privat]");
        var moreAliases = Set.of("[More Users]");
        assertEquals(Optional.of(BlockLockerHeaderMatcher.Type.PRIVATE),
                BlockLockerHeaderMatcher.match("  §a[Privat]  ", privateAliases, moreAliases));
        assertEquals(Optional.of(BlockLockerHeaderMatcher.Type.PRIVATE),
                BlockLockerHeaderMatcher.match("[PRIVATE]", privateAliases, moreAliases));
    }

    @Test void moreUsersHeaderIsDistinctFromMainProtectionSign() throws Exception {
        assertEquals(Optional.of(BlockLockerHeaderMatcher.Type.MORE_USERS),
                BlockLockerHeaderMatcher.match("§b[more users]",
                        Set.of("[Private]"), Set.of("[More Users]")));
    }

    @Test void unknownHeaderDoesNotBecomeEvidenceOfUnprotectedContainer() throws Exception {
        assertTrue(BlockLockerHeaderMatcher.match("[Unrecognized]",
                Set.of("[Private]"), Set.of("[More Users]")).isEmpty());
    }

    @Test void missingOrAmbiguousRuntimeAliasConfigurationFailsClosed() {
        assertThrows(IOException.class, () -> BlockLockerHeaderMatcher.match(
                "[Private]", Set.of(), Set.of("[More Users]")));
        assertThrows(IOException.class, () -> BlockLockerHeaderMatcher.match(
                "[Private]", Set.of("[Private]"), Set.of("[PRIVATE]")));
        assertThrows(IOException.class, () -> BlockLockerHeaderMatcher.match(
                "[Private]", Set.of("  "), Set.of("[More Users]")));
        assertThrows(IOException.class, () -> BlockLockerHeaderMatcher.match(
                null, Set.of("[Private]"), Set.of("[More Users]")));
    }
}
