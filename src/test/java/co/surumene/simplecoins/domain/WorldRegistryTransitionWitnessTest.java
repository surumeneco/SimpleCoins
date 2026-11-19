package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Registered Paper World lifecycle changes invalidate read-only discovery. */
class WorldRegistryTransitionWitnessTest {

    @Test void priorWorldStartupEventsAreIncludedInCapturedBaseline() throws Exception {
        var registry = new WorldRegistryTransitionWitness();
        registry.observeTransition();  // world creation before ServerLoadEvent
        registry.observeTransition();  // another dimension before first capture
        var captured = registry.snapshot();
        registry.requireUnchanged(captured);
        assertEquals(captured, registry.snapshot());
    }

    @Test void transientUnloadAndReloadMustNotValidateAsSameWorldGeneration()
            throws Exception {
        var registry = new WorldRegistryTransitionWitness();
        var before = registry.snapshot();
        registry.observeTransition(); // WorldUnloadEvent
        registry.observeTransition(); // WorldLoadEvent: same name/UUID/folder
        assertThrows(IOException.class, () -> registry.requireUnchanged(before));
    }

    @Test void singlePostObservationWorldTransitionInvalidatesEarlierToken() throws Exception {
        var registry = new WorldRegistryTransitionWitness();
        var first = registry.snapshot();
        registry.observeTransition();
        assertThrows(IOException.class, () -> registry.requireUnchanged(first));
        var second = registry.snapshot();
        registry.requireUnchanged(second);
        assertNotEquals(first, second);
        registry.observeTransition();
        assertThrows(IOException.class, () -> registry.requireUnchanged(second));
    }

    @Test void absentTokenMustNotValidateWorldDiscovery() {
        var registry = new WorldRegistryTransitionWitness();
        assertThrows(IOException.class, () -> registry.requireUnchanged(null));
    }
}
