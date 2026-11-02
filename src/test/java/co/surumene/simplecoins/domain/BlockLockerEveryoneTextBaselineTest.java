package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Observed code-to-spec behavior only; not proof of a protected container. */
class BlockLockerEveryoneTextBaselineTest {
    @Test void coloredVisibleTagMatchesUncoloredRuntimeLocalization() throws Exception {
        assertTrue(BlockLockerEveryoneText.matches(" §a[EVERYONE]§r ", Set.of("Everyone")));
        assertTrue(BlockLockerEveryoneText.matches("§b[全員]", Set.of("Everyone", "全員")));
        assertTrue(BlockLockerEveryoneText.matches("[everyone]", Set.of("Everyone")));
        assertFalse(BlockLockerEveryoneText.matches("[Unknown]", Set.of("Everyone", "全員")));
    }

    @Test void onlyTheEntireBracketedVisibleTagCanMatch() throws Exception {
        Set<String> aliases = Set.of("Everyone");
        assertFalse(BlockLockerEveryoneText.matches("Everyone", aliases));
        assertFalse(BlockLockerEveryoneText.matches("[[Everyone]]", aliases));
        assertFalse(BlockLockerEveryoneText.matches("[Everyone] extra", aliases));
        assertFalse(BlockLockerEveryoneText.matches("prefix [Everyone]", aliases));
        assertFalse(BlockLockerEveryoneText.matches("[Everyone", aliases));
    }

    @Test void missingRuntimeEvidenceFailsClosed() {
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches(null, Set.of("Everyone")));
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches("[Everyone]", null));
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches("[Everyone]", Set.of()));
    }

    @Test void anyMalformedAliasRejectsTheEntireRuntimeSet() {
        Set<String> names = new HashSet<>(Set.of("Everyone"));
        names.add(null);
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches("[Everyone]", names));
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches("[Everyone]", Set.of("Everyone", " ")));
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches("[Everyone]", Set.of("Everyone", "[Everyone]")));
        assertThrows(IOException.class, () -> BlockLockerEveryoneText.matches("[Everyone]", Set.of("Everyone", "Everyone]")));
    }
}
