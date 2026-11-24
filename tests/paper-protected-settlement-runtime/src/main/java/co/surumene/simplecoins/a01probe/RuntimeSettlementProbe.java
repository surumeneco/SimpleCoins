package co.surumene.simplecoins.a01probe;

import co.surumene.simplecoins.domain.*;
import co.surumene.simplecoins.paper.CoinItemCodec;
import co.surumene.simplecoins.paper.PaperProtectedSettlementService;
import co.surumene.simplecoins.paper.PaperPlayerGrantDelivery;
import co.surumene.simplecoins.paper.PaperAdminCommand;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Disposable real Paper scheduler and save path; never arms the actual product fence. */
public final class RuntimeSettlementProbe extends JavaPlugin {
    private static final UUID ID=UUID.fromString("00000000-0000-0000-0000-000000000660");
    private NormalMutationJournal journal;
    private PaperProtectedSettlementService service;
    @Override public void onEnable() {
        if (!"127.0.0.1".equals(getServer().getIp()) || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IllegalStateException("Isolated loopback fixture required");
        Bukkit.getScheduler().runTaskLater(this,()->{
            try { runProbe(); } catch(Throwable failure) { fail(failure); }
        },120L);
    }
    private void runProbe() throws Exception {
        var product=Bukkit.getPluginManager().getPlugin("SimpleCoins");
        var field=product.getClass().getDeclaredField("publication"); field.setAccessible(true);
        if (!(field.get(product) instanceof SettlementRuntime real) || real.mayAdmit())
            throw new AssertionError("Actual product admission not disarmed");
        var serviceField=product.getClass().getDeclaredField("protectedSettlement"); serviceField.setAccessible(true);
        if (!(serviceField.get(product) instanceof PaperProtectedSettlementService actual))
            throw new AssertionError("Product does not own runtime service");
        try { actual.start(null,()->{throw new AssertionError("Disarmed source ran");},()->true,RuntimeSettlementProbe::ready,10000); throw new AssertionError("Product started unsealed"); }
        catch(IllegalStateException expected) { /* deny without changing idle gate */ }
        // Actual detached CraftPlayer.saveData(), copied to a separate fixture source universe.
        Object mc=Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
        Class<?> server=Class.forName("net.minecraft.server.MinecraftServer");
        Class<?> level=Class.forName("net.minecraft.server.level.ServerLevel");
        Class<?> profile=Class.forName("com.mojang.authlib.GameProfile");
        Class<?> info=Class.forName("net.minecraft.server.level.ClientInformation");
        Class<?> playerClass=Class.forName("net.minecraft.server.level.ServerPlayer");
        Object nativePlayer=playerClass.getConstructor(server,level,profile,info).newInstance(mc,
                server.getMethod("overworld").invoke(mc),profile.getConstructor(UUID.class,String.class).newInstance(ID,"ProbeSettlement"),
                info.getMethod("createDefault").invoke(null));
        Player player=(Player) playerClass.getMethod("getBukkitEntity").invoke(nativePlayer);
        var grantField=product.getClass().getDeclaredField("issuanceGrants"); grantField.setAccessible(true);
        var grants=(GrantLedger)grantField.get(product);
        var command=Bukkit.getPluginCommand("simplecoins");
        if(command==null||!(command.getExecutor() instanceof PaperAdminCommand))
            throw new AssertionError("Product give command is not registered");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"sc give "+ID+" 5200");
        if(Boolean.getBoolean("a02.liveGrantReload")) {
            if(grants.pending(ID).size()!=1)throw new AssertionError("Unexpected grant on reload");
            player.loadData();
            runGrantProbe(player,grants,true);
            Bukkit.shutdown();
            return;
        }
        if(!grants.historySnapshot().isEmpty())throw new AssertionError("Guarded command issued before settlement");
        player.getInventory().setItem(0,new CoinItemCodec(CoinItemCodec.defaultNames()).create(CoinDenomination.FIVE_THOUSAND,1));
        player.saveData();
        var root=getDataFolder().toPath().resolve("fixture-world");
        var target=root.resolve("playerdata/"+ID+".dat"); Files.createDirectories(target.getParent());
        Files.copy(getServer().getWorldContainer().toPath().resolve("world/players/data/"+ID+".dat"),target);
        journal=new NormalMutationJournal(getDataFolder().toPath().resolve("fixture-journal"));
        var fence=new WriterEpochFence(); fence.arm(); var seal=fence.seal(); var gate=new AdmissionBarrier();
        var runtime=new SettlementRuntime(gate,fence);
        service=new PaperProtectedSettlementService(this,runtime,journal,this::fail);
        service.start(seal,()->{
            if (Bukkit.isPrimaryThread()) throw new AssertionError("SAVED scan on primary");
            var scope=new WorldScope(true,Set.of(),Set.of());
            var manifest=WorldStorageDiscovery.discover(Map.of("fixture",root),scope,256);
            return new SettlementRuntime.Captured(SettlementScanRunner.stage(manifest,scope,Set.of(),
                    (w,pos)->{throw new java.io.IOException("Unexpected fixture tile");},(w,nbt)->Map.of(),16*1024*1024,64*1024*1024),manifest);
        },()->true,()->{
            if(!Bukkit.isPrimaryThread()) throw new AssertionError("Final handoff off primary");
            return ready(); // isolated fixture evidence, NOT full writer census
        },30000);
        if(runtime.mayAdmit()) throw new AssertionError("Premature admission");
        Bukkit.getScheduler().runTaskTimer(this,task->{
            try {
                if(!runtime.mayAdmit()) return;
                if(real.mayAdmit() || fence.state()!=WriterEpochFence.State.OPEN || !runtime.current().orElseThrow().statistics().worldTotal().equals(BigInteger.valueOf(5000)))
                    throw new AssertionError("Incorrect normal handoff");
                service.close();
                if(runtime.mayAdmit() || fence.state()!=WriterEpochFence.State.FAULTED) throw new AssertionError("Close did not revoke");
                task.cancel(); getLogger().info("PAPER_A01_RUNTIME_SETTLEMENT_HANDOFF_PASSED");
                runOwnerProbe(root,()->{
                    try{runGrantProbe(player,grants,false);runTraderProbe(product);}
                    catch(Throwable failure){fail(failure);}
                });
            } catch(Throwable failure) { task.cancel(); fail(failure); }
        },1L,1L);
    }
    /** Actual Paper profile BAN and the actual detached player's saved file.
     * Isolated runtime only: the product admission gate remains closed. */
    @SuppressWarnings({"rawtypes","unchecked","deprecation"})
    private void runOwnerProbe(java.nio.file.Path root,Runnable next)throws Exception{
        var scope=new WorldScope(true,Set.of(),Set.of());
        var coverage=co.surumene.simplecoins.paper.PaperProfileBanSource.captureCompleteStandard(Set.of("paper-profile","paper-ip"),Map.of());
        var runtime=new SettlementRuntime(new AdmissionBarrier(),new WriterEpochFence());
        var attempt=runtime.beginMemory(()->true,()->Bukkit.getOnlinePlayers().isEmpty());
        Bukkit.getScheduler().runTaskAsynchronously(this,()->{
            try{
                var manifest=WorldStorageDiscovery.discover(Map.of("fixture",root),scope,256);
                var prepared=runtime.prepareMemory(attempt,()->new SettlementRuntime.Captured(
                    SettlementScanRunner.stageWithBanCoverage(manifest,scope,coverage,(w,pos)->{throw new java.io.IOException("Unexpected tile");},
                        (w,nbt)->Map.of(),16*1024*1024,64*1024*1024),manifest,coverage));
                Bukkit.getScheduler().runTask(this,()->{
                    try{
                        runtime.completeMemory(prepared,ready());
                        if(!runtime.mayAdmit()||!runtime.current().orElseThrow().personalBalances().get(ID).equals(BigInteger.valueOf(5000)))
                            throw new AssertionError("Incorrect owner baseline");
                        var entities=new co.surumene.simplecoins.paper.PaperEntityAccounting(this,scope,runtime::memoryAccountingReady,moves->{},this::fail);
                        var refresh=new co.surumene.simplecoins.paper.PaperOwnerStateReconciliation(this,runtime,entities,()->{},this::fail);
                        var file=PaperPlayerGrantDelivery.playerdata(ID);
                        var backup=file.resolveSibling(file.getFileName()+".owner-probe-backup");
                        org.bukkit.BanList bans=Bukkit.getBanList(org.bukkit.BanList.Type.PROFILE);
                        var profile=Bukkit.createProfile(ID,"ProbeSettlement");
                        bans.addBan(profile,"Owner reconciliation fixture",(java.util.Date)null,"probe");
                        if(refresh.fresh())throw new AssertionError("Stale P exposed after actual BAN");
                        int[] stage={0},ticks={0};
                        Bukkit.getScheduler().runTaskTimer(this,task->{
                            try{
                                if(++ticks[0]>300)throw new AssertionError("Owner refresh timed out at stage "+stage[0]);
                                if(!refresh.fresh())return;
                                var result=runtime.current().orElseThrow();
                                switch(stage[0]){
                                    case 0 -> {
                                        if(result.personalBalances().containsKey(ID)||!result.dead().equals(BigInteger.valueOf(5000))||
                                                result.deadLocations().stream().noneMatch(x->x.owner().equals(ID)&&x.reason()==AssetOwner.DeadReason.CURRENT_BAN&&x.location().startsWith("playerdata/"+ID)))
                                            throw new AssertionError("Actual profile BAN did not move P to located D: "+result);
                                        Files.move(file,backup);stage[0]++;
                                    }
                                    case 1 -> {
                                        if(result.personalBalances().containsKey(ID)||result.dead().signum()!=0)throw new AssertionError("Deleted actual playerdata still counted");
                                        bans.pardon(profile);stage[0]++;
                                    }
                                    case 2 -> {
                                        if(result.personalBalances().containsKey(ID)||result.dead().signum()!=0)throw new AssertionError("Absent unbanned player registered");
                                        Files.move(backup,file);stage[0]++;
                                    }
                                    case 3 -> {
                                        if(!BigInteger.valueOf(5000).equals(result.personalBalances().get(ID))||result.dead().signum()!=0)
                                            throw new AssertionError("Restored playerdata did not return to P: "+result);
                                        task.cancel();runtime.revoke();getLogger().info("PAPER_DYNAMIC_OWNER_BAN_DELETE_RESTORE_PASSED total=5000");next.run();
                                    }
                                }
                                ticks[0]=0;
                            }catch(Throwable failure){task.cancel();try{if(Files.exists(backup)&&!Files.exists(file))Files.move(backup,file);bans.pardon(profile);}catch(Exception suppressed){failure.addSuppressed(suppressed);}fail(failure);}
                        },1L,1L);
                    }catch(Throwable failure){fail(failure);}
                });
            }catch(Throwable failure){Bukkit.getScheduler().runTask(this,()->fail(failure));}
        });
    }

