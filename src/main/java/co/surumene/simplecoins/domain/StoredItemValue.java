package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only value extraction for stored 26.x Java item components, including
 * bundle/shulker descendants. The enclosing settlement must provide a stable
 * snapshot and abort on any IOException rather than publishing partial totals.
 */
public final class StoredItemValue {
    private static final int MAX_DEPTH = 32;
    private static final Set<String> VANILLA_BUNDLE_IDS = Set.of(
            "minecraft:bundle", "minecraft:white_bundle", "minecraft:orange_bundle",
            "minecraft:magenta_bundle", "minecraft:light_blue_bundle",
            "minecraft:yellow_bundle", "minecraft:lime_bundle",
            "minecraft:pink_bundle", "minecraft:gray_bundle",
            "minecraft:light_gray_bundle", "minecraft:cyan_bundle",
            "minecraft:purple_bundle", "minecraft:blue_bundle",
            "minecraft:brown_bundle", "minecraft:green_bundle",
            "minecraft:red_bundle", "minecraft:black_bundle");
    private StoredItemValue() {}

    public static BigInteger value(Map<String, Object> item) throws IOException {
        return value(item, new HashSet<>());
    }

    /**
     * The caller supplies a mutable identity set for one authoritative source
     * or full validated generation. Duplicate wallet UUIDs mean ambiguous
     * physical value, never a valid extra balance.
     *
     * Discard this set on a failed scan rather than attempting partial reuse.
     */
    public static BigInteger value(Map<String, Object> item,
                                   Set<UUID> seenWallets) throws IOException {
        Objects.requireNonNull(seenWallets, "wallet identity context");
        return value(item, 0, seenWallets);
    }

