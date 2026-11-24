package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.WriterRosterWitness;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Observes the actual registered plugin universe. It never arms a fence. */
public final class PaperWriterRosterMonitor implements Listener, AutoCloseable {
    private final JavaPlugin owner;
    private final Consumer<String> invalidate;
    private final WriterRosterWitness witness = new WriterRosterWitness();
    private BukkitTask poll;
    private boolean started, captured, closed, failed;

    public PaperWriterRosterMonitor(JavaPlugin owner, Consumer<String> invalidate) {
        this.owner = Objects.requireNonNull(owner);
        this.invalidate = Objects.requireNonNull(invalidate);
    }
    public void start() {
        if (!Bukkit.isPrimaryThread() || started || closed) throw new IllegalStateException("Invalid roster setup");
        started = true;
        Bukkit.getPluginManager().registerEvents(this, owner);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(ServerLoadEvent event) {
        if (closed || failed || poll != null) return;
        poll = Bukkit.getScheduler().runTaskTimer(owner, this::observe, 1L, 20L);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnable(PluginEnableEvent event) { transitioned("enabled", event.getPlugin()); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisable(PluginDisableEvent event) { transitioned("disabled", event.getPlugin()); }
    private void transitioned(String kind, Plugin plugin) {
        if (captured && !closed && !failed) fail("Plugin " + kind + ": " + plugin.getName());
    }
    private void observe() {
        if (closed || failed) return;
        try {
            if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Roster capture requires primary thread");
            Map<String, WriterRosterWitness.Identity> roster = new LinkedHashMap<>();
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                if (roster.putIfAbsent(plugin.getName(), new WriterRosterWitness.Identity(plugin,
                        plugin.getClass().getClassLoader(), plugin.getDescription().getVersion(), plugin.isEnabled())) != null)
                    throw new IllegalStateException("Duplicate plugin ID: " + plugin.getName());
            }
            if (!captured) {
                witness.capture(roster);
                captured = true;
                owner.getLogger().info("SIMPLECOINS_WRITER_ROSTER_OBSERVED count=" + roster.size()
                        + " (registration only; complete writer coverage unproven)");
            } else if (!witness.check(roster)) fail("Registered plugin identity or enabled status changed");
        } catch (RuntimeException e) { fail("Cannot establish plugin roster continuity: " + e); }
    }
    private void fail(String reason) {
        if (closed || failed) return;
        failed = true;
        witness.transition();
        if (poll != null) poll.cancel();
        owner.getLogger().warning("SIMPLECOINS_WRITER_ROSTER_INVALIDATED " + reason);
        invalidate.accept(reason);
    }
    @Override public void close() {
        if (closed) return;
        closed = true;
        witness.close();
        if (poll != null) poll.cancel();
        HandlerList.unregisterAll(this);
    }
}
