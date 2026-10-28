package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.WalletBalance;
import co.surumene.simplecoins.domain.WalletDragProtection;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class WalletMenu implements Listener {
    private final CoinItems items;
    private final SimpleCoinsPlugin plugin;

    public WalletMenu(CoinItems items, SimpleCoinsPlugin plugin) {
        this.items = items; this.plugin = plugin;
    }

    private static final class Holder implements InventoryHolder {
        private Inventory inventory;
        private final UUID player;
        private final String walletId;
        private final int slot;

        private Holder(Player player, String walletId) {
            this.player = player.getUniqueId();
            this.walletId = walletId;
            this.slot = player.getInventory().getHeldItemSlot();
        }

        @Override
        public Inventory getInventory() { return inventory; }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack wallet = player.getInventory().getItemInMainHand();
        if (!items.isWallet(wallet)) return;
        event.setCancelled(true);
        Holder holder = new Holder(player, items.walletId(wallet));
        holder.inventory = Bukkit.createInventory(holder, 9, "Wallet (" + items.currency() + ")");
        render(holder);
        player.openInventory(holder.inventory);
    }

    private ItemStack current(Player player, Holder holder) {
        if (!player.getUniqueId().equals(holder.player)) return null;
        ItemStack wallet = player.getInventory().getItem(holder.slot);
        return items.isWallet(wallet) && holder.walletId.equals(items.walletId(wallet)) ? wallet : null;
    }

    private void render(Holder holder) {
        Player player = Bukkit.getPlayer(holder.player);
        if (player == null) return;
        ItemStack wallet = current(player, holder);
        if (wallet == null) return;
        long amount;
        try { amount = items.balance(wallet); } catch (RuntimeException bad) { return; }
        int index = 0;
        for (CoinDenomination d : CoinDenomination.values()) {
            int displayCount = (int) Math.max(1, Math.min(64, amount / d.value()));
            holder.inventory.setItem(index++, items.coin(d, displayCount));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack wallet = current(player, holder);
        if (wallet == null) { event.setCancelled(true); player.closeInventory(); return; }
        boolean top = event.getRawSlot() < 9 && event.getRawSlot() >= 0;
        if (!top) {
            // Prevent the backing wallet item from being taken/replaced while the menu is open.
            if (event.getClickedInventory() instanceof PlayerInventory &&
                    event.getSlot() == holder.slot) {
                event.setCancelled(true);
                return;
            }
            // Shift-clicking a coin in the player's inventory deposits its whole stack.
            if (event.isShiftClick() && items.denomination(event.getCurrentItem()) != null) {
                event.setCancelled(true);
                ItemStack stack = event.getCurrentItem();
                if (stack != null && deposit(player, holder, wallet, stack.getAmount() *
                        (long) items.denomination(stack).value())) {
                    event.getClickedInventory().setItem(event.getSlot(), null);
                }
            }
            return;
        }
        event.setCancelled(true); // GUI icons must never themselves be taken.
        if (event.getClick().isCreativeAction()) return;
        CoinDenomination denomination = CoinDenomination.values()[event.getRawSlot()];
        ItemStack cursor = event.getCursor();
        CoinDenomination fromCursor = items.denomination(cursor);
        if (fromCursor != null) {
            if (fromCursor != denomination) return;
            int count = event.isRightClick() ? 1 : cursor.getAmount();
            if (deposit(player, holder, wallet, count * (long) denomination.value())) {
                if (count == cursor.getAmount()) event.getView().setCursor(null);
                else { ItemStack remaining = cursor.clone(); remaining.setAmount(cursor.getAmount() - count); event.getView().setCursor(remaining); }
            }
            return;
        }
        if (cursor != null && cursor.getType() != Material.AIR) return;
        long available = items.balance(wallet) / denomination.value();
        if (available <= 0) return;
        int count = (int) Math.min(event.isRightClick() ? 1 : 64, available);
        if (event.isShiftClick()) {
            ItemStack payout = items.coin(denomination, count);
            if (!fits(player.getInventory(), payout)) return;
            player.getInventory().addItem(payout);
        } else {
            event.getView().setCursor(items.coin(denomination, count));
        }
        apply(player, holder, wallet, new WalletBalance(items.balance(wallet)).withdraw(count * (long) denomination.value()).amount());
    }

    private boolean deposit(Player player, Holder holder, ItemStack wallet, long amount) {
        try {
            long next = new WalletBalance(items.balance(wallet)).deposit(amount, items.maximumBalance()).amount();
            apply(player, holder, wallet, next);
            return true;
        } catch (IllegalArgumentException | IllegalStateException | ArithmeticException rejected) {
            player.sendMessage("Cannot deposit: " + rejected.getMessage());
            return false;
        }
    }

    private void apply(Player player, Holder holder, ItemStack wallet, long balance) {
        player.getInventory().setItem(holder.slot, items.updateWallet(wallet, balance));
        render(holder);
    }

    private static boolean fits(PlayerInventory inventory, ItemStack candidate) {
        int remaining = candidate.getAmount();
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                remaining -= 64;
            } else if (slot.isSimilar(candidate)) {
                remaining -= Math.max(0, 64 - slot.getAmount());
            }
            if (remaining <= 0) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        if (WalletDragProtection.shouldCancel(event.getRawSlots(),
                event.getView().getTopInventory().getSize(),
                event.getView().getTopInventory().getSize() + 27,
                holder.slot)) {
            event.setCancelled(true);
        }
    }
}
