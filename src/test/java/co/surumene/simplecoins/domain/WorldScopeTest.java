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

    /** Code-to-spec: exhaustive observation of the existing override/default behavior. */
    @Test void allExplicitMembershipCombinationsFollowObservedTruthTable() {
        int cases = 0;
        for (boolean defaultIncluded : new boolean[]{false, true}) {
            for (boolean in : new boolean[]{false, true}) {
                for (boolean out : new boolean[]{false, true}) {
                    var scope = new WorldScope(defaultIncluded,
                            in ? Set.of("world") : Set.of(),
                            out ? Set.of("world") : Set.of());
                    assertEquals(in == out ? defaultIncluded : in, scope.includes("world"));
                    assertEquals(defaultIncluded, scope.includes("other"));
                    assertTrue(scope.includes(null));
                    cases++;
                }
            }
        }
        assertEquals(8, cases);
    }

    @Test void bothConfigurationSetsRemainSnapshotsAfterCallerMutatesThem() {
        var included = new java.util.HashSet<>(Set.of("included"));
        var excluded = new java.util.HashSet<>(Set.of("excluded"));
        var scope = new WorldScope(false, included, excluded);
        included.clear();
        excluded.clear();
        included.add("excluded");
        excluded.add("included");
        assertTrue(scope.includes("included"));
        assertFalse(scope.includes("excluded"));
        assertFalse(scope.includes("new"));
        assertThrows(UnsupportedOperationException.class,
                () -> scope.included().add("new"));
        assertThrows(UnsupportedOperationException.class,
                () -> scope.excluded().add("new"));
    }

}
