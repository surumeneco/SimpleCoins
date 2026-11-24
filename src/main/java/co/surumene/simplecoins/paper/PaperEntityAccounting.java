package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.*;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.function.*;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** First actual image and last confirmed image of a locally touched entity.
 * A verified open WCL cargo GUI replaces only its writable stored cargo.
 * Known loaded WCL holders are also reconciled in a bounded round-robin.
 * These transient images are not a persisted balance cache. */
public final class PaperEntityAccounting implements Listener {
    private static final int COMPRESSED=16*1024*1024, INFLATED=64*1024*1024;
    private final Plugin consumer;
    private final WorldScope scope;
    private final BooleanSupplier ready;
    private final Consumer<List<MemoryAccounting.Movement>> sink;
    private final Consumer<Exception> fatal;
    private final Consumer<List<OwnerStorageIndex.Change>> ownerLocations;
    private PaperOwnerStateReconciliation.Resolver ownerResolver=PaperOwnerStateReconciliation::currentOwner;
    public void setOwnerResolver(PaperOwnerStateReconciliation.Resolver resolver){ownerResolver=Objects.requireNonNull(resolver);}
    private final Map<UUID,OwnerStorageIndex.Location> unloadedLocations=new HashMap<>();
    private final List<OwnerStorageIndex.Change> pendingLocations=new ArrayList<>();
    private final Map<UUID,Entity> touched=new HashMap<>();
    private final Map<UUID,Inventory> openCargo=new HashMap<>();
    private final Map<UUID,Map<MemoryAccounting.Endpoint,BigInteger>> before=new HashMap<>();
    private final Map<UUID,Map<MemoryAccounting.Endpoint,BigInteger>> unloaded=new HashMap<>();
    private record Tracked(Entity entity, Map<MemoryAccounting.Endpoint,BigInteger> accounted) {}
    private final LinkedHashMap<UUID,Tracked> tracked=new LinkedHashMap<>();
    private static final int WCL_READS_PER_TICK=16;

    /** Seed already-accounted live holders at the final, still-frozen startup readback. */
    public void seedLoadedWcl(Collection<PaperWonderfulLiveEntityDiscovery.Holder> holders) throws IOException {
        if(!Bukkit.isPrimaryThread()||!tracked.isEmpty()||!before.isEmpty())
            throw new IOException("Invalid WCL accounting seed boundary");
        for(var holder:holders){
            Entity entity=holder.entity();
            if(!provider(entity).filter(holder.providerId()::equals).isPresent()
                    ||!scope.includes(entity.getWorld().getName())
                    ||tracked.putIfAbsent(entity.getUniqueId(),new Tracked(entity,image(entity)))!=null)
                throw new IOException("Invalid already-accounted WCL holder "+entity.getUniqueId());
        }
    }

    public PaperEntityAccounting(Plugin consumer,WorldScope scope,BooleanSupplier ready,
            Consumer<List<MemoryAccounting.Movement>> sink,Consumer<Exception> fatal) {
        this(consumer,scope,ready,sink,fatal,ignored->{});
    }
    public PaperEntityAccounting(Plugin consumer,WorldScope scope,BooleanSupplier ready,
            Consumer<List<MemoryAccounting.Movement>> sink,Consumer<Exception> fatal,
            Consumer<List<OwnerStorageIndex.Change>> ownerLocations) {
        this.ownerLocations=Objects.requireNonNull(ownerLocations);
        this.consumer=Objects.requireNonNull(consumer);this.scope=Objects.requireNonNull(scope);
        this.ready=Objects.requireNonNull(ready);this.sink=Objects.requireNonNull(sink);
        this.fatal=Objects.requireNonNull(fatal);
    }

