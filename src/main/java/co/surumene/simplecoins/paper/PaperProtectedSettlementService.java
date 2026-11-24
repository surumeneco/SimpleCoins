package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.NormalMutationJournal;
import co.surumene.simplecoins.domain.SettlementRuntime;
import co.surumene.simplecoins.domain.SettlementWriterEpochDecision;
import co.surumene.simplecoins.domain.WriterEpochFence;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Owns one protected startup handoff, with immutable off-main preparation.
 * Only the enclosing independently audited runtime may start it. Idle bootstrap
 * starts no worker and cannot mint an audited seal or positive evidence. */
public final class PaperProtectedSettlementService implements AutoCloseable {
    private final JavaPlugin owner;
    private final SettlementRuntime runtime;
    private final NormalMutationJournal journal;
    private final Consumer<Throwable> failStop;
    private final ExecutorService executor=Executors.newSingleThreadExecutor(task -> {
        Thread thread=new Thread(task,"simplecoins-protected-settlement");
        thread.setDaemon(true); return thread;
    });
    private boolean started,closed;
    private long deadline;
    private Future<SettlementRuntime.Prepared> future;
    private BukkitTask task;
    private Supplier<SettlementWriterEpochDecision.Evidence> evidence;
    public PaperProtectedSettlementService(JavaPlugin owner, SettlementRuntime runtime,
            NormalMutationJournal journal, Consumer<Throwable> failStop) {
        this.owner=Objects.requireNonNull(owner); this.runtime=Objects.requireNonNull(runtime);
        this.journal=Objects.requireNonNull(journal); this.failStop=Objects.requireNonNull(failStop);
    }
    public void start(WriterEpochFence.Seal seal, SettlementRuntime.SourceStage source,
            BooleanSupplier normalTracking, Supplier<SettlementWriterEpochDecision.Evidence> finalEvidence,
            long timeoutMillis) {
        primary();
        if (closed || started) throw new IllegalStateException("Settlement service closed or used");
        Objects.requireNonNull(source); Objects.requireNonNull(normalTracking); Objects.requireNonNull(finalEvidence);
        if(timeoutMillis<1 || timeoutMillis>300_000) throw new IllegalArgumentException("Bounded settlement timeout required");
        var attempt=runtime.begin(seal,journal,normalTracking,()->Bukkit.getOnlinePlayers().isEmpty());
        started=true; evidence=finalEvidence;
        deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        try {
            future=executor.submit(()->runtime.prepare(attempt,source));
            task=Bukkit.getScheduler().runTaskTimer(owner,this::poll,1L,1L);
        } catch(RuntimeException | Error failure) { fail(failure); throw failure; }
    }
    private void poll() {
        primary();
        if (closed || future==null) return;
        try {
            if (System.nanoTime()-deadline>=0) throw new IllegalStateException("Protected settlement timed out");
            if (!Bukkit.getOnlinePlayers().isEmpty()) throw new IllegalStateException("Players arrived during protected settlement");
            if (!future.isDone()) return;
            var prepared=future.get(); // only after isDone, never waits on disk work
            var observed=Objects.requireNonNull(evidence.get());
            if (System.nanoTime()-deadline>=0) throw new IllegalStateException("Final evidence exceeded settlement deadline");
            if (runtime.complete(prepared,observed)!=SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH)
                throw new IllegalStateException("Incomplete protected settlement evidence");
            stopPolling(); executor.shutdown();
        } catch(Exception | Error failure) { fail(failure); }
    }
    private void stopPolling() {
        if (task!=null) task.cancel(); task=null; future=null; evidence=null;
    }
    private void fail(Throwable failure) { close(); failStop.accept(failure); }
    private static void primary() { if(!Bukkit.isPrimaryThread()) throw new IllegalStateException("Paper primary thread required"); }
    @Override public void close() {
        primary(); if(closed) return; closed=true;
        runtime.revoke();
        if(future!=null) future.cancel(true);
        stopPolling(); executor.shutdownNow();
    }
}