    /** Actual physical inventory, real saved file and real product ledger.
     * A detached player is used instead of a networked client; no product admission bypass. */
    private void runGrantProbe(Player player,GrantLedger grants,boolean reload) throws Exception {
        var accounting=new MemoryAccounting(Map.of(ID,BigInteger.valueOf(reload?5064:5000)),
                BigInteger.ZERO,Map.of());
        var delivery=new PaperPlayerGrantDelivery(grants,new CoinItemCodec(CoinItemCodec.defaultNames()),
                (id,value)->{
                    if(!id.equals(ID)||accounting.recordNext(List.of(new MemoryAccounting.Movement(
                            MemoryAccounting.Endpoint.external(),MemoryAccounting.Endpoint.personal(id),
                            BigInteger.valueOf(value))),Set.of())!=MemoryAccounting.Outcome.APPLIED)
                        throw new AssertionError("Failed issuance accounting");
                });
        if(!reload) {
            for(int i=1;i<36;i++)player.getInventory().setItem(i,new ItemStack(Material.STONE,64));
            player.saveData();
            grants.reserveCoins(ID,CoinDenomination.ONE,65,"actual Paper probe");
            var full=delivery.deliver(player);
            if(full.deliveredNow()!=0||full.pendingCount()!=1)
                throw new AssertionError("Full storage unexpectedly issued");
            player.getInventory().setItem(1,null);
        } else {
            if(grants.pending(ID).getFirst().confirmedPartial()!=64)
                throw new AssertionError("Saved partial amount lost on restart");
            player.getInventory().setItem(2,null);
        }
        var result=delivery.deliver(player);
        long expected=reload?5065:5064;
        if(result.deliveredNow()!=(reload?1:64)||result.pendingCount()!=(reload?0:1))
            throw new AssertionError("Wrong physical batch result: "+result);
        var file=PaperPlayerGrantDelivery.playerdata(ID);
        var value=SavedPlayerAssets.extract(PlayerDataReader.read(file,16*1024*1024,64*1024*1024))
                .values().stream().reduce(BigInteger.ZERO,BigInteger::add);
        if(!value.equals(BigInteger.valueOf(expected))
                ||!accounting.snapshot().personal().get(ID).equals(value))
            throw new AssertionError("Saved coin total disagrees with accounting: "+value);
        var repeat=delivery.deliver(player);
        if(repeat.deliveredNow()!=0)throw new AssertionError("Replayed saved batch");
        if(reload) {
            if(!grants.pending(ID).isEmpty()||!grants.historySnapshot().getFirst().delivered())
                throw new AssertionError("Full grant not acknowledged");
            getLogger().info("PAPER_A02_MINIMAL_PLAYER_GRANT_RESTART_PASSED total=5065 pending=0");
        } else {
            if(grants.pending(ID).getFirst().confirmedPartial()!=64)
                throw new AssertionError("Partial floor not persisted");
            getLogger().info("PAPER_A02_MINIMAL_PLAYER_GRANT_PARTIAL_PASSED total=5064 remaining=1");
        }
    }

