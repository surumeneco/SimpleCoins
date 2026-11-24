package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.*;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.Plugin;

/** One owner's indexed physical sources. Only the disk phase runs off-primary;
 * loaded inventories and the WCL provider are observed on primary. A stale or
 * incomplete selection never supplies a replacement balance. */
final class PaperOwnerAssetReader {
    private static final int COMPRESSED=16*1024*1024, INFLATED=64*1024*1024;
    private static final Set<String> TRANSIENT_INPUTS=Set.of("CRAFTING","WORKBENCH","ANVIL",
            "SMITHING","MERCHANT","CARTOGRAPHY","GRINDSTONE","LOOM","STONECUTTER","ENCHANTING");
    record WorldSource(String name,UUID id,Path root) {}
    record Plan(UUID owner,Path playerdata,boolean online,Map<OwnerStorageIndex.Location,WorldSource> live,
                Map<OwnerStorageIndex.Location,WorldSource> saved,Map<OwnerStorageIndex.Location,Path> paths) {}
    record Disk(Plan plan,Map<Path,BasicFileAttributes> attributes,
                Map<OwnerStorageIndex.Holder,OwnerSavedStorage.Selected> selected,Map<String,Object> playerdata) {}
    private final Plugin plugin;
    private final PaperEntityAccounting entities;
    private PaperInvseeViews invsee;
    void setInvseeViews(PaperInvseeViews views){invsee=views;}
    PaperOwnerAssetReader(Plugin plugin,PaperEntityAccounting entities){this.plugin=plugin;this.entities=entities;}

