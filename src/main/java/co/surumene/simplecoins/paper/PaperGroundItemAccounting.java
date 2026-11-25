package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.*;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;
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
    private PaperInvseeViews invsee;
    private PaperOwnerStateReconciliation.Resolver ownerResolver;
    // Only actual open spectator targets, discarded when their view closes.
    private final Map<UUID,BigInteger> activeLast=new HashMap<>();
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
    public void connectInvsee(org.bukkit.plugin.java.JavaPlugin plugin,PaperOwnerStateReconciliation reconciliation)throws ReflectiveOperationException{
        ownerResolver=reconciliation::accountedOwner;
        invsee=PaperInvseeViews.install(plugin,this);
        reconciliation.setInvseeViews(invsee);
    }
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
    private Map<String,BigInteger> playerLocations(UUID id,boolean missingAllowed)throws IOException{
        Player player=Bukkit.getPlayer(id);
        if(InvseeSourceSelection.select(player!=null&&player.isOnline(),invsee!=null&&invsee.has(id))==InvseeSourceSelection.Source.LIVE)return PaperOwnerAssetReader.livePlayerLocations(player,new HashSet<>());
        var path=PaperPlayerGrantDelivery.playerdata(id);
        if(path==null&&!missingAllowed&&(invsee==null||!invsee.has(id)))throw new IOException("Touched offline playerdata unavailable");
        var root=path==null?Map.<String,Object>of():PlayerDataReader.read(path,COMPRESSED,INFLATED);
        if(invsee!=null)return invsee.saved(id,root,new HashSet<>());
        return path==null?Map.of():PaperOwnerAssetReader.savedPlayerLocations(id,root,new HashSet<>());
    }
    private BigInteger playerValue(UUID id,boolean missingAllowed)throws IOException{
        return playerLocations(id,missingAllowed).values().stream().reduce(BigInteger.ZERO,BigInteger::add);
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
        player(player.getUniqueId(),player,alreadyRemoved);
    }
    private void player(UUID id,Player player,BigInteger alreadyRemoved)throws IOException{
        if(players.containsKey(id))return;
        if(deadOwner(id)!=null)deadBefore.put(id,accountingRuntime.deadPlayerAssets(id));
        else {
            BigInteger last=activeLast.get(id);
            changes.observe(playerKey(id),MemoryAccounting.Endpoint.personal(id),last!=null?last:playerValue(id,player==null).add(alreadyRemoved));
        }
        players.put(id,player);
    }
    private void observeInvsee(Inventory inventory,boolean track)throws IOException{
        if(invsee==null)return;
        var target=invsee.target(inventory);if(target==null)return;
        var live=Bukkit.getPlayer(target.owner());
        if(live==null&&ownerResolver!=null)ownerResolver.resolve(target.owner(),false);
        player(target.owner(),live,BigInteger.ZERO);
        if(track)invsee.track(target);
    }
    /** Actual InvSee save event is fired before the ordinary main-thread save. */
    void observeInvseeSave(Inventory inventory){observe(()->observeInvsee(inventory,false));}
    void invseeFailure(Exception failure){if(ready.getAsBoolean()){clear();fatal.accept(failure);}}

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
    @EventHandler(priority=EventPriority.LOWEST)
    public void open(InventoryOpenEvent event){
        if(event.getPlayer() instanceof Player player)observe(()->player(player,BigInteger.ZERO));
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void openInvsee(InventoryOpenEvent event){observe(()->observeInvsee(event.getInventory(),true));}
    @EventHandler(priority=EventPriority.LOWEST)
    public void click(InventoryClickEvent event) {
        if(event.getWhoClicked() instanceof Player player)observe(()->{
            player(player,BigInteger.ZERO);observeInvsee(event.getView().getTopInventory(),true);observeBlock.accept(event.getView().getTopInventory());
        });
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void drag(InventoryDragEvent event) {
        if(event.getWhoClicked() instanceof Player player)observe(()->{
            player(player,BigInteger.ZERO);observeInvsee(event.getView().getTopInventory(),true);observeBlock.accept(event.getView().getTopInventory());
        });
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void close(InventoryCloseEvent event) {
        if(event.getPlayer() instanceof Player player)observe(()->{
            player(player,BigInteger.ZERO);observeInvsee(event.getView().getTopInventory(),true);observeBlock.accept(event.getView().getTopInventory());
        });
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void beforeJoin(PlayerJoinEvent event){
        if(invsee!=null&&invsee.has(event.getPlayer().getUniqueId()))observe(()->player(event.getPlayer(),BigInteger.ZERO));
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void playerCommand(PlayerCommandPreprocessEvent event){
        observeClear(event.getPlayer(),event.getMessage());
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void serverCommand(ServerCommandEvent event){
        observeClear(event.getSender(),event.getCommand());
    }
    private static boolean clearCommand(String token){
        return token.equalsIgnoreCase("clear")||token.equalsIgnoreCase("minecraft:clear");
    }
    private void observeClear(CommandSender sender,String command){
        String[] tokens=command.strip().replaceFirst("^/","").split("\\s+");
        if(clearCommand(tokens[0])){
            observe(()->{
                if(tokens.length==1){
                    if(sender instanceof Player self)player(self,BigInteger.ZERO);
                    return;
                }
                String target=tokens[1];
                if(target.startsWith("@")){
                    try {
                        for(var entity:Bukkit.selectEntities(sender,target))
                            if(entity instanceof Player selected)player(selected,BigInteger.ZERO);
                    } catch(IllegalArgumentException unresolved){
                        // A selector can contain spaces or use a command-specific context.
                        for(Player online:Bukkit.getOnlinePlayers())player(online,BigInteger.ZERO);
                    }
                } else {
                    Player selected=Bukkit.getPlayerExact(target);
                    if(selected==null)try{selected=Bukkit.getPlayer(UUID.fromString(target));}
                    catch(IllegalArgumentException notUuid){/* Invalid or absent target changes nothing. */}
                    if(selected!=null)player(selected,BigInteger.ZERO);
                }
            });
        } else if(tokens[0].equalsIgnoreCase("execute")||tokens[0].equalsIgnoreCase("minecraft:execute")){
            for(int i=1;i+1<tokens.length;i++)if(tokens[i].equalsIgnoreCase("run")&&clearCommand(tokens[i+1])){
                // Execute can change sender, world and position: do not resolve @s/@p against the wrong context.
                observe(()->{for(Player online:Bukkit.getOnlinePlayers())player(online,BigInteger.ZERO);});
                break;
            }
        }
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void swapHands(PlayerSwapHandItemsEvent event){observe(()->player(event.getPlayer(),BigInteger.ZERO));}
    @EventHandler(priority=EventPriority.MONITOR)
    public void join(PlayerJoinEvent event){
        if(deadOwner(event.getPlayer().getUniqueId())!=null)observe(()->player(event.getPlayer(),BigInteger.ZERO));
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void quit(PlayerQuitEvent event){
        if(deadOwner(event.getPlayer().getUniqueId())!=null||invsee!=null&&invsee.has(event.getPlayer().getUniqueId()))observe(()->player(event.getPlayer(),BigInteger.ZERO));
    }
    /** A confirmed D grant changes the exact touched player locations, not P. */
    public boolean observeConfirmedDeadGrant(UUID id,Player player)throws IOException{
        if(deadOwner(id)==null)return false;
        if(player==null||!id.equals(player.getUniqueId()))throw new IOException("Confirmed grant player is unavailable");
        player(player,BigInteger.ZERO);return true;
    }
    /** Durable grant delivery has already applied its own verified ledger leg. */
    public void excludeConfirmedGrant(UUID id,long amount) {
        if(activeLast.containsKey(id)&&!changes.contains(playerKey(id))){
            changes.observe(playerKey(id),MemoryAccounting.Endpoint.personal(id),activeLast.get(id));
            players.put(id,Bukkit.getPlayer(id));
        }
        changes.excludeAlreadyRecorded(playerKey(id),BigInteger.valueOf(amount));
    }
    private void clear(){changes.clear();players.clear();deadBefore.clear();items.clear();unloaded.clear();}
    public void flush(){
        if(!ready.getAsBoolean()){clear();activeLast.clear();if(invsee!=null)invsee.clear();return;}
        try{
            // Capture previously accounted open views even for direct plugin
            // changes, then switch closed views back to their actual saved NBT.
            if(invsee!=null){
                for(UUID id:invsee.owners())player(id,Bukkit.getPlayer(id),BigInteger.ZERO);
                invsee.prune();
            }
            var deadMovements=new ArrayList<MemoryAccounting.Movement>();
            for(var e:players.entrySet()){
                var player=e.getValue();var id=e.getKey();
                if(deadBefore.containsKey(id)){
                    var owner=deadOwner(id);if(owner==null)throw new IOException("Dead player class changed before flush");
                    Map<MemoryAccounting.Endpoint,BigInteger> after=new HashMap<>();
                    var locations=playerLocations(id,true);
                    locations.forEach((location,value)->after.put(MemoryAccounting.Endpoint.dead(id,owner.reason(),location),value));
                    if(invsee!=null&&invsee.has(id))activeLast.put(id,locations.values().stream().reduce(BigInteger.ZERO,BigInteger::add));else activeLast.remove(id);
                    deadMovements.addAll(InventoryAccountingDelta.between(deadBefore.get(id),after));
                    continue;
                }
                BigInteger amount=playerValue(id,player==null);
                if(amount.signum()>0&&PaperPlayerGrantDelivery.playerdata(id)==null){
                    Player live=Bukkit.getPlayer(id);
                    if(live==null||!live.isOnline())throw new IOException("Playerdata missing");
                    live.saveData();
                    if(PaperPlayerGrantDelivery.playerdata(e.getKey())==null)
                        throw new IOException("Playerdata unavailable after save");
                }
                changes.finish(playerKey(id),MemoryAccounting.Endpoint.personal(id),amount);
                if(invsee!=null&&invsee.has(id))activeLast.put(id,amount);else activeLast.remove(id);
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
