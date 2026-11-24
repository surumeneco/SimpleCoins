package co.surumene.simplecoins.a01probe;

import co.surumene.simplecoins.domain.*;
import co.surumene.simplecoins.paper.CoinItemCodec;
import co.surumene.simplecoins.paper.PaperProtectedSettlementService;
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
                task.cancel(); getLogger().info("PAPER_A01_RUNTIME_SETTLEMENT_HANDOFF_PASSED"); Bukkit.shutdown();
            } catch(Throwable failure) { task.cancel(); fail(failure); }
        },1L,1L);
    }
    private static SettlementWriterEpochDecision.Evidence ready() {return new SettlementWriterEpochDecision.Evidence(true,true,true,true,true,true,true,false);}
    private void fail(Throwable failure) {getLogger().log(Level.SEVERE,"PAPER_A01_RUNTIME_SETTLEMENT_FAILED",failure); Bukkit.shutdown();}
    @Override public void onDisable() {
        if(service!=null) service.close();
        if(journal!=null) try {journal.close();} catch(java.io.IOException failure){getLogger().log(Level.SEVERE,"Fixture journal close failed",failure);}
    }
}
