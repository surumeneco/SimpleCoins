package co.surumene.simplecoins.paper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Main-thread, read-only WCL/WWC/WWW service-registration observation.
 *
 * No optional WCL class is statically linked into SimpleCoins: the service
 * interface is resolved from the running WonderfulCoreLibrary plugin and every
 * Bukkit registration is enumerated (not ServicesManager.load's single winner).
 * This is registration evidence ONLY. It cannot establish any entity inventory,
 * saved PDC, deployment JAR hashes, ownership, source-generation closure,
 * settlement publication or admission permit.
 */
public final class PaperWonderfulReaderRegistry {
    private PaperWonderfulReaderRegistry() {}

    static final String SERVICE =
            "co.surumene.wcl.plugin.inventory.WonderfulStorageReader";
    static final Map<String, String> EXPECTED = Map.of(
            "wwc", "WhatAWonderfulChicken",
            "www", "WhatAWonderfulWolf");

    public enum Status { PRESENT, INCOMPLETE, CONFLICT }

    /** A registration's origin is verified by Bukkit plugin-object identity. */
    public record Entry(String providerId, String ownerName, boolean ownerEnabled,
                        boolean ownerIdentityMatches, boolean implementsService) {
        public Entry {
            Objects.requireNonNull(providerId);
            Objects.requireNonNull(ownerName);
        }
    }

    public record Assessment(Status status, String reason, Map<String, String> owners) {
        public Assessment {
            Objects.requireNonNull(status);
            Objects.requireNonNull(reason);
            owners = Map.copyOf(Objects.requireNonNull(owners));
        }

        public boolean registrationsPresent() {
            return status == Status.PRESENT;
        }
    }

    /**
     * A pure, fail-closed registration-set classifier. This does not authenticate
     * caller-supplied facts; only inspect() obtains them from running Bukkit.
     */
    static Assessment classify(boolean wclEnabled,
                               Map<String, Boolean> expectedPluginEnabled,
                               List<Entry> registrations) {
        Objects.requireNonNull(expectedPluginEnabled);
        Objects.requireNonNull(registrations);
        if (!wclEnabled)
            return new Assessment(Status.INCOMPLETE, "wcl-missing-or-disabled", Map.of());
        Map<String, String> accepted = new HashMap<>();
        for (Entry entry : registrations) {
            if (entry == null || !EXPECTED.containsKey(entry.providerId()))
                return new Assessment(Status.CONFLICT, "unexpected-provider-id", Map.of());
            String expectedOwner = EXPECTED.get(entry.providerId());
            if (!expectedOwner.equals(entry.ownerName())
                    || !entry.ownerEnabled() || !entry.ownerIdentityMatches()
                    || !entry.implementsService())
                return new Assessment(Status.CONFLICT, "untrusted-provider-owner", Map.of());
            if (accepted.putIfAbsent(entry.providerId(), entry.ownerName()) != null)
                return new Assessment(Status.CONFLICT, "duplicate-provider-id", Map.of());
        }
        for (var expected : EXPECTED.entrySet()) {
            if (!Boolean.TRUE.equals(expectedPluginEnabled.get(expected.getValue())))
                return new Assessment(Status.INCOMPLETE, "required-plugin-disabled", Map.of());
            if (!expected.getValue().equals(accepted.get(expected.getKey())))
                return new Assessment(Status.INCOMPLETE, "missing-provider", Map.of());
        }
        return new Assessment(Status.PRESENT, "two-registration-sources-present", accepted);
    }

    /**
     * Called strictly on Paper's main thread. Any failed class resolution or
     * provider invocation is incomplete, never evidence of an empty inventory.
     */
    public static Assessment inspect(Plugin consumer) {
        Objects.requireNonNull(consumer);
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("WCL services must be read on the Paper main thread");
        var server = consumer.getServer();
        var plugins = server.getPluginManager();
        Plugin wcl = plugins.getPlugin("WonderfulCoreLibrary");
        if (wcl == null || !wcl.isEnabled())
            return new Assessment(Status.INCOMPLETE, "wcl-missing-or-disabled", Map.of());
        Map<String, Boolean> expectedEnabled = new HashMap<>();
        for (String ownerName : EXPECTED.values()) {
            Plugin plugin = plugins.getPlugin(ownerName);
            expectedEnabled.put(ownerName, plugin != null && plugin.isEnabled());
        }

        try {
            Class<?> contract = wcl.getClass().getClassLoader().loadClass(SERVICE);
            Method providerIdMethod = contract.getMethod("providerId");
            // Generic type is supplied by a runtime WCL class, not a compile-time dependency.
            @SuppressWarnings({"rawtypes", "unchecked"})
            Collection<RegisteredServiceProvider<?>> registrations = (Collection)
                    server.getServicesManager().getRegistrations((Class) contract);
            List<Entry> found = new ArrayList<>();
            for (RegisteredServiceProvider<?> registration : registrations) {
                Plugin owner = registration.getPlugin();
                Object provider = registration.getProvider();
                if (owner == null || provider == null)
                    return new Assessment(Status.CONFLICT, "null-registration-owner", Map.of());
                Object id = providerIdMethod.invoke(provider);
                if (!(id instanceof String providerName))
                    return new Assessment(Status.CONFLICT, "non-string-provider-id", Map.of());
                found.add(new Entry(providerName, owner.getName(), owner.isEnabled(),
                        plugins.getPlugin(owner.getName()) == owner,
                        registration.getService() == contract && contract.isInstance(provider)));
            }
            return classify(true, expectedEnabled, found);
        } catch (ReflectiveOperationException | LinkageError | ClassCastException failure) {
            return new Assessment(Status.INCOMPLETE,
                    "wcl-service-incompatible-or-unreadable", Map.of());
        } catch (RuntimeException failure) {
            return new Assessment(Status.INCOMPLETE,
                    "wcl-service-registration-failed", Map.of());
        }
    }
}
