package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monotonic local observation of Paper WorldLoad/WorldUnload notifications.
 *
 * World identity set equality alone misses an unload followed by a reload
 * with the same name, UUID and folder. A lifecycle transition invalidates
 * the saved-discovery interval even if the set later looks identical.
 *
 * The Paper adapter must register its listeners BEFORE taking snapshot().
 * This is not a world writer exclusion protocol, source-value epoch, or
 * authority to publish settlement statistics or admit players.
 */
public final class WorldRegistryTransitionWitness {
    public record Snapshot(long sequence) {}

    private final AtomicLong sequence = new AtomicLong();

    /** Called only upon an observed actual World lifecycle notification. */
    public void observeTransition() {
        sequence.incrementAndGet();
    }

    /** Bind a source selection to the present observed transition sequence. */
    public Snapshot snapshot() {
        return new Snapshot(sequence.get());
    }

    /**
     * A changed registry lifecycle history is not made valid by a later
     * equal-looking World set. Start a fresh capture instead of resetting
     * the witness of the old generation.
     */
    public void requireUnchanged(Snapshot captured) throws IOException {
        if (captured == null || captured.sequence() != sequence.get())
            throw new IOException("Paper World registry transitioned during saved discovery");
    }
}
