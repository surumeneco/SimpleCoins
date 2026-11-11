package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.WalletBalance;
import co.surumene.simplecoins.domain.WalletTransferTransaction;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Nine-denomination wallet GUI: all operations remain main-thread and are
 * rejected before mutation if item identity, balance, value, or space is bad.
 *
 * This is a Paper transaction candidate, not a durable two-phase world/journal
 * transaction: G0 must reconcile crash boundaries before admitting players.
 */
public final class WalletMenu implements Listener {
    private final WalletItemCodec wallets;
    private final CoinItemCodec coins;
    private final long capacity;
    private final Consumer<Throwable> onUnrecoverable;
    private boolean halted;

    public WalletMenu(WalletItemCodec wallets, CoinItemCodec coins, long capacity,
                      Consumer<Throwable> onUnrecoverable) {
        this.wallets = Objects.requireNonNull(wallets, "wallets");
        this.coins = Objects.requireNonNull(coins, "coins");
        if (capacity < 0) throw new IllegalArgumentException("Negative wallet capacity");
        this.capacity = capacity;
        this.onUnrecoverable = Objects.requireNonNull(onUnrecoverable, "onUnrecoverable");
    }

    private void unrecoverable(Throwable failure) {
        halted = true; // No further in-process wallet operations, even before shutdown.
        try {
            // Main plugin must synchronously revoke its published admission
            // permission, not wait for the independent guard's next poll.
            onUnrecoverable.accept(failure);
        } finally {
            Bukkit.shutdown();
        }
    }

    private static final class Holder implements InventoryHolder {
        final UUID player;
        final UUID walletId;
        final int hotbarSlot;
        Inventory inventory;

        Holder(Player player, UUID walletId) {
            this.player = player.getUniqueId();
            this.walletId = walletId;
            this.hotbarSlot = player.getInventory().getHeldItemSlot();
        }

        @Override public Inventory getInventory() { return inventory; }
    }

    private static boolean empty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private WalletItemCodec.Wallet identity(ItemStack item) {
        return wallets.read(item).orElseThrow(
                () -> new IllegalStateException("Wallet source is missing"));
    }

