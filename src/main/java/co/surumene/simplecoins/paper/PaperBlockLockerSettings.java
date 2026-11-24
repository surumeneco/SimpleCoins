package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.SavedBlockLockerTerrainObservations;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Immutable settings captured on the primary thread from installed BlockLocker 1.15. */
public record PaperBlockLockerSettings(
        Set<String> containers, Set<String> privateHeaders,
        Set<String> moreHeaders, Set<String> everyoneAliases) {
    public PaperBlockLockerSettings {
        containers = Set.copyOf(containers);
        privateHeaders = Set.copyOf(privateHeaders);
        moreHeaders = Set.copyOf(moreHeaders);
        everyoneAliases = Set.copyOf(everyoneAliases);
    }

    public static PaperBlockLockerSettings capture() throws IOException {
        if (!Bukkit.isPrimaryThread())
            throw new IOException("BlockLocker settings must be captured on primary");
        Plugin plugin = Bukkit.getPluginManager().getPlugin("BlockLocker");
        if (plugin == null || !plugin.isEnabled()
                || !"1.15".equals(plugin.getDescription().getVersion()))
            throw new IOException("Installed BlockLocker 1.15 is required");
        try {
            ClassLoader loader = plugin.getClass().getClassLoader();
            String prefix = "nl.rutgerkok.blocklocker.";
            Class<?> api = Class.forName(prefix + "BlockLockerAPIv2", false, loader);
            Class<?> provider = Class.forName(prefix + "BlockLockerPlugin", false, loader);
            Object instance = api.getMethod("getPlugin").invoke(null);
            if (instance != plugin) throw new IOException("BlockLocker provider identity mismatch");
            Class<?> settingsType = Class.forName(prefix + "ChestSettings", false, loader);
            Object settings = provider.getMethod("getChestSettings").invoke(instance);
            if (!Boolean.FALSE.equals(settingsType.getMethod("getConnectContainers").invoke(settings))
                    || !(settingsType.getMethod("getChestExpireDate").invoke(settings) instanceof Optional<?> expiry)
                    || expiry.isPresent()
                    || !(settingsType.getMethod("getExtraProtectables").invoke(settings) instanceof List<?> extra)
                    || !extra.isEmpty())
                throw new IOException("Unsupported BlockLocker connection, expiry or extra protectables");
            Class<?> signType = Class.forName(prefix + "SignType", false, loader);
            var headers = settingsType.getMethod("getSimpleLocalizedHeaders", signType);
            Set<String> privateNames = aliases(headers.invoke(settings, signType.getField("PRIVATE").get(null)));
            Set<String> moreNames = aliases(headers.invoke(settings, signType.getField("MORE_USERS").get(null)));
            for (String a : privateNames)
                for (String b : moreNames)
                    if (a.trim().equalsIgnoreCase(b.trim()))
                        throw new IOException("Ambiguous BlockLocker sign headers");
            Class<?> translatorType = Class.forName(prefix + "Translator", false, loader);
            Class<?> translationType = Class.forName(prefix + "Translator$Translation", false, loader);
            Object translator = provider.getMethod("getTranslator").invoke(instance);
            Set<String> everyone = aliases(translatorType.getMethod("getAllWithoutColor", translationType)
                    .invoke(translator, translationType.getField("TAG_EVERYONE").get(null)));
            return new PaperBlockLockerSettings(materials(plugin.getConfig().get("protectableContainers")),
                    privateNames, moreNames, everyone);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            throw new IOException("Could not capture installed BlockLocker settings", e);
        }
    }

    private static Set<String> aliases(Object raw) throws IOException {
        if (!(raw instanceof List<?> list) || list.isEmpty())
            throw new IOException("Missing BlockLocker sign aliases");
        Set<String> result = new HashSet<>();
        for (Object value : list) {
            if (!(value instanceof String alias) || alias.isBlank())
                throw new IOException("Invalid BlockLocker sign alias");
            result.add(alias);
        }
        return result;
    }

    private static Set<String> materials(Object raw) throws IOException {
        if (!(raw instanceof List<?> list))
            throw new IOException("Missing BlockLocker protectableContainers");
        Set<String> result = new HashSet<>();
        for (Object entry : list) {
            if (!(entry instanceof String name) || name.isBlank())
                throw new IOException("Invalid BlockLocker container configuration");
            if (name.startsWith("#")) {
                var key = NamespacedKey.fromString(name.substring(1));
                var tag = key == null ? null : Bukkit.getTag("blocks", key, Material.class);
                if (tag == null) throw new IOException("Unknown BlockLocker container tag: " + name);
                for (Material material : tag.getValues()) result.add(material.getKey().toString());
            } else {
                Material material = Material.matchMaterial(name);
                if (material == null) material = Material.matchMaterial(name, true);
                if (material == null || !material.isBlock())
                    throw new IOException("Unknown BlockLocker container: " + name);
                result.add(material.getKey().toString());
            }
        }
        return result;
    }

    /** No Bukkit reads here: use the captured settings in the SAVED worker traversal. */
    public SavedBlockLockerTerrainObservations.Snapshot captureSaved(
            WorldStorageDiscovery.Manifest manifest, WorldScope scope,
            int maximumCompressedBytes, int maximumInflatedBytes,
            int maximumSavedTerrainChunks) throws IOException {
        return SavedBlockLockerTerrainObservations.captureConfigured(manifest, scope,
                maximumCompressedBytes, maximumInflatedBytes, maximumSavedTerrainChunks,
                privateHeaders, moreHeaders, everyoneAliases,
                PaperSavedBlockLockerSignText.nativeText(), containers);
    }
}