    private static final NamespacedKey WWC=new NamespacedKey("whatawonderfulchicken","type");
    private static final NamespacedKey WWW=new NamespacedKey("whatawonderfulwolf","type");
    private static Optional<String> provider(Entity entity) throws IOException {
        // Only the actual marked chicken/wolf is a WCL monetary holder.
        // WWC armor stand projections are handled as aliases below.
        if(!(entity instanceof Chicken)&&!(entity instanceof Wolf))return Optional.empty();
        var pdc=entity.getPersistentDataContainer();
        Set<String> keys=new HashSet<>();
        for(var key:pdc.getKeys())keys.add(key.toString());
        return WclLiveMarker.provider(entity.getType().getKey().toString(),keys,
                pdc.get(WWC,PersistentDataType.STRING),pdc.get(WWW,PersistentDataType.STRING));
    }
    private static boolean eligible(Entity entity) {
        return !(entity instanceof Player)&&!(entity instanceof Item)
                &&(entity instanceof LivingEntity||entity instanceof InventoryHolder
                ||entity instanceof ItemFrame||entity instanceof ItemDisplay);
    }
    private static MemoryAccounting.Endpoint endpoint(AssetOwner.Result owner,String location) {
        return switch(owner.kind()){
            case FREE -> MemoryAccounting.Endpoint.free();
            case PERSONAL -> MemoryAccounting.Endpoint.personal(owner.player());
            case DEAD -> MemoryAccounting.Endpoint.dead(owner.player(),owner.reason(),location);
        };
    }
    private Map<MemoryAccounting.Endpoint,BigInteger> image(Entity entity) throws IOException {
        return image(entity,null,null,null);
    }

    /** Read one actual owned holder with physical slot identities, before classification. */
    Map<String,BigInteger> readOwnerStorage(Entity entity,UUID owner,Set<UUID> wallets) throws IOException {
        var values = new HashMap<String,BigInteger>();
        image(entity,owner,values,wallets);
        return Map.copyOf(values);
    }
    private Map<MemoryAccounting.Endpoint,BigInteger> image(Entity entity,UUID requestedOwner,Map<String,BigInteger> raw,Set<UUID> sharedWallets) throws IOException {
        if(!Bukkit.isPrimaryThread())throw new IOException("Entity accounting off primary");
        if(!scope.includes(entity.getWorld().getName()))return Map.of();
        if(!entity.isValid())throw new IOException("Entity image not loaded: "+entity.getUniqueId());
        String prefix="world/"+entity.getWorld().getName()+"/";
        String path="entity/"+entity.getUniqueId()+"/";
        Map<String,BigInteger> values=new HashMap<>();
        Set<UUID> wallets=sharedWallets==null?new HashSet<>():sharedWallets;
        Map<String,ItemStack> nativeStacks=new HashMap<>();
        if(entity instanceof LivingEntity living){
            var gear=living.getEquipment();
            if(gear!=null){
                String[] names={"mainhand","offhand","head","chest","legs","feet"};
                ItemStack[] stacks={gear.getItemInMainHand(),gear.getItemInOffHand(),gear.getHelmet(),
                        gear.getChestplate(),gear.getLeggings(),gear.getBoots()};
                for(int i=0;i<names.length;i++){
                    var stack=stacks[i];
                    if(stack==null||stack.getType().isAir())continue;
                    String key=path+"equipment/"+names[i];
                    var value=PaperLiveItemValue.stack(stack,COMPRESSED,INFLATED,wallets);
                    if(value.signum()>0&&PaperWwcDisplayPhysicalAlias.verifiedPhysicalKey(
                            entity,names[i],entity.getWorld().getName())!=null)continue;
                    nativeStacks.put(key,stack);
                    values.put(key,value);
                }
            }
        }else if(entity instanceof ItemFrame frame){
            values.put(path+"display",PaperLiveItemValue.stack(frame.getItem(),COMPRESSED,INFLATED,wallets));
        }else if(entity instanceof ItemDisplay display){
            values.put(path+"display",PaperLiveItemValue.stack(display.getItemStack(),COMPRESSED,INFLATED,wallets));
        }
        if(entity instanceof InventoryHolder holder&&(PaperLoadedChunkAssets.isIndependentInventoryHolder(entity.getClass())
                ||PaperLoadedChunkAssets.isVillagerInternalInventorySource(entity.getClass()))){
            ItemStack[] contents=holder.getInventory().getContents();
            for(int i=0;i<contents.length;i++)values.put(path+"inventory/"+i,
                    PaperLiveItemValue.stack(contents[i],COMPRESSED,INFLATED,wallets));
        }
        UUID owner=entity instanceof Tameable tame&&tame.getOwner()!=null?tame.getOwner().getUniqueId():null;
        var id=provider(entity);
        if(id.isPresent()){
            var candidate=PaperWonderfulLiveCandidates.collectForHybrid(consumer,entity,id.orElseThrow(),
                    wallets,COMPRESSED,INFLATED,openCargo.get(entity.getUniqueId()));
            if(candidate.ownerEvidence().kind()==WclOwnerEvidence.Kind.HOLD)
                throw new IOException("Incomplete WCL ownership "+entity.getUniqueId());
            owner=candidate.ownerEvidence().owner();
            for(var entry:candidate.slotValues().entrySet()){
                String key=candidate.physicalSlotKeys().get(entry.getKey());
                if(nativeStacks.containsKey(key)){
                    if(!Objects.equals(values.get(key),entry.getValue())
                            ||!nativeStacks.get(key).equals(ItemStack.deserializeBytes(
                                    candidate.sourceItems().get(entry.getKey()).bytes())))
                        throw new IOException("WCL native mirror differs "+key);
                }else{
                    if(values.putIfAbsent(key,entry.getValue())!=null)
                        throw new IOException("Duplicate WCL monetary slot "+key);
                }
            }
        }
        if(raw!=null){
            if(!Objects.equals(owner,requestedOwner))throw new IOException("Indexed entity owner changed: "+entity.getUniqueId());
            values.forEach((key,value)->{if(value.signum()>0)raw.put(prefix+key,value);});
            return Map.of();
        }
        if(values.values().stream().noneMatch(v->v.signum()>0))return Map.of();
        AssetOwner.Result resolved=ownerResolver.resolve(owner,false);
        var out=new HashMap<MemoryAccounting.Endpoint,BigInteger>();
        values.forEach((key,value)->{
            if(value.signum()>0)out.merge(endpoint(resolved,prefix+key),value,BigInteger::add);
        });
        return Map.copyOf(out);
    }

