package co.surumene.simplecoins.paper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Read-only collector for an independently identified, already loaded
 * Paper entity. A successful WCL snapshot is an UNPUBLISHED candidate:
 * no money is decoded, classified or adopted here, and neither G0
 * generation closure nor full world/source discovery is implied.
 */
public final class PaperWonderfulLoadedEvidence {
    private PaperWonderfulLoadedEvidence() {}

    public enum Status { SUCCESS, NOT_FOUND, FAILURE, INCOMPLETE, MISMATCH }

    /** Serialized WCL item with its physical provider slot and defensive bytes. */
    public record StoredItem(String slot, byte[] bytes) {
        public StoredItem {
            if (slot == null || slot.isBlank() || bytes == null || bytes.length == 0)
                throw new IllegalArgumentException("Incomplete WCL stored item");
            bytes = bytes.clone();
        }

        @Override public byte[] bytes() { return bytes.clone(); }
    }

    /** Exact source identities are part of evidence, not inferred ownership. */
    public record Snapshot(UUID entityId, UUID worldId, String providerId,
                           Map<String, UUID> relationships, List<StoredItem> items) {
        public Snapshot {
            Objects.requireNonNull(entityId);
            Objects.requireNonNull(worldId);
            if (providerId == null || providerId.isBlank())
                throw new IllegalArgumentException("Missing WCL provider identity");
            relationships = Map.copyOf(Objects.requireNonNull(relationships));
            items = List.copyOf(Objects.requireNonNull(items));
        }
    }

    public record Result(Status status, Optional<Snapshot> snapshot, String reason) {
        public Result {
            Objects.requireNonNull(status);
            snapshot = Objects.requireNonNull(snapshot);
            Objects.requireNonNull(reason);
            if ((status == Status.SUCCESS) != snapshot.isPresent())
                throw new IllegalArgumentException("Only successful WCL results carry evidence");
        }
    }

    private static Result no(Status status, String reason) {
        return new Result(status, Optional.empty(), reason);
    }

    /**
     * Invoke a proven registered provider via WCL's actual runtime classloader.
     * The argument must be an existing valid Paper entity. Neither this method
     * nor WCL readLoaded may discover/load other chunks or publish statistics.
     */
    public static Result readLoaded(Plugin consumer, Entity entity, String providerId) {
        Objects.requireNonNull(consumer);
        Objects.requireNonNull(entity);
        Objects.requireNonNull(providerId);
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("WCL loaded reads require Paper primary thread");
        if (!PaperWonderfulReaderRegistry.EXPECTED.containsKey(providerId))
            return no(Status.MISMATCH, "unknown-expected-provider");
        UUID entityId = entity.getUniqueId();
        UUID worldId = entity.getWorld().getUID();
        if (!entity.isValid() || Bukkit.getEntity(entityId) != entity)
            return no(Status.INCOMPLETE, "entity-not-authoritatively-loaded");
        return readVerified(consumer, entityId, worldId, providerId, null);
    }

    /**
     * Unpublished candidate from an independently observed saved Entity PDC.
     * This method does NOT read disk, prove that the supplied PDC is authentic,
     * establish same-generation saved authority, load a chunk or publish money.
     * The caller must own the saved source manifest, exact Entity identity,
     * bounded NBT reconstruction, pre/post content witness and deduplication.
     */
    public static Result readStored(Plugin consumer, UUID entityId, UUID worldId,
                                    String providerId, PersistentDataContainer savedPdc) {
        Objects.requireNonNull(consumer);
        Objects.requireNonNull(entityId);
        Objects.requireNonNull(worldId);
        Objects.requireNonNull(providerId);
        Objects.requireNonNull(savedPdc);
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("WCL stored reads require Paper primary thread");
        return readVerified(consumer, entityId, worldId, providerId, savedPdc);
    }

