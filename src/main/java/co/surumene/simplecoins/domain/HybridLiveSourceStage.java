package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Domain-only bridge from explicitly captured LIVE holder snapshots into the
 * SAME pending accounting transaction as on-disk NBT staging.
 *
 * Neither this bridge nor any caller-created snapshot proves that Paper
 * sources were actually frozen/observed. The production coordinator must
 * supply immutable Paper-main-thread snapshots, a validated ownership/BAN
 * resolver and an unchanged generation before eventual publication.
 */
public final class HybridLiveSourceStage {
    private HybridLiveSourceStage() {}

    /** One validated positive monetary physical stack (zero holders use no entries). */
    public record Value(String physicalKey, String world, BigInteger amount) {
        public Value {
            if (physicalKey == null || physicalKey.isBlank() ||
                    amount == null || amount.signum() <= 0)
                throw new IllegalArgumentException("Invalid live monetary entry");
        }
    }

    public sealed interface Source permits ChunkSource, PlayerSource {
        String id();
        List<Value> values();
    }

    /** A chunk is a mandatory accounting source even with no monetary items. */
    public record ChunkSource(SavedLiveSourcePartition.Chunk chunk,
                              List<Value> values) implements Source {
        public ChunkSource {
            Objects.requireNonNull(chunk, "live Chunk identity");
            values = List.copyOf(values);
        }
        @Override public String id() {
            return SavedLiveSourcePartition.liveChunkSourceId(chunk);
        }
    }

    /** Online Player inventory is world-independent and must retain the UUID owner. */
    public record PlayerSource(UUID player, List<Value> values) implements Source {
        public PlayerSource {
            Objects.requireNonNull(player, "online Player UUID");
            values = List.copyOf(values);
        }
        @Override public String id() {
            return SavedLiveSourcePartition.livePlayerSourceId(player);
        }
    }

    @FunctionalInterface
    public interface Classifier {
        /**
         * Resolve actual ownership, [Everyone], current BAN and playerdata
         * evidence; return an Asset with exactly the supplied source/key/world/
         * amount. The caller must fail rather than guess missing ownership.
         */
        SettlementAccumulator.Asset classify(Source source, Value value) throws IOException;
    }

    /**
     * Check the complete source selection before touching the accumulator.
     * Then stage and close exactly one source at a time. On any failure, the
     * caller MUST discard the whole candidate generation; no partial or
     * unsuccessful source set may be made publishable.
     */
    public static void stage(SettlementScanRunner.Staged saved,
                             SavedLiveSourcePartition partition,
                             Collection<? extends Source> liveSources,
                             Classifier classifier) throws IOException {
        Objects.requireNonNull(saved, "pending saved settlement");
        Objects.requireNonNull(partition, "selected LIVE sources");
        Objects.requireNonNull(liveSources, "LIVE monetary sources");
        Objects.requireNonNull(classifier, "authoritative ownership classifier");

        Set<String> required = partition.requiredLiveSourceIds();
        if (!saved.requiredSourceIds().containsAll(required))
            throw new IOException("Hybrid settlement did not declare all LIVE money sources");
        List<? extends Source> selected = List.copyOf(liveSources);
        Set<String> observed = new HashSet<>();
        for (Source source : selected) {
            String id = source.id();
            if (!required.contains(id) || !observed.add(id))
                throw new IOException("Unexpected or repeated LIVE monetary source: " + id);
        }
        if (!observed.equals(required)) {
            Set<String> missing = new HashSet<>(required);
            missing.removeAll(observed);
            throw new IOException("Missing mandatory LIVE monetary sources: " + missing);
        }

        SettlementAccumulator accumulator = saved.pending();
        for (Source source : selected) {
            for (Value value : source.values()) {
                if (source instanceof ChunkSource chunk) {
                    if (!chunk.chunk().world().equals(value.world()))
                        throw new IOException("Loaded Chunk currency world mismatch: " + source.id());
                } else if (source instanceof PlayerSource && value.world() != null) {
                    throw new IOException("World-bound online Player inventory: " + source.id());
                }
                SettlementAccumulator.Asset asset = classifier.classify(source, value);
                if (asset == null)
                    throw new IOException("Missing authoritative LIVE asset classification: "
                            + source.id());
                if (!source.id().equals(asset.source()) ||
                        !value.physicalKey().equals(asset.physicalKey()) ||
                        !Objects.equals(value.world(), asset.world()) ||
                        !value.amount().equals(asset.value()))
                    throw new IOException("Resolved LIVE asset differs from physical source: " + source.id());
                if (source instanceof PlayerSource player &&
                        (!player.player().equals(asset.owner()) || asset.everyoneProtected()))
                    throw new IOException("Online Player currency ownership mismatch: " + player.player());
                try {
                    accumulator.accept(asset);
                } catch (RuntimeException invalid) {
                    throw new IOException("LIVE physical slot rejected by accounting: "
                            + value.physicalKey(), invalid);
                }
            }
            try {
                accumulator.completeSource(source.id());
            } catch (RuntimeException invalid) {
                throw new IOException("LIVE monetary source could not be completed: "
                        + source.id(), invalid);
            }
        }
    }
}
