package co.surumene.simplecoins.domain;

import java.math.BigInteger;
import java.util.*;

/** Transient observations of touched physical sources only. One first image
 * per source, one confirmed final image, no retained world/inventory index. */
public final class LocalValueChanges<K> {
    private record Image(MemoryAccounting.Endpoint endpoint, BigInteger value) {
        Image {
            Objects.requireNonNull(endpoint);
            Objects.requireNonNull(value);
            if (endpoint.owner() == null || value.signum() < 0)
                throw new IllegalArgumentException("Invalid physical observation");
        }
    }
    private final Map<K,Image> before = new HashMap<>(), after = new HashMap<>();

    public boolean contains(K key) { return before.containsKey(key); }
    public void observe(K key, MemoryAccounting.Endpoint endpoint, BigInteger value) {
        before.putIfAbsent(Objects.requireNonNull(key), new Image(endpoint, value));
    }
    public void excludeAlreadyRecorded(K key, BigInteger value) {
        if (value.signum() < 0) throw new IllegalArgumentException("Negative recorded issuance");
        before.computeIfPresent(key, (k,v) -> new Image(v.endpoint(), v.value().add(value)));
    }
    public void finish(K key, MemoryAccounting.Endpoint endpoint, BigInteger value) {
        if (!before.containsKey(key)) throw new IllegalStateException("Missing physical before-image");
        if (after.putIfAbsent(key, new Image(endpoint, value)) != null)
            throw new IllegalStateException("Repeated final observation");
    }
    public List<MemoryAccounting.Movement> movements() {
        if (!before.keySet().equals(after.keySet()))
            throw new IllegalStateException("Incomplete local observations");
        return InventoryAccountingDelta.between(totals(before), totals(after));
    }
    private static Map<MemoryAccounting.Endpoint,BigInteger> totals(Map<?,Image> images) {
        var totals = new HashMap<MemoryAccounting.Endpoint,BigInteger>();
        for (var image:images.values()) totals.merge(image.endpoint(),image.value(),BigInteger::add);
        return totals;
    }
    public void clear() { before.clear(); after.clear(); }
}
