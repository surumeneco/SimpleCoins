package co.surumene.simplecoins.a01probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.paper.CoinItemCodec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.nio.file.attribute.FileTime;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * CI-only external writer, independent of SimpleCoins accounting.
 * Runs with no player connections and loopback-only admission default-deny.
 * The three phases run in SEPARATE real Paper server processes.
 *
 * This proves only that player-count zero is not a writer barrier. It does not
 * establish synchronized G0 snapshot or authorization to admit players.
 */
public final class ZeroPlayerWriterProbe extends JavaPlugin {
    private static final int X = 32, Y = 120, Z = 32;
    private static final int VALUE = 5000;
    private static final CoinDenomination DENOMINATION = CoinDenomination.fromValue(VALUE);

    @Override public void onEnable() {
        if (!"127.0.0.1".equals(Bukkit.getServer().getIp())
                || !Bukkit.getOnlinePlayers().isEmpty()) {
            getLogger().severe("A01_PROBE_INVALID_ISOLATION");
            Bukkit.shutdown();
            return;
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                if (!Bukkit.getOnlinePlayers().isEmpty())
                    throw new IllegalStateException("Player connected during no-player proof");
                World world = Bukkit.getWorld("world");
                if (world == null) throw new IllegalStateException("No vanilla overworld");
                String phase = System.getProperty("a01.probe.phase", "seed");
                Block block = world.getBlockAt(X, Y, Z);
                CoinItemCodec coins = new CoinItemCodec(CoinItemCodec.defaultNames());
                if ("seed".equals(phase)) {
                    block.setType(Material.CHEST, false);
                    if (!(block.getState() instanceof Chest chest))
                        throw new IllegalStateException("Chest did not materialize");
                    chest.getBlockInventory().setItem(0, coins.create(DENOMINATION, 1));
                    assertCurrency(block, coins, 1);
                    world.save();
                    getLogger().info("PAPER_A01_ZERO_PLAYER_EXTERNAL_WRITER_SEED_PASSED"
                            + " worldPath=" + world.getWorldPath());
                } else if ("mutate".equals(phase)) {
                    assertCurrency(block, coins, 1);
                    Chest chest = (Chest) block.getState();
                    chest.getBlockInventory().setItem(0, coins.create(DENOMINATION, 2));
                    assertCurrency(block, coins, 2);
                    world.save();
                    getLogger().info("PAPER_A01_ZERO_PLAYER_EXTERNAL_WRITER_MUTATED_PASSED"
                            + " worldPath=" + world.getWorldPath());
                } else if ("verify".equals(phase)) {
                    assertCurrency(block, coins, 2);
                    getLogger().info("PAPER_A01_ZERO_PLAYER_RELOAD_NEW_VALUE_PASSED"
                            + " worldPath=" + world.getWorldPath());

                    // This already-loaded genuine 2-coin Chest was recovered
                    // from the earlier JVM's persisted World. Mutate LIVE only
                    // and compare the disk witness *within this main-thread
                    // tick*, before calling shutdown or any explicit save.
                    // The subsequent normal shutdown is free to persist count 3.
                    Path saved = world.getWorldPath().resolve("region/r.0.0.mca");
                    if (!Files.isRegularFile(saved))
                        throw new IllegalStateException("Actual saved Anvil region missing: " + saved);
                    byte[] priorSha = MessageDigest.getInstance("SHA-256")
                            .digest(Files.readAllBytes(saved));
                    FileTime priorMtime = Files.getLastModifiedTime(saved);
                    Chest chest = (Chest) block.getState();
                    chest.getBlockInventory().setItem(0, coins.create(DENOMINATION, 3));
                    assertCurrency(block, coins, 3);
                    byte[] afterSha = MessageDigest.getInstance("SHA-256")
                            .digest(Files.readAllBytes(saved));
                    FileTime afterMtime = Files.getLastModifiedTime(saved);
                    if (!Arrays.equals(priorSha, afterSha) || !priorMtime.equals(afterMtime))
                        throw new IllegalStateException("Disk witness changed in same tick; "
                                + "cannot isolate an unchanged-saved versus LIVE currency state");
                    getLogger().info("PAPER_A01_LIVE_VS_SAVED_MONETARY_DIVERGENCE_PASSED"
                            + " savedCoinsAtLoad=2 liveCoinsNow=3"
                            + " diskShaUnchanged=true diskMtimeUnchanged=true"
                            + " onlinePlayers=" + Bukkit.getOnlinePlayers().size());
                } else {
                    throw new IllegalArgumentException("Unknown phase " + phase);
                }
                if (!Bukkit.getOnlinePlayers().isEmpty())
                    throw new IllegalStateException("Player count changed during probe");
                Bukkit.shutdown();
            } catch (Throwable failure) {
                getLogger().log(Level.SEVERE, "PAPER_A01_ZERO_PLAYER_PROBE_FAILED", failure);
                Bukkit.shutdown();
            }
        }, 50L);
    }

    private static void assertCurrency(Block block, CoinItemCodec codec, int expected) {
        if (!(block.getState() instanceof Chest chest))
            throw new IllegalStateException("Expected persisted chest at fixed test coordinate");
        ItemStack coin = chest.getBlockInventory().getItem(0);
        if (coin == null || coin.getType() != Material.PAPER
                || coin.getAmount() != expected
                || codec.identify(coin).orElse(null) != DENOMINATION) {
            throw new IllegalStateException("External chest contains unexpected currency: "
                    + coin + " expected=" + expected);
        }
    }
}