    private static OwnerStorageIndex.Location location(Entity entity) {
        var pos=entity.getLocation();
        String world=entity.getWorld().getName();
        return new OwnerStorageIndex.Location(OwnerStorageIndex.Holder.entity(entity.getUniqueId()),world,
                SavedLiveSourcePartition.liveChunkSourceId(new SavedLiveSourcePartition.Chunk(world,pos.getBlockX()>>4,pos.getBlockZ()>>4)));
    }
    private void noteLocation(OwnerStorageIndex.Location location, Map<MemoryAccounting.Endpoint,BigInteger> image) throws IOException {
        UUID owner=null;
        for(var endpoint:image.keySet()) {
            if(endpoint.owner().player()==null) continue;
            if(owner!=null&&!owner.equals(endpoint.owner().player()))throw new IOException("Conflicting entity ownership");
            owner=endpoint.owner().player();
        }
        pendingLocations.add(owner==null?OwnerStorageIndex.Change.remove(location.holder()):OwnerStorageIndex.Change.replace(owner,location));
    }

    public void observeEntity(Entity entity){
        if(!ready.getAsBoolean()||!eligible(entity)||before.containsKey(entity.getUniqueId()))return;
        try {
            UUID id=entity.getUniqueId();
            Tracked prior=tracked.get(id);
            before.put(id,prior==null?image(entity):prior.accounted());touched.put(id,entity);
        }catch(IOException|RuntimeException failure){fail(failure);}
    }
    /** Recognize only the actual distributed provider holder class and entity. */
    private Entity cargoEntity(Inventory inventory) throws ReflectiveOperationException,IOException {
        if(inventory==null)return null;
        var holder=inventory.getHolder();
        if(holder==null)return null;
        String pluginName,method,expected;
        switch(holder.getClass().getName()){
            case "co.surumene.whatawonderfulchicken.gui.WonderfulChickenInventoryHolder" ->
                {pluginName="WhatAWonderfulChicken";method="chickenId";expected="wwc";}
            case "co.surumene.www.ui.WonderfulWolfInventoryHolder" ->
                {pluginName="WhatAWonderfulWolf";method="wolfId";expected="www";}
            default -> {return null;}
        }
        var plugin=Bukkit.getPluginManager().getPlugin(pluginName);
        if(plugin==null||!plugin.isEnabled()||plugin.getClass().getClassLoader()!=holder.getClass().getClassLoader()
                ||holder.getInventory()==null||holder.getInventory().getHolder()!=holder)
            throw new IOException("Unverified WCL inventory holder");
        UUID uuid=(UUID)holder.getClass().getMethod(method).invoke(holder);
        Entity entity=Bukkit.getEntity(uuid);
        if(entity==null||!provider(entity).filter(expected::equals).isPresent())
            throw new IOException("WCL inventory holder entity unavailable");
        return entity;
    }

