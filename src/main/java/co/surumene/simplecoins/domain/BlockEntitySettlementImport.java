package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Stages currency from a terrain chunk into a pending settlement.
 * This class does not infer BlockLocker ownership or complete a region source.
 * The caller must resolve ownership from an authoritative protection snapshot,
 * then complete the source only after every chunk has been examined.
 */
public final class BlockEntitySettlementImport {
    private static final Set<String> INVENTORY_BLOCK_IDS = Set.of(
            "minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel",
            "minecraft:shulker_box", "minecraft:hopper", "minecraft:dispenser",
            "minecraft:dropper", "minecraft:furnace", "minecraft:blast_furnace",
            "minecraft:smoker", "minecraft:brewing_stand", "minecraft:crafter",
            "minecraft:chiseled_bookshelf");

    private BlockEntitySettlementImport() {}

    public record Ownership(UUID owner, boolean everyoneProtected,
                            boolean currentlyBanned, boolean hasPlayerdata) {}

    @FunctionalInterface
    public interface OwnershipResolver {
        Ownership resolve(String blockPosition) throws IOException;
    }

    public static void stageChunk(SettlementAccumulator settlement, String source,
                                  String world, Map<String,Object> chunk,
                                  OwnershipResolver resolver) throws IOException {
        Objects.requireNonNull(settlement);
        Objects.requireNonNull(resolver);
        try {
            if (world == null || world.isBlank())
                throw new IOException("A physical world identity is required");
            Map<String, BigInteger> assets = StoredBlockEntityAssets.extract(chunk);
            // One physical container has one owner in a settlement snapshot,
            // even when it contains currency in multiple inventory slots.
            Map<String, Ownership> ownershipByBlock = new java.util.HashMap<>();
            // Empty protected inventories still establish a zero-balance registered
            // owner. Stage a separate physical container identity even when its
            // slots contain no currency; this also detects duplicate containers
            // across independently staged sources.
            for (Object rawBlock : (List<?>) chunk.get("block_entities")) {
                Map<?, ?> entity = (Map<?, ?>) rawBlock; // validated by extract()
                // An empty vanilla container may omit the Items tag entirely.
                if (!entity.containsKey("Items") &&
                        !INVENTORY_BLOCK_IDS.contains(entity.get("id"))) continue;
                String block = entity.get("x") + "," + entity.get("y") + "," + entity.get("z");
                Ownership ownership = resolver.resolve(block);
                if (ownership == null)
                    throw new IOException("Ownership unresolved for block " + block);
                ownershipByBlock.put(block, ownership);
                String marker = "world/" + world + "/" + block + ":container";
                settlement.accept(new SettlementAccumulator.Asset(
                        source, marker, world, ownership.owner(),
                        ownership.everyoneProtected(), ownership.currentlyBanned(),
                        ownership.hasPlayerdata(), BigInteger.ZERO, marker));
            }
            for (var entry : assets.entrySet()) {
                String slot = entry.getKey();
                int separator = slot.lastIndexOf(':');
                if (separator <= 0)
                    throw new IOException("Invalid block inventory slot: " + slot);
                String block = slot.substring(0, separator);
                Ownership ownership = ownershipByBlock.get(block);
                if (ownership == null) {
                    ownership = resolver.resolve(block);
                    if (ownership == null)
                        throw new IOException("Ownership unresolved for block " + block);
                    ownershipByBlock.put(block, ownership);
                }
                String location = "world/" + world + "/" + slot;
                settlement.accept(new SettlementAccumulator.Asset(
                        source, location, world, ownership.owner(),
                        ownership.everyoneProtected(), ownership.currentlyBanned(),
                        ownership.hasPlayerdata(), entry.getValue(), location));
            }
            // The source may contain other chunks. It must not be completed here.
        } catch (IOException failure) {
            settlement.failSource(source, "BlockEntity chunk could not be accounted: " + failure.getMessage());
            throw failure;
        } catch (RuntimeException failure) {
            settlement.failSource(source, "BlockEntity chunk was rejected: " + failure.getMessage());
            throw failure;
        }
    }
}
