package co.surumene.simplecoins.paper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

/**
 * Read-only BlockLocker 1.15 evidence for a LOADED Paper Block.
 *
 * This is deliberately not the mutable BlockLockerAPIv2.getOwner(Block)
 * helper: that helper may update a sign and guess a UUID from a display
 * name. No loading of unloaded Blocks, saved sign ownership or final BAN
 * classification is performed here.
 */
public final class BlockLockerLiveEvidence {
    private static final String PLUGIN_NAME = "BlockLocker";
    private static final String VERIFIED_VERSION = "1.15";

    public record Ownership(UUID owner, boolean everyoneProtected) {
        public Ownership {
            if (everyoneProtected && owner != null)
                throw new IllegalArgumentException("Everyone protection is Free, not owned");
        }
    }

    private BlockLockerLiveEvidence() {}

    /**
     * A profile is an owner only when the provider supplies an actual UUID.
     * The Everyone profile has precedence even over an otherwise unknown
     * player profile, as required by the confirmed statistics rules.
     */
    static Ownership classify(Optional<?> owner, Collection<?> allowed,
                              Object everyone, Function<Object, Optional<UUID>> toUuid)
            throws IOException {
        if (owner == null || allowed == null || everyone == null || toUuid == null)
            throw new IOException("Incomplete BlockLocker protection evidence");
        if (allowed.stream().anyMatch(profile -> profile != null && profile.equals(everyone)))
            return new Ownership(null, true);
        Object primary = owner.orElseThrow(
                () -> new IOException("BlockLocker protection has no primary owner"));
        if (!allowed.contains(primary))
            throw new IOException("BlockLocker primary owner is missing from allowed profiles");
        Optional<UUID> uuid;
        try {
            uuid = toUuid.apply(primary);
        } catch (RuntimeException failure) {
            throw new IOException("BlockLocker owner UUID could not be read", failure);
        }
        if (uuid == null || uuid.isEmpty())
            throw new IOException("Protected BlockLocker owner has no verified UUID");
        return new Ownership(uuid.orElseThrow(), false);
    }

    /**
     * Reads currently loaded BlockLocker protection evidence. A provider
     * absence/version mismatch is NOT an unprotected container and cannot
     * be converted to Free. Only genuinely missing protection in the live
     * provider's lookup is Free. Caller must independently verify chunk
     * coverage and saved sources before an authoritative settlement.
     */
    public static Ownership observe(Block block) throws IOException {
        Objects.requireNonNull(block, "block");
        if (!Bukkit.isPrimaryThread())
            throw new IOException("BlockLocker owner evidence requires Paper main thread");
        if (!block.getWorld().isChunkLoaded(Math.floorDiv(block.getX(), 16),
                Math.floorDiv(block.getZ(), 16)))
            throw new IOException("Cannot classify an unloaded BlockLocker container");
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled())
            throw new IOException("BlockLocker provider is absent or disabled");
        String version = plugin.getDescription().getVersion();
        if (!VERIFIED_VERSION.equals(version))
            throw new IOException("Unverified BlockLocker API version: " + version);

        try {
            ClassLoader loader = plugin.getClass().getClassLoader();
            Class<?> api = Class.forName("nl.rutgerkok.blocklocker.BlockLockerAPIv2", false, loader);
            Class<?> finderType = Class.forName("nl.rutgerkok.blocklocker.ProtectionFinder", false, loader);
            Class<?> protectionType = Class.forName(
                    "nl.rutgerkok.blocklocker.protection.Protection", false, loader);
            Class<?> profileFactoryType = Class.forName(
                    "nl.rutgerkok.blocklocker.ProfileFactory", false, loader);
            Class<?> playerProfileType = Class.forName(
                    "nl.rutgerkok.blocklocker.profile.PlayerProfile", false, loader);

            Object provider = api.getMethod("getPlugin").invoke(null);
            if (provider != plugin)
                throw new IOException("BlockLocker classloader points at another plugin instance");
            Object finder = plugin.getClass().getMethod("getProtectionFinder").invoke(provider);
            Object raw = finderType.getMethod("findProtection", Block.class).invoke(finder, block);
            if (!(raw instanceof Optional<?> found))
                throw new IOException("Unexpected BlockLocker protection lookup result");
            if (found.isEmpty()) return new Ownership(null, false);

            Object protection = found.orElseThrow();
            Object rawOwner = protectionType.getMethod("getOwner").invoke(protection);
            Object rawAllowed = protectionType.getMethod("getAllowed").invoke(protection);
            if (!(rawOwner instanceof Optional<?> owner) || !(rawAllowed instanceof Collection<?> allowed))
                throw new IOException("Incomplete BlockLocker protection data");
            Object factory = plugin.getClass().getMethod("getProfileFactory").invoke(provider);
            Object everyone = profileFactoryType.getMethod("fromEveryone").invoke(factory);
            Method uuidMethod = playerProfileType.getMethod("getUniqueId");
            return classify(owner, allowed, everyone, candidate -> {
                if (!playerProfileType.isInstance(candidate)) return Optional.empty();
                try {
                    Object id = uuidMethod.invoke(candidate);
                    if (!(id instanceof Optional<?> optional) || optional.isEmpty())
                        return Optional.empty();
                    Object value = optional.orElseThrow();
                    if (!(value instanceof UUID uuid)) return Optional.empty();
                    return Optional.of(uuid);
                } catch (IllegalAccessException | InvocationTargetException reflection) {
                    throw new IllegalStateException("BlockLocker player UUID lookup failed", reflection);
                }
            });
        } catch (IOException error) {
            throw error;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            throw new IOException("BlockLocker 1.15 owner evidence is unavailable", error);
        }
    }
}
