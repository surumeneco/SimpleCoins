package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.*;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Re-read one changed owner at a time. Status observation is separate from
 * classification of normal movement endpoints, so a partially changed owner
 * never splits the ledger between old and new P/D classes. */
public final class PaperOwnerStateReconciliation implements org.bukkit.event.Listener {
    @FunctionalInterface public interface Resolver { AssetOwner.Result resolve(UUID owner,boolean everyone)throws IOException; }
    private final JavaPlugin plugin;
    private final SettlementRuntime runtime;
    private final PaperEntityAccounting entities;
    private final PaperOwnerAssetReader reader;
    private final Runnable flush;
    private final Consumer<Exception> fatal;
    private final PaperIpBanAssociations ipAssociations;
    private boolean pending;
    private String lastFailure;
    private long retryAfter;
    public PaperOwnerStateReconciliation(JavaPlugin plugin,SettlementRuntime runtime,PaperEntityAccounting entities,Runnable flush,Consumer<Exception> fatal){
        this(plugin,runtime,entities,flush,fatal,null);
    }
    public PaperOwnerStateReconciliation(JavaPlugin plugin,SettlementRuntime runtime,PaperEntityAccounting entities,Runnable flush,Consumer<Exception> fatal,PaperIpBanAssociations ipAssociations){
        this.ipAssociations=ipAssociations;
        this.plugin=Objects.requireNonNull(plugin);this.runtime=Objects.requireNonNull(runtime);this.entities=Objects.requireNonNull(entities);
        this.reader=new PaperOwnerAssetReader(plugin,entities);this.flush=Objects.requireNonNull(flush);this.fatal=Objects.requireNonNull(fatal);
    }
    private BanSourceCoverage bans()throws IOException{
        return PaperProfileBanSource.captureCompleteStandard(Set.of("paper-profile","paper-ip"),Map.of(),ipAssociations);
    }
    private static SettlementRuntime.OwnerState state(UUID owner,BanSourceCoverage coverage)throws IOException{
        return new SettlementRuntime.OwnerState(coverage.currentlyBanned(owner),PaperPlayerGrantDelivery.playerdata(owner)!=null);
    }
    static AssetOwner.Result currentOwner(UUID owner,boolean everyone)throws IOException{
        if(owner==null||everyone)return AssetOwner.resolve(null,false,false,false);
        return state(owner,PaperProfileBanSource.captureCompleteStandard(Set.of("paper-profile","paper-ip"),Map.of())).classification(owner);
    }
    public AssetOwner.Result accountedOwner(UUID owner,boolean everyone)throws IOException{
        if(owner==null||everyone)return AssetOwner.resolve(null,false,false,false);
        var known=runtime.accountedOwnerState(owner);
        if(known==null)known=runtime.rememberOwnerState(owner,state(owner,bans()));
        return known.classification(owner);
    }
    /** Call on primary after the ordinary movement batch, or immediately before
     * a read-only command. A current status mismatch withholds statistics until
     * the complete physical replacement has been applied. */
    public boolean fresh(){
        if(!Bukkit.isPrimaryThread()||!runtime.memoryAccountingReady())return false;
        flush.run();
        if(!runtime.memoryAccountingReady()||pending)return false;
        try{
            var coverage=bans();
            for(var entry:runtime.accountedOwnerStates().entrySet()){
                UUID owner=entry.getKey();var current=state(owner,coverage);
                if(current.equals(entry.getValue()))continue;
                if(System.nanoTime()<retryAfter)return false;
                // Do not publish a dead classification for an active player;
                // Paper normally disconnects a player when a BAN is applied.
                // Until that source is settled, withhold current statistics.
                if(Bukkit.getPlayer(owner)!=null&&current.classification(owner).kind()==AssetOwner.Kind.DEAD)return false;
                var ledgerPlan=runtime.planOwnerRead(owner);var diskPlan=reader.capture(ledgerPlan);
                pending=true;
                Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                    try{
                        var disk=reader.readDisk(diskPlan);
                        Bukkit.getScheduler().runTask(plugin,()->apply(ledgerPlan,current,disk));
                    }catch(IOException|RuntimeException failure){Bukkit.getScheduler().runTask(plugin,()->{pending=false;failure(failure);});}
                });
                return false;
            }
            lastFailure=null;retryAfter=0;
            return true;
        }catch(IOException|RuntimeException failure){failure(failure);return false;}
    }
    /** A previously dead saved account must be reconciled before its owner
     * can spend the saved balance after unban or playerdata restoration.
     * A genuinely missing, unbanned new player may join and create playerdata. */
    @org.bukkit.event.EventHandler(priority=org.bukkit.event.EventPriority.HIGHEST)
    public void preLogin(org.bukkit.event.player.AsyncPlayerPreLoginEvent event){
        if(event.getLoginResult()!=org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.ALLOWED||!runtime.memoryAccountingReady())return;
        boolean allowed=false;
        try{
            if(Bukkit.isPrimaryThread())allowed=loginReady(event.getUniqueId(),event.getAddress());
            else allowed=Bukkit.getScheduler().callSyncMethod(plugin,()->loginReady(event.getUniqueId(),event.getAddress())).get(3,java.util.concurrent.TimeUnit.SECONDS);
        }catch(Exception unavailable){if(unavailable instanceof InterruptedException)Thread.currentThread().interrupt();}
        if(!allowed)event.disallow(org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                "Server accounting is updating. Please retry shortly.");
    }
    private boolean loginReady(UUID owner,java.net.InetAddress incoming)throws IOException{
        if(!runtime.memoryAccountingReady())return false;
        var current=state(owner,bans());
        if(current.banned()){
            // A former IP can still be banned after the player changes network.
            // Never make that stale address a permanent login block. The real
            // join updates it; any old dead balance is reconciled offline before play.
            if(ipAssociations!=null&&PaperProfileBanSource.observe(owner).status()!=BanEvidence.Status.BANNED
                    &&!ipAssociations.wouldBeBanned(owner,incoming,Bukkit.getIPBans()))return true;
            return false;
        }
        var accounted=runtime.accountedOwnerState(owner);
        if(accounted==null||current.equals(accounted))return true;
        fresh();return false;
    }
    @org.bukkit.event.EventHandler(priority=org.bukkit.event.EventPriority.MONITOR)
    public void joined(org.bukkit.event.player.PlayerJoinEvent event){
        if(ipAssociations==null)return;
        var player=event.getPlayer();
        try{
            var address=player.getAddress();
            if(address==null||address.getAddress()==null){
                plugin.getLogger().warning("Cannot observe a joined player's numeric IP; retaining existing association.");return;
            }
            ipAssociations.record(player.getUniqueId(),address.getAddress());
            var old=runtime.accountedOwnerState(player.getUniqueId());
            if(old!=null&&old.classification(player.getUniqueId()).kind()==AssetOwner.Kind.DEAD
                    &&!old.equals(state(player.getUniqueId(),bans()))){
                player.kick(net.kyori.adventure.text.Component.text("Server accounting is updating your assets. Please reconnect shortly."));
            }
        }catch(IOException|RuntimeException failure){fatal.accept(failure);}
    }
    private void apply(SettlementRuntime.OwnerReadPlan plan,SettlementRuntime.OwnerState expected,PaperOwnerAssetReader.Disk disk){
        try{
            flush.run();
            if(!runtime.memoryAccountingReady()||!expected.equals(state(plan.owner(),bans())))return;
            var values=reader.finish(disk);
            if(!runtime.reconcileOwner(plan,expected.banned(),expected.playerdata(),plan.locations().stream().anyMatch(l->l.holder().kind()==OwnerStorageIndex.Kind.BLOCK),values))return;
            try{entities.rebaseOwner(plan.owner());}catch(IOException|RuntimeException fatalFailure){fatal.accept(fatalFailure);}
            lastFailure=null;retryAfter=0;
        }catch(IOException|RuntimeException failure){failure(failure);}
        finally{pending=false;}
    }
    private void failure(Exception failure){
        String message=failure.getMessage();
        if(!Objects.equals(message,lastFailure))plugin.getLogger().warning("Owner accounting temporarily unavailable: "+message);
        lastFailure=message;retryAfter=System.nanoTime()+java.time.Duration.ofSeconds(5).toNanos();
    }
}
