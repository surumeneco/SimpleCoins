package co.surumene.simplecoins.paper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Read-only live Paper Player currency inventory source. Does NOT include
 * offline playerdata, external ownership, or a settled generation.
 *
 * The storage, armor and extra API arrays are disjoint logical slots; using
 * PlayerInventory.getContents() as a fourth source would count armor/offhand
 * multiple times. World filtering must not omit player/ender storage.
 */
public final class PaperLivePlayerAssets {
    private PaperLivePlayerAssets() {}

    public record Entry(String physicalKey, UUID owner, BigInteger amount) {
        public Entry {
            Objects.requireNonNull(owner);
            if (physicalKey == null || physicalKey.isBlank() ||
                    amount == null || amount.signum() <= 0)
                throw new IllegalArgumentException("Invalid live Player asset");
        }
    }

    public record Snapshot(UUID owner, int scannedSlots, List<Entry> entries) {
        public Snapshot {
            Objects.requireNonNull(owner);
            if (scannedSlots < 0) throw new IllegalArgumentException("Negative Player slot count");
            entries = List.copyOf(entries);
            if (entries.stream().anyMatch(e -> !e.owner().equals(owner)))
                throw new IllegalArgumentException("Foreign Player asset owner");
        }

        public BigInteger total() {
            BigInteger total = BigInteger.ZERO;
            for (Entry entry : entries) total = total.add(entry.amount());
            return total;
        }
        public boolean isCompleteSettlementSource() { return false; }
    }

    public record OnlineSet(int scannedPlayers, int scannedSlots, List<Snapshot> players) {
        public OnlineSet {
            if (scannedPlayers < 0 || scannedSlots < 0)
                throw new IllegalArgumentException("Negative online-source count");
            players = List.copyOf(players);
            if (players.size() != scannedPlayers)
                throw new IllegalArgumentException("Mismatched online Player source count");
            Set<UUID> unique = new HashSet<>();
            int countedSlots = 0;
            for (Snapshot player : players) {
                if (!unique.add(player.owner()))
                    throw new IllegalArgumentException("Duplicate Player source");
                countedSlots = Math.addExact(countedSlots, player.scannedSlots());
            }
            if (countedSlots != scannedSlots)
                throw new IllegalArgumentException("Online Player slot count mismatch");
        }

        public BigInteger total() {
            BigInteger result = BigInteger.ZERO;
            for (Snapshot player : players) result = result.add(player.total());
            return result;
        }
        public boolean isCompleteSettlementSource() { return false; }
    }

    public static Snapshot scan(Player player, int maxSlots,
                                int maxSerializedBytes, int maxInflatedBytes)
            throws IOException {
        return scan(player, maxSlots, maxSerializedBytes, maxInflatedBytes, new HashSet<>());
    }

    /** Shared wallet identity context is supplied by the higher-level scan. */
    static Snapshot scan(Player player, int maxSlots,
                         int maxSerializedBytes, int maxInflatedBytes,
                         Set<UUID> walletIds) throws IOException {
        Objects.requireNonNull(player, "Player source");
        Objects.requireNonNull(walletIds, "shared wallet UUID context");
        if (maxSlots < 1 || maxSerializedBytes < 1 || maxInflatedBytes < 1)
            throw new IllegalArgumentException("Positive Player source budgets required");
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Player inventory reads require Paper primary thread");
        if (!player.isOnline())
            throw new IOException("Player went offline before settlement read: " + player.getUniqueId());

        PlayerInventory inventory = Objects.requireNonNull(player.getInventory(), "Player inventory unavailable");
        List<Entry> entries = new ArrayList<>();
        Set<String> physicalSlots = new HashSet<>();
        UUID owner = player.getUniqueId();
        String prefix = "player/" + owner + "/";
        int slots = 0;
        slots = append(inventory.getStorageContents(), prefix + "inventory/",
                owner, maxSlots, slots, maxSerializedBytes, maxInflatedBytes,
                walletIds, physicalSlots, entries);
        slots = append(inventory.getArmorContents(), prefix + "armor/",
                owner, maxSlots, slots, maxSerializedBytes, maxInflatedBytes,
                walletIds, physicalSlots, entries);
        slots = append(inventory.getExtraContents(), prefix + "extra/",
                owner, maxSlots, slots, maxSerializedBytes, maxInflatedBytes,
                walletIds, physicalSlots, entries);
        slots = append(Objects.requireNonNull(player.getEnderChest(), "Ender Chest unavailable").getContents(),
                prefix + "ender/", owner, maxSlots, slots,
                maxSerializedBytes, maxInflatedBytes, walletIds, physicalSlots, entries);
        if (!player.isOnline())
            throw new IOException("Player went offline during settlement read: " + owner);
        return new Snapshot(owner, slots, entries);
    }

