package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * One-shot background filesystem inventory for guarded startup. A discovered
 * Manifest contains file names ONLY, not a verified physical-money snapshot.
 *
 * No loaded Chunk, player inventory or Bukkit state is accessed off thread;
 * Paper world names and paths are captured before the worker is scheduled.
 */
public final class PaperSavedSourceDiscovery implements AutoCloseable {
    private static final long DISCOVERY_TIMEOUT_TICKS = 20L * 180L;

    private final JavaPlugin plugin;
    private final WorldScope scope;
    private final int maxSourceEntries;
    private final Runnable safeStop;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean finished = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<WorldStorageDiscovery.Manifest> observed =
            new AtomicReference<>();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();

    public PaperSavedSourceDiscovery(JavaPlugin plugin, WorldScope scope,
                                     int maxSourceEntries, Runnable safeStop) {
        this.plugin = Objects.requireNonNull(plugin, "Paper plugin");
        this.scope = Objects.requireNonNull(scope, "world inclusion");
        if (maxSourceEntries < 1)
            throw new IllegalArgumentException("Invalid discovery maximum");
        this.maxSourceEntries = maxSourceEntries;
        this.safeStop = Objects.requireNonNull(safeStop, "safe stop on failure");
    }

    public void start() {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("World roots must be captured on Paper main thread");
        if (!started.compareAndSet(false, true))
            throw new IllegalStateException("Saved-source discovery is one-shot");

        Map<String, Path> worldRoots = new LinkedHashMap<>();
        for (World world : Bukkit.getWorlds()) {
            if (world == null || world.getName().isBlank())
                throw new IllegalStateException("Invalid live Paper world");
            Path prior = worldRoots.putIfAbsent(
                    world.getName(), world.getWorldFolder().toPath().toAbsolutePath().normalize());
            if (prior != null)
                throw new IllegalStateException("Duplicate live Paper world name: " + world.getName());
        }
        if (worldRoots.isEmpty())
            throw new IllegalStateException("No loaded worlds for source inventory");
        Map<String, Path> immutableRoots = Map.copyOf(worldRoots);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                var manifest = WorldStorageDiscovery.discover(
                        immutableRoots, scope, maxSourceEntries);
                // A directory that gains a NEW saved file while being walked
                // must not be reported as an unchanged discovered generation.
                manifest.requireUnchangedDiscovery();
                if (closed.get() || !finished.compareAndSet(false, true)) return;
                observed.set(manifest);
                plugin.getLogger().info("SIMPLECOINS_SAVED_DISCOVERY_OBSERVED"
                        + " worldRoots=" + immutableRoots.size()
                        + " authoritativeFiles=" + manifest.worldFiles().size()
                        + " playerdataFiles=" + manifest.playerDataFiles().size()
                        + " (UNPUBLISHED; admission remains disabled)");
            } catch (Exception | Error fail) {
                signalFailure(fail);
            }
        });

        // A stalled filesystem walk must not leave an apparently successful
        // startup. This watchdog runs on Paper's main thread, not on the worker.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (closed.get() || !finished.compareAndSet(false, true)) return;
            IOException timeout = new IOException(
                    "Saved-source discovery did not finish within 180 seconds");
            failure.compareAndSet(null, timeout);
            plugin.getLogger().log(Level.SEVERE, "SIMPLECOINS_SAVED_DISCOVERY_FAILED", timeout);
            safeStop.run();
        }, DISCOVERY_TIMEOUT_TICKS);
    }

    private void signalFailure(Throwable fail) {
        if (closed.get() || !finished.compareAndSet(false, true)) return;
        failure.compareAndSet(null, fail);
        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (closed.get() || !plugin.isEnabled()) return;
                plugin.getLogger().log(Level.SEVERE,
                        "SIMPLECOINS_SAVED_DISCOVERY_FAILED", fail);
                safeStop.run();
            });
        } catch (RuntimeException schedulerUnavailable) {
            // Paper may already be shutting down; the admission guard has
            // never been opened, so do not schedule an unsafe late callback.
            if (!closed.get())
                plugin.getLogger().log(Level.SEVERE,
                        "Cannot dispatch source-discovery failure during shutdown", schedulerUnavailable);
        }
    }

    public Optional<WorldStorageDiscovery.Manifest> observed() {
        if (closed.get()) return Optional.empty();
        return Optional.ofNullable(observed.get());
    }

    public Optional<Throwable> failure() {
        return Optional.ofNullable(failure.get());
    }

    @Override public void close() {
        closed.set(true);
        observed.set(null);
    }
}
