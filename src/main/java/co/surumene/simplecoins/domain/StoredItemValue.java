package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Read-only value extraction for stored 26.x Java item components, including
 * bundle/shulker descendants. The enclosing settlement must provide a stable
 * snapshot and abort on any IOException rather than publishing partial totals.
 */
public final class StoredItemValue {
    private static final int MAX_DEPTH = 32;
    private StoredItemValue() {}

    public static BigInteger value(Map<String, Object> item) throws IOException {
        return value(item, 0);
    }

    private static BigInteger value(Map<String, Object> item, int depth) throws IOException {
        Objects.requireNonNull(item);
        if (depth > MAX_DEPTH)
            throw new IOException("Stored container nesting exceeds " + MAX_DEPTH);
        Object rawId = item.get("id");
        Object rawCount = item.get("count");
        if (!(rawId instanceof String id) || id.isBlank() ||
                !(rawCount instanceof Number count) || count.longValue() <= 0 ||
                count.longValue() > Integer.MAX_VALUE ||
                count instanceof Float || count instanceof Double)
            throw new IOException("Invalid stored item identity or count");
        long quantity = count.longValue();

        Object rawComponents = item.get("components");
        if (rawComponents == null) return BigInteger.ZERO;
        if (!(rawComponents instanceof Map<?,?> components))
            throw new IOException("Invalid stored item components");

        Map<?,?> pdc = null;
        Object rawCustom = components.get("minecraft:custom_data");
        if (rawCustom != null) {
            if (!(rawCustom instanceof Map<?,?> custom))
                throw new IOException("Invalid stored custom data");
            Object rawPdc = custom.get("PublicBukkitValues");
            if (rawPdc != null) {
                if (!(rawPdc instanceof Map<?,?> values))
                    throw new IOException("Invalid Bukkit persistent data");
                pdc = values;
            }
        }

        if (pdc != null) {
            if (pdc.containsKey("simplecoins:denomination")) {
                if (!id.equals("minecraft:paper") || quantity > 64 ||
                        !(pdc.get("simplecoins:denomination") instanceof Integer denomination))
                    throw new IOException("Malformed stored coin");
                try {
                    return BigInteger.valueOf(CoinDenomination.fromValue(denomination).value())
                            .multiply(BigInteger.valueOf(quantity));
                } catch (IllegalArgumentException invalid) {
                    throw new IOException("Unknown stored coin denomination", invalid);
                }
            }
            if (pdc.containsKey("simplecoins:wallet_id")) {
                if (!(id.equals("minecraft:bundle") || id.endsWith("_bundle")) || quantity != 1 ||
                        !(pdc.get("simplecoins:wallet_id") instanceof String walletId) ||
                        walletId.isBlank() ||
                        !(pdc.get("simplecoins:wallet_balance") instanceof Long balance) || balance < 0)
                    throw new IOException("Malformed stored wallet");
                return BigInteger.valueOf(balance);
            }
            if (pdc.containsKey("simplecoins:wallet_balance"))
                throw new IOException("Orphaned wallet balance tag");
        }

        BigInteger nested = BigInteger.ZERO;
        Object rawBundle = components.get("minecraft:bundle_contents");
        if (rawBundle != null) {
            if (!(rawBundle instanceof List<?> contents))
                throw new IOException("Invalid bundle contents");
            for (Object child : contents)
                nested = nested.add(value(itemMap(child), depth + 1));
        }
        Object rawContainer = components.get("minecraft:container");
        if (rawContainer != null) {
            if (!(rawContainer instanceof List<?> contents))
                throw new IOException("Invalid container contents");
            for (Object entry : contents) {
                if (!(entry instanceof Map<?,?> slot) || !slot.containsKey("item"))
                    throw new IOException("Malformed container slot");
                nested = nested.add(value(itemMap(slot.get("item")), depth + 1));
            }
        }
        return nested.multiply(BigInteger.valueOf(quantity));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> itemMap(Object raw) throws IOException {
        if (!(raw instanceof Map<?, ?> contents) ||
                contents.keySet().stream().anyMatch(key -> !(key instanceof String)))
            throw new IOException("Malformed nested item");
        return (Map<String, Object>) contents;
    }
}
