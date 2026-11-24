package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.*;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

/** Actual product startup window, discovery and complete saved/live handoff.
 * A prepared result is not a permit. No native writer Agent is required. */
public final class PaperStartupSettlementCoordinator implements AutoCloseable {
    private final JavaPlugin plugin;
    private final AdmissionBarrier admission;
    private final SettlementRuntime runtime;
    private final PaperProtectedSettlementService settlement;
    private final WorldScope scope;
    private final BooleanSupplier normalTracking;
    private final PaperEntityAccounting entityAccounting;
    private final Map<World, Boolean> originalAutosave = new LinkedHashMap<>();
    private boolean originalFrozen, active;
    private long startedAt;
    private PaperStartupSettlementSource source;

    public PaperStartupSettlementCoordinator(JavaPlugin plugin, AdmissionBarrier admission,
            SettlementRuntime runtime, PaperProtectedSettlementService settlement,
            WorldScope scope, BooleanSupplier normalTracking, PaperEntityAccounting entityAccounting) {
        this.plugin=Objects.requireNonNull(plugin);
        this.admission=Objects.requireNonNull(admission);
        this.runtime=Objects.requireNonNull(runtime);
        this.settlement=Objects.requireNonNull(settlement);
        this.scope=Objects.requireNonNull(scope);
        this.normalTracking=Objects.requireNonNull(normalTracking);
        this.entityAccounting=Objects.requireNonNull(entityAccounting);
    }

    /** After ServerLoadEvent and before starting saved discovery. */
    public long beginWindow() {
        if (!Bukkit.isPrimaryThread() || active || admission.state()!=AdmissionBarrier.State.BLOCKED
                || !Bukkit.getOnlinePlayers().isEmpty() || runtime.mayAdmit())
            throw new IllegalStateException("Unsafe static startup window");
        startedAt=System.nanoTime();
        var ticks=Bukkit.getServer().getServerTickManager();
        originalFrozen=ticks.isFrozen();
        active=true;
        try {
            ticks.setFrozen(true);
            for (World world : Bukkit.getWorlds()) {
                originalAutosave.put(world,world.isAutoSave());
                world.setAutoSave(false);
            }
            if (originalAutosave.isEmpty()) throw new IllegalStateException("No registered Worlds");
            plugin.getLogger().info("SIMPLECOINS_STARTUP_STATIC_WINDOW_BEGIN worlds="+originalAutosave.size());
            return 40L;
        } catch (RuntimeException | Error failure) {
            close();
            throw failure;
        }
    }

    private void requireWindow() throws IOException {
        if (!Bukkit.isPrimaryThread() || !active || runtime.mayAdmit()
                || !Bukkit.getOnlinePlayers().isEmpty()
                || !Bukkit.getServer().getServerTickManager().isFrozen()
                || !new HashSet<>(Bukkit.getWorlds()).equals(originalAutosave.keySet()))
            throw new IOException("Managed startup window no longer active");
        for (World world : originalAutosave.keySet())
            if (world.isAutoSave()) throw new IOException("World autosave reenabled during settlement");
    }

    private static BanSourceCoverage currentBans() throws IOException {
        // The target server uses standard Paper BAN lists; a nonempty IP
        // list requires authenticated UUID associations before publication.
        return PaperProfileBanSource.captureCompleteStandard(Set.of("paper-profile","paper-ip"),Map.of());
    }

    public void onDiscovered(WorldStorageDiscovery.Manifest manifest) throws IOException {
        requireWindow();
        var budget = new PaperLiveUniverseAssets.Budget(256, 65536, 262144,
                262144, 8_000_000, 1, 16*1024*1024, 64*1024*1024);
        source=PaperStartupSettlementSource.capturePrimary(plugin, Bukkit.getWorlds(),manifest,scope,
                currentBans(),budget,2_000_000);
        plugin.getLogger().info("SIMPLECOINS_PRODUCT_SETTLEMENT_WORKER_BEGIN files="
                +manifest.worldFiles().size()+" players="+manifest.playerDataFiles().size());
        settlement.startMemory(source, normalTracking, () -> {
            try {
                requireWindow();
                source.requireCurrentPrimary(currentBans());
                // Only reached after actual complete worker preparation.
                boolean tracking=normalTracking.getAsBoolean();
                if (tracking) source.seedNormalWcl(entityAccounting);
                if (!tracking) plugin.getLogger().warning("SIMPLECOINS_STARTUP_NORMAL_TRACKING_INCOMPLETE");
                return new SettlementWriterEpochDecision.Evidence(true,true,true,tracking,true,false);
            } catch (IOException failure) {
                throw new IllegalStateException("Startup final readback failed",failure);
            }
        }, 300_000, prepared -> {
            var snapshot=prepared.summary();
            BigInteger p=snapshot.personal().values().stream().reduce(BigInteger.ZERO,BigInteger::add);
            BigInteger d=snapshot.dead().values().stream().reduce(BigInteger.ZERO,BigInteger::add);
            long elapsed=(System.nanoTime()-startedAt)/1_000_000;
            plugin.getLogger().info("SIMPLECOINS_PRODUCT_SETTLEMENT_PREPARED_UNPUBLISHED personal="+p
                    +" free="+snapshot.free()+" dead="+d+" registered="+snapshot.personal().size()
                    +" elapsedMillis="+elapsed);
        }, () -> {
            close();
            plugin.getLogger().info("SIMPLECOINS_PRODUCT_SETTLEMENT_PUBLISHED");
        });
    }

    /** Restore the original settings. Never open admission here. */
    @Override public void close() {
        if (!active) return;
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Primary thread required");
        active=false;
        try {
            for (var entry : originalAutosave.entrySet())
                entry.getKey().setAutoSave(entry.getValue());
        } finally {
            originalAutosave.clear();
            Bukkit.getServer().getServerTickManager().setFrozen(originalFrozen);
            source=null;
        }
    }
}
