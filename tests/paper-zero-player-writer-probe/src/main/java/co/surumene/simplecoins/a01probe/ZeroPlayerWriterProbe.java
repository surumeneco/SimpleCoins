package co.surumene.simplecoins.a01probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.paper.CoinItemCodec;
import co.surumene.simplecoins.paper.PaperCapturedHybridSettlement;
import co.surumene.simplecoins.paper.PaperLiveUniverseAssets;
import co.surumene.simplecoins.paper.PaperSavedSourceDiscovery;
import co.surumene.simplecoins.domain.HybridLiveSourceStage;
import co.surumene.simplecoins.domain.SettlementAccumulator;
import co.surumene.simplecoins.domain.BlockEntitySettlementImport;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.util.Map;
import java.util.Set;
import java.math.BigInteger;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.nio.file.attribute.FileTime;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
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
        if ("registry-veto".equals(System.getProperty("a01.probe.phase", "seed"))) {
            proveRealPaperWorldRegistrationInvalidation();
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

                    // Acquire the actual product live monetary source capture,
                    // not a hand-made report. Paper's main thread still owns
                    // all Block/Inventory calls and no player can connect.
                    var budget = new PaperLiveUniverseAssets.Budget(
                            2, 1024, 16384, 32768, 300000, 16,
                            16 * 1024 * 1024, 64 * 1024 * 1024);
                    var beforeCapture = PaperCapturedHybridSettlement.capture(
                            List.of(world), budget);
                    if (!BigInteger.valueOf(10_000).equals(
                            beforeCapture.observed().report().total()))
                        throw new IllegalStateException(
                                "Actual Paper live source did not observe 10000 before mutation");
                    String physical = "world/" + world.getName() + "/"
                            + X + "," + Y + "," + Z + ":0";
                    if (!containsCurrencySlot(beforeCapture, physical,
                            BigInteger.valueOf(10_000)))
                        throw new IllegalStateException(
                                "First Paper capture omitted the real monetary Chest slot");

                    Chest chest = (Chest) block.getState();
                    chest.getBlockInventory().setItem(0, coins.create(DENOMINATION, 3));
                    assertCurrency(block, coins, 3);
                    byte[] afterSha = MessageDigest.getInstance("SHA-256")
                            .digest(Files.readAllBytes(saved));
                    FileTime afterMtime = Files.getLastModifiedTime(saved);
                    var afterCapture = PaperCapturedHybridSettlement.capture(
                            List.of(world), budget);
                    if (!BigInteger.valueOf(15_000).equals(
                            afterCapture.observed().report().total())
                            || !containsCurrencySlot(afterCapture, physical,
                                    BigInteger.valueOf(15_000)))
                        throw new IllegalStateException(
                                "Fresh Paper live source capture failed to detect 15000");
                    if (!BigInteger.valueOf(10_000).equals(
                            beforeCapture.observed().report().total())
                            || !containsCurrencySlot(beforeCapture, physical,
                                    BigInteger.valueOf(10_000)))
                        throw new IllegalStateException(
                                "Earlier captured LIVE source unexpectedly changed with physical Chest");
                    if (!Arrays.equals(priorSha, afterSha) || !priorMtime.equals(afterMtime))
                        throw new IllegalStateException("Disk witness changed in same tick; "
                                + "cannot isolate an unchanged-saved versus LIVE currency state");

                    // Exercise real product hybrid staging, NOT a manually
                    // constructed empty manifest. The old 10000 LIVE capture
                    // is now stale (real Chest = 15000) but both Paper saved
                    // MCA content and the discovered source set are unchanged.
                    Path dimension = world.getWorldPath().toAbsolutePath().normalize();
                    Path levelRoot = dimension.getParent().getParent().getParent();
                    var scope = new WorldScope(true, Set.of(), Set.of());
                    var realManifest = WorldStorageDiscovery.discover(
                            Map.of(world.getName(), dimension), scope, 100000,
                            Set.of(levelRoot));
                    if (realManifest.worldFiles().isEmpty())
                        throw new IllegalStateException("Hybrid test lacked a real Paper region");
                    // The CI environment deliberately installs ONLY
                    // SimpleCoins, its all-player admission guard, and this
                    // probe. Verify that no external ownership/protection
                    // plugin exists before assigning all fixture containers
                    // to the free world owner (not unresolved/null evidence).
                    Set<String> installed = Arrays.stream(
                            Bukkit.getPluginManager().getPlugins())
                            .map(plugin -> plugin.getName()).collect(
                                    java.util.stream.Collectors.toSet());
                    if (!installed.equals(Set.of("SimpleCoins",
                            "SimpleCoinsGuard", "SimpleCoinsZeroPlayerWriterProbe")))
                        throw new IllegalStateException(
                                "Unexpected protection/owner plugin in isolated Paper: " + installed);
                    var staged = beforeCapture.stageCandidate(
                            realManifest, scope, Set.of(),
                            (worldId, position) ->
                                    new BlockEntitySettlementImport.Ownership(
                                            null, false, false, false),
                            (worldId, chunk) -> Map.of(),
                            16 * 1024 * 1024, 64 * 1024 * 1024,
                            (source, value) -> new SettlementAccumulator.Asset(
                                    source.id(), value.physicalKey(), value.world(),
                                    null, false, false, false, value.amount(),
                                    "isolated-chest/" + value.physicalKey()));
                    // Commit only the disposable memory-only test accumulator;
                    // the plugin's publication/admission caches are untouched.
                    var candidate = staged.pending().commit();
                    if (!BigInteger.valueOf(10_000).equals(candidate.statistics().worldTotal())
                            || !BigInteger.valueOf(15_000).equals(
                                    afterCapture.observed().report().total()))
                        throw new IllegalStateException(
                                "Actual hybrid candidate did not retain obsolete LIVE value");
                    if (staged.savedContentWitness() == null)
                        throw new IllegalStateException("Real saved file witness missing");
                    staged.savedContentWitness().requireUnchanged();
                    if (!Arrays.equals(priorSha, MessageDigest.getInstance("SHA-256")
                            .digest(Files.readAllBytes(saved)))
                            || !priorMtime.equals(Files.getLastModifiedTime(saved)))
                        throw new IllegalStateException("Saved file changed during hybrid stage");
                    getLogger().info("PAPER_A01_REAL_HYBRID_STALE_LIVE_CANDIDATE_OBSERVED"
                            + " candidate=10000 actualLive=15000"
                            + " realSavedManifest=true savedWitnessUnchanged=true"
                            + " admissionBlocked=true");

                    getLogger().info("PAPER_A01_PRODUCT_LIVE_CAPTURE_VALUE_DRIFT_OBSERVED"
                            + " originalCapture=10000 freshCapture=15000"
                            + " immutableSourceKey=true diskShaUnchanged=true"
                            + " onlinePlayers=" + Bukkit.getOnlinePlayers().size());
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

    /**
     * Separate disposable JVM: wait until SimpleCoins has genuinely finished
     * its normal post-ServerLoad saved-source discovery, then create a REAL
     * Paper World. The product's public read-only discovery method is reached
     * through a CI-only reflection seam; nothing is exposed by production.
     */
    private void proveRealPaperWorldRegistrationInvalidation() {
        try {
            Plugin simpleCoins = Bukkit.getPluginManager().getPlugin("SimpleCoins");
            if (simpleCoins == null || !simpleCoins.isEnabled())
                throw new IllegalStateException("SimpleCoins absent in World transition fixture");
            var field = simpleCoins.getClass().getDeclaredField("sourceDiscovery");
            field.setAccessible(true);
            Object source = field.get(simpleCoins);
            if (!(source instanceof PaperSavedSourceDiscovery discovery))
                throw new IllegalStateException("Product source discovery unavailable");
            new BukkitRunnable() {
                private int polls;
                @Override public void run() {
                    try {
                        if (!discovery.observed().isPresent()) {
                            if (++polls > 80)
                                throw new IllegalStateException(
                                        "Saved source did not reach OBSERVED before World transition");
                            return;
                        }
                        if (!Bukkit.getOnlinePlayers().isEmpty())
                            throw new IllegalStateException("Unexpected online players");
                        getLogger().info("PAPER_A01_WORLD_REGISTRY_VETO_PRECONDITION_OBSERVED");
                        World additional = Bukkit.createWorld(
                                new WorldCreator("a01-actual-dynamic-world"));
                        if (additional == null)
                            throw new IllegalStateException("Real dynamic Paper World was not loaded");
                        if (discovery.observed().isPresent() || discovery.failure().isEmpty())
                            throw new IllegalStateException(
                                    "WorldLoad after OBSERVED did not revoke source discovery");
                        getLogger().info("PAPER_A01_WORLD_REGISTRY_VETO_FAIL_STOP_PASSED"
                                + " registeredWorld=" + additional.getName());
                        cancel();
                        // The product listener already invokes shutdown.
                    } catch (Throwable failure) {
                        getLogger().log(Level.SEVERE,
                                "PAPER_A01_ZERO_PLAYER_PROBE_FAILED", failure);
                        cancel();
                        Bukkit.shutdown();
                    }
                }
            }.runTaskTimer(this, 5L, 5L);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            getLogger().log(Level.SEVERE, "PAPER_A01_ZERO_PLAYER_PROBE_FAILED", failure);
            Bukkit.shutdown();
        }
    }

    /** Compare the actual production LIVE physical slot, not merely a total. */
    private static boolean containsCurrencySlot(PaperCapturedHybridSettlement capture,
                                                String key, BigInteger value) {
        return capture.observed().sources().stream()
                .filter(source -> source instanceof HybridLiveSourceStage.ChunkSource)
                .flatMap(source -> source.values().stream())
                .anyMatch(v -> key.equals(v.physicalKey()) && value.equals(v.amount()));
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
