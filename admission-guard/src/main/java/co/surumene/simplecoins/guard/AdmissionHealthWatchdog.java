package co.surumene.simplecoins.guard;

/**
 * One-way health latch for a separately loaded admission guard.
 *
 * Before the first verified admission-ready state, absent or incomplete
 * accounting is an ordinary fail-closed startup: reject logins, do not stop.
 * Once a healthy state has been observed (from either the login path or a
 * heartbeat), loss of that state means an accounting generation has failed
 * and the server must shut down. A later positive signal cannot resurrect it.
 *
 * This class observes an already verified health signal; it does not validate
 * journal coverage, snapshots or external plugin state by itself.
 */
public final class AdmissionHealthWatchdog {
    private boolean observedHealthy;
    private boolean terminal;

    /** @return true if the server must stop (and must never reopen admission). */
    public synchronized boolean mustStop(boolean healthy) {
        if (terminal) return true;
        if (healthy) {
            observedHealthy = true;
            return false;
        }
        if (observedHealthy) terminal = true;
        return terminal;
    }

    public synchronized boolean hasObservedHealthy() {
        return observedHealthy;
    }
}
