package co.surumene.simplecoins.domain;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Derived, process-local accounting. The event owner supplies each confirmed
 * physical operation once, with one sequence for all of its classified movements.
 * Rebuild from the physical settlement at restart, never replay these deltas. */
public final class MemoryAccounting {
    public enum Outcome { APPLIED, DUPLICATE, REJECTED }

    /** Only dead assets require a retained physical location. Null owner is external supply. */
    public record Endpoint(AssetOwner.Result owner, String location) {
        public Endpoint {
            boolean dead = owner != null && owner.kind() == AssetOwner.Kind.DEAD;
            if (dead ? location == null || location.isBlank() : location != null)
                throw new IllegalArgumentException("Location must identify a dead asset");
        }
        public static Endpoint external() { return new Endpoint(null, null); }
        public static Endpoint free() { return new Endpoint(AssetOwner.resolve(null, false, false, false), null); }
        public static Endpoint personal(UUID id) {
            return new Endpoint(new AssetOwner.Result(AssetOwner.Kind.PERSONAL, id, null), null);
        }
        public static Endpoint dead(UUID id, AssetOwner.DeadReason reason, String location) {
            return new Endpoint(new AssetOwner.Result(AssetOwner.Kind.DEAD, id, reason), location);
        }
    }
    public record Movement(Endpoint from, Endpoint to, BigInteger amount) {
        public Movement {
            Objects.requireNonNull(from); Objects.requireNonNull(to); Objects.requireNonNull(amount);
            if (amount.signum() <= 0) throw new IllegalArgumentException("Positive movement required");
        }
    }
    public record Snapshot(Map<UUID, BigInteger> personal, BigInteger free,
                           Map<Endpoint, BigInteger> dead, long sequence) {}

    private final Map<UUID, BigInteger> personal;
    private final Map<Endpoint, BigInteger> dead;
    private BigInteger free;
    private long sequence;
    private final Map<UUID,Long> ownerSequences=new HashMap<>();

    public MemoryAccounting(Map<UUID, BigInteger> personal, BigInteger free,
                            Map<Endpoint, BigInteger> dead) {
        this.personal = new HashMap<>(Map.copyOf(personal));
        this.dead = new HashMap<>(Map.copyOf(dead));
        this.free = nonnegative(free);
        this.personal.values().forEach(MemoryAccounting::nonnegative);
        this.dead.forEach((key, value) -> {
            if (key.owner() == null || key.owner().kind() != AssetOwner.Kind.DEAD || value.signum() <= 0)
                throw new IllegalArgumentException("Invalid dead-location baseline");
        });
    }

    public synchronized Outcome record(long order, List<Movement> movements) {
        return record(order, movements, Set.of());
    }

    /** Allocate one process-local order for a newly confirmed physical operation.
     * Explicit registration is used only for proven eligible zero-value assets. */
    public synchronized Outcome recordNext(List<Movement> movements, Set<UUID> registered) {
        var additions = Set.copyOf(registered);
        var outcome = record(Math.addExact(sequence, 1), movements, Set.of());
        if (outcome == Outcome.APPLIED)
            additions.forEach(id -> {if(personal.putIfAbsent(id, BigInteger.ZERO)==null)ownerSequences.put(id,sequence);});
        return outcome;
    }

