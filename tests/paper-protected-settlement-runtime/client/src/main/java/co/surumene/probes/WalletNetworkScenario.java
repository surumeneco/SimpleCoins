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
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
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
    private volatile ContainerType expectedScreen=ContainerType.GENERIC_9X1;
    private volatile int stateId;
    private volatile ItemStack[] items;
    private volatile boolean completed;
    private boolean loaded;
    private volatile boolean dead;
    boolean completed(){return completed;}

    @Override public void packetReceived(Session session,Packet packet) {
        if(packet instanceof ClientboundLoginPacket) {
            Thread.ofVirtual().start(()->run(session));
        } else if(packet instanceof ClientboundPlayerPositionPacket pos) {
            session.send(new ServerboundAcceptTeleportationPacket(pos.getId()));
            if(!loaded){loaded=true;session.send(ServerboundPlayerLoadedPacket.INSTANCE);}
        } else if(packet instanceof ClientboundPlayerCombatKillPacket) {
            dead=true;
            System.out.println("NETWORK_PLAYER_DEATH_OBSERVED");
        } else if(packet instanceof ClientboundOpenScreenPacket screen) {
            System.out.println("NETWORK_SCREEN "+screen.getType()+" "+screen.getTitle());
            if(screen.getType()==expectedScreen
                    &&(expectedScreen!=ContainerType.GENERIC_9X1
                    ||(screen.getTitle() instanceof net.kyori.adventure.text.TextComponent title
                    &&title.content().equals("Wallet")))){
                container=screen.getContainerId();
                System.out.println("NETWORK_WALLET_SCREEN_OPENED "+container);
            }
        } else if(packet instanceof ClientboundContainerSetContentPacket full&&full.getContainerId()==container) {
            stateId=full.getStateId();
            items=full.getItems().clone();
            System.out.println("NETWORK_WALLET_CONTENT size="+items.length);
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
        if(snapshot==null||slot<0||slot>=snapshot.length)return -1;
        ItemStack item=snapshot[slot];
        return item==null?0:item.getAmount();
    }
    private void shift(Session session,int slot){
        // Server processes the click from actual container state; do not claim
        // a fabricated changed-slot hash or a client-side balance.
        session.send(new ServerboundContainerClickPacket(container,stateId,slot,
                ContainerActionType.SHIFT_CLICK_ITEM,ShiftClickItemAction.LEFT_CLICK,null,Map.of()));
    }
    private void openBlock(Session session,ContainerType type,int x,int size,int sequence)throws Exception{
        expectedScreen=type;
        items=null;container=-1;
        session.send(new ServerboundSetCarriedItemPacket(1));
        Thread.sleep(150);
        session.send(new ServerboundUseItemOnPacket(Vector3i.from(x,180,1),
                Direction.NORTH,Hand.MAIN_HAND,0.5f,0.5f,0f,false,false,sequence));
        await(()->container>0&&items!=null&&items.length==size,"real "+type+" screen",12);
    }
    private void click(Session session,int slot){
        session.send(new ServerboundContainerClickPacket(container,stateId,slot,
                ContainerActionType.CLICK_ITEM,ClickItemAction.LEFT_CLICK,null,Map.of()));
    }
    private int total(int from,int end){
        int sum=0;
        for(int i=from;i<end;i++){int n=count(i);if(n<0)return -1;sum+=n;}
        return sum;
    }
    private void run(Session session){
        try{
            signal("product-wallet-ready",50);
            session.send(new ServerboundSetCarriedItemPacket(0));
            Thread.sleep(150);
            session.send(new ServerboundUseItemPacket(Hand.MAIN_HAND,1,0,-90));
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
            System.out.println("NETWORK_WALLET_REDEPOSIT_AND_CLOSE_PASSED");
            signal("product-ground-drop",25);
            session.send(new ServerboundSetCarriedItemPacket(0));
            Thread.sleep(150);
            session.send(new ServerboundPlayerActionPacket(PlayerAction.DROP_ITEM_STACK,
                    Vector3i.ZERO, Direction.DOWN, 0));
            System.out.println("NETWORK_WALLET_DROP_SENT");
            await(()->dead,"real death packet",35);
            signal("product-ground-respawn",20);
            session.send(new ServerboundClientCommandPacket(ClientCommand.PERFORM_RESPAWN));
            System.out.println("NETWORK_RESPAWN_SENT");
            signal("product-storage-ready",30);
            openBlock(session,ContainerType.GENERIC_9X3,0,63,2);
            if(count(54)!=1||total(0,27)!=0||total(27,63)!=1)
                throw new IllegalStateException("Unexpected empty chest or wallet input");
            shift(session,54);
            await(()->count(54)==0&&count(0)==1&&total(27,63)==0,"wallet stored in chest",12);
            System.out.println("NETWORK_STORAGE_DEPOSIT_OBSERVED");
            signal("product-storage-withdraw",25);
            shift(session,0);
            await(()->total(0,27)==0&&total(27,63)==1,"wallet withdrawn from chest",12);
            session.send(new ServerboundContainerClosePacket(container));
            System.out.println("NETWORK_STORAGE_WITHDRAW_OBSERVED");
            signal("product-exchange-ready",25);
            openBlock(session,ContainerType.CRAFTING,2,46,3);
            int source=-1;
            for(int slot=10;slot<46;slot++)if(count(slot)==2){source=slot;break;}
            if(source<0||total(10,46)!=3)throw new IllegalStateException("Missing two input coins and wallet");
            final int input=source;
            click(session,input);
            await(()->count(input)==0,"craft input on cursor",12);
            click(session,1);
            await(()->count(1)==2&&count(0)==10,"real exchange output preview",12);
            shift(session,0);
            await(()->count(1)==0&&count(0)==0&&total(10,46)==21,"two consecutive ten-to-one exchanges",12);
            session.send(new ServerboundContainerClosePacket(container));
            System.out.println("NETWORK_EXCHANGE_SHIFT_OBSERVED");
            completed=true;
        }catch(Exception failure){
            failure.printStackTrace();
        }
    }
}
