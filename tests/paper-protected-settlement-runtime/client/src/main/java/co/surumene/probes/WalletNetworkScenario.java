package co.surumene.probes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ShiftClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;

/** Actual protocol-level wallet use; only in the disposable product fixture. */
final class WalletNetworkScenario extends SessionAdapter {
    private volatile int container=-1;
    private volatile int stateId;
    private volatile ItemStack[] items;
    private volatile boolean completed;
    private boolean loaded;
    boolean completed(){return completed;}

    @Override public void packetReceived(Session session,Packet packet) {
        if(packet instanceof ClientboundLoginPacket) {
            Thread.ofVirtual().start(()->run(session));
        } else if(packet instanceof ClientboundPlayerPositionPacket pos) {
            var xyz=pos.getPosition();
            session.send(new ServerboundAcceptTeleportationPacket(pos.getId(),xyz.getX(),xyz.getY(),xyz.getZ(),pos.getYRot(),pos.getXRot()));
            if(!loaded){loaded=true;session.send(ServerboundPlayerLoadedPacket.INSTANCE);}
        } else if(packet instanceof ClientboundOpenScreenPacket screen) {
            if(screen.getType()==ContainerType.GENERIC_9X1
                    &&screen.getTitle() instanceof net.kyori.adventure.text.TextComponent title
                    &&title.content().equals("Wallet")){
                container=screen.getContainerId();
                System.out.println("NETWORK_WALLET_SCREEN_OPENED "+container);
            }
        } else if(packet instanceof ClientboundContainerSetContentPacket full&&full.getContainerId()==container) {
            stateId=full.getStateId();
            items=full.getItems().clone();
        } else if(packet instanceof ClientboundContainerSetSlotPacket slot&&slot.getContainerId()==container) {
            stateId=slot.getStateId();
            ItemStack[] old=items;
            if(old!=null&&slot.getSlot()>=0&&slot.getSlot()<old.length){
                ItemStack[] next=old.clone();next[slot.getSlot()]=slot.getItem();items=next;
            }
        }
    }

    private static void await(BooleanSupplier condition,String observation,int seconds)throws Exception{
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);
        while(!condition.getAsBoolean()){
            if(System.nanoTime()>until)throw new IllegalStateException("Missing "+observation);
            Thread.sleep(25);
        }
    }
    private static void signal(String name,int seconds)throws Exception{
        await(()->Files.exists(Path.of(name)),name,seconds);
    }
    private int count(int slot){
        ItemStack[] snapshot=items;
        if(snapshot==null||snapshot.length!=45)return -1;
        ItemStack item=snapshot[slot];
        return item==null?0:item.getAmount();
    }
    private void shift(Session session,int slot){
        // Server processes the click from actual container state; do not claim
        // a fabricated changed-slot hash or a client-side balance.
        session.send(new ServerboundContainerClickPacket(container,stateId,slot,
                ContainerActionType.SHIFT_CLICK_ITEM,ShiftClickItemAction.LEFT_CLICK,null,Map.of()));
    }
    private void run(Session session){
        try{
            signal("product-wallet-ready",50);
            session.send(new ServerboundSetCarriedItemPacket(0));
            Thread.sleep(150);
            session.send(new ServerboundUseItemPacket(Hand.MAIN_HAND,1,0,0));
            await(()->container>0&&items!=null&&items.length==45,"real 9x1 wallet screen",12);
            ItemStack[] initial=items;
            for(int slot=0;slot<9;slot++)
                if(initial[slot]==null||initial[slot].getAmount()!=1)
                    throw new IllegalStateException("Missing wallet denomination icon "+slot);
            if(count(36)!=1||count(37)!=2||initial[4].getId()!=initial[37].getId())
                throw new IllegalStateException("Unexpected actual wallet input slots");
            shift(session,37);
            await(()->count(37)==0&&count(4)==2,"200C deposit screen update",12);
            System.out.println("NETWORK_WALLET_DEPOSIT_PACKET_OBSERVED");
            signal("product-wallet-withdraw",20);
            shift(session,3);
            await(()->count(37)==4&&count(3)==1,"four 50C coins withdrawn",12);
            System.out.println("NETWORK_WALLET_WITHDRAW_PACKET_OBSERVED");
            signal("product-wallet-redeposit",20);
            shift(session,37);
            await(()->count(37)==0&&count(3)==4,"200C redeposit screen update",12);
            session.send(new ServerboundContainerClosePacket(container));
            completed=true;
            System.out.println("NETWORK_WALLET_REDEPOSIT_AND_CLOSE_PASSED");
        }catch(Exception failure){
            failure.printStackTrace();
        }
    }
}
