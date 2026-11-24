package co.surumene.simplecoins.paper;

import java.io.IOException;
import co.surumene.simplecoins.domain.InvseeSourceSelection;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** The installed InvSee++ public API exposes the real inventory of the
 * target. An open offline editor temporarily replaces only its main or ender
 * partition; the other partition is still the saved playerdata. Online mirrors
 * are never counted in addition to the actual Player. No forced saves. */
final class PaperInvseeViews {
    private static final int COMPRESSED=16*1024*1024,INFLATED=64*1024*1024;
    record Target(UUID owner,boolean main,Inventory inventory) {}
    private final Class<?> mainType,enderType;
    private final Method owner,storage,armor,extra,cursor,personal;
    private final Map<UUID,Map<Boolean,Inventory>> active=new HashMap<>();
    private PaperInvseeViews(Class<?> mainType,Class<?> enderType,Method owner,
            Method storage,Method armor,Method extra,Method cursor,Method personal){
        this.mainType=mainType;this.enderType=enderType;this.owner=owner;this.storage=storage;
        this.armor=armor;this.extra=extra;this.cursor=cursor;this.personal=personal;
    }
    @SuppressWarnings("unchecked")
    static PaperInvseeViews install(JavaPlugin plugin,PaperGroundItemAccounting ground)throws ReflectiveOperationException {
        var installed=Bukkit.getPluginManager().getPlugin("InvSeePlusPlus");
        if(installed==null||!installed.isEnabled())return new PaperInvseeViews(null,null,null,null,null,null,null,null);
        var loader=installed.getClass().getClassLoader();String prefix="com.janboerman.invsee.spigot.api.";
        Class<?> main=loader.loadClass(prefix+"MainSpectatorInventory"),ender=loader.loadClass(prefix+"EnderSpectatorInventory");
        var result=new PaperInvseeViews(main,ender,loader.loadClass(prefix+"SpectatorInventory").getMethod("getSpectatedPlayerId"),
                main.getMethod("getStorageContents"),main.getMethod("getArmourContents"),main.getMethod("getOffHandContents"),
                main.getMethod("getCursorContents"),main.getMethod("getPersonalContents"));
        Class<? extends Event> save=(Class<? extends Event>)loader.loadClass(prefix+"event.SpectatorInventorySaveEvent").asSubclass(Event.class);
        Method inventory=save.getMethod("getInventory");
        Bukkit.getPluginManager().registerEvent(save,new Listener(){},EventPriority.LOWEST,(listener,event)->{
            try{ground.observeInvseeSave((Inventory)inventory.invoke(event));}
            catch(ReflectiveOperationException|ClassCastException failure){ground.invseeFailure(failure);}
        },plugin,true);
        return result;
    }
    Target target(Inventory inventory)throws IOException{
        if(mainType==null||inventory==null||!mainType.isInstance(inventory)&&!enderType.isInstance(inventory))return null;
        try{return new Target((UUID)Objects.requireNonNull(owner.invoke(inventory)),mainType.isInstance(inventory),inventory);}
        catch(ReflectiveOperationException|RuntimeException failure){throw new IOException("InvSee target identity unavailable",failure);}
    }
    void track(Target target)throws IOException{
        var views=active.computeIfAbsent(target.owner(),ignored->new HashMap<>());
        var old=views.putIfAbsent(target.main(),target.inventory());
        if(old!=null&&old!=target.inventory()){
            // Join may replace an open view with the live mirror. Two
            // simultaneous offline views of the same partition are ambiguous.
            if(Bukkit.getPlayer(target.owner())==null&&!old.getViewers().isEmpty())
                throw new IOException("Multiple active InvSee sources for one offline partition");
            views.put(target.main(),target.inventory());
        }
    }
    Set<UUID> owners(){return Set.copyOf(active.keySet());}
    boolean has(UUID id){return active.containsKey(id);}
    void prune(){
        active.values().forEach(views->views.values().removeIf(v->v.getViewers().isEmpty()));
        active.values().removeIf(Map::isEmpty);
    }
    void clear(){active.clear();}
    private static void put(Map<String,BigInteger> out,String key,ItemStack item,Set<UUID> wallets)throws IOException{
        BigInteger value=PaperLiveItemValue.stack(item,COMPRESSED,INFLATED,wallets);
        if(value.signum()>0&&out.putIfAbsent(key,value)!=null)throw new IOException("Duplicate InvSee physical slot");
    }
    private static void slots(Map<String,BigInteger> out,String prefix,ItemStack[] items,Set<UUID> wallets)throws IOException{
        for(int i=0;i<items.length;i++)put(out,prefix+i,items[i],wallets);
    }
    /** Filter the saved mirror before valuation, including wallet IDs. */
    Map<String,BigInteger> saved(UUID id,Map<String,Object> root,Set<UUID> wallets)throws IOException{
        var views=active.get(id);
        if(views==null)return root.isEmpty()?Map.of():PaperOwnerAssetReader.savedPlayerLocations(id,root,wallets);
        Map<String,Object> nbt=new HashMap<>(root);
        if(root.isEmpty()){nbt.put("Inventory",List.of());nbt.put("EnderItems",List.of());}
        if(InvseeSourceSelection.select(false,views.containsKey(true))==InvseeSourceSelection.Source.EDITOR){nbt.put("Inventory",List.of());nbt.remove("equipment");}
        if(InvseeSourceSelection.select(false,views.containsKey(false))==InvseeSourceSelection.Source.EDITOR)nbt.put("EnderItems",List.of());
        var values=new HashMap<>(PaperOwnerAssetReader.savedPlayerLocations(id,nbt,wallets));
        String prefix="playerdata/"+id+"/";
        try{
            var main=views.get(true);
            if(main!=null){
                slots(values,prefix+"inventory:",(ItemStack[])storage.invoke(main),wallets);
                var equipment=(ItemStack[])armor.invoke(main);String[] names={"feet","legs","chest","head"};
                if(equipment.length!=names.length)throw new IOException("InvSee armor layout mismatch");
                for(int i=0;i<names.length;i++)put(values,prefix+"equipment:"+names[i],equipment[i],wallets);
                var extras=(ItemStack[])extra.invoke(main);String[] extraNames={"offhand","body","saddle"};
                if(extras.length>extraNames.length)throw new IOException("InvSee extra layout mismatch");
                for(int i=0;i<extras.length;i++)put(values,prefix+"equipment:"+extraNames[i],extras[i],wallets);
                put(values,prefix+"invsee/cursor",(ItemStack)cursor.invoke(main),wallets);
                slots(values,prefix+"invsee/transient/",(ItemStack[])personal.invoke(main),wallets);
            }
            var ender=views.get(false);
            if(ender!=null)slots(values,prefix+"ender:",ender.getStorageContents(),wallets);
        }catch(ReflectiveOperationException|RuntimeException failure){throw new IOException("Cannot read real InvSee view",failure);}
        return Map.copyOf(values);
    }
}
