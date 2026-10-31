package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Stages saved Entity equipment and container inventories only after an
 * external authoritative adapter has resolved the owner of every holder.
 * A missing decision fails the entire source; this method never completes it.
 */
public final class ResolvedEntitySettlementImport {
    public record Ownership(UUID owner, boolean currentlyBanned, boolean hasPlayerdata) {
        public Ownership {
            if (owner == null && (currentlyBanned || hasPlayerdata))
                throw new IllegalArgumentException("Ownerless Entity cannot have player status");
        }
    }

    private ResolvedEntitySettlementImport() {}

    public static void stageChunk(SettlementAccumulator settlement, String source, String world,
                                  Map<String, Object> chunk,
                                  Map<String, Ownership> ownershipByUuid) throws IOException {
        Objects.requireNonNull(settlement);
        try {
            if (world == null || world.isBlank())
                throw new IOException("A physical world identity is required");
            Objects.requireNonNull(ownershipByUuid);
            Map<String, BigInteger> equipment = StoredEntityEquipmentAssets.extract(chunk);
            Map<String, BigInteger> inventory = StoredEntityInventoryAssets.extract(chunk);
            List<SettlementAccumulator.Asset> pending = new ArrayList<>();
            collect(pending, source, world, "equipment", equipment, ownershipByUuid);
            collect(pending, source, world, "inventory", inventory, ownershipByUuid);
            for (var asset : pending) settlement.accept(asset);
        } catch (IOException failure) {
            settlement.failSource(source, "Entity owner/inventory unreadable: " + failure.getMessage());
            throw failure;
        } catch (RuntimeException failure) {
            settlement.failSource(source, "Entity owner/inventory rejected: " + failure.getMessage());
            throw failure;
        }
    }

    private static void collect(List<SettlementAccumulator.Asset> pending, String source,
                                String world, String kind, Map<String, BigInteger> assets,
                                Map<String, Ownership> ownership) throws IOException {
        for (var entry : assets.entrySet()) {
            String key = entry.getKey();
            int separator = key.indexOf(':');
            if (separator < 1 || separator == key.length() - 1)
                throw new IOException("Invalid Entity slot key: " + key);
            String uuid = key.substring(0, separator);
            String slot = key.substring(separator + 1);
            if (kind.equals("inventory")) {
                if (!slot.startsWith("inventory:"))
                    throw new IOException("Invalid Entity inventory key: " + key);
                slot = slot.substring("inventory:".length());
            }
            Ownership decision = ownership.get(uuid);
            if (decision == null)
                throw new IOException("Missing authoritative Entity owner decision: " + uuid);
            String location = "world/" + world + "/entity/" + uuid + "/" + kind + "/" + slot;
            String physicalKey = "entity/" + uuid + "/" + kind + "/" + slot;
            pending.add(new SettlementAccumulator.Asset(source, physicalKey, world,
                    decision.owner(), false, decision.currentlyBanned(),
                    decision.hasPlayerdata(), entry.getValue(), location));
        }
    }
}