    /** Removing registration is reserved for a complete authoritative BAN/playerdata
     * reclassification; every remaining personal balance must move in this same operation. */
    public synchronized Outcome record(long order, List<Movement> movements, Set<UUID> unregister) {
        if (order <= 0) return Outcome.REJECTED;
        if (order <= sequence) return Outcome.DUPLICATE;
        if (order - sequence != 1) return Outcome.REJECTED;
        var delta = new HashMap<Endpoint, BigInteger>();
        for (Movement move : List.copyOf(movements)) {
            if (move.from().equals(move.to())) continue;
            if (move.from().owner() != null) delta.merge(move.from(), move.amount().negate(), BigInteger::add);
            if (move.to().owner() != null) delta.merge(move.to(), move.amount(), BigInteger::add);
        }
        var next = new HashMap<Endpoint, BigInteger>();
        for (var change : delta.entrySet()) {
            BigInteger value = balance(change.getKey()).add(change.getValue());
            if (value.signum() < 0) return Outcome.REJECTED;
            next.put(change.getKey(), value);
        }
        var removed = Set.copyOf(unregister);
        for (UUID id : removed) {
            Endpoint endpoint = Endpoint.personal(id);
            if (next.getOrDefault(endpoint, balance(endpoint)).signum() != 0) return Outcome.REJECTED;
        }
        // All validation is complete. No full player-map copy, filesystem IO or
        // statistics sort on the movement path; only changed accounts are updated.
        next.forEach((endpoint, value) -> {
            switch (endpoint.owner().kind()) {
                case FREE -> free = value;
                case PERSONAL -> personal.put(endpoint.owner().player(), value);
                case DEAD -> { if (value.signum() == 0) dead.remove(endpoint); else dead.put(endpoint, value); }
            }
        });
        removed.forEach(personal::remove);
        for(var endpoint:next.keySet())if(endpoint.owner().player()!=null)ownerSequences.put(endpoint.owner().player(),order);
        removed.forEach(id->ownerSequences.put(id,order));
        sequence = order;
        return Outcome.APPLIED;
    }

    /** Replace only one owner's derived balances from a complete, fresh physical observation.
     * The caller must establish completeness and exclude assets no longer owned or present.
     * This operation does not acquire physical observations or decide when they are fresh. */
    public synchronized Outcome reconcileOwner(UUID owner, boolean complete, boolean banned,
            boolean hasPlayerdata, boolean registrationBasis, Map<String, BigInteger> observed) {
        if (!complete || owner == null || observed == null) return Outcome.REJECTED;
        Map<Endpoint, BigInteger> replacement = new HashMap<>();
        BigInteger total = BigInteger.ZERO;
        var classification = AssetOwner.resolve(owner, false, banned, hasPlayerdata);
        try {
            for (var entry : Map.copyOf(observed).entrySet()) {
                if (entry.getKey().isBlank() || entry.getValue().signum() < 0) return Outcome.REJECTED;
                if (entry.getValue().signum() == 0) continue;
                total = total.add(entry.getValue());
                if (classification.kind() == AssetOwner.Kind.DEAD)
                    replacement.put(Endpoint.dead(owner, classification.reason(), entry.getKey()), entry.getValue());
            }
        } catch (NullPointerException invalidObservation) { return Outcome.REJECTED; }
        long next = Math.addExact(sequence, 1);
        boolean register = classification.kind() == AssetOwner.Kind.PERSONAL
                && (registrationBasis || total.signum() > 0);
        dead.keySet().removeIf(endpoint -> owner.equals(endpoint.owner().player()));
        dead.putAll(replacement);
        if (register) personal.put(owner, total); else personal.remove(owner);
        sequence = next;
        ownerSequences.put(owner,next);
        return Outcome.APPLIED;
    }

    public synchronized long sequence() { return sequence; }
    public synchronized long sequence(UUID owner){return ownerSequences.getOrDefault(owner,0L);}

    public synchronized boolean isRegistered(UUID player) {
        return personal.containsKey(player);
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(Map.copyOf(personal), free, Map.copyOf(dead), sequence);
    }
    private BigInteger balance(Endpoint endpoint) {
        return switch (endpoint.owner().kind()) {
            case FREE -> free;
            case PERSONAL -> personal.getOrDefault(endpoint.owner().player(), BigInteger.ZERO);
            case DEAD -> dead.getOrDefault(endpoint, BigInteger.ZERO);
        };
    }
    private static BigInteger nonnegative(BigInteger value) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException("Invalid baseline balance");
        return value;
    }
}
