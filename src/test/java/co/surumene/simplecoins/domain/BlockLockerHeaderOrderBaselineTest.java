package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Observed code-to-spec contract; matching a header is not ownership proof. */
class BlockLockerHeaderOrderBaselineTest {
    private static Set<String> aliases(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    @Test void matchingDoesNotDependOnRuntimeAliasIterationOrder() throws Exception {
        for (Set<String> privateNames : List.of(
                aliases("[Private]", "[Privat]"), aliases("[Privat]", "[Private]"))) {
            for (Set<String> moreNames : List.of(
                    aliases("[More Users]", "[Andere]"), aliases("[Andere]", "[More Users]"))) {
                assertEquals(Optional.of(BlockLockerHeaderMatcher.Type.PRIVATE),
                        BlockLockerHeaderMatcher.match(" §a[PRIVATE]§r ", privateNames, moreNames));
                assertEquals(Optional.of(BlockLockerHeaderMatcher.Type.MORE_USERS),
                        BlockLockerHeaderMatcher.match("[Andere]", privateNames, moreNames));
                assertEquals(Optional.empty(),
                        BlockLockerHeaderMatcher.match("[Unrelated]", privateNames, moreNames));
            }
        }
    }

    @Test void crossTypeCollisionAlwaysRejectsBeforePositiveMatch() {
        for (Set<String> privateNames : List.of(
                aliases("[Private]", "[Privat]"), aliases("[Privat]", "[Private]"))) {
            for (Set<String> moreNames : List.of(
                    aliases("[More Users]", " §b[private]§r "),
                    aliases(" §b[private]§r ", "[More Users]"))) {
                assertThrows(IOException.class, () -> BlockLockerHeaderMatcher.match(
                        "[Private]", privateNames, moreNames));
            }
        }
    }

    @Test void malformedAliasAlwaysRejectsBeforePositiveMatch() {
        for (Set<String> privateNames : List.of(
                aliases("[Private]", "§a"), aliases("§a", "[Private]"))) {
            assertThrows(IOException.class, () -> BlockLockerHeaderMatcher.match(
                    "[Private]", privateNames, aliases("[More Users]")));
        }
        for (Set<String> moreNames : List.of(
                aliases("[More Users]", null), aliases(null, "[More Users]"))) {
            assertThrows(IOException.class, () -> BlockLockerHeaderMatcher.match(
                    "[Private]", aliases("[Private]"), moreNames));
        }
    }

    @Test void regexMetacharactersAreComparedAsLiteralHeaderText() throws Exception {
        Set<String> privateNames = aliases("[Pri.*]");
        Set<String> moreNames = aliases("[More Users]");
        assertEquals(Optional.of(BlockLockerHeaderMatcher.Type.PRIVATE),
                BlockLockerHeaderMatcher.match("[Pri.*]", privateNames, moreNames));
        assertEquals(Optional.empty(),
                BlockLockerHeaderMatcher.match("[Private]", privateNames, moreNames));
    }
}
