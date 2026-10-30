package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

/**
 * Interprets stored Java item NBT without Bukkit API. This is a value decoder
 * for the known coin/wallet PDC contract, not a complete settlement scanner.
 * Unknown or malformed currency tags are errors, never an asset value of zero.
 */
public final class StoredItemValue {
    private StoredItemValue() {}

    public static BigInteger value(Map<String, Object> item) throws IOException {
        Objects.requireNonNull(item);
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

        Object rawCustom = components.get("minecraft:custom_data");
        if (rawCustom == null) return BigInteger.ZERO;
        if (!(rawCustom instanceof Map<?,?> custom))
            throw new IOException("Invalid stored custom data");
        Object rawPdc = custom.get("PublicBukkitValues");
        if (rawPdc == null) return BigInteger.ZERO;
        if (!(rawPdc instanceof Map<?,?> pdc))
            throw new IOException("Invalid Bukkit persistent data");

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
        return BigInteger.ZERO;
    }
}