    public void observeInventory(Inventory inventory){
        if(!ready.getAsBoolean()||inventory==null)return;
        try{
            if(inventory.getHolder() instanceof Entity entity){observeEntity(entity);return;}
            Entity entity=cargoEntity(inventory);
            if(entity!=null)observeEntity(entity);
        }catch(ReflectiveOperationException|IOException|RuntimeException failure){fail(failure);}
    }

    /** Record the stored preimage before switching to the real open GUI. */
    public void openCargo(Inventory inventory){
        if(!ready.getAsBoolean())return;
        try{
            Entity entity=cargoEntity(inventory);
            if(entity==null)return;
            UUID id=entity.getUniqueId();
            Inventory existing=openCargo.get(id);
            if(existing!=null&&existing.getHolder()==inventory.getHolder())return;
            if(existing!=null)throw new IOException("Two active WCL cargo inventories for "+id);
            observeEntity(entity);
            if(ready.getAsBoolean())openCargo.put(id,inventory.getHolder().getInventory());
        }catch(ReflectiveOperationException|IOException|RuntimeException failure){fail(failure);}
    }

    /** Called after the provider's close handler persisted or replaced cargo. */
    public void closeCargo(Inventory inventory){
        // Do not read the obsolete GUI again: WWC shulker swaps intentionally
        // skip saving the old inventory. The first image was captured at LOWEST.
        if(inventory!=null)openCargo.values().removeIf(active->active.getHolder()==inventory.getHolder());
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void open(InventoryOpenEvent event){openCargo(event.getInventory());}
    @EventHandler(priority=EventPriority.MONITOR)
    public void close(InventoryCloseEvent event){closeCargo(event.getInventory());}
    public boolean hasImage(Inventory inventory){
        return inventory!=null&&inventory.getHolder() instanceof Entity entity
                &&before.containsKey(entity.getUniqueId());
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void interact(PlayerInteractEntityEvent event){observeEntity(event.getRightClicked());}
    @EventHandler(priority=EventPriority.LOWEST)
    public void interactAt(PlayerInteractAtEntityEvent event){observeEntity(event.getRightClicked());}
    @EventHandler(priority=EventPriority.LOWEST)
    public void pickup(EntityPickupItemEvent event){observeEntity(event.getEntity());}
    @EventHandler(priority=EventPriority.LOWEST)
    public void death(EntityDeathEvent event){observeEntity(event.getEntity());}
    @EventHandler(priority=EventPriority.LOWEST)
    public void damage(EntityDamageEvent event){observeEntity(event.getEntity());}
    @EventHandler(priority=EventPriority.LOWEST)
    public void tame(EntityTameEvent event){observeEntity(event.getEntity());}
    @EventHandler(priority=EventPriority.LOWEST)
    public void teleport(EntityTeleportEvent event){observeEntity(event.getEntity());}
    @EventHandler(priority=EventPriority.LOWEST)
    public void destroy(VehicleDestroyEvent event){observeEntity(event.getVehicle());}
    @EventHandler(priority=EventPriority.LOWEST)
    public void hanging(HangingBreakEvent event){observeEntity(event.getEntity());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void spawn(CreatureSpawnEvent event){
        Entity entity=event.getEntity();
        if(!ready.getAsBoolean()||!(entity instanceof Chicken||entity instanceof Wolf)
                ||!scope.includes(entity.getWorld().getName()))return;
        UUID id=entity.getUniqueId();
        if(tracked.containsKey(id)||before.containsKey(id)){
            fail(new IOException("Duplicate new WCL candidate "+id));return;
        }
        // Actual creation is not a saved-holder load. The provider can set its marker
        // and cargo after the spawn event, so read the final image at the tick boundary.
        before.put(id,Map.of());touched.put(id,entity);
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void load(EntitiesLoadEvent event){
        if(!ready.getAsBoolean())return;
        try{
            for(var entity:event.getEntities()){
                if(!scope.includes(entity.getWorld().getName())||!eligible(entity))continue;
                boolean wcl=provider(entity).isPresent();
                if(!wcl&&!(entity instanceof Tameable tame&&tame.getOwner()!=null))continue;
                UUID id=entity.getUniqueId();
                // The saved physical holder is already in the journal. Loading is not issuance.
                // If it unloaded earlier in the same tick, preserve the original before image.
                if(unloaded.remove(id)!=null){unloadedLocations.remove(id);touched.put(id,entity);continue;}
                var actual=image(entity);
                noteLocation(location(entity),actual);
                if(wcl&&tracked.putIfAbsent(id,new Tracked(entity,actual))!=null)
                    throw new IOException("Duplicate WCL holder load "+id);
            }
        }catch(IOException|RuntimeException failure){fail(failure);}
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void unload(EntitiesUnloadEvent event){
        if(!ready.getAsBoolean())return;
        try{
            for(var e:event.getEntities()){
                UUID id=e.getUniqueId();
                if(tracked.containsKey(id))observeEntity(e);
                if(before.containsKey(id)) {
                    unloaded.put(id,image(e));unloadedLocations.put(id,location(e));
                } else if(scope.includes(e.getWorld().getName())&&eligible(e)&&e instanceof Tameable tame&&tame.getOwner()!=null) {
                    noteLocation(location(e),image(e));
                }
                openCargo.remove(id);
            }
        }catch(IOException|RuntimeException failure){fail(failure);}
    }
    void rebaseOwner(UUID owner)throws IOException {
        if(!Bukkit.isPrimaryThread()||!before.isEmpty()||!touched.isEmpty())throw new IOException("Owner rebase outside flush boundary");
        for(var entry:tracked.entrySet()){
            Tracked prior=entry.getValue();
            if(prior.accounted().keySet().stream().anyMatch(e->owner.equals(e.owner().player())))
                entry.setValue(new Tracked(prior.entity(),image(prior.entity())));
        }
    }
    public void flush(){
        if(!ready.getAsBoolean()){clear();openCargo.clear();tracked.clear();return;}
        try{
            // Poll only known loaded WCL holders, never enumerate worlds or chunks here.
            // Explicit events still take priority and use the same last-accounted preimage.
            int budget=Math.min(WCL_READS_PER_TICK,tracked.size());
            for(int i=0;i<budget;i++){
                var first=tracked.pollFirstEntry();
                UUID id=first.getKey(); Tracked prior=first.getValue();
                tracked.put(id,prior);
                if(touched.containsKey(id))continue;
                if(!prior.entity().isValid())throw new IOException("Unobserved WCL holder disappearance "+id);
                before.put(id,prior.accounted());touched.put(id,prior.entity());
            }
            List<MemoryAccounting.Movement> changes=new ArrayList<>();
            for(var entry:touched.entrySet()){
                Entity entity=entry.getValue();
                UUID id=entry.getKey();
                Map<MemoryAccounting.Endpoint,BigInteger> after=unloaded.containsKey(id)?unloaded.get(id):
                        entity.isValid()?image(entity):Map.of();
                changes.addAll(InventoryAccountingDelta.between(before.get(id),after));
                if(unloadedLocations.containsKey(id))noteLocation(unloadedLocations.get(id),after);
                else if(entity.isValid()&&scope.includes(entity.getWorld().getName()))noteLocation(location(entity),after);
                else pendingLocations.add(OwnerStorageIndex.Change.remove(OwnerStorageIndex.Holder.entity(id)));
                if(entity.isValid()&&!unloaded.containsKey(id)&&provider(entity).isPresent()){
                    tracked.remove(id);tracked.put(id,new Tracked(entity,after));
                }else{tracked.remove(id);if(!entity.isValid())openCargo.remove(id);}
            }
            if(!changes.isEmpty())sink.accept(List.copyOf(changes));
            if(!pendingLocations.isEmpty())ownerLocations.accept(List.copyOf(pendingLocations));
        }catch(IOException|RuntimeException failure){fail(failure);}
        finally{clear();}
    }
    private void clear(){before.clear();touched.clear();unloaded.clear();unloadedLocations.clear();pendingLocations.clear();}
    private void fail(Exception failure){clear();openCargo.clear();tracked.clear();fatal.accept(failure);}
}
