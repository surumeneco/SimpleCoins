package co.surumene.simplecoins;

import java.io.IOException;
import java.nio.file.Path;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PackDelivery implements Listener {
    private final SimpleCoinsPlugin plugin;
    private final byte[] hash;

    public PackDelivery(SimpleCoinsPlugin plugin, Path archive) throws IOException {
        this.plugin = plugin;
        this.hash = co.surumene.simplecoins.domain.ResourcePackHash.sha1(archive);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String configured = plugin.getConfig().getString("resource-pack.public-url", "");
        try {
            var publicUrl = co.surumene.simplecoins.domain.ResourcePackUrl.validate(configured);
            if (publicUrl.isPresent()) event.getPlayer().setResourcePack(publicUrl.get(), hash);
        } catch (IOException invalid) {
            plugin.getLogger().severe("Invalid resource-pack.public-url: " + invalid.getMessage());
        }
    }
}