    private ItemStack currentWallet(Player player, Holder holder) {
        if (!player.getUniqueId().equals(holder.player)) return null;
        ItemStack current = player.getInventory().getItem(holder.hotbarSlot);
        try {
            return identity(current).id().equals(holder.walletId) ? current : null;
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (halted) {
            if (wallets.hasWalletMarker(event.getItem())) event.setCancelled(true);
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            // A marked wallet in the other hand may not bypass our GUI.
            if (wallets.hasWalletMarker(player.getInventory().getItemInOffHand()))
                event.setCancelled(true);
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!wallets.hasWalletMarker(item)) return;
        event.setCancelled(true);
        WalletItemCodec.Wallet wallet;
        try { wallet = identity(item); }
        catch (RuntimeException invalid) { return; }
        Holder holder = new Holder(player, wallet.id());
        holder.inventory = Bukkit.createInventory(holder, 9, "Wallet");
        render(player, holder);
        player.openInventory(holder.inventory);
    }

    private void render(Player player, Holder holder) {
        ItemStack item = currentWallet(player, holder);
        if (item == null) return;
        long balance;
        try { balance = identity(item).balance(); }
        catch (RuntimeException invalid) { return; }
        for (int i = 0; i < CoinDenomination.values().length; i++) {
            CoinDenomination d = CoinDenomination.values()[i];
            int count = (int) Math.min(64L, Math.max(1L, balance / d.value()));
            holder.inventory.setItem(i, coins.create(d, count));
        }
    }

    private boolean deposit(Player player, Holder holder, ItemStack wallet,
                            long value, Runnable consume, Runnable restoreCoins) {
        ItemStack oldWallet = wallet.clone();
        try {
            long next = new WalletBalance(identity(wallet).balance())
                    .deposit(value, capacity).amount();
            ItemStack nextWallet = wallets.withBalance(wallet, next);
            WalletTransferTransaction.execute(
                    () -> player.getInventory().setItem(holder.hotbarSlot, nextWallet),
                    consume,
                    () -> player.getInventory().setItem(holder.hotbarSlot, oldWallet),
                    restoreCoins);
        } catch (WalletTransferTransaction.Unrecoverable cannotRestore) {
            // The player/world may already contain newly minted or destroyed
            // money. Shutdown is necessary but NOT proof of durable recovery.
            unrecoverable(cannotRestore);
            return false;
        } catch (RuntimeException rejected) {
            return false;
        }
        // GUI pseudo-items are not monetary state. Failure to repaint must
        // never cause the completed monetary transaction to be retried.
        try { render(player, holder); } catch (RuntimeException displayFailure) {
            player.closeInventory();
        }
        return true;
    }

    private static ItemStack[] copyStorage(ItemStack[] contents) {
        ItemStack[] snapshot = contents.clone();
        for (int i = 0; i < snapshot.length; i++)
            if (snapshot[i] != null) snapshot[i] = snapshot[i].clone();
        return snapshot;
    }

    private static boolean fits(PlayerInventory inventory, ItemStack payment) {
        int remaining = payment.getAmount();
        for (ItemStack item : inventory.getStorageContents()) {
            if (empty(item)) remaining -= 64;
            else if (item.isSimilar(payment))
                remaining -= Math.max(0, 64 - item.getAmount());
            if (remaining <= 0) return true;
        }
        return false;
    }

    private static CoinDenomination coin(CoinItemCodec codec, ItemStack item) {
        if (empty(item)) return null;
        if (!codec.hasCoinMarker(item)) return null;
        try { return codec.identify(item).orElse(null); }
        catch (RuntimeException invalid) { return null; }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        event.setCancelled(true);
        if (halted) return;
        if (!(event.getWhoClicked() instanceof Player player)
                || !player.getUniqueId().equals(holder.player)) return;
        ItemStack wallet = currentWallet(player, holder);
        if (wallet == null) {
            player.closeInventory();
            return;
        }

        // No vanilla transfers may take display coins or replace/move the
        // underlying wallet, including hotbar swaps and offhand pickup.
        if (event.getClick() == ClickType.NUMBER_KEY
                || event.getClick() == ClickType.SWAP_OFFHAND
                || event.getAction() == InventoryAction.COLLECT_TO_CURSOR
                || event.getClick() == ClickType.DOUBLE_CLICK
                || event.getClick().isCreativeAction()) return;

        int slot = event.getRawSlot();
        if (slot < 0) return;
        if (slot >= 9) {
            if (event.getClickedInventory() == null
                    || event.getClickedInventory().getType() != org.bukkit.event.inventory.InventoryType.PLAYER
                    || event.getSlot() == holder.hotbarSlot) return;
            // Ordinary left/right clicks in the player's lower inventory
            // remain vanilla, unless either item is a wallet or the source
            // hotbar slot is involved. All top GUI currency icons remain locked.
            if (!event.isShiftClick()) {
                if ((event.getClick() == ClickType.LEFT
                        || event.getClick() == ClickType.RIGHT)
                        && !wallets.hasWalletMarker(event.getCurrentItem())
                        && !wallets.hasWalletMarker(event.getCursor()))
                    event.setCancelled(false);
                return;
            }
            // Deposit an entire real coin stack on Shift-clicking the player's
            // inventory. Reject corrupt denomination tags and amount 0.
            ItemStack offered = event.getCurrentItem();
            CoinDenomination denomination = coin(coins, offered);
            if (denomination == null || offered == null
                    || offered.getAmount() < 1 || offered.getAmount() > 64) return;
            long value = Math.multiplyExact((long) denomination.value(), offered.getAmount());
            int playerSlot = event.getSlot();
            var clicked = event.getClickedInventory();
            ItemStack before = offered.clone();
            deposit(player, holder, wallet, value,
                    () -> clicked.setItem(playerSlot, null),
                    () -> clicked.setItem(playerSlot, before));
            return;
        }

        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT
                && event.getClick() != ClickType.SHIFT_LEFT
                && event.getClick() != ClickType.SHIFT_RIGHT) return;
        CoinDenomination denomination = CoinDenomination.values()[slot];
        ItemStack cursor = event.getCursor();
        if (!empty(cursor)) {
            if (event.isShiftClick()) return;
            if (coin(coins, cursor) != denomination || cursor.getAmount() < 1
                    || cursor.getAmount() > 64) return;
            int count = event.isRightClick() ? 1 : cursor.getAmount();
            long value = (long) count * denomination.value();
            ItemStack before = cursor.clone();
            deposit(player, holder, wallet, value, () -> {
                if (cursor.getAmount() == count) event.getView().setCursor(null);
                else {
                    ItemStack remainder = cursor.clone();
                    remainder.setAmount(cursor.getAmount() - count);
                    event.getView().setCursor(remainder);
                }
            }, () -> event.getView().setCursor(before));
            return;
        }

        try {
            long balance = identity(wallet).balance();
            long available = balance / denomination.value();
            if (available < 1) return;
            int count = (int) Math.min(available,
                    event.isRightClick() ? 1L : 64L);
            ItemStack payout = coins.create(denomination, count);
            ItemStack nextWallet = wallets.withBalance(wallet,
                    new WalletBalance(balance).withdraw((long) count * denomination.value()).amount());

            Runnable writeCoins;
            Runnable restoreCoins;
            if (event.isShiftClick()) {
                if (!fits(player.getInventory(), payout)) return;
                ItemStack[] originalStorage = copyStorage(player.getInventory().getStorageContents());
                writeCoins = () -> {
                    var leftovers = player.getInventory().addItem(payout);
                    if (!leftovers.isEmpty())
                        throw new IllegalStateException("Inventory changed during wallet payout");
                };
                restoreCoins = () -> player.getInventory().setStorageContents(originalStorage);
            } else {
                ItemStack oldCursor = event.getView().getCursor();
                ItemStack cursorSnapshot = oldCursor == null ? null : oldCursor.clone();
                writeCoins = () -> event.getView().setCursor(payout);
                restoreCoins = () -> event.getView().setCursor(cursorSnapshot);
            }
            ItemStack oldWallet = wallet.clone();
            WalletTransferTransaction.execute(
                    () -> player.getInventory().setItem(holder.hotbarSlot, nextWallet),
                    writeCoins,
                    () -> player.getInventory().setItem(holder.hotbarSlot, oldWallet),
                    restoreCoins);
        } catch (WalletTransferTransaction.Unrecoverable cannotRestore) {
            unrecoverable(cannotRestore);
            return;
        } catch (RuntimeException invalid) {
            // In-memory snapshots were restored when this was a partial write.
            return;
        }
        try { render(player, holder); } catch (RuntimeException displayFailure) {
            player.closeInventory();
        }
    }

    /**
     * Bundle is only a durable physical shell for the independent wallet PDC.
     * Vanilla Bundle content insertion would create a second unmanaged asset
     * compartment, so reject native right-click wallet interactions outside
     * the dedicated nine-slot GUI as well.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNativeBundleClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) return;
        if (!event.isRightClick()) return;
        if (wallets.hasWalletMarker(event.getCurrentItem())
                || wallets.hasWalletMarker(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            // Preserve both wallet source slot and all nine pseudo-items.
            // A later JE/BE UX enhancement may support narrower drag patterns.
            event.setCancelled(true);
        }
    }
}
