package co.surumene.simplecoins;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PackDelivery implements Listener {
    private final SimpleCoinsPlugin plugin;
    private final byte[] hash;

    public PackDelivery(SimpleCoinsPlugin plugin, Path archive) throws IOException {
        this.plugin = plugin;
        try {
            this.hash = MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(archive));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-1 unavailable", impossible);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String publicUrl = plugin.getConfig().getString("resource-pack.public-url", "").trim();
        if (publicUrl.isEmpty()) return; // Operator must host the generated ZIP and configure its URL.
        if (!publicUrl.startsWith("https://") && !publicUrl.startsWith("http://")) {
            plugin.getLogger().severe("resource-pack.public-url must be HTTP(S)");
            return;
        }
        event.getPlayer().setResourcePack(publicUrl, hash);
    }
}
