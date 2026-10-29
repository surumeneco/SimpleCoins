package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Main-thread delivery. Receipt and inventory are persisted together in playerdata. */
public final class OfflineGrants implements Listener {
    private final SimpleCoinsPlugin plugin;
    private final CoinItems items;
    private final GrantLedger ledger;
    private final NamespacedKey completedKey, activeKey, paidKey;
    private final Set<UUID> scheduled = new HashSet<>();
    private boolean failed;

    public OfflineGrants(SimpleCoinsPlugin plugin, CoinItems items) throws IOException {
        this.plugin=plugin; this.items=items;
        ledger=new GrantLedger(plugin.getDataFolder().toPath().resolve("issuance"));
        completedKey=new NamespacedKey(plugin,"grant_completed");
        activeKey=new NamespacedKey(plugin,"grant_active");
        paidKey=new NamespacedKey(plugin,"grant_paid");
    }
    public GrantLedger.Grant reserve(UUID player,long amount,String issuer) throws IOException {
        if(failed)throw new IOException("Issuance is stopped after a persistence failure");
        try { return ledger.reserve(player,amount,issuer); }
        catch(IOException ex){ fail(ex); throw ex; }
    }
    public void schedule(Player player) {
        if(failed || !scheduled.add(player.getUniqueId()))return;
        Bukkit.getScheduler().runTask(plugin,()->{
            scheduled.remove(player.getUniqueId());
            if(player.isOnline() && !failed)deliver(player);
        });
    }
    @EventHandler(priority=EventPriority.MONITOR) public void join(PlayerJoinEvent event){schedule(event.getPlayer());}
    @EventHandler(priority=EventPriority.MONITOR) public void close(InventoryCloseEvent event){
        if(event.getPlayer() instanceof Player player)schedule(player);
    }
    private long get(Player player,NamespacedKey key){return player.getPersistentDataContainer().getOrDefault(key,PersistentDataType.LONG,0L);}
    private void set(Player player,NamespacedKey key,long value){player.getPersistentDataContainer().set(key,PersistentDataType.LONG,value);}

    private void deliver(Player player) {
        try {
            long completed=get(player,completedKey),active=get(player,activeKey),paid=get(player,paidKey);
            ledger.validateReceipt(player.getUniqueId(),completed);
            List<GrantLedger.Grant> pending=ledger.pending(player.getUniqueId());
            if(active<0 || paid<0 || (active==0 && paid!=0))throw new IOException("Invalid partial receipt");
            if(active!=0 && pending.stream().noneMatch(g->g.sequence()==active))throw new IOException("Unknown partial grant");
            for(var grant:pending) {
                if(ledger.wasApplied(grant,completed)) {ledger.acknowledge(grant,completed);continue;}
                if(active!=0 && active!=grant.sequence())throw new IOException("Out-of-order partial receipt");
                long already=active==grant.sequence()?paid:0;
                if(already>=grant.amount())throw new IOException("Invalid partial grant amount");
                var inventory=Bukkit.createInventory(null,36);
                ItemStack[] before=player.getInventory().getStorageContents();
                ItemStack[] copy=Arrays.stream(before).map(i->i==null?null:i.clone()).toArray(ItemStack[]::new);
                inventory.setContents(copy);
                long inserted=0;
                for(var entry:CoinMoney.minimumCoins(grant.amount()-already).entrySet()) {
                    long count=entry.getValue();
                    // The simulation touches at most the player's 36 storage slots per stack.
                    while(count>0) {
                        int batch=(int)Math.min(64,count);
                        var leftovers=inventory.addItem(items.coin(entry.getKey(),batch));
                        int rejected=leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
                        inserted=Math.addExact(inserted,(batch-rejected)*(long)entry.getKey().value());
                        count-=batch;
                        if(rejected>0)break;
                    }
                }
                if(inserted==0)return; // Keep the outstanding reservation, do not drop it.
                long total=Math.addExact(already,inserted);
                player.getInventory().setStorageContents(inventory.getContents());
                set(player,completedKey,completed);
                if(total==grant.amount()) {
                    set(player,completedKey,grant.sequence()); set(player,activeKey,0);set(player,paidKey,0);
                } else {
                    set(player,activeKey,grant.sequence());set(player,paidKey,total);
                }
                player.saveData();
                verifySavedReceipt(player);
                if(total==grant.amount())ledger.acknowledge(grant,grant.sequence());
                player.sendMessage("Received "+inserted+items.currency()+" (reservation "+grant.id()+").");
                // One journal/save transaction per tick; inventories are never exposed between mutation and save.
                schedule(player);
                return;
            }
        } catch(Exception failure) {fail(failure);}
    }
    private void verifySavedReceipt(Player player) throws IOException {
        Set<Path> candidates=new HashSet<>();
        for(World world:Bukkit.getWorlds()) {
            Path root=world.getWorldFolder().toPath();
            for(String directory:List.of("players/data","playerdata")) {
                Path candidate=root.resolve(directory).resolve(player.getUniqueId()+".dat");
                if(Files.isRegularFile(candidate))candidates.add(candidate.toRealPath());
            }
        }
        if(candidates.size()!=1)throw new IOException("Cannot identify unique saved playerdata: "+player.getUniqueId());
        Map<String,Object> root;
        try(var input=new GZIPInputStream(Files.newInputStream(candidates.iterator().next()))) {
            root=NbtReader.read(input,32*1024*1024);
        }
        if(!(root.get("BukkitValues") instanceof Map<?,?> pdc))throw new IOException("Missing saved receipt PDC");
        for(NamespacedKey key:List.of(completedKey,activeKey,paidKey)) {
            Object saved=pdc.get(key.toString());
            if(!(saved instanceof Long value) || value!=get(player,key))throw new IOException("Player receipt save was not confirmed: "+key);
        }
    }
    private void fail(Exception failure) {
        failed=true;
        plugin.getLogger().log(java.util.logging.Level.SEVERE,"Issuance persistence failed; stopping to prevent duplicate delivery",failure);
        Bukkit.shutdown();
    }
}
