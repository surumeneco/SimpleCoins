package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.GroundItemMovement;
import co.surumene.simplecoins.domain.MemoryAccounting;
import co.surumene.simplecoins.domain.WorldScope;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/** Ordinary ground-item deltas. Spawn and drop can be emitted in either order,
 * so combine their legs before applying the next tick's changes. No disk WAL. */
public final class PaperGroundItemAccounting implements Listener {
    private final WorldScope scope;
    private final BooleanSupplier ready;
    private final Consumer<List<MemoryAccounting.Movement>> sink;
    private final Consumer<Exception> fatal;
    private final List<MemoryAccounting.Movement> pending=new ArrayList<>();

    public PaperGroundItemAccounting(WorldScope scope, BooleanSupplier ready,
            Consumer<List<MemoryAccounting.Movement>> sink, Consumer<Exception> fatal) {
        this.scope=Objects.requireNonNull(scope);
        this.ready=Objects.requireNonNull(ready);
        this.sink=Objects.requireNonNull(sink);
        this.fatal=Objects.requireNonNull(fatal);
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void spawn(ItemSpawnEvent event) {
        observe(GroundItemMovement.SPAWN,event.getEntity().getWorld().getName(),null,event.getEntity().getItemStack());
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void despawn(ItemDespawnEvent event) {
        observe(GroundItemMovement.DESPAWN,event.getEntity().getWorld().getName(),null,event.getEntity().getItemStack());
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void drop(PlayerDropItemEvent event) {
        observe(GroundItemMovement.PLAYER_DROP,event.getItemDrop().getWorld().getName(),
                event.getPlayer(),event.getItemDrop().getItemStack());
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void pickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack collected=event.getItem().getItemStack().clone();
        int count=collected.getAmount()-event.getRemaining();
        if(count<=0)return;
        collected.setAmount(count);
        observe(GroundItemMovement.PLAYER_PICKUP,event.getItem().getWorld().getName(),player,collected);
    }

    private void observe(GroundItemMovement event,String world,Player player,ItemStack stack) {
        if(!ready.getAsBoolean())return;
        try {
            if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Ground event off main thread");
            var delta=event.delta(scope.includes(world),true);
            if(delta.free()==0&&delta.personal()==0)return;
            BigInteger value=PaperLiveItemValue.stack(stack,16*1024*1024,64*1024*1024);
            if(value.signum()==0)return;
            var external=MemoryAccounting.Endpoint.external();
            var free=MemoryAccounting.Endpoint.free();
            MemoryAccounting.Endpoint personal=null;
            if(delta.personal()!=0){
                if(player==null||player.isBanned()||(player.getAddress()!=null&&player.getAddress().getAddress()!=null
                        &&Bukkit.getIPBans().contains(player.getAddress().getAddress().getHostAddress())))
                    throw new IOException("Player attribution changed during ground movement");
                if(PaperPlayerGrantDelivery.playerdata(player.getUniqueId())==null)player.saveData();
                if(PaperPlayerGrantDelivery.playerdata(player.getUniqueId())==null)
                    throw new IOException("Playerdata unavailable for ground movement");
                personal=MemoryAccounting.Endpoint.personal(player.getUniqueId());
            }
            if(delta.free()<0&&delta.personal()>0){
                pending.add(new MemoryAccounting.Movement(free,personal,value));
            }else{
                if(delta.free()!=0)pending.add(new MemoryAccounting.Movement(delta.free()<0?free:external,
                        delta.free()<0?external:free,value));
                if(delta.personal()!=0)pending.add(new MemoryAccounting.Movement(delta.personal()<0?personal:external,
                        delta.personal()<0?external:personal,value));
            }
        }catch(IOException|RuntimeException failure){pending.clear();fatal.accept(failure);}
    }

    public void flush(){
        if(!ready.getAsBoolean()){pending.clear();return;}
        if(pending.isEmpty())return;
        var movements=List.copyOf(pending);
        pending.clear();
        try{sink.accept(movements);}catch(RuntimeException failure){fatal.accept(failure);}
    }
}
