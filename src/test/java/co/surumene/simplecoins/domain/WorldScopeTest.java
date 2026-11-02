package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorldScopeTest {
    @Test void conflictsRevertToDefault() {
        var off = new WorldScope(false, Set.of("on", "both"), Set.of("off", "both"));
        assertTrue(off.includes("on"));
        assertFalse(off.includes("off"));
        assertFalse(off.includes("both"));
        assertFalse(off.includes("other"));
        var on = new WorldScope(true, Set.of("both"), Set.of("both"));
        assertTrue(on.includes("both"));
    }

    @Test void independentPlayerdataIsNeverExcluded() {
        var scope = new WorldScope(false, Set.of(), Set.of("world"));
        assertTrue(scope.includes(null));
        assertFalse(scope.includes("world"));
    }

    @Test void capturesImmutableConfiguration() {
        var selected = new java.util.HashSet<>(Set.of("one"));
        var scope = new WorldScope(false, selected, Set.of());
        selected.add("two");
        assertTrue(scope.includes("one"));
        assertFalse(scope.includes("two"));
        assertThrows(UnsupportedOperationException.class, () -> scope.included().add("three"));
    }
}
