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
    private final Map<UUID,Map<MemoryAccounting.Endpoint,BigInteger>> deadBefore=new HashMap<>();
    private SettlementRuntime accountingRuntime;
    private final Map<UUID,Item> items=new HashMap<>();
    private final Map<UUID,BigInteger> unloaded=new HashMap<>();

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

    public void setAccountingRuntime(SettlementRuntime runtime){this.accountingRuntime=Objects.requireNonNull(runtime);}
    private AssetOwner.Result deadOwner(UUID id){
        var state=accountingRuntime==null?null:accountingRuntime.accountedOwnerState(id);
        return state!=null&&state.classification(id).kind()==AssetOwner.Kind.DEAD?state.classification(id):null;
    }
    private static String playerKey(UUID id) { return "player/"+id; }
    private static String itemKey(UUID id) { return "item/"+id; }
    private static BigInteger value(ItemStack stack) throws IOException {
        return PaperLiveItemValue.stack(stack,COMPRESSED,INFLATED);
    }
    private BigInteger groundValue(Item item) throws IOException {
        return scope.includes(item.getWorld().getName()) ? value(item.getItemStack()) : BigInteger.ZERO;
    }
    private Map<String,BigInteger> playerLocations(Player player,boolean missingAllowed)throws IOException{
        if(player.isOnline())return PaperOwnerAssetReader.livePlayerLocations(player,new HashSet<>());
        var path=PaperPlayerGrantDelivery.playerdata(player.getUniqueId());
        if(path==null){if(missingAllowed)return Map.of();throw new IOException("Touched offline playerdata unavailable");}
        return PaperOwnerAssetReader.savedPlayerLocations(player.getUniqueId(),PlayerDataReader.read(path,COMPRESSED,INFLATED),new HashSet<>());
    }
    private BigInteger playerValue(Player player)throws IOException{
        return playerLocations(player,false).values().stream().reduce(BigInteger.ZERO,BigInteger::add);
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
        if(players.containsKey(id))return;
        if(deadOwner(id)!=null)deadBefore.put(id,accountingRuntime.deadPlayerAssets(id));
        else changes.observe(playerKey(id),MemoryAccounting.Endpoint.personal(id),playerValue(player).add(alreadyRemoved));
        players.put(id,player);
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
        } else observe(()->ground(event.getItem(),false));
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
    public void interact(PlayerInteractEntityEvent event){observe(()->player(event.getPlayer(),BigInteger.ZERO));}
    @EventHandler(priority=EventPriority.LOWEST)
    public void interactAt(PlayerInteractAtEntityEvent event){observe(()->player(event.getPlayer(),BigInteger.ZERO));}
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true)
    public void open(InventoryOpenEvent event){
        if(event.getPlayer() instanceof Player player)observe(()->player(player,BigInteger.ZERO));
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
    @EventHandler(priority=EventPriority.MONITOR)
    public void join(PlayerJoinEvent event){
        if(deadOwner(event.getPlayer().getUniqueId())!=null)observe(()->player(event.getPlayer(),BigInteger.ZERO));
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void quit(PlayerQuitEvent event){
        if(deadOwner(event.getPlayer().getUniqueId())!=null)observe(()->player(event.getPlayer(),BigInteger.ZERO));
    }
    /** A confirmed D grant changes the exact touched player locations, not P. */
    public boolean observeConfirmedDeadGrant(UUID id,Player player)throws IOException{
        if(deadOwner(id)==null)return false;
        if(player==null||!id.equals(player.getUniqueId()))throw new IOException("Confirmed grant player is unavailable");
        player(player,BigInteger.ZERO);return true;
    }
    /** Durable grant delivery has already applied its own verified ledger leg. */
    public void excludeConfirmedGrant(UUID id,long amount) {
        changes.excludeAlreadyRecorded(playerKey(id),BigInteger.valueOf(amount));
    }
    private void clear(){changes.clear();players.clear();deadBefore.clear();items.clear();unloaded.clear();}
    public void flush(){
        if(!ready.getAsBoolean()){clear();return;}
        try{
            var deadMovements=new ArrayList<MemoryAccounting.Movement>();
            for(var e:players.entrySet()){
                var player=e.getValue();var id=e.getKey();
                if(deadBefore.containsKey(id)){
                    var owner=deadOwner(id);if(owner==null)throw new IOException("Dead player class changed before flush");
                    Map<MemoryAccounting.Endpoint,BigInteger> after=new HashMap<>();
                    playerLocations(player,true).forEach((location,value)->after.put(MemoryAccounting.Endpoint.dead(id,owner.reason(),location),value));
                    deadMovements.addAll(InventoryAccountingDelta.between(deadBefore.get(id),after));
                    continue;
                }
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
            var movements=new ArrayList<>(changes.movements());movements.addAll(deadMovements);
            if(!movements.isEmpty())sink.accept(List.copyOf(movements));
        }catch(IOException|RuntimeException failure){fatal.accept(failure);}
        finally{clear();}
    }
}
