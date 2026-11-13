package co.surumene.simplecoins.guard;

import co.surumene.simplecoins.guard.api.AdmissionPermit;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Independent STARTUP-loaded connection gate, deliberately loaded before the
 * SimpleCoins accounting plugin. It remains active if SimpleCoins fails to
 * enable or is disabled, assuming this independent guard itself was loaded.
 *
 * IMPORTANT: a server missing BOTH plugins still needs an external startup
 * preflight; no missing JavaPlugin can reject connections from a running JVM.
 */
public final class SimpleCoinsAdmissionGuard extends JavaPlugin implements Listener {
    private static final Component UNAVAILABLE = Component.text(
            "Server accounting is not verified. Please reconnect later.");

    private volatile boolean stopping;

    @Override
    public void onEnable() {
        stopping = false;
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Admission guard active (default-deny until verified SimpleCoins service)");
    }

    /**
     * The ServicesManager identity check prevents an unrelated plugin from
     * announcing a superficially healthy accounting state.
     */
    private boolean canAdmit() {
        if (stopping) return false;
        try {
            RegisteredServiceProvider<AdmissionPermit> registration =
                    getServer().getServicesManager().getRegistration(AdmissionPermit.class);
            if (registration == null) return false;
            var owner = registration.getPlugin();
            boolean ownerReady = owner != null && "SimpleCoins".equals(owner.getName())
                    && owner.isEnabled();
            if (!ownerReady) return false;
            return AdmissionGuardPolicy.allowed(true, true,
                    registration.getProvider().mayAdmit(), stopping);
        } catch (Throwable failure) {
            // Fail closed. The permission provider can fail independently of
            // the listener, but it may never open a connection on exception.
            getLogger().log(Level.SEVERE, "Accounting permission signal failed; denying login", failure);
            return false;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConnection(PlayerConnectionValidateLoginEvent event) {
        if (event.isAllowed() && !canAdmit()) event.kickMessage(UNAVAILABLE);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSimpleCoinsDisabled(PluginDisableEvent event) {
        if ("SimpleCoins".equals(event.getPlugin().getName())) {
            // PluginDisableEvent may fire before the ServiceManager removes the
            // main plugin's provider. Revoke immediately; Netty logins may be
            // checked off-thread while the disable sequence is still running.
            stopping = true;
            getLogger().severe("SimpleCoins disabled while guard is active; stopping server");
            Bukkit.shutdown();
        }
    }

    @Override
    public void onDisable() {
        stopping = true;
        // When an administrator disables only the guard, the trusted listener
        // disappears. Never continue accepting logins after its removal.
        getLogger().severe("SimpleCoins admission guard disabled; stopping server");
        Bukkit.shutdown();
    }
}
