package co.surumene.simplecoins.domain;

/**
 * Experimental, domain-only admission barrier for a staged server-side settlement.
 * Not a Paper listener, normative contract, or deployed change.
 * Publication verification must be performed by the owner of the settlement process.
 */
public final class AdmissionBarrier {
    public enum State { BLOCKED, SCANNING, READY, FAILED, STOPPED }

    private volatile State state = State.BLOCKED;
    private long currentGeneration;

    /** Admission is denied by default, including before a scan can be started. */
    public boolean mayAdmit() {
        return state == State.READY;
    }

    public State state() {
        return state;
    }

    /** Starts one exclusive generation; the returned token binds its completion. */
    public synchronized long begin() {
        if (state != State.BLOCKED)
            throw new IllegalStateException("Settlement scan cannot begin in state " + state);
        currentGeneration = Math.addExact(currentGeneration, 1);
        state = State.SCANNING;
        return currentGeneration;
    }

    /**
     * The publisher must first validate every source, the new complete snapshot and
     * online-player-zero invariant. A partial/failed snapshot can never open admission.
     */
    public synchronized void complete(long generation, boolean snapshotVerified, boolean zeroOnlinePlayers) {
        requireCurrent(generation);
        if (!snapshotVerified || !zeroOnlinePlayers) {
            state = State.FAILED;
            throw new IllegalStateException("Unverified snapshot or players online");
        }
        state = State.READY;
    }

    public synchronized void fail(long generation) {
        requireCurrent(generation);
        state = State.FAILED;
    }

    /** A post-publication consistency fault must revoke admission immediately. */
    public synchronized void fatalFailure() {
        if (state != State.STOPPED) state = State.FAILED;
    }

    public synchronized void stop() {
        state = State.STOPPED;
    }

    private void requireCurrent(long generation) {
        if (state != State.SCANNING || currentGeneration != generation)
            throw new IllegalStateException("Stale or unexpected settlement generation");
    }
}
