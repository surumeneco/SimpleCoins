package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.NormalMutationJournal;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Cheap, periodic liveness check for the single-writer monetary journal.
 * It does NOT audit mutation files or prove saved-world commit/reconciliation.
 */
public final class PaperNormalJournalWatchdog implements AutoCloseable {
    private static final long FIRST_CHECK_TICKS = 20L;
    private static final long CHECK_PERIOD_TICKS = 100L;

    private final JavaPlugin plugin;
    private final NormalMutationJournal journal;
    private final Runnable failStop;
    private final AtomicBoolean tripped = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private BukkitTask task;

    public PaperNormalJournalWatchdog(JavaPlugin plugin,
                                      NormalMutationJournal journal,
                                      Runnable failStop) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.journal = Objects.requireNonNull(journal, "monetary journal");
        this.failStop = Objects.requireNonNull(failStop, "fail-stop callback");
    }

    public void start() {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Journal watchdog must register on the Paper primary thread");
        if (closed.get() || task != null)
            throw new IllegalStateException("Journal watchdog already started or closed");
        // Reject a corrupt journal BEFORE installing the scheduled monitor.
        journal.checkHealthy();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::poll,
                FIRST_CHECK_TICKS, CHECK_PERIOD_TICKS);
    }

    /** Pure failure gate used by the Paper scheduler and ordinary JUnit. */
    static void checkOnce(Runnable check, AtomicBoolean tripped,
                          Consumer<Throwable> onFailure) {
        Objects.requireNonNull(check, "health check");
        Objects.requireNonNull(tripped, "failure latch");
        Objects.requireNonNull(onFailure, "failure callback");
        if (tripped.get()) return;
        try {
            check.run();
        } catch (RuntimeException | Error failure) {
            if (tripped.compareAndSet(false, true))
                onFailure.accept(failure);
        }
    }

    private void poll() {
        if (closed.get()) return;
        checkOnce(journal::checkHealthy, tripped, failure -> {
            if (closed.get()) return;
            BukkitTask running = task;
            if (running != null) running.cancel();
            plugin.getLogger().log(Level.SEVERE,
                    "SIMPLECOINS_JOURNAL_WATCHDOG_FAILED: revoking admission and stopping Paper",
                    failure);
            failStop.run();
        });
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        BukkitTask running = task;
        task = null;
        if (running != null) running.cancel();
    }
}
