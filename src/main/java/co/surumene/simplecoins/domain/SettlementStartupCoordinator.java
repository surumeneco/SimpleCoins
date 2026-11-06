package co.surumene.simplecoins.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Experimental startup coordinator. Its callbacks must supply an authoritative,
 * fully validated snapshot and an atomic main-thread publisher. This class cannot
 * verify Paper threading, live world quiescence or scanner completeness.
 *
 * The failure callback is a THREAD-SAFE SIGNAL ONLY; any Bukkit shutdown must be
 * dispatched by the integrating plugin on the main thread.
 */
public final class SettlementStartupCoordinator<T> {
    @FunctionalInterface public interface Stage<T> { T run() throws Exception; }
    @FunctionalInterface public interface Publish<T> { void run(T result) throws Exception; }

    private final AdmissionBarrier gate;
    private final Executor background;
    private final Executor mainThread;
    private final Stage<T> stage;
    private final Publish<T> publisher;
    private final BooleanSupplier noOnlinePlayers;
    private final Consumer<Throwable> failureSignal;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();

    public SettlementStartupCoordinator(AdmissionBarrier gate, Executor background,
                                         Executor mainThread, Stage<T> stage,
                                         Publish<T> publisher, BooleanSupplier noOnlinePlayers,
                                         Consumer<Throwable> failureSignal) {
        this.gate = Objects.requireNonNull(gate);
        this.background = Objects.requireNonNull(background);
        this.mainThread = Objects.requireNonNull(mainThread);
        this.stage = Objects.requireNonNull(stage);
        this.publisher = Objects.requireNonNull(publisher);
        this.noOnlinePlayers = Objects.requireNonNull(noOnlinePlayers);
        this.failureSignal = Objects.requireNonNull(failureSignal);
    }

    public void start() {
        if (!started.compareAndSet(false, true))
            throw new IllegalStateException("Startup settlement already scheduled");
        final long generation = gate.begin();
        try {
            background.execute(() -> {
                try {
                    T candidate = Objects.requireNonNull(stage.run(), "Missing verified snapshot");
                    mainThread.execute(() -> publish(generation, candidate));
                } catch (Throwable error) {
                    abort(error);
                }
            });
        } catch (Throwable error) {
            abort(error);
        }
    }

    private void publish(long generation, T candidate) {
        try {
            if (gate.state() != AdmissionBarrier.State.SCANNING)
                throw new IllegalStateException("Settlement stopped before publication");
            if (!noOnlinePlayers.getAsBoolean())
                throw new IllegalStateException("Players online before settlement publication");
            // The actual publisher must be all-or-nothing; this cannot enforce it.
            publisher.run(candidate);
            if (!noOnlinePlayers.getAsBoolean())
                throw new IllegalStateException("Players online during settlement publication");
            gate.complete(generation, true, true);
        } catch (Throwable error) {
            abort(error);
        }
    }

    /** Failure is terminal; no stale worker can re-open the gate. */
    private void abort(Throwable error) {
        if (failure.compareAndSet(null, error)) {
            gate.fatalFailure();
            try {
                failureSignal.accept(error);
            } catch (Throwable signalFailure) {
                if (signalFailure != error) error.addSuppressed(signalFailure);
            }
        }
    }

    public Optional<Throwable> failure() {
        return Optional.ofNullable(failure.get());
    }
}
