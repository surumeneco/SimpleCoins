package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.*;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Read only touched players/items. Paper temporarily changes pickup stack
 * counts; use the attempt's original image and the real end-of-tick state.
 * Spawn, removal, merge, drops and death may describe the same operation. */
public final class PaperGroundItemAccounting implements Listener {
    private static final int COMPRESSED=16*1024*1024, INFLATED=64*1024*1024;
    private final WorldScope scope;
    private final BooleanSupplier ready;
    private final Consumer<List<MemoryAccounting.Movement>> sink;
    private final Consumer<Exception> fatal;
    private final Consumer<Inventory> observeBlock;
    private final LocalValueChanges<String> changes=new LocalValueChanges<>();
    private final Map<UUID,Player> players=new HashMap<>();
    private final Map<UUID,Item> items=new HashMap<>();
    private final Map<UUID,BigInteger> unloaded=new HashMap<>();
    private static final Set<String> TRANSIENT_INPUTS=Set.of("CRAFTING","WORKBENCH","ANVIL",
            "SMITHING","MERCHANT","CARTOGRAPHY","GRINDSTONE","LOOM","STONECUTTER","ENCHANTING");

    public PaperGroundItemAccounting(WorldScope scope, BooleanSupplier ready,
            Consumer<List<MemoryAccounting.Movement>> sink, Consumer<Exception> fatal) {
        this(scope,ready,sink,fatal,ignored->{});
    }

    public PaperGroundItemAccounting(WorldScope scope, BooleanSupplier ready,
            Consumer<List<MemoryAccounting.Movement>> sink, Consumer<Exception> fatal,
            Consumer<Inventory> observeBlock) {
        this.scope=Objects.requireNonNull(scope);
        this.ready=Objects.requireNonNull(ready);
        this.sink=Objects.requireNonNull(sink);
        this.fatal=Objects.requireNonNull(fatal);
        this.observeBlock=Objects.requireNonNull(observeBlock);
    }

