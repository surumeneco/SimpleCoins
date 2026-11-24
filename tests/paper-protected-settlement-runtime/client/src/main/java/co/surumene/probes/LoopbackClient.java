package co.surumene.probes;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;

/** Offline-mode disposable loopback client; no accounts, credentials, or production access. */
public final class LoopbackClient {
    public static void main(String[] args)throws Exception {
        var joined=new AtomicBoolean();var disconnected=new CountDownLatch(1);
        var client=ClientNetworkSessionFactory.factory()
            .setRemoteSocketAddress(new InetSocketAddress("127.0.0.1",25598))
            .setProtocol(new MinecraftProtocol("ProbeSettlement")).create();
        client.addListener(new SessionAdapter(){
            @Override public void packetReceived(Session session,Packet packet){
                if(packet instanceof ClientboundLoginPacket){joined.set(true);System.out.println("NETWORK_CLIENT_PLAY_LOGIN");}
            }
            @Override public void disconnected(DisconnectedEvent event){
                System.out.println("NETWORK_CLIENT_DISCONNECTED "+event.getReason());disconnected.countDown();
            }
        });
        client.connect();
        boolean finished=disconnected.await(65,TimeUnit.SECONDS);
        System.exit(finished&&joined.get()?0:1);
    }
}
