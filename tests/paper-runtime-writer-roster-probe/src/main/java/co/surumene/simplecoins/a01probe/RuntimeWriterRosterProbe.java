package co.surumene.simplecoins.a01probe;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Disposable runtime observation only; never a global writer freeze, a
 * production plugin, a guarantee of loaded classes or an admission permit.
 */
public final class RuntimeWriterRosterProbe extends JavaPlugin {
    @Override public void onEnable() {
        if (!"127.0.0.1".equals(Bukkit.getServer().getIp())
                || !Bukkit.getOnlinePlayers().isEmpty()) {
            getLogger().severe("A01_RUNTIME_ROSTER_INVALID_ISOLATION");
            Bukkit.shutdown();
            return;
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                inspect();
                getLogger().info("A01_RUNTIME_ROSTER_POINT_IN_TIME_OBSERVED");
            } catch (Throwable error) {
                getLogger().log(Level.SEVERE, "A01_RUNTIME_ROSTER_PROBE_FAILED", error);
            } finally {
                Bukkit.shutdown();
            }
        }, 80L);
    }

    private void inspect() throws IOException, URISyntaxException, NoSuchAlgorithmException {
        String configuration = System.getProperty("a01.requiredPluginIds", "");
        Set<String> required = new LinkedHashSet<>();
        for (String item : configuration.split(",", -1)) {
            if (item.isBlank() || !item.matches("[A-Za-z][A-Za-z0-9_+.-]{0,127}")
                    || !required.add(item))
                throw new IllegalArgumentException("Missing/invalid/duplicate expected plugin ID");
        }
        if (required.size() != 5)
            throw new IllegalArgumentException("Expected five fixed runtime writer plugin IDs");

        Map<String, Plugin> loaded = new TreeMap<>();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            String key = plugin.getName();
            if (loaded.putIfAbsent(key, plugin) != null)
                throw new IllegalStateException("Duplicate live Paper plugin ID " + key);
        }
        for (String core : new String[]{"SimpleCoins", "SimpleCoinsGuard",
                "SimpleCoinsRuntimeRosterProbe"}) {
            Plugin plugin = loaded.get(core);
            if (plugin == null || !plugin.isEnabled())
                throw new IllegalStateException("Missing mandatory safe test plugin: " + core);
        }
        Path pluginsRoot = getDataFolder().toPath().toAbsolutePath()
                .normalize().getParent();
        if (pluginsRoot == null || !Files.isDirectory(pluginsRoot,
                LinkOption.NOFOLLOW_LINKS))
            throw new IllegalStateException("Cannot determine actual plugins root");

        for (String name : required) {
            Plugin plugin = loaded.get(name);
            if (plugin == null || !plugin.isEnabled())
                throw new IllegalStateException("Pinned distributed plugin not enabled: " + name);
            CodeSource source = plugin.getClass().getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null)
                throw new IllegalStateException("Loaded plugin lacks CodeSource: " + name);
            Path location = Path.of(source.getLocation().toURI())
                    .toAbsolutePath().normalize();
            if (!location.startsWith(pluginsRoot)
                    || !Files.isRegularFile(location, LinkOption.NOFOLLOW_LINKS))
                throw new IllegalStateException("Unrecognized plugin CodeSource file: " + name);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            long total = 0;
            try (InputStream in = Files.newInputStream(location)) {
                byte[] buffer = new byte[65536];
                int n;
                while ((n = in.read(buffer)) >= 0) {
                    if (n == 0) continue;
                    sha.update(buffer, 0, n);
                    total += n;
                }
            }
            if (total != Files.size(location) || total <= 0)
                throw new IOException("Loaded plugin CodeSource changed while reading: " + name);
            String relative = pluginsRoot.relativize(location).toString();
            getLogger().info("A01_RUNTIME_WRITER_PLUGIN_ENABLED"
                    + " name=" + name
                    + " version=" + plugin.getDescription().getVersion()
                    + " source=" + relative
                    + " sha256=" + HexFormat.of().formatHex(sha.digest())
                    + " bytes=" + total
                    + " codeSourceEqualsOriginalDistribution=unverified");
        }
        if (!Bukkit.getOnlinePlayers().isEmpty())
            throw new IllegalStateException("Players entered during runtime roster capture");
        getLogger().info("A01_ALL_FIVE_PINNED_WRITERS_ENABLED"
                + " expected=" + required.size()
                + " pluginManagerRegistered=" + loaded.size()
                + " zeroPlayers=true"
                + " admissionClosed=true"
                + " writerEpochProven=false");
    }
}
