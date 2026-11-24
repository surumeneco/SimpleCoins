package co.surumene.simplecoins.domain;

import java.util.*;

/** Process-local locator index for targeted owner-state reconciliation.
 * It retains no monetary value, and is rebuilt from authoritative settlement.
 * Playerdata is resolved directly from the owner UUID and is not indexed here. */
public final class OwnerStorageIndex {
    public enum Kind { BLOCK, ENTITY }
    public record Holder(Kind kind, String world, String identity) {
        public Holder {
            Objects.requireNonNull(kind); Objects.requireNonNull(identity);
            if (identity.isBlank() || (kind == Kind.BLOCK && (world == null || world.isBlank()))
                    || (kind == Kind.ENTITY && world != null)) throw new IllegalArgumentException("Invalid storage identity");
            if (kind == Kind.ENTITY) UUID.fromString(identity);
        }
        public static Holder entity(UUID id) { return new Holder(Kind.ENTITY,null,id.toString()); }
        public static Holder block(String world, int x, int y, int z) {
            return new Holder(Kind.BLOCK, world, x+","+y+","+z);
        }
    }
    public record Location(Holder holder, String world, String source) {
        public Location {
            Objects.requireNonNull(holder); Objects.requireNonNull(world); Objects.requireNonNull(source);
            if (world.isBlank() || source.isBlank() || (holder.kind() == Kind.BLOCK && !world.equals(holder.world())))
                throw new IllegalArgumentException("Invalid world storage locator");
        }
    }
    public record Change(Holder holder, UUID owner, Location location) {
        public Change {
            Objects.requireNonNull(holder);
            if ((owner == null) != (location == null) || location != null && !holder.equals(location.holder()))
                throw new IllegalArgumentException("Invalid owner location change");
        }
        public static Change replace(UUID owner, Location location) { return new Change(location.holder(),Objects.requireNonNull(owner),location); }
        public static Change remove(Holder holder) { return new Change(holder,null,null); }
    }
    private final Map<Holder, Change> byHolder = new HashMap<>();
    private final Map<UUID, Map<Holder,Location>> byOwner = new HashMap<>();
    private long revision;
    private final Map<UUID,Long> ownerRevisions=new HashMap<>();

    public static OwnerStorageIndex fromAssets(Collection<SettlementAccumulator.Asset> assets, WorldScope scope) {
        var index = new OwnerStorageIndex();
        for (var asset : assets) {
            if (asset.world() == null || asset.owner() == null || asset.everyoneProtected() || !scope.includes(asset.world())) continue;
            String prefix = "world/"+asset.world()+"/";
            if (!asset.location().startsWith(prefix)) throw new IllegalArgumentException("Unsupported owned world location: "+asset.location());
            String tail = asset.location().substring(prefix.length());
            Holder holder;
            if (tail.startsWith("entity/")) {
                int end = tail.indexOf('/',"entity/".length());
                if (end < 0) throw new IllegalArgumentException("Missing entity slot locator");
                holder = Holder.entity(UUID.fromString(tail.substring("entity/".length(),end)));
            } else {
                int end = tail.lastIndexOf(':');
                if (end < 0) throw new IllegalArgumentException("Missing block slot locator");
                String[] xyz = tail.substring(0,end).split(",",-1);
                if (xyz.length != 3) throw new IllegalArgumentException("Malformed block position");
                holder = Holder.block(asset.world(),Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
            }
            var change = Change.replace(asset.owner(),new Location(holder,asset.world(),asset.source()));
            var existing = index.byHolder.get(holder);
            if (existing != null && !existing.equals(change)) throw new IllegalArgumentException("Conflicting physical holder ownership/source: "+holder);
            index.apply(List.of(change));
        }
        return index;
    }

    /** Last confirmed location of a holder replaces the old location, including
     * its previous owner and any previous world. Removing a holder is not a debit. */
    public synchronized void apply(List<Change> changes) {
        for (var change : List.copyOf(changes)) {
            var previous = byHolder.get(change.holder());
            if (Objects.equals(previous,change) || previous == null && change.owner() == null) continue;
            if (previous != null) {
                var owned = byOwner.get(previous.owner());
                owned.remove(change.holder());
                if (owned.isEmpty()) byOwner.remove(previous.owner());
                byHolder.remove(change.holder());
            }
            if (change.owner() != null) {
                byHolder.put(change.holder(),change);
                byOwner.computeIfAbsent(change.owner(),ignored -> new HashMap<>()).put(change.holder(),change.location());
            }
            revision = Math.addExact(revision,1);
            if(previous!=null)ownerRevisions.put(previous.owner(),revision);
            if(change.owner()!=null)ownerRevisions.put(change.owner(),revision);
        }
    }
    public synchronized Set<Location> locations(UUID owner) {
        return Set.copyOf(byOwner.getOrDefault(owner,Map.of()).values());
    }
    public synchronized Set<UUID> owners() { return Set.copyOf(byOwner.keySet()); }
    public synchronized long revision() { return revision; }
    public synchronized long revision(UUID owner){return ownerRevisions.getOrDefault(owner,0L);}
}