    private static String playerKey(UUID id) { return "player/"+id; }
    private static String itemKey(UUID id) { return "item/"+id; }
    private static BigInteger value(ItemStack stack) throws IOException {
        return PaperLiveItemValue.stack(stack,COMPRESSED,INFLATED);
    }
    private BigInteger groundValue(Item item) throws IOException {
        return scope.includes(item.getWorld().getName()) ? value(item.getItemStack()) : BigInteger.ZERO;
    }
    private BigInteger playerValue(Player player) throws IOException {
        if (!player.isOnline()) {
            var path=PaperPlayerGrantDelivery.playerdata(player.getUniqueId());
            if(path==null) throw new IOException("Touched offline playerdata unavailable");
            return SavedPlayerAssets.extract(PlayerDataReader.read(path,COMPRESSED,INFLATED))
                    .values().stream().reduce(BigInteger.ZERO,BigInteger::add);
        }
        var total=PaperLivePlayerAssets.scan(player,256,COMPRESSED,INFLATED).total();
        var view=player.getOpenInventory();
        total=total.add(value(view.getCursor()));
        if(TRANSIENT_INPUTS.contains(view.getTopInventory().getType().name())) {
            for(int i=0;i<view.getTopInventory().getSize();i++)
                if(view.getSlotType(i)!=InventoryType.SlotType.RESULT)
                    total=total.add(value(view.getTopInventory().getItem(i)));
        }
        return total;
    }
    @FunctionalInterface private interface Read { void run() throws IOException; }
    private void observe(Read operation) {
        if(!ready.getAsBoolean())return;
        try {
            if(!Bukkit.isPrimaryThread())throw new IOException("Monetary event off primary");
            operation.run();
        }catch(IOException|RuntimeException failure){clear();fatal.accept(failure);}
    }
    private void ground(Item item, boolean newItem) throws IOException {
        UUID id=item.getUniqueId();
        if(!changes.contains(itemKey(id))) {
            changes.observe(itemKey(id),MemoryAccounting.Endpoint.free(),
                    newItem?BigInteger.ZERO:groundValue(item));
            items.put(id,item);
        }
    }
    private void player(Player player, BigInteger alreadyRemoved) throws IOException {
        UUID id=player.getUniqueId();
        if(!changes.contains(playerKey(id))) {
            changes.observe(playerKey(id),MemoryAccounting.Endpoint.personal(id),
                    playerValue(player).add(alreadyRemoved));
            players.put(id,player);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void spawn(ItemSpawnEvent event) { observe(()->ground(event.getEntity(),true)); }
    @EventHandler(priority=EventPriority.LOWEST)
    public void despawn(ItemDespawnEvent event) { observe(()->ground(event.getEntity(),false)); }
    @EventHandler(priority=EventPriority.LOWEST)
    public void merge(ItemMergeEvent event) {
        observe(()->{ground(event.getEntity(),false);ground(event.getTarget(),false);});
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void damage(EntityDamageEvent event) {
        if(event.getEntity() instanceof Item item)observe(()->ground(item,false));
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void teleport(EntityTeleportEvent event) {
        if(event.getEntity() instanceof Item item)observe(()->ground(item,false));
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void removed(EntityRemoveEvent event) {
        if(!(event.getEntity() instanceof Item item))return;
        observe(()->{
            ground(item,false);
            if(event.getCause()==EntityRemoveEvent.Cause.UNLOAD)
                unloaded.put(item.getUniqueId(),groundValue(item));
        });
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void drop(PlayerDropItemEvent event) {
        observe(()->{
            var amount=value(event.getItemDrop().getItemStack());
            if(amount.signum()==0)return;
            // PlayerDropItemEvent is emitted after the initial inventory debit.
            // Earlier GUI/pickup observations retain the true first image.
            player(event.getPlayer(),amount);
            ground(event.getItemDrop(),true);
        });
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void attempt(PlayerAttemptPickupItemEvent event) {
        observe(()->{
            if(value(event.getItem().getItemStack()).signum()==0)return;
            ground(event.getItem(),false);
            player(event.getPlayer(),BigInteger.ZERO);
        });
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void pickup(EntityPickupItemEvent event) {
        if(event.getEntity() instanceof Player player) {
            observe(()->{
                if(value(event.getItem().getItemStack()).signum()==0)return;
                // The earlier Paper attempt event saw the real stack. Never
                // subtract remaining from the temporarily adjusted event stack.
                if(!changes.contains(itemKey(event.getItem().getUniqueId())))
                    throw new IOException("Monetary pickup lacks original attempt observation");
                player(player,BigInteger.ZERO);
            });
        }
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void hopperPickup(InventoryPickupItemEvent event) {
        observe(()->{
            if(value(event.getItem().getItemStack()).signum()==0)return;
            ground(event.getItem(),false);
            observeBlock.accept(event.getInventory());
        });
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void death(PlayerDeathEvent event) {
        observe(()->player(event.getEntity(),BigInteger.ZERO));
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void click(InventoryClickEvent event) {
        if(event.getWhoClicked() instanceof Player player)observe(()->{
            player(player,BigInteger.ZERO);observeBlock.accept(event.getView().getTopInventory());
        });
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void drag(InventoryDragEvent event) {
        if(event.getWhoClicked() instanceof Player player)observe(()->{
            player(player,BigInteger.ZERO);observeBlock.accept(event.getView().getTopInventory());
        });
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void close(InventoryCloseEvent event) {
        if(event.getPlayer() instanceof Player player)observe(()->{
            player(player,BigInteger.ZERO);observeBlock.accept(event.getView().getTopInventory());
        });
    }
    /** Durable grant delivery has already applied its own verified ledger leg. */
    public void excludeConfirmedGrant(UUID id,long amount) {
        changes.excludeAlreadyRecorded(playerKey(id),BigInteger.valueOf(amount));
    }
    private void clear(){changes.clear();players.clear();items.clear();unloaded.clear();}
    public void flush(){
        if(!ready.getAsBoolean()){clear();return;}
        try{
            for(var e:players.entrySet()){
                var player=e.getValue();
                if(player.isBanned()||(player.getAddress()!=null&&player.getAddress().getAddress()!=null
                        &&Bukkit.getIPBans().contains(player.getAddress().getAddress().getHostAddress())))
                    throw new IOException("Player attribution changed during local movement");
                BigInteger amount=playerValue(player);
                if(amount.signum()>0&&PaperPlayerGrantDelivery.playerdata(e.getKey())==null){
                    if(!player.isOnline())throw new IOException("Playerdata missing");
                    player.saveData();
                    if(PaperPlayerGrantDelivery.playerdata(e.getKey())==null)
                        throw new IOException("Playerdata unavailable after save");
                }
                changes.finish(playerKey(e.getKey()),MemoryAccounting.Endpoint.personal(e.getKey()),amount);
            }
            for(var e:items.entrySet()){
                var item=e.getValue();
                var amount=item.isValid()?groundValue(item):unloaded.getOrDefault(e.getKey(),BigInteger.ZERO);
                changes.finish(itemKey(e.getKey()),MemoryAccounting.Endpoint.free(),amount);
            }
            var movements=changes.movements();
            if(!movements.isEmpty())sink.accept(movements);
        }catch(IOException|RuntimeException failure){fatal.accept(failure);}
        finally{clear();}
    }
}