    private void runTraderProbe(org.bukkit.plugin.Plugin product) {
        var handlers=org.bukkit.event.HandlerList.getRegisteredListeners(product);
        if(handlers.stream().noneMatch(h->h.getListener() instanceof co.surumene.simplecoins.paper.TraderDeathIssuanceListener)
                ||handlers.stream().noneMatch(h->h.getListener() instanceof co.surumene.simplecoins.paper.PaperGroundItemAccounting))
            throw new AssertionError("Actual product trader/ground listeners not registered");
        var accounting=new MemoryAccounting(Map.of(),BigInteger.ZERO,Map.of());
        var scope=new WorldScope(true,Set.of(),Set.of());
        var ground=new co.surumene.simplecoins.paper.PaperGroundItemAccounting(scope,()->true,
                moves->{if(accounting.recordNext(moves,Set.of())!=MemoryAccounting.Outcome.APPLIED)
                    throw new AssertionError("Ground movement rejected");},this::fail);
        var codec=new CoinItemCodec(CoinItemCodec.defaultNames());
        var policy=new WanderingTraderDropPolicy(true,1,1,1,Map.of(CoinDenomination.HUNDRED,1));
        var trader=new co.surumene.simplecoins.paper.TraderDeathIssuanceListener(
                new co.surumene.simplecoins.paper.WanderingTraderDropConfigReader.Loaded(policy,1),
                codec,()->true,new java.util.Random(42),this::fail);
        Bukkit.getPluginManager().registerEvents(ground,this);
        Bukkit.getPluginManager().registerEvents(trader,this);
        var world=Bukkit.getWorlds().getFirst();
        var pos=world.getSpawnLocation().add(0,4,0);
        // No real player is connected: retain this chunk until the physical
        // readback, otherwise Paper may unload it immediately after spawning.
        var chunk=pos.getChunk();
        chunk.addPluginChunkTicket(this);
        world.spawn(pos,org.bukkit.entity.WanderingTrader.class).setHealth(0.0);
        Bukkit.getScheduler().runTaskLater(this,()->{
            try{
                ground.flush();
                BigInteger observed=BigInteger.ZERO;
                for(var item:world.getEntitiesByClass(org.bukkit.entity.Item.class)){
                    var denomination=codec.identify(item.getItemStack());
                    if(denomination.isPresent())observed=observed.add(BigInteger.valueOf(
                            (long)denomination.orElseThrow().value()*item.getItemStack().getAmount()));
                }
                if(!observed.equals(BigInteger.valueOf(100))||!accounting.snapshot().free().equals(observed))
                    throw new AssertionError("Real trader spawn mismatch: "+observed+" / "+accounting.snapshot().free());
                getLogger().info("PAPER_NORMAL_TRADER_SPAWN_PASSED total=100");
                chunk.removePluginChunkTicket(this);
                Bukkit.shutdown();
            }catch(Throwable failure){fail(failure);}
        },2L);
    }

    private static SettlementWriterEpochDecision.Evidence ready() {return new SettlementWriterEpochDecision.Evidence(true,true,true,true,true,true,true,false);}
    private void fail(Throwable failure) {getLogger().log(Level.SEVERE,"PAPER_A01_RUNTIME_SETTLEMENT_FAILED",failure); Bukkit.shutdown();}
    @Override public void onDisable() {
        if(service!=null) service.close();
        if(journal!=null) try {journal.close();} catch(java.io.IOException failure){getLogger().log(Level.SEVERE,"Fixture journal close failed",failure);}
    }
}