    private static BigInteger value(Map<String, Object> item, int depth,
                                    Set<UUID> seenWallets) throws IOException {
        Objects.requireNonNull(item);
        if (depth > MAX_DEPTH)
            throw new IOException("Stored container nesting exceeds " + MAX_DEPTH);
        Object rawId = item.get("id");
        // Modern ItemStack NBT omits the count field when it is one.
        // An explicitly present but malformed count must still fail closed.
        Object rawCount = item.getOrDefault("count", Integer.valueOf(1));
        if (!(rawId instanceof String id) || id.isBlank() ||
                !(rawCount instanceof Number count) ||
                !(count instanceof Byte || count instanceof Short ||
                  count instanceof Integer || count instanceof Long) ||
                count.longValue() <= 0 || count.longValue() > 99)
            throw new IOException("Invalid stored item identity or count");
        long quantity = count.longValue();
        // Air is not a physical saved inventory stack. An air identifier
        // cannot legitimately carry hidden nested assets or a coin balance.
        if ("minecraft:air".equals(id))
            throw new IOException("Nonphysical stored air item");

        if (!item.containsKey("components")) return BigInteger.ZERO;
        Object rawComponents = item.get("components");
        if (!(rawComponents instanceof Map<?,?> components))
            throw new IOException("Invalid stored item components");
        // A single physical item cannot expose two independent views of the
        // same nested inventory. Summing both would risk double-counting coins.
        if (components.containsKey("minecraft:bundle_contents") &&
                components.containsKey("minecraft:container"))
            throw new IOException("Ambiguous stored item nested storage components");
        // Nonempty vanilla bundles and shulker boxes are individual physical
        // containers. A fabricated stack count must not multiply their assets.
        if (quantity > 1 && id.startsWith("minecraft:") &&
                (id.equals("minecraft:bundle") || id.endsWith("_bundle") ||
                 id.equals("minecraft:shulker_box") || id.endsWith("_shulker_box")) &&
                ((components.get("minecraft:bundle_contents") instanceof List<?> bundle &&
                        !bundle.isEmpty()) ||
                 (components.get("minecraft:container") instanceof List<?> container &&
                        !container.isEmpty())))
            throw new IOException("Stacked populated container cannot be accounted safely");

        Map<?,?> pdc = null;
        Object rawCustom = components.get("minecraft:custom_data");
        if (components.containsKey("minecraft:custom_data")) {
            if (!(rawCustom instanceof Map<?,?> custom))
                throw new IOException("Invalid stored custom data");
            Object rawPdc = custom.get("PublicBukkitValues");
            if (custom.containsKey("PublicBukkitValues")) {
                if (!(rawPdc instanceof Map<?,?> values))
                    throw new IOException("Invalid Bukkit persistent data");
                pdc = values;
            }
        }

        if (pdc != null) {
            // The active Paper ItemStack codec writes coin_denomination,
            // while older saved-data fixtures use denomination. Never treat
            // real issued coins as ordinary paper worth zero during settlement.
            boolean activeCoin = pdc.containsKey("simplecoins:coin_denomination");
            boolean legacyCoin = pdc.containsKey("simplecoins:denomination");
            if (activeCoin && legacyCoin)
                throw new IOException("Conflicting stored coin denomination tags");
            // A physical item cannot simultaneously be a coin and a wallet.
            if ((activeCoin || legacyCoin) &&
                    (pdc.containsKey("simplecoins:wallet_id") ||
                     pdc.containsKey("simplecoins:wallet_balance")))
                throw new IOException("Conflicting coin and wallet identity tags");
            if (activeCoin || legacyCoin) {
                Object rawDenomination = pdc.get(activeCoin
                        ? "simplecoins:coin_denomination" : "simplecoins:denomination");
                if (!id.equals("minecraft:paper") || quantity > 64 ||
                        !(rawDenomination instanceof Integer denomination))
                    throw new IOException("Malformed stored coin");
                // A marked monetary item may not hide a second asset inside it.
                for (String childKey : List.of("minecraft:bundle_contents", "minecraft:container")) {
                    if (components.containsKey(childKey))
                        throw new IOException("Coin contains unexpected nested storage: " + childKey);
                }
                try {
                    CoinDenomination face = CoinDenomination.fromValue(denomination);
                    if (activeCoin) {
                        // Match CoinItemCodec.modelKey, not mutable display text.
                        // A forged or missing item_model cannot mint currency.
                        Object rawModel = components.get("minecraft:item_model");
                        if (!(rawModel instanceof String model) ||
                                !model.equals("simplecoins:coin_" + face.value()))
                            throw new IOException("Stored coin item_model mismatch");
                    }
                    return BigInteger.valueOf(face.value())
                            .multiply(BigInteger.valueOf(quantity));
                } catch (IllegalArgumentException invalid) {
                    throw new IOException("Unknown stored coin denomination", invalid);
                }
            }
            if (pdc.containsKey("simplecoins:wallet_id")) {
                if (!VANILLA_BUNDLE_IDS.contains(id) || quantity != 1 ||
                        !(pdc.get("simplecoins:wallet_id") instanceof String walletId) ||
                        walletId.isBlank() ||
                        !(pdc.get("simplecoins:wallet_balance") instanceof Long balance) || balance < 0)
                    throw new IOException("Malformed stored wallet");
                try {
                    if (!UUID.fromString(walletId).toString().equals(walletId))
                        throw new IOException("Noncanonical wallet UUID");
                } catch (IllegalArgumentException invalid) {
                    throw new IOException("Invalid wallet UUID", invalid);
                }
                for (String childKey : List.of("minecraft:bundle_contents", "minecraft:container")) {
                    Object children = components.get(childKey);
                    if (components.containsKey(childKey) &&
                            (!(children instanceof List<?> list) || !list.isEmpty()))
                        throw new IOException("Wallet contains unexpected nested items: " + childKey);
                }
                if (!seenWallets.add(UUID.fromString(walletId)))
                    throw new IOException("Duplicate physical wallet UUID: " + walletId);
                return BigInteger.valueOf(balance);
            }
            if (pdc.containsKey("simplecoins:wallet_balance"))
                throw new IOException("Orphaned wallet balance tag");
        }

        BigInteger nested = BigInteger.ZERO;
        Object rawBundle = components.get("minecraft:bundle_contents");
        if (components.containsKey("minecraft:bundle_contents")) {
            if (!(rawBundle instanceof List<?> contents))
                throw new IOException("Invalid bundle contents");
            for (Object child : contents)
                nested = nested.add(value(itemMap(child), depth + 1, seenWallets));
        }
        Object rawContainer = components.get("minecraft:container");
        if (components.containsKey("minecraft:container")) {
            if (!(rawContainer instanceof List<?> contents))
                throw new IOException("Invalid container contents");
            Set<Integer> occupiedSlots = new HashSet<>();
            for (Object entry : contents) {
                if (!(entry instanceof Map<?,?> slot) || !slot.containsKey("item"))
                    throw new IOException("Malformed container slot");
                Object rawSlot = slot.get("slot");
                if (!(rawSlot instanceof Number number) ||
                        !(number instanceof Byte || number instanceof Short ||
                          number instanceof Integer || number instanceof Long) ||
                        number.longValue() < 0 || number.longValue() > 255)
                    throw new IOException("Invalid nested container slot");
                // Vanilla shulker boxes have 27 slots. Treat an out-of-range
                // saved slot as corrupt instead of publishing fictitious assets.
                if ((id.equals("minecraft:shulker_box") ||
                        (id.startsWith("minecraft:") && id.endsWith("_shulker_box"))) &&
                        number.longValue() > 26)
                    throw new IOException("Invalid shulker box slot: " + number.longValue());
                if (!occupiedSlots.add(number.intValue()))
                    throw new IOException("Duplicate nested container slot: " + number.intValue());
                nested = nested.add(value(itemMap(slot.get("item")), depth + 1, seenWallets));
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