    /** Shared, source-bound WCL contract and strict identity/result normalization. */
    private static Result readVerified(Plugin consumer, UUID entityId, UUID worldId,
                                       String providerId, PersistentDataContainer savedPdc) {
        if (!PaperWonderfulReaderRegistry.EXPECTED.containsKey(providerId))
            return no(Status.MISMATCH, "unknown-expected-provider");
        PaperWonderfulReaderRegistry.Assessment registration =
                PaperWonderfulReaderRegistry.inspect(consumer);
        if (!registration.registrationsPresent()) {
            return no(registration.status() == PaperWonderfulReaderRegistry.Status.CONFLICT
                    ? Status.MISMATCH : Status.INCOMPLETE,
                    "provider-registry-" + registration.reason());
        }

        var server = consumer.getServer();
        var manager = server.getPluginManager();
        Plugin wcl = manager.getPlugin("WonderfulCoreLibrary");
        if (wcl == null || !wcl.isEnabled())
            return no(Status.INCOMPLETE, "wcl-not-enabled-at-read");

        try {
            ClassLoader loader = wcl.getClass().getClassLoader();
            Class<?> service = loader.loadClass(PaperWonderfulReaderRegistry.SERVICE);
            Method providerName = service.getMethod("providerId");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Collection<RegisteredServiceProvider<?>> registrations = (Collection)
                    server.getServicesManager().getRegistrations((Class) service);
            Object reader = null;
            for (RegisteredServiceProvider<?> registered : registrations) {
                Plugin owner = registered.getPlugin();
                Object candidate = registered.getProvider();
                if (owner == null || candidate == null || !service.isInstance(candidate)
                        || registered.getService() != service
                        || manager.getPlugin(owner.getName()) != owner || !owner.isEnabled())
                    return no(Status.MISMATCH, "registration-changed-before-read");
                Object rawId = providerName.invoke(candidate);
                if (!(rawId instanceof String id)
                        || !PaperWonderfulReaderRegistry.EXPECTED.containsKey(id)
                        || !PaperWonderfulReaderRegistry.EXPECTED.get(id).equals(owner.getName()))
                    return no(Status.MISMATCH, "provider-identity-changed-before-read");
                if (id.equals(providerId)) {
                    if (reader != null) return no(Status.MISMATCH, "provider-duplicated-before-read");
                    reader = candidate;
                }
            }
            if (reader == null)
                return no(Status.INCOMPLETE, "selected-provider-disappeared");

            Object raw = savedPdc == null
                    ? service.getMethod("readLoaded", UUID.class).invoke(reader, entityId)
                    : service.getMethod("readStored", UUID.class, UUID.class, Optional.class,
                            PersistentDataContainer.class).invoke(
                                    reader, entityId, worldId, Optional.empty(), savedPdc);
            Class<?> success = loader.loadClass(
                    "co.surumene.wcl.api.inventory.StorageReadResult$Success");
            Class<?> notFound = loader.loadClass(
                    "co.surumene.wcl.api.inventory.StorageReadResult$NotFound");
            Class<?> failure = loader.loadClass(
                    "co.surumene.wcl.api.inventory.StorageReadResult$Failure");
            if (notFound.isInstance(raw))
                return no(Status.NOT_FOUND, "provider-reported-not-found");
            if (failure.isInstance(raw))
                return no(Status.FAILURE, "provider-reported-failure");
            if (!success.isInstance(raw))
                return no(Status.INCOMPLETE, "unsupported-provider-result");

            Object rawSnapshot = success.getMethod("snapshot").invoke(raw);
            Class<?> snapshotType = loader.loadClass(
                    "co.surumene.wcl.api.inventory.StorageSnapshot");
            if (!snapshotType.isInstance(rawSnapshot))
                return no(Status.MISMATCH, "wrong-snapshot-runtime-type");
            if (!entityId.equals(snapshotType.getMethod("entityId").invoke(rawSnapshot))
                    || !worldId.equals(snapshotType.getMethod("worldId").invoke(rawSnapshot))
                    || !providerId.equals(snapshotType.getMethod("providerId").invoke(rawSnapshot)))
                return no(Status.MISMATCH, "snapshot-identity-mismatch");

            Object relationObject = snapshotType.getMethod("relationships").invoke(rawSnapshot);
            if (!(relationObject instanceof Map<?, ?> originalRelations))
                return no(Status.MISMATCH, "malformed-provider-relationships");
            Map<String, UUID> relationships = new HashMap<>();
            for (var entry : originalRelations.entrySet()) {
                if (!(entry.getKey() instanceof String key) || key.isBlank()
                        || !(entry.getValue() instanceof UUID value))
                    return no(Status.MISMATCH, "invalid-provider-relationship");
                relationships.put(key, value);
            }

            Object itemObject = snapshotType.getMethod("items").invoke(rawSnapshot);
            if (!(itemObject instanceof List<?> rawItems))
                return no(Status.MISMATCH, "malformed-provider-items");
            Class<?> itemType = loader.loadClass("co.surumene.wcl.api.inventory.StoredItem");
            Method slotReader = itemType.getMethod("slot");
            Method byteReader = itemType.getMethod("bytes");
            Set<String> seen = new HashSet<>();
            List<StoredItem> items = new ArrayList<>();
            for (Object rawItem : rawItems) {
                if (!itemType.isInstance(rawItem))
                    return no(Status.MISMATCH, "wrong-stored-item-type");
                Object rawSlot = slotReader.invoke(rawItem);
                Object rawBytes = byteReader.invoke(rawItem);
                if (!(rawSlot instanceof String slot) || slot.isBlank()
                        || !seen.add(slot) || !(rawBytes instanceof byte[] bytes)
                        || bytes.length == 0)
                    return no(Status.MISMATCH, "invalid-or-duplicate-provider-slot");
                items.add(new StoredItem(slot, bytes));
            }
            return new Result(Status.SUCCESS, Optional.of(new Snapshot(entityId, worldId,
                    providerId, relationships, items)),
                    savedPdc == null ? "verified-loaded-provider-snapshot"
                            : "verified-stored-provider-snapshot");
        } catch (ReflectiveOperationException | LinkageError | ClassCastException failed) {
            return no(Status.INCOMPLETE, "wcl-runtime-interface-unavailable");
        } catch (RuntimeException failed) {
            return no(Status.INCOMPLETE, "wcl-read-unavailable");
        }
    }
}