    private static int[] blockCoordinates(OwnerStorageIndex.Holder holder){
        return Arrays.stream(holder.identity().split(",")).mapToInt(Integer::parseInt).toArray();
    }
    private static WorldSource world(World world){return new WorldSource(world.getName(),world.getUID(),world.getWorldPath().toAbsolutePath().normalize());}
    private static World requireWorld(WorldSource source)throws IOException {
        World world=Bukkit.getWorld(source.id());
        if(world==null||!world(world).equals(source))throw new IOException("Indexed world changed: "+source.name());
        return world;
    }
    private static boolean loaded(World world,OwnerStorageIndex.Holder holder){
        if(holder.kind()==OwnerStorageIndex.Kind.BLOCK){int[] p=blockCoordinates(holder);return world.isChunkLoaded(p[0]>>4,p[2]>>4);}
        Entity e=Bukkit.getEntity(UUID.fromString(holder.identity()));return e!=null&&e.isValid();
    }
    private static Path savedPath(OwnerStorageIndex.Location loc,WorldSource world)throws IOException {
        String storage=loc.holder().kind()==OwnerStorageIndex.Kind.BLOCK?"region":"entities";
        Path path;
        if(loc.source().startsWith("live:chunk/")){
            String[] pieces=loc.source().split("/",-1);
            if(pieces.length!=4)throw new IOException("Invalid indexed live source");
            String name=new String(Base64.getUrlDecoder().decode(pieces[1]),java.nio.charset.StandardCharsets.UTF_8);
            if(!world.name().equals(name))throw new IOException("Indexed source world mismatch");
            int x=Integer.parseInt(pieces[2]),z=Integer.parseInt(pieces[3]);
            path=world.root().resolve(storage).resolve("r."+(x>>5)+"."+(z>>5)+".mca");
        }else path=Path.of(loc.source()).toAbsolutePath().normalize();
        if(!path.startsWith(world.root())||path.getParent()==null||!storage.equals(path.getParent().getFileName().toString()))
            throw new IOException("Indexed saved source is outside selected world storage: "+path);
        return path;
    }
    Plan capture(SettlementRuntime.OwnerReadPlan owner)throws IOException {
        if(!Bukkit.isPrimaryThread())throw new IOException("Owner selection off primary");
        Map<OwnerStorageIndex.Location,WorldSource> live=new HashMap<>(),saved=new HashMap<>();
        Map<OwnerStorageIndex.Location,Path> paths=new HashMap<>();
        for(var loc:owner.locations()){
            World w=Bukkit.getWorld(loc.world());if(w==null)throw new IOException("Indexed world unavailable: "+loc.world());
            var source=world(w);
            if(loaded(w,loc.holder()))live.put(loc,source);
            else{saved.put(loc,source);paths.put(loc,savedPath(loc,source));}
        }
        return new Plan(owner.owner(),PaperPlayerGrantDelivery.playerdata(owner.owner()),Bukkit.getPlayer(owner.owner())!=null,
                Map.copyOf(live),Map.copyOf(saved),Map.copyOf(paths));
    }
    private static BasicFileAttributes stamp(Path path)throws IOException {
        return Files.readAttributes(path,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
    }
    private static void requireStamp(Path path,BasicFileAttributes before)throws IOException {
        var now=stamp(path);
        if(!now.isRegularFile()||now.size()!=before.size()||!now.lastModifiedTime().equals(before.lastModifiedTime())||!Objects.equals(now.fileKey(),before.fileKey()))
            throw new IOException("Indexed saved source changed: "+path);
    }
    Disk readDisk(Plan plan)throws IOException {
        if(Bukkit.isPrimaryThread())throw new IOException("Owner saved read on primary");
        Map<Path,BasicFileAttributes> attributes=new HashMap<>();
        Map<Path,Set<OwnerStorageIndex.Holder>> groups=new HashMap<>();
        plan.paths().forEach((loc,path)->groups.computeIfAbsent(path,ignored->new HashSet<>()).add(loc.holder()));
        Map<OwnerStorageIndex.Holder,OwnerSavedStorage.Selected> selected=new HashMap<>();
        for(var e:groups.entrySet()){
            Path path=e.getKey();attributes.put(path,stamp(path));
            for(Path side:AnvilRegionReader.externalSidecars(path))attributes.put(side,stamp(side));
            for(var holder:OwnerSavedStorage.read(path,e.getValue(),COMPRESSED,INFLATED).entrySet())
                if(selected.putIfAbsent(holder.getKey(),holder.getValue())!=null)throw new IOException("Duplicate selected holder");
        }
        Map<String,Object> playerdata=Map.of();
        if(!plan.online()&&plan.playerdata()!=null){
            attributes.put(plan.playerdata(),stamp(plan.playerdata()));
            playerdata=PlayerDataReader.read(plan.playerdata(),COMPRESSED,INFLATED);
        }
        for(var e:attributes.entrySet())requireStamp(e.getKey(),e.getValue());
        return new Disk(plan,Map.copyOf(attributes),Map.copyOf(selected),playerdata);
    }
    private static void putAll(Map<String,BigInteger> target,Map<String,BigInteger> values)throws IOException {
        for(var e:values.entrySet())if(e.getValue().signum()>0&&target.putIfAbsent(e.getKey(),e.getValue())!=null)
            throw new IOException("Duplicate owner asset slot: "+e.getKey());
    }
    /** Shared physical slot identities for full owner and ordinary dead-player accounting. */
    static Map<String,BigInteger> livePlayerLocations(org.bukkit.entity.Player player,Set<UUID> wallets)throws IOException {
        Map<String,BigInteger> values=new HashMap<>();UUID owner=player.getUniqueId();
        for(var e:PaperLivePlayerAssets.scan(player,256,COMPRESSED,INFLATED,wallets).entries())
            putAll(values,Map.of(e.physicalKey(),e.amount()));
        var view=player.getOpenInventory();String prefix="player/"+owner+"/";
        putAll(values,Map.of(prefix+"cursor",PaperLiveItemValue.stack(view.getCursor(),COMPRESSED,INFLATED,wallets)));
        if(TRANSIENT_INPUTS.contains(view.getTopInventory().getType().name()))for(int i=0;i<view.getTopInventory().getSize();i++)
            if(view.getSlotType(i)!=InventoryType.SlotType.RESULT)putAll(values,Map.of(prefix+"transient/"+i,
                    PaperLiveItemValue.stack(view.getTopInventory().getItem(i),COMPRESSED,INFLATED,wallets)));
        return Map.copyOf(values);
    }
    static Map<String,BigInteger> savedPlayerLocations(UUID owner,Map<String,Object> nbt,Set<UUID> wallets)throws IOException {
        Map<String,BigInteger> values=new HashMap<>();
        for(var e:SavedPlayerAssets.extract(nbt,wallets).entrySet())putAll(values,Map.of("playerdata/"+owner+"/"+e.getKey(),e.getValue()));
        return Map.copyOf(values);
    }
    Map<String,BigInteger> finish(Disk disk)throws IOException {
        if(!Bukkit.isPrimaryThread())throw new IOException("Owner physical read off primary");
        Plan plan=disk.plan();UUID owner=plan.owner();
        var player=Bukkit.getPlayer(owner);
        if(plan.online()!=(player!=null)||!Objects.equals(plan.playerdata(),PaperPlayerGrantDelivery.playerdata(owner)))
            throw new IOException("Player source selection changed");
        var values=new HashMap<String,BigInteger>();var wallets=new HashSet<UUID>();
        if(InvseeSourceSelection.select(player!=null,invsee!=null&&invsee.has(owner))==InvseeSourceSelection.Source.LIVE)putAll(values,livePlayerLocations(player,wallets));
        else if(invsee!=null)putAll(values,invsee.saved(owner,disk.playerdata(),wallets));
        else if(plan.playerdata()!=null)putAll(values,savedPlayerLocations(owner,disk.playerdata(),wallets));
        for(var e:plan.live().entrySet()){
            var loc=e.getKey();World w=requireWorld(e.getValue());
            if(!loaded(w,loc.holder()))throw new IOException("Indexed live source unloaded");
            if(loc.holder().kind()==OwnerStorageIndex.Kind.BLOCK){int[] p=blockCoordinates(loc.holder());putAll(values,PaperHopperAccounting.readOwnerStorage(w.getBlockAt(p[0],p[1],p[2]),owner,wallets));}
            else{
                Entity entity=Bukkit.getEntity(UUID.fromString(loc.holder().identity()));
                if(entity==null||entity.getWorld()!=w)throw new IOException("Indexed live entity moved worlds");
                putAll(values,entities.readOwnerStorage(entity,owner,wallets));
            }
        }
        for(var e:plan.saved().entrySet()){
            var loc=e.getKey();World w=requireWorld(e.getValue());var selected=disk.selected().get(loc.holder());
            if(selected==null||loaded(w,loc.holder())||w.isChunkLoaded(selected.chunkX(),selected.chunkZ()))throw new IOException("Indexed saved source is no longer authoritative");
            String prefix="world/"+w.getName()+"/";
            if(loc.holder().kind()==OwnerStorageIndex.Kind.BLOCK){
                // The last confirmed protection identity is retained by the index;
                // unloaded containers cannot change through ordinary loaded events.
                for(var entry:StoredBlockEntityAssets.extract(selected.nbt(),wallets).entrySet())putAll(values,Map.of(prefix+entry.getKey(),entry.getValue()));
            }else{
                var uuid=UUID.fromString(loc.holder().identity());
                var chunk=new AnvilRegionReader.Chunk(selected.chunkX(),selected.chunkZ(),selected.nbt());
                var source=new WorldStorageDiscovery.SourceFile(w.getName(),plan.paths().get(loc),WorldStorageDiscovery.Kind.ENTITY);
                var candidates=PaperWonderfulSavedChunkCandidates.collectForHybrid(plugin,source,w.getUID(),chunk,new HashSet<>(),wallets,COMPRESSED,INFLATED);
                UUID actual=SavedEntityOwner.extract(selected.nbt()).get(uuid.toString());
                if(!candidates.isEmpty()){
                    var candidate=candidates.getFirst();var decision=candidate.savedOwnerEvidence(true,chunk);
                    if(decision.kind()==WclOwnerEvidence.Kind.HOLD)throw new IOException("Unresolved indexed WCL owner");actual=decision.owner();
                    for(var slot:candidate.slotValues().entrySet()){
                        if(slot.getValue().signum()==0)continue;
                        boolean mirror=(candidate.providerId().equals("www")&&slot.getKey().equals("weapon"))||(candidate.providerId().equals("wwc")&&slot.getKey().equals("head"));
                        if(mirror)WclSavedEquipmentMirrorProof.prove(selected.nbt(),uuid,candidate.providerId(),slot.getKey(),candidate.sourceItems().get(slot.getKey()).bytes(),COMPRESSED,INFLATED,Bukkit.getUnsafe().getDataVersion());
                        else putAll(values,Map.of(prefix+candidate.physicalSlotKeys().get(slot.getKey()),slot.getValue()));
                    }
                }
                if(!owner.equals(actual))throw new IOException("Indexed saved entity owner changed");
                for(var entry:StoredEntityEquipmentAssets.extract(selected.nbt(),wallets).entrySet())
                    putAll(values,Map.of(prefix+"entity/"+uuid+"/equipment/"+entry.getKey().substring(uuid.toString().length()+1),entry.getValue()));
                for(var entry:StoredEntityInventoryAssets.extract(selected.nbt(),wallets).entrySet())
                    putAll(values,Map.of(prefix+"entity/"+uuid+"/inventory/"+entry.getKey().substring((uuid+":inventory:").length()),entry.getValue()));
            }
        }
        for(var e:disk.attributes().entrySet())requireStamp(e.getKey(),e.getValue());
        return Map.copyOf(values);
    }
}
