package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Stages currency from a terrain chunk into a pending settlement.
 * This class does not infer BlockLocker ownership or complete a region source.
 * The caller must resolve ownership from an authoritative protection snapshot,
 * then complete the source only after every chunk has been examined.
 */
public final class BlockEntitySettlementImport {
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
            for (var entry : assets.entrySet()) {
                String slot = entry.getKey();
                int separator = slot.lastIndexOf(':');
                if (separator <= 0)
                    throw new IOException("Invalid block inventory slot: " + slot);
                String block = slot.substring(0, separator);
                Ownership ownership = resolver.resolve(block);
                if (ownership == null)
                    throw new IOException("Ownership unresolved for block " + block);
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
