package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import co.surumene.simplecoins.domain.WorldSourceIdentitySnapshot;
import co.surumene.simplecoins.domain.PaperSavedWorldMetadataWitness;
import co.surumene.simplecoins.domain.WorldRegistryTransitionWitness;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
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
    private static final int MAX_PLAYERDATA_COMPRESSED_BYTES = 16 * 1024 * 1024;
    private static final int MAX_PLAYERDATA_INFLATED_BYTES = 64 * 1024 * 1024;

    private final JavaPlugin plugin;
    private final WorldScope scope;
    private final int maxSourceEntries;
    private final Runnable safeStop;
    private final GrantLedger issuanceLedger;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean launched = new AtomicBoolean();
    private final AtomicBoolean finished = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<WorldStorageDiscovery.Manifest> observed =
            new AtomicReference<>();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private final WorldRegistryTransitionWitness worldTransitions =
            new WorldRegistryTransitionWitness();

    public PaperSavedSourceDiscovery(JavaPlugin plugin, WorldScope scope,
                                     int maxSourceEntries, Runnable safeStop) {
        this(plugin, scope, maxSourceEntries, safeStop, null);
    }

    /** A non-null verified journal requires a same-source saved receipt audit. */
    public PaperSavedSourceDiscovery(JavaPlugin plugin, WorldScope scope,
                                     int maxSourceEntries, Runnable safeStop,
                                     GrantLedger issuanceLedger) {
        this.plugin = Objects.requireNonNull(plugin, "Paper plugin");
        this.scope = Objects.requireNonNull(scope, "world inclusion");
        if (maxSourceEntries < 1)
            throw new IllegalArgumentException("Invalid discovery maximum");
        this.maxSourceEntries = maxSourceEntries;
        this.safeStop = Objects.requireNonNull(safeStop, "safe stop on failure");
        this.issuanceLedger = issuanceLedger;
    }

    private record WorldRoots(Map<String, Path> immutableRoots,
                              Set<Path> immutablePlayerRoots,
                              WorldSourceIdentitySnapshot capturedIdentities,
                              WorldRegistryTransitionWitness.Snapshot lifecycleVersion) {}

    public void start() {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Saved-source discovery setup requires Paper main thread");
        if (!started.compareAndSet(false, true))
            throw new IllegalStateException("Saved-source discovery is one-shot");

        // Paper initial spawn generation can create new region files after
        // plugin onEnable. A walk begun here would treat expected startup I/O
        // as conflicting monetary authority and unnecessarily stop the server.
        // Collect all currently loaded worlds and begin the strict walk only
        // AFTER ServerLoadEvent, on the next server tick. The manifest and
        // content verification are unchanged; no money is published here.
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onWorldLoaded(WorldLoadEvent event) {
                worldRegistryTransition("loaded", event.getWorld());
            }

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onWorldUnloaded(WorldUnloadEvent event) {
                worldRegistryTransition("unloaded", event.getWorld());
            }

            @EventHandler(priority = EventPriority.MONITOR)
            public void onServerReady(ServerLoadEvent event) {
                if (closed.get() || !launched.compareAndSet(false, true)) return;
                try {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (closed.get()) return;
                        try {
                            WorldRoots roots = captureWorldRoots();
                            plugin.getLogger().info("SIMPLECOINS_SAVED_DISCOVERY_SERVER_READY");
                            launchDiscovery(roots);
                        } catch (IOException | RuntimeException | Error fail) {
                            signalFailure(fail);
                        }
                    });
                } catch (RuntimeException schedulingFailure) {
                    signalFailure(schedulingFailure);
                }
            }
        }, plugin);

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

    /**
     * WorldLoad/Unload is a registration identity change even when a World
     * with the exact same name, UUID and folder reappears. Reject both ABA
     * changes during the async walk and later reuse of a stale observed
     * Manifest. This does not observe item, Chunk or region-file writers.
     */
    private void worldRegistryTransition(String kind, World world) {
        if (closed.get()) return;
        worldTransitions.observeTransition();
        if (observed.getAndSet(null) != null) {
            IOException stale = new IOException(
                    "Paper World " + kind + " after source discovery: "
                    + (world == null ? "<unknown>" : world.getName()));
            failure.compareAndSet(null, stale);
            plugin.getLogger().log(Level.SEVERE,
                    "SIMPLECOINS_SAVED_DISCOVERY_FAILED", stale);
            safeStop.run(); // invoked only on Bukkit's synchronous event thread
        }
    }

    /** Run only on the primary thread after Paper has finished startup load. */
    private WorldRoots captureWorldRoots() throws IOException {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("World roots must be captured on Paper main thread");
        Map<String, Path> worldRoots = new LinkedHashMap<>();
        Map<String, WorldSourceIdentitySnapshot.Identity> identities = new LinkedHashMap<>();
        Set<Path> sharedPlayerRoots = new HashSet<>();
        for (World world : Bukkit.getWorlds()) {
            if (world == null || world.getName().isBlank())
                throw new IllegalStateException("Invalid live Paper world");
            Path worldPath = world.getWorldPath().toAbsolutePath().normalize();
            Path prior = worldRoots.putIfAbsent(world.getName(), worldPath);
            identities.putIfAbsent(world.getName(),
                    new WorldSourceIdentitySnapshot.Identity(world.getUID(), worldPath));
            // Paper 26.2 exposes a dimension folder here. Playerdata is a
            // level-wide source at <level>/players/data, NOT a child of that
            // dimension. Do not guess a sibling world name from Bukkit names.
            Path namespaceFolder = worldPath.getParent();
            Path dimensionsFolder = namespaceFolder == null ? null : namespaceFolder.getParent();
            Path levelFolder = dimensionsFolder == null ? null : dimensionsFolder.getParent();
            if (dimensionsFolder != null && levelFolder != null
                    && dimensionsFolder.getFileName() != null
                    && "dimensions".equals(dimensionsFolder.getFileName().toString())) {
                sharedPlayerRoots.add(levelFolder);
            }
            if (prior != null)
                throw new IllegalStateException("Duplicate live Paper world name: " + world.getName());
        }
        if (worldRoots.isEmpty())
            throw new IllegalStateException("No loaded worlds for source inventory");
        return new WorldRoots(Map.copyOf(worldRoots), Set.copyOf(sharedPlayerRoots),
                WorldSourceIdentitySnapshot.capture(identities),
                worldTransitions.snapshot());

    }

    private record DiscoveryObservation(WorldStorageDiscovery.Manifest manifest,
                                        PaperSavedWorldMetadataWitness.Witness diskWorldIds,
                                        SavedGrantReceiptUniverseAudit.Report receipts) {}

    private void requireDiscoveryActive(WorldRegistryTransitionWitness.Snapshot lifecycle)
            throws IOException {
        if (closed.get() || finished.get() || Thread.currentThread().isInterrupted())
            throw new IOException("Saved discovery no longer active");
        worldTransitions.requireUnchanged(lifecycle);
    }

    private void launchDiscovery(WorldRoots roots) {
        if (closed.get()) return;
        Map<String, Path> immutableRoots = roots.immutableRoots();
        Set<Path> immutablePlayerRoots = roots.immutablePlayerRoots();
        WorldSourceIdentitySnapshot capturedIdentities = roots.capturedIdentities();
        WorldRegistryTransitionWitness.Snapshot capturedLifecycle =
                roots.lifecycleVersion();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Paper may finish initial region saves after ServerLoadEvent.
                // A mismatch between two successful complete discoveries is
                // retryable ONLY here, before any candidate is published.
                // Every attempt starts with fresh source, metadata and receipt
                // evidence, while retaining the original registry generation.
                DiscoveryObservation observation =
                        WorldStorageDiscovery.retryUnpublishedDiscovery(5, () -> {
                            requireDiscoveryActive(capturedLifecycle);
                            var manifest = WorldStorageDiscovery.discover(
                                    immutableRoots, scope, maxSourceEntries, immutablePlayerRoots);
                            capturedIdentities.requireDiscoveredManifest(manifest);
                            PaperSavedWorldMetadataWitness.Witness diskWorldIds =
                                    PaperSavedWorldMetadataWitness.capture(
                                            manifest, capturedIdentities.worldIds());
                            manifest.requireUnchangedDiscovery();
                            SavedGrantReceiptUniverseAudit.Report report = issuanceLedger == null
                                    ? null : SavedGrantReceiptUniverseAudit.scan(
                                            manifest, issuanceLedger,
                                            MAX_PLAYERDATA_COMPRESSED_BYTES,
                                            MAX_PLAYERDATA_INFLATED_BYTES);
                            manifest.requireUnchangedDiscovery();
                            requireDiscoveryActive(capturedLifecycle);
                            diskWorldIds.requireUnchanged();
                            return new DiscoveryObservation(manifest, diskWorldIds, report);
                        }, (attempt, drift) -> {
                            requireDiscoveryActive(capturedLifecycle);
                            plugin.getLogger().warning(
                                    "SIMPLECOINS_SAVED_DISCOVERY_RETRY abandonedAttempt="
                                            + attempt + " maximumAttempts=5; " + drift.getMessage()
                                            + " (UNPUBLISHED; admission remains disabled)");
                            // Only this async worker waits; the existing global
                            // watchdog is not restarted or extended by retries.
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException interrupted) {
                                Thread.currentThread().interrupt();
                                throw new IOException("Saved discovery retry interrupted", interrupted);
                            }
                            requireDiscoveryActive(capturedLifecycle);
                        });
                var manifest = observation.manifest();
                var diskWorldIds = observation.diskWorldIds();
                if (closed.get() || finished.get()) return;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (closed.get() || finished.get()) return;
                    try {
                        WorldRoots current = captureWorldRoots(); // main thread
                        worldTransitions.requireUnchanged(capturedLifecycle);
                        capturedIdentities.requireSameRegisteredWorlds(
                                current.capturedIdentities());
                        if (!immutablePlayerRoots.equals(current.immutablePlayerRoots()))
                            throw new IOException("Paper shared playerdata roots changed during discovery");
                        capturedIdentities.requireDiscoveredManifest(manifest);
                        diskWorldIds.requireUnchanged();
                        if (closed.get() || !finished.compareAndSet(false, true)) return;
                        observed.set(manifest);
                        var report = observation.receipts();
                        if (report != null) {
                            plugin.getLogger().info("SIMPLECOINS_SAVED_GRANT_UNIVERSE_AUDITED"
                                    + " players=" + report.playersRead()
                                    + " acknowledged=" + report.acknowledgedGrants()
                                    + " pending=" + report.pendingGrants()
                                    + " awaitingAcknowledgement=" + report.awaitingAcknowledgement()
                                    + " (OBSERVATION ONLY; admission remains blocked)");
                        }
                        plugin.getLogger().info(
                                "SIMPLECOINS_SAVED_WORLD_SOURCE_IDENTITY_VERIFIED"
                                + " worlds=" + capturedIdentities.worldIds().size()
                                + " (ONE-SHOT PRECONDITION ONLY; no world writer freeze)");
                        plugin.getLogger().info(
                                "SIMPLECOINS_SAVED_WORLD_LIFECYCLE_STABLE"
                                + " (NO WorldLoad/Unload since registered capture;"
                                + " not a writer freeze)");
                        plugin.getLogger().info("SIMPLECOINS_SAVED_DISCOVERY_OBSERVED"
                                + " worldRoots=" + immutableRoots.size()
                                + " authoritativeFiles=" + manifest.worldFiles().size()
                                + " playerdataFiles=" + manifest.playerDataFiles().size()
                                + " (UNPUBLISHED; admission remains disabled)");
                    } catch (IOException | RuntimeException | Error changed) {
                        signalFailure(changed);
                    }
                });
            } catch (Exception | Error fail) {
                signalFailure(fail);
            }
        });

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
