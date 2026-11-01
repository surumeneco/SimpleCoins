package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.CompanionOwnerResolution;
import co.surumene.simplecoins.domain.ExternalOwnerDecision;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Optional read-only adapter to WhatAWonderfulWolf's public loadedIndividuals()
 * and find(UUID) API. This is deliberately not a mandatory binary dependency.
 * Unloaded wolves and unavailable plugin contracts remain UNKNOWN; the caller
 * must not treat them as verified absence or zero-valued assets.
 */
public final class PaperWonderfulWolfOwnerAdapter {
    private PaperWonderfulWolfOwnerAdapter() {}

    public static ExternalOwnerDecision.Evidence readPaper(UUID wolfId) {
        if (wolfId == null)
            return ExternalOwnerDecision.unknown();
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("WhatAWonderfulWolf");
            if (plugin == null || !plugin.isEnabled())
                return ExternalOwnerDecision.unknown();
            return readPlugin(plugin, wolfId);
        } catch (RuntimeException unavailable) {
            return ExternalOwnerDecision.unknown();
        }
    }

    /** Testable reflection boundary for the optional, currently loaded WWW API. */
    public static ExternalOwnerDecision.Evidence readPlugin(Object plugin, UUID wolfId) {
        if (plugin == null || wolfId == null)
            return ExternalOwnerDecision.unknown();
        try {
            Object loaded = plugin.getClass().getMethod("loadedIndividuals").invoke(plugin);
            if (loaded == null)
                return ExternalOwnerDecision.unknown();
            Object found = loaded.getClass().getMethod("find", UUID.class).invoke(loaded, wolfId);
            if (!(found instanceof Optional<?> individual) || individual.isEmpty())
                return ExternalOwnerDecision.unknown();
            Object value = individual.orElseThrow();
            if (value == null)
                return ExternalOwnerDecision.unknown();
            var commander = identity(value.getClass().getMethod("commanderId").invoke(value));
            var owner = identity(value.getClass().getMethod("ownerId").invoke(value));
            return CompanionOwnerResolution.resolve(commander, owner);
        } catch (ReflectiveOperationException | RuntimeException | java.io.IOException unavailable) {
            return ExternalOwnerDecision.unknown();
        }
    }

    private static CompanionOwnerResolution.Identity identity(Object raw) {
        if (!(raw instanceof Optional<?> value))
            return CompanionOwnerResolution.unknown();
        if (value.isEmpty())
            return CompanionOwnerResolution.absent();
        return value.orElseThrow() instanceof UUID id
                ? CompanionOwnerResolution.known(id)
                : CompanionOwnerResolution.unknown();
    }
}