    /**
     * Inspect the selected online players exactly once each. Wallet IDs stay
     * unique across players; a failed scan yields no results or partial set.
     */
    public static OnlineSet scanOnline(int maxPlayers, int maxSlots,
                                       int maxSerializedBytes, int maxInflatedBytes)
            throws IOException {
        return scanOnline(maxPlayers, maxSlots, maxSerializedBytes,
                maxInflatedBytes, new HashSet<>());
    }

    /** Global wallet identity context from the selected Paper world scan. */
    static OnlineSet scanOnline(int maxPlayers, int maxSlots,
                                int maxSerializedBytes, int maxInflatedBytes,
                                Set<UUID> seenWallets) throws IOException {
        Objects.requireNonNull(seenWallets, "shared wallet IDs");
        if (maxPlayers < 1 || maxSlots < 1 ||
                maxSerializedBytes < 1 || maxInflatedBytes < 1)
            throw new IllegalArgumentException("Positive online Player budgets required");
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Online Player scan requires Paper primary thread");
        var selected = List.copyOf(Bukkit.getOnlinePlayers());
        if (selected.size() > maxPlayers)
            throw new IOException("Online Player count exceeds budget");
        Set<UUID> seenOwners = new HashSet<>();
        List<Snapshot> result = new ArrayList<>();
        int slots = 0;
        for (Player player : selected) {
            if (player == null || !seenOwners.add(player.getUniqueId()))
                throw new IOException("Duplicate/invalid live Player source");
            Snapshot candidate = scan(player, Math.max(1, maxSlots - slots),
                    maxSerializedBytes, maxInflatedBytes, seenWallets);
            if (candidate.scannedSlots() > maxSlots - slots)
                throw new IOException("Total Player slot budget exceeded");
            slots = Math.addExact(slots, candidate.scannedSlots());
            result.add(candidate);
        }
        Set<UUID> actualOwners = new HashSet<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || !actualOwners.add(online.getUniqueId()))
                throw new IOException("Invalid Player set after scan");
        }
        if (!actualOwners.equals(seenOwners))
            throw new IOException("Online Player set changed while scanning");
        return new OnlineSet(result.size(), slots, result);
    }

    private static int append(ItemStack[] stacks, String prefix, UUID owner,
                              int maxSlots, int scanned, int maxSerializedBytes,
                              int maxInflatedBytes, Set<UUID> walletIds,
                              Set<String> physicalSlots, List<Entry> entries)
            throws IOException {
        if (stacks == null || stacks.length > maxSlots - scanned)
            throw new IOException("Missing or oversized live Player inventory partition: " + prefix);
        for (int slot = 0; slot < stacks.length; slot++) {
            String key = prefix + slot;
            if (!physicalSlots.add(key))
                throw new IOException("Duplicate live Player physical slot: " + key);
            BigInteger amount = PaperLiveItemValue.stack(stacks[slot],
                    maxSerializedBytes, maxInflatedBytes, walletIds);
            if (amount.signum() > 0) entries.add(new Entry(key, owner, amount));
        }
        return scanned + stacks.length;
    }
}
