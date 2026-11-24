package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.JavaPackDelivery;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.Plugin;

/** Sends only the SimpleCoins Java pack, without replacing other server packs. */
public final class PaperJavaPackDelivery implements Listener {
    private final Plugin owner;
    private final JavaPackDelivery delivery;
    private boolean warned;

    public PaperJavaPackDelivery(Plugin owner, JavaPackDelivery delivery) {
        this.owner = Objects.requireNonNull(owner);
        this.delivery = Objects.requireNonNull(delivery);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (owner.getServer().getPluginManager().isPluginEnabled("Nexo")) return;
        try {
            delivery.request(false, isJava(player.getUniqueId()), (id, url, hash) -> {
                player.addResourcePack(id, url, hash, "SimpleCoins currency textures", false);
                owner.getLogger().info("SIMPLECOINS_JAVA_PACK_REQUESTED player=" + player.getUniqueId());
            });
        } catch (RuntimeException failure) {
            // Do not log the URL: it may include a signed access token.
            owner.getLogger().warning("SimpleCoins Java pack request failed; check the configured URL and client.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStatus(PlayerResourcePackStatusEvent event) {
        if (JavaPackDelivery.PACK_ID.equals(event.getID()))
            owner.getLogger().info("SIMPLECOINS_JAVA_PACK_STATUS player=" + event.getPlayer().getUniqueId()
                    + " status=" + event.getStatus());
    }

    private boolean isJava(UUID id) {
        var plugins = owner.getServer().getPluginManager();
        try {
            // Optional APIs are resolved via the installed plugin's loader;
            // no hard dependency or guessed username prefix.
            Plugin geyser = plugins.getPlugin("Geyser-Spigot");
            Plugin floodgate = plugins.getPlugin("floodgate");
            if (geyser != null && geyser.isEnabled() && isBedrock(geyser,
                    "org.geysermc.geyser.api.GeyserApi", "api", id)) return false;
            if (floodgate != null && floodgate.isEnabled() && isBedrock(floodgate,
                    "org.geysermc.floodgate.api.FloodgateApi", "getInstance", id)) return false;
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException unavailable) {
            if (!warned) {
                warned = true;
                owner.getLogger().warning("Cannot determine JE/BE connection through Geyser/Floodgate; skipping Java pack request.");
            }
            return false;
        }
    }

    private static boolean isBedrock(Plugin plugin, String apiName, String accessor, UUID id)
            throws ReflectiveOperationException {
        Class<?> api = Class.forName(apiName, true, plugin.getClass().getClassLoader());
        Object instance = api.getMethod(accessor).invoke(null);
        return (Boolean) api.getMethod("isBedrockPlayer", UUID.class).invoke(instance, id);
    }
}
