package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.AdmissionBarrier;
import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import co.surumene.simplecoins.domain.WriterEpochFence;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Primary-thread lifecycle owner of the existing protected grant protocol.
 * It never arms the writer fence, grants player admission, registers a command
 * or starts automatically. Only the enclosing audited runtime may start it.
 * A successful partial proof still requires separate multi-session custody.
 */
public final class PaperSavedGrantDeliveryService implements AutoCloseable {
    public record Completion(Optional<SavedGrantDeliveryReadback.Proof> proof) {
        public Completion { Objects.requireNonNull(proof, "verified complete proof or no space"); }
    }

    private final JavaPlugin owner;
    private final AdmissionBarrier admission;
    private final WriterEpochFence fence;
    private final GrantLedger ledger;
    private final CoinItemCodec coins;
    private final Consumer<Throwable> failStop;
    private final SavedGrantCommitWorker worker = new SavedGrantCommitWorker();
    private BukkitTask task;
    private Flight active;
    private boolean closed;

    public PaperSavedGrantDeliveryService(JavaPlugin owner, AdmissionBarrier admission,
            WriterEpochFence fence, GrantLedger ledger, CoinItemCodec coins,
            Consumer<Throwable> failStop) {
        this.owner = Objects.requireNonNull(owner);
        this.admission = Objects.requireNonNull(admission);
        this.fence = Objects.requireNonNull(fence);
        this.ledger = Objects.requireNonNull(ledger);
        this.coins = Objects.requireNonNull(coins);
        this.failStop = Objects.requireNonNull(failStop);
    }

    /** Only for a grant already reserved by the enclosing protected runtime. */
    public void start(Player detached, GrantLedger.Grant grant,
            WorldStorageDiscovery.DiscoveryOrigin origin, long timeoutMillis,
            int compressedLimit, int inflatedLimit, Consumer<Completion> complete) {
        requirePrimary();
        if (closed || active != null) throw new IllegalStateException("Delivery service closed or busy");
        Objects.requireNonNull(complete, "terminal callback");
        // An idle, disarmed service cannot become an issuance route at bootstrap.
        requireProtectedAdmission();
        if (fence.state() != WriterEpochFence.State.OPEN)
            throw new IllegalStateException("An independently audited OPEN fence is required");
        try {
            var pending = PaperGrantSavedBatchCoordinator.schedulePreflightOnMain(detached, ledger,
                    grant, fence, origin, worker, timeoutMillis, compressedLimit, inflatedLimit);
            active = new Flight(detached, timeoutMillis, complete, pending);
            task = Bukkit.getScheduler().runTaskTimer(owner, this::poll, 1L, 1L);
        } catch (RuntimeException | Error failure) {
            fail(failure);
            throw failure;
        }
    }

    private void poll() {
        requirePrimary();
        Flight flight = active;
        if (closed || flight == null) return;
        try {
            requireProtectedAdmission();
            if (flight.commit == null) {
                var status = flight.preflight.status();
                if (status == SavedGrantCommitWorker.Status.RUNNING) return;
                requireSuccess(status);
                var saved = flight.preflight.saveOnMain(flight.detached, coins);
                if (saved.isEmpty()) {
                    finish(flight, new Completion(Optional.empty()));
                    return;
                }
                flight.saved = saved.orElseThrow();
                flight.commit = flight.saved.scheduleOffMain(worker, flight.timeoutMillis);
                return;
            }
            var status = flight.commit.status();
            if (status == SavedGrantCommitWorker.Status.RUNNING) return;
            requireSuccess(status);
            var proof = flight.commit.result();
            if (proof.receipt().active())
                throw new IllegalStateException("Verified partial grant still requires protected custody");
            flight.saved.resumeAfterVerifiedCompleteOnMain();
            finish(flight, new Completion(Optional.of(proof)));
        } catch (IOException | RuntimeException | Error failure) {
            fail(failure);
        }
    }

    private void finish(Flight flight, Completion result) {
        if (closed || active != flight) throw new IllegalStateException("Stale delivery callback");
        stopTask();
        active = null;
        flight.complete.accept(result); // exactly once, only on primary, after verified handback
    }

    private void requireProtectedAdmission() {
        var state = admission.state();
        if ((state != AdmissionBarrier.State.BLOCKED && state != AdmissionBarrier.State.SCANNING)
                || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IllegalStateException("Protected saved delivery requires blocked zero-player admission");
    }

    private static void requirePrimary() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Paper primary thread required");
    }

    private static void requireSuccess(SavedGrantCommitWorker.Status status) {
        if (status != SavedGrantCommitWorker.Status.SUCCEEDED)
            throw new IllegalStateException("Saved grant worker did not succeed: " + status);
    }

    private void stopTask() {
        var running = task;
        task = null;
        if (running != null) running.cancel();
    }

    private void fail(Throwable failure) {
        admission.fatalFailure();
        close(); // abandon custody; never wait on the writer monitor during ACK
        failStop.accept(failure);
    }

    @Override public void close() {
        requirePrimary();
        if (closed) return;
        closed = true;
        if (active != null) admission.fatalFailure();
        active = null;
        stopTask();
        worker.close();
    }

    private static final class Flight {
        final Player detached; // retained on primary only, never captured by worker
        final long timeoutMillis;
        final Consumer<Completion> complete;
        final PaperGrantSavedBatchCoordinator.PendingPreflight preflight;
        PaperGrantSavedBatchCoordinator.SealedFirstBatch saved;
        SavedGrantCommitWorker.Ticket<SavedGrantDeliveryReadback.Proof> commit;

        Flight(Player detached, long timeoutMillis, Consumer<Completion> complete,
                PaperGrantSavedBatchCoordinator.PendingPreflight preflight) {
            this.detached = detached;
            this.timeoutMillis = timeoutMillis;
            this.complete = complete;
            this.preflight = preflight;
        }
    }
}
