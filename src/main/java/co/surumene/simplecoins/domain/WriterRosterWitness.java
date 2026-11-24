package co.surumene.simplecoins.domain;

import java.util.Map;
import java.util.Objects;

/** Registration continuity only; never proof of hook coverage or loaded byte integrity. */
public final class WriterRosterWitness implements AutoCloseable {
    public record Identity(Object instance, Object loader, String version, boolean enabled) {
        public Identity {
            Objects.requireNonNull(instance); Objects.requireNonNull(loader); Objects.requireNonNull(version);
        }
        private boolean same(Identity other) {
            return other != null && instance == other.instance && loader == other.loader
                    && version.equals(other.version) && enabled == other.enabled;
        }
    }
    private Map<String, Identity> baseline;
    private boolean invalidated;

    public synchronized void capture(Map<String, Identity> identities) {
        if (baseline != null || invalidated) throw new IllegalStateException("Roster cannot be recaptured");
        Map<String, Identity> copy = Map.copyOf(identities);
        if (copy.isEmpty()) throw new IllegalArgumentException("Empty plugin roster");
        baseline = copy;
    }
    public synchronized boolean current() { return baseline != null && !invalidated; }
    public synchronized void transition() { invalidated = true; }
    public synchronized boolean check(Map<String, Identity> identities) {
        if (!current()) return false;
        if (!baseline.keySet().equals(identities.keySet()) || baseline.entrySet().stream()
                .anyMatch(entry -> !entry.getValue().same(identities.get(entry.getKey()))))
            invalidated = true;
        return !invalidated;
    }
    @Override public synchronized void close() { invalidated = true; baseline = null; }
}
