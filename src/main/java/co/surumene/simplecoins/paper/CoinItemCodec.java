package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.CoinDenomination;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Paper 26.2 physical currency codec (Task002 technology candidate).
 *
 * Canonical identity is an integer PDC denomination under a fixed plugin
 * namespace, never display name or resource-pack text. The item_model is
 * stable per denomination across configurable display names.
 *
 * Do not deploy as a complete coin system: vanilla-paper recipe exclusion,
 * the 20 exchange recipes, crafter support, trader drops and ledger journals
 * are separate pending integration units.
 */
public final class CoinItemCodec {
    public static final NamespacedKey DENOMINATION_KEY =
            new NamespacedKey("simplecoins", "coin_denomination");

    private final Map<CoinDenomination, String> displayNames;

    public CoinItemCodec(Map<CoinDenomination, String> configuredNames) {
        Objects.requireNonNull(configuredNames, "configuredNames");
        EnumMap<CoinDenomination, String> names = new EnumMap<>(CoinDenomination.class);
        for (CoinDenomination denomination : CoinDenomination.values()) {
            String value = Objects.requireNonNull(configuredNames.get(denomination),
                    "Missing name for " + denomination);
            if (value.isBlank()) throw new IllegalArgumentException("Blank name for " + denomination);
            names.put(denomination, value);
        }
        this.displayNames = Map.copyOf(names);
    }

    public static Map<CoinDenomination, String> defaultNames() {
        EnumMap<CoinDenomination, String> result = new EnumMap<>(CoinDenomination.class);
        for (CoinDenomination denomination : CoinDenomination.values()) {
            result.put(denomination, denomination.value() + " Coin");
        }
        return Map.copyOf(result);
    }

    public static NamespacedKey modelKey(CoinDenomination denomination) {
        Objects.requireNonNull(denomination, "denomination");
        return new NamespacedKey("simplecoins", "coin_" + denomination.value());
    }

    /** Creates exactly 1..64 authentic PAPER coins of one denomination. */
    public ItemStack create(CoinDenomination denomination, int count) {
        Objects.requireNonNull(denomination, "denomination");
        if (count < 1 || count > 64)
            throw new IllegalArgumentException("Coin stack size must be within 1..64");
        ItemStack stack = new ItemStack(Material.PAPER, count);
        ItemMeta meta = Objects.requireNonNull(stack.getItemMeta(), "Paper ItemMeta unavailable");
        meta.getPersistentDataContainer().set(
                DENOMINATION_KEY, PersistentDataType.INTEGER, denomination.value());
        meta.setItemModel(modelKey(denomination));
        meta.displayName(Component.text(displayNames.get(denomination), NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Ordinary PAPER is not money. A malformed denomination marker, material,
     * or item_model is a hard failure: accounting must never silently convert
     * corrupt currency into a zero-value ordinary item.
     *
     * The display name is intentionally NOT part of the persistent identity.
     */
    public Optional<CoinDenomination> identify(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return Optional.empty();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(DENOMINATION_KEY))
            return Optional.empty();
        Integer faceValue = meta.getPersistentDataContainer().get(
                DENOMINATION_KEY, PersistentDataType.INTEGER);
        if (faceValue == null) throw new IllegalStateException("Invalid currency marker type");
        CoinDenomination denomination;
        try {
            denomination = CoinDenomination.fromValue(faceValue);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalStateException("Unknown currency denomination", invalid);
        }
        if (stack.getType() != Material.PAPER)
            throw new IllegalStateException("Currency marker on non-paper material");
        if (!modelKey(denomination).equals(meta.getItemModel()))
            throw new IllegalStateException("Currency item_model mismatch");
        return Optional.of(denomination);
    }

    /** Recognize even corrupt tagged items so recipe guards can deny them. */
    public boolean hasCoinMarker(ItemStack stack) {
        return stack != null && !stack.getType().isAir()
                && stack.getItemMeta() != null
                && stack.getItemMeta().getPersistentDataContainer().has(DENOMINATION_KEY);
    }
}
