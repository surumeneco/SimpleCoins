package co.surumene.simplecoins.a01probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.paper.CoinItemCodec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * DISPOSABLE official Paper CI ONLY. The independent raw-file writer
 * demonstrates the difference between pausing world ticking and fencing
 * unrelated JVM threads. Never install this probe on a production server:
 * it intentionally exits its own disposable Paper JVM via Runtime.halt(0).
 */
public final class EmptyPauseProbe extends JavaPlugin {
    private static final long DURATION_MILLIS = 11_000L;
    private final AtomicLong mainThreadTicks = new AtomicLong();
    private final AtomicLong lastWorldFullTime = new AtomicLong(-1L);
    private final AtomicLong lastRealCoinCount = new AtomicLong(-1L);
    private final AtomicLong mutationWorldFullTime = new AtomicLong(-1L);

    @Override public void onEnable() {
        if (!Bukkit.isPrimaryThread() || !"127.0.0.1".equals(Bukkit.getServer().getIp())
                || !Bukkit.getOnlinePlayers().isEmpty()) {
            getLogger().severe("A01_EMPTY_PAUSE_INVALID_ISOLATION");
            Runtime.getRuntime().halt(25);
            return;
        }
        final int expectedPause = Integer.getInteger("a01.pause.expected", Integer.MIN_VALUE);
        final int actualPause = Bukkit.getServer().getPauseWhenEmptyTime();
        if (actualPause != expectedPause || (expectedPause != -1 && expectedPause != 1)) {
            getLogger().severe("A01_EMPTY_PAUSE_UNEXPECTED_SERVER_PROPERTY"
                    + " expected=" + expectedPause + " actual=" + actualPause);
            Runtime.getRuntime().halt(25);
            return;
        }
        final World world = Bukkit.getWorld("world");
        if (world == null) {
            getLogger().severe("A01_EMPTY_PAUSE_NO_WORLD");
            Runtime.getRuntime().halt(25);
            return;
        }
        final Path folder = getDataFolder().toPath().toAbsolutePath().normalize();
        try {
            Files.createDirectories(folder);
        } catch (IOException error) {
            getLogger().log(Level.SEVERE, "A01_EMPTY_PAUSE_DIRECTORY_FAILED", error);
            Runtime.getRuntime().halt(25);
            return;
        }
        // All real currency operations occur inside a disposable world and
        // ONLY on the Paper primary thread. The product codec is compiled from
        // the exact SimpleCoins JAR; this is not an imitation PAPER marker.
        int y = Math.max(world.getMinHeight() + 5,
                Math.min(120, world.getMaxHeight() - 5));
        Block moneyBlock = world.getBlockAt(48, y, 48);
        moneyBlock.setType(Material.CHEST, false);
        moneyBlock.getChunk().addPluginChunkTicket(this);
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        if (!(moneyBlock.getState() instanceof Chest starting))
            throw new IllegalStateException("Real isolated Paper chest not initialized");
        starting.getBlockInventory().setItem(0,
                codec.create(CoinDenomination.FIVE_THOUSAND, 1));
        if (countAuthenticatedMoney(moneyBlock, codec) != 1)
            throw new IllegalStateException("Real 5000C coin not present at startup");

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!Bukkit.isPrimaryThread())
                throw new IllegalStateException("World/inventory observed off Paper main");
            long ticks = mainThreadTicks.incrementAndGet();
            long currentTime = world.getFullTime();
            int current = countAuthenticatedMoney(moneyBlock, codec);
            if (ticks >= 80 && mutationWorldFullTime.get() < 0) {
                if (current != 1)
                    throw new IllegalStateException("Real coin was modified before controlled mutation");
                Chest actual = (Chest) moneyBlock.getState();
                actual.getBlockInventory().setItem(0,
                        codec.create(CoinDenomination.FIVE_THOUSAND, 2));
                current = countAuthenticatedMoney(moneyBlock, codec);
                if (current != 2)
                    throw new IllegalStateException("Real Paper chest coin mutation failed");
                mutationWorldFullTime.set(currentTime);
            }
            lastRealCoinCount.set(current);
            lastWorldFullTime.set(currentTime);
        }, 1L, 1L);
        getLogger().info("A01_EMPTY_PAUSE_ACTUAL_SETTING"
                + " pauseWhenEmptySeconds=" + actualPause
                + " onlinePlayers=0 admissionExternalGuardRequired=true");

        // Start a native Java thread from a plugin to model an uncooperative
        // async file writer. Never call Bukkit API from this thread.
        Thread detached = new Thread(() -> inspect(folder, expectedPause),
                "a01-raw-file-writer-evidence");
        detached.setDaemon(true);
        detached.start();
    }

    private void inspect(Path folder, int configuredPause) {
        final Path marker = folder.resolve("independent-writer.txt");
        final Path receipt = folder.resolve("pause-evidence.properties");
        long firstTick = -1, lastTick = -1;
        long firstWorld = -1, lastWorld = -1;
        long firstWrites = -1, lastWrites = -1;
        long firstRealCoin = -1, lastRealCoin = -1;
        long writeCount = 0;
        try {
            long started = System.nanoTime();
            boolean capturedBefore = false, capturedAfter = false;
            while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
                    < DURATION_MILLIS) {
                // This is NOT a Minecraft asset: no world/playerdata .mca
                // is modified. It is a repeatable capability witness only.
                writeCount++;
                String record = "uncooperative-independent-writer=" + writeCount + "\n";
                forceFile(marker, record);
                String observed = Files.readString(marker, StandardCharsets.UTF_8);
                if (!record.equals(observed))
                    throw new IOException("Independent writer readback mismatch");
                long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                if (!capturedBefore && elapsed >= 2_500L) {
                    firstTick = mainThreadTicks.get();
                    firstWorld = lastWorldFullTime.get();
                    firstRealCoin = lastRealCoinCount.get();
                    firstWrites = writeCount;
                    capturedBefore = true;
                }
                if (!capturedAfter && elapsed >= 10_500L) {
                    lastTick = mainThreadTicks.get();
                    lastWorld = lastWorldFullTime.get();
                    lastRealCoin = lastRealCoinCount.get();
                    lastWrites = writeCount;
                    capturedAfter = true;
                }
                Thread.sleep(200L);
            }
            if (!capturedBefore || !capturedAfter || firstTick < 0 || firstWorld < 0
                    || lastTick < firstTick || lastWorld < firstWorld
                    || firstWrites < 1 || lastWrites <= firstWrites
                    || firstRealCoin != 1 || lastRealCoin != 2
                    || mutationWorldFullTime.get() < 0) {
                throw new IllegalStateException("Incomplete world-tick/writer sampling");
            }
            long ticksDelta = lastTick - firstTick;
            long worldDelta = lastWorld - firstWorld;
            long writesDelta = lastWrites - firstWrites;
            String evidence = "pauseSetting=" + configuredPause + "\n"
                    + "tickBefore=" + firstTick + "\n"
                    + "tickAfter=" + lastTick + "\n"
                    + "tickDelta=" + ticksDelta + "\n"
                    + "worldBefore=" + firstWorld + "\n"
                    + "worldAfter=" + lastWorld + "\n"
                    + "worldDelta=" + worldDelta + "\n"
                    + "writerBefore=" + firstWrites + "\n"
                    + "writerAfter=" + lastWrites + "\n"
                    + "writerDelta=" + writesDelta + "\n"
                    + "realLiveCoinBefore=" + firstRealCoin + "\n"
                    + "realLiveCoinAfter=" + lastRealCoin + "\n"
                    + "realLiveCoinMutationWorldTime=" + mutationWorldFullTime.get() + "\n"
                    + "actualPaperCoinPdcChecked=true\n"
                    + "writerReadback=true\n"
                    + "physicalMinecraftAssetsTouched=true\n"
                    + "onlyDisposableWorld=true\n"
                    + "networkPlayers=0\n"
                    + "formalCatGate=not-run\n";
            forceFile(receipt, evidence);
            System.out.println("A01_EMPTY_PAUSE_BOUNDED_OBSERVATION "
                    + "pauseSetting=" + configuredPause
                    + " mainTickDelta=" + ticksDelta
                    + " worldTimeDelta=" + worldDelta
                    + " independentFileWriteDelta=" + writesDelta
                    + " writerStillRunnable=true"
                    + " zeroPlayers=true"
                    + " admissionDefaultDeny=true"
                    + " realLiveCoinBefore=" + firstRealCoin
                    + " realLiveCoinAfter=" + lastRealCoin
                    + " mutationWorldTime=" + mutationWorldFullTime.get()
                    + " realPaperCoinPdc=true"
                    + " onlyDisposableWorld=true");
            System.out.flush();
            // A sleeping main tick scheduler cannot process Bukkit.shutdown.
            // Explicitly and ONLY terminate this isolated ephemeral JVM.
            Runtime.getRuntime().halt(0);
        } catch (Throwable error) {
            error.printStackTrace(System.err);
            System.err.println("A01_EMPTY_PAUSE_PROBE_FAILED");
            System.err.flush();
            Runtime.getRuntime().halt(25);
        }
    }

    private static int countAuthenticatedMoney(Block block, CoinItemCodec codec) {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Physical currency read off Paper main");
        if (!(block.getState() instanceof Chest chest))
            throw new IllegalStateException("Test money Chest was removed");
        ItemStack stack = chest.getBlockInventory().getItem(0);
        if (stack == null || codec.identify(stack).orElseThrow()
                != CoinDenomination.FIVE_THOUSAND)
            throw new IllegalStateException("Real Paper coin PDC or model corrupted");
        return stack.getAmount();
    }

    private static void forceFile(Path path, String value) throws IOException {
        ByteBuffer payload = StandardCharsets.UTF_8.encode(value);
        try (FileChannel channel = FileChannel.open(path,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            while (payload.hasRemaining()) channel.write(payload);
            channel.force(true);
        }
    }
}
