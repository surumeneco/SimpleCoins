package co.surumene.probes;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundResourcePackPushPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundResourcePackPacket;
import org.geysermc.mcprotocollib.protocol.data.game.ResourcePackStatus;
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
        boolean requirePack=Arrays.asList(args).contains("--require-pack");
        var wallet=Arrays.asList(args).contains("--wallet-scenario")?new WalletNetworkScenario():null;
        var packCount=new AtomicInteger();
        var downloaded=new AtomicBoolean();
        var packFailed=new AtomicBoolean();
        var client=ClientNetworkSessionFactory.factory()
            .setRemoteSocketAddress(new InetSocketAddress("127.0.0.1",25598))
            .setProtocol(new MinecraftProtocol("ProbeSettlement")).create();
        client.addListener(new SessionAdapter(){
            @Override public void packetReceived(Session session,Packet packet){
                if(packet instanceof ClientboundLoginPacket){joined.set(true);System.out.println("NETWORK_CLIENT_PLAY_LOGIN");}
                if(requirePack && packet instanceof ClientboundResourcePackPushPacket pack) {
                    if(!UUID.nameUUIDFromBytes("simplecoins:java-currency-pack".getBytes(StandardCharsets.UTF_8)).equals(pack.getId())) return;
                    if(packCount.incrementAndGet()!=1) {packFailed.set(true);return;}
                    try {
                        session.send(new ServerboundResourcePackPacket(pack.getId(),ResourcePackStatus.ACCEPTED));
                        verifyDownload(pack);
                        downloaded.set(true);
                        session.send(new ServerboundResourcePackPacket(pack.getId(),ResourcePackStatus.DOWNLOADED));
                        System.out.println("NETWORK_JAVA_PACK_HTTP_HASH_AND_NINE_MODELS_PASSED");
                    } catch(Exception failure) {
                        packFailed.set(true);
                        failure.printStackTrace();
                        session.send(new ServerboundResourcePackPacket(pack.getId(),ResourcePackStatus.FAILED_DOWNLOAD));
                    }
                }
            }
            @Override public void disconnected(DisconnectedEvent event){
                System.out.println("NETWORK_CLIENT_DISCONNECTED "+event.getReason());disconnected.countDown();
            }
        });
        if(wallet!=null)client.addListener(wallet);
        client.connect();
        boolean finished=disconnected.await(wallet==null?65:175,TimeUnit.SECONDS);
        System.exit(finished&&joined.get()&&(!requirePack||
                downloaded.get()&&packCount.get()==1&&!packFailed.get())&&(wallet==null||wallet.completed())?0:1);
    }
    /** Real download and model/texture verification, not a claim of graphical reload. */
    private static void verifyDownload(ClientboundResourcePackPushPacket pack) throws Exception {
        URI uri=URI.create(pack.getUrl());
        if(!uri.equals(URI.create("http://127.0.0.1:25599/SimpleCoins-java.zip")))
            throw new IllegalStateException("Unexpected fixture pack URL");
        var response=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build().send(
                HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
        byte[] bytes;
        try(var body=response.body()){bytes=body.readNBytes(2*1024*1024+1);}
        if(response.statusCode()!=200||bytes.length==0||bytes.length>2*1024*1024)
            throw new IllegalStateException("Actual pack HTTP response invalid");
        if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes)).equals(pack.getHash()))
            throw new IllegalStateException("Actual ZIP and requested SHA-1 disagree");
        Map<String,byte[]> entries=new HashMap<>();
        try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for(var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry()) {
                byte[] raw=zip.readNBytes(1024*1024+1);
                if(raw.length>1024*1024||entries.putIfAbsent(entry.getName(),raw)!=null||entries.size()>28)
                    throw new IllegalStateException("Invalid ZIP entry");
            }
        }
        if(entries.size()!=28||!entries.containsKey("pack.mcmeta"))
            throw new IllegalStateException("Unexpected pack entry count");
        Set<String> textureHashes=new HashSet<>();
        for(int amount:new int[]{1,5,10,50,100,500,1000,5000,10000}){
            String id="coin_"+amount;
            String model=new String(Objects.requireNonNull(entries.get("assets/simplecoins/models/item/"+id+".json")),StandardCharsets.UTF_8);
            String item=new String(Objects.requireNonNull(entries.get("assets/simplecoins/items/"+id+".json")),StandardCharsets.UTF_8);
            if(!model.contains("\"simplecoins:item/"+id+"\"")||!item.contains("\"simplecoins:item/"+id+"\""))
                throw new IllegalStateException("Wrong model/texture link "+id);
            byte[] png=Objects.requireNonNull(entries.get("assets/simplecoins/textures/item/"+id+".png"));
            if(ImageIO.read(new ByteArrayInputStream(png))==null||
                !textureHashes.add(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png))))
                throw new IllegalStateException("Invalid or duplicated denomination texture "+id);
        }
    }
}
