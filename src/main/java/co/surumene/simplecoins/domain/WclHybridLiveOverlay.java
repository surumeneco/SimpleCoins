package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * An UNPUBLISHED supplement for already captured LIVE Chunk monetary sources.
 * Canonical WCL physical keys and the same required Chunk source ID are used
 * throughout; no additional money source is invented.
 *
 * This does not discover all WWC/WWW Entities, validate an external source
 * epoch, prove that an item mirrored in vanilla equipment equals WCL, or
 * authorize a settlement commit / admission.
 */
public final class WclHybridLiveOverlay {
    private WclHybridLiveOverlay() {}

    /** Exact loaded Chunk selection independently validated by the Paper caller. */
    public record Selected(SavedLiveSourcePartition.Chunk chunk,
                           UUID entityId, String providerId,
                           WclOwnerEvidence.Decision ownerEvidence,
                           Map<String, BigInteger> slotValues) {
        public Selected {
            Objects.requireNonNull(chunk, "verified LIVE chunk");
            Objects.requireNonNull(entityId, "WCL Entity UUID");
            if (providerId == null || providerId.isBlank())
                throw new IllegalArgumentException("Missing WCL provider");
            Objects.requireNonNull(ownerEvidence, "WCL owner evidence");
            slotValues = Map.copyOf(Objects.requireNonNull(slotValues, "WCL slots"));
            for (var value : slotValues.values()) {
                if (value.signum() < 0)
                    throw new IllegalArgumentException("Negative WCL monetary amount");
            }
        }
    }

    /** The original LIVE sources with verified extra WCL values, not a new source. */
    public record Overlay(List<HybridLiveSourceStage.Source> sources,
                          Map<String, WclOwnerEvidence.Decision> wclOwners) {
        public Overlay {
            sources = List.copyOf(Objects.requireNonNull(sources));
            wclOwners = Map.copyOf(Objects.requireNonNull(wclOwners));
        }

        /** A local overlay never proves whole-world source closure. */
        public boolean isCompleteSettlementSource() { return false; }
    }

    /**
     * Derive candidate WCL values only for exact selected loaded Chunk sources.
     * Every conflicting physical view is an error, including equal-value
     * mirrors. In particular, without a separate equivalence witness, neither
     * WWW weapon nor WWC head may be added independently of vanilla equipment.
     * Discard the entire caller generation on IOException.
     */
    public static Overlay attach(Collection<? extends HybridLiveSourceStage.Source> observed,
                                 Collection<Selected> selected) throws IOException {
        Objects.requireNonNull(observed, "captured LIVE monetary sources");
        Objects.requireNonNull(selected, "explicit selected WCL candidates");
        List<? extends HybridLiveSourceStage.Source> sources = List.copyOf(observed);
        List<Selected> candidates = List.copyOf(selected);

        Map<SavedLiveSourcePartition.Chunk, HybridLiveSourceStage.ChunkSource> chunks =
                new LinkedHashMap<>();
        Set<String> physicalKeys = new HashSet<>();
        Set<String> sourceIds = new HashSet<>();
        for (var source : sources) {
            if (source == null || !sourceIds.add(source.id()))
                throw new IOException("Duplicate or null captured LIVE source");
            if (source instanceof HybridLiveSourceStage.ChunkSource chunk
                    && chunks.putIfAbsent(chunk.chunk(), chunk) != null)
                throw new IOException("Duplicate selected LIVE Chunk: " + chunk.chunk());
            for (var value : source.values()) {
                if (value == null || !physicalKeys.add(value.physicalKey()))
                    throw new IOException("Duplicate or null captured LIVE physical slot");
            }
        }

        Set<UUID> seenEntities = new HashSet<>();
        Map<SavedLiveSourcePartition.Chunk, List<HybridLiveSourceStage.Value>> added = new HashMap<>();
        Map<String, WclOwnerEvidence.Decision> ownership = new LinkedHashMap<>();
        for (Selected candidate : candidates) {
            if (candidate == null)
                throw new IOException("Null selected WCL holder");
            if (!chunks.containsKey(candidate.chunk()))
                throw new IOException("WCL holder is not in a captured LIVE Chunk: "
                        + candidate.entityId());
            if (!seenEntities.add(candidate.entityId()))
                throw new IOException("Duplicate WCL Entity in LIVE generation: "
                        + candidate.entityId());
            if (candidate.ownerEvidence().kind() == WclOwnerEvidence.Kind.HOLD)
                throw new IOException("Unresolved WCL Entity owner: " + candidate.entityId());

            Map<String, String> aliases = WclPhysicalSlotIdentity.keys(
                    candidate.entityId(), candidate.providerId(), candidate.slotValues().keySet());
            for (var entry : candidate.slotValues().entrySet()) {
                String alias = aliases.get(entry.getKey());
                String physical = "world/" + candidate.chunk().world() + "/" + alias;
                if (!physicalKeys.add(physical))
                    throw new IOException("WCL/vanilla alias or duplicate physical slot: " + physical);
                // The following two slots can mirror independent vanilla
                // equipment even if the ordinary scan found zero currency.
                // Without item-identity evidence a positive amount is HOLD.
                if (entry.getValue().signum() > 0 &&
                        (alias.endsWith("/equipment/mainhand")
                                || alias.endsWith("/equipment/head")))
                    throw new IOException("Unproven WCL equipment mirror: " + physical);
                if (entry.getValue().signum() == 0)
                    continue;
                var money = new HybridLiveSourceStage.Value(
                        physical, candidate.chunk().world(), entry.getValue());
                added.computeIfAbsent(candidate.chunk(), ignored -> new ArrayList<>()).add(money);
                if (ownership.putIfAbsent(physical, candidate.ownerEvidence()) != null)
                    throw new IOException("Duplicate WCL monetary identity: " + physical);
            }
        }

        List<HybridLiveSourceStage.Source> staged = new ArrayList<>();
        for (var source : sources) {
            if (source instanceof HybridLiveSourceStage.ChunkSource chunk) {
                List<HybridLiveSourceStage.Value> extras = added.get(chunk.chunk());
                if (extras == null || extras.isEmpty()) {
                    staged.add(source);
                } else {
                    List<HybridLiveSourceStage.Value> values = new ArrayList<>(chunk.values());
                    values.addAll(extras);
                    staged.add(new HybridLiveSourceStage.ChunkSource(chunk.chunk(), values));
                }
            } else staged.add(source);
        }
        return new Overlay(staged, ownership);
    }
}
