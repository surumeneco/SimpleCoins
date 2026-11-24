package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.BanEvidence;
import co.surumene.simplecoins.domain.BanSourceCoverage;
import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.HybridLiveSourceStage;
import co.surumene.simplecoins.domain.SavedLiveSourcePartition;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.BlockFace;
import org.bukkit.block.sign.Side;
import org.bukkit.plugin.java.JavaPlugin;

/** Test-only Paper 26.2 + upstream BlockLocker 1.15 live observation probe. */
public final class BlockLockerLiveProbe extends JavaPlugin {
    private static final UUID OWNER = UUID.fromString("11111111-1111-4111-8111-111111111111");

    @Override public void onEnable() {
        try {
            if (!Bukkit.isPrimaryThread()) throw new IOException("Not on main thread");
            var settings = PaperBlockLockerSettings.capture();
            if (!settings.containers().containsAll(Set.of("minecraft:chest", "minecraft:barrel", "minecraft:hopper", "minecraft:white_shulker_box"))
                    || !settings.privateHeaders().contains("[Private]")
                    || !settings.moreHeaders().contains("[More Users]")
                    || !settings.everyoneAliases().contains("Everyone"))
                throw new IOException("Installed BlockLocker configuration was not captured");
            getLogger().info("PAPER_A04_RUNTIME_BLOCKLOCKER_SETTINGS_PASSED");
            World world = Bukkit.getWorlds().getFirst();
            if ("reload".equals(System.getProperty("a04.probe.phase", "create"))) {
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    try {
                        verifyRestartedOwnerEvidence(world);
                        getLogger().info("PAPER_BLOCKLOCKER_115_RELOADED_PROTECTION_PASSED");
                    } catch (Throwable failure) {
                        getLogger().log(Level.SEVERE, "PAPER_BLOCKLOCKER_115_RELOAD_FAILED", failure);
                    } finally {
                        Bukkit.shutdown();
                    }
                }, 40L);
                return;
            }
            int baseY = Math.max(world.getMinHeight() + 20, Math.min(world.getMaxHeight() - 4,
                    world.getHighestBlockYAt(4, 4) + 3));
            Block none = placeChest(world, 3, baseY, 4, null, null);
            var noneResult = BlockLockerLiveEvidence.observe(none);
            if (noneResult.owner() != null || noneResult.everyoneProtected())
                throw new IOException("Unprotected chest incorrectly has an owner");

            Block privateChest = placeChest(world, 6, baseY, 4,
                    "ProbeOwner#" + OWNER, null);
            var privateResult = BlockLockerLiveEvidence.observe(privateChest);
            if (!OWNER.equals(privateResult.owner()) || privateResult.everyoneProtected())
                throw new IOException("Protected chest failed exact UUID resolution");

            Block everyoneChest = placeChest(world, 9, baseY, 4,
                    "UnknownNoUUID", "[Everyone]");
            var everyoneResult = BlockLockerLiveEvidence.observe(everyoneChest);
            if (everyoneResult.owner() != null || !everyoneResult.everyoneProtected())
                throw new IOException("Everyone did not override unknown owner");

            Block unknown = placeChest(world, 12, baseY, 4, "UnknownNoUUID", null);
            var legacy = BlockLockerLiveEvidence.observe(unknown);
            if (legacy.owner() != null || legacy.everyoneProtected())
                throw new IOException("Legacy UUID-unidentifiable sign is not Free");
            // One BlockLocker sign protects a two-block chest whose halves
            // physically cross a Chunk boundary (x=15 and x=16).
            world.getChunkAt(0, 0);
            world.getChunkAt(1, 0);
            Block west = placeChest(world, 15, baseY, 4, "ProbeOwner#" + OWNER, null);
            Block east = world.getBlockAt(16, baseY, 4);
            east.setType(Material.CHEST);
            configureChestHalf(west, org.bukkit.block.data.type.Chest.Type.LEFT);
            configureChestHalf(east, org.bukkit.block.data.type.Chest.Type.RIGHT);
            if (!(west.getState() instanceof org.bukkit.block.Chest westChest)
                    || !(east.getState() instanceof org.bukkit.block.Chest eastChest)
                    || westChest.getInventory().getSize() != 54
                    || eastChest.getInventory().getSize() != 54)
                throw new IOException("Fixture is not a genuine 54-slot double chest");
            var westOwner = BlockLockerLiveEvidence.observe(west);
            var eastOwner = BlockLockerLiveEvidence.observe(east);
            if (!OWNER.equals(westOwner.owner()) || !OWNER.equals(eastOwner.owner())
                    || westOwner.everyoneProtected() || eastOwner.everyoneProtected())
                throw new IOException("Double chest lost the same protected UUID across chunk border");

            // Paper 1.20+ signs also have a back side. BlockLocker v1.15
            // officially parses the [More Users] back lines if a front
            // [Private] sign already exists.
            Block backChest = placeChest(world, 21, baseY, 4, "UnknownNoUUID", null);
            Sign backSign = (Sign) world.getBlockAt(21, baseY, 5).getState();
            backSign.getSide(Side.BACK).setLine(0, "[More Users]");
            backSign.getSide(Side.BACK).setLine(1, "[Everyone]");
            if (!backSign.update(true, false))
                throw new IOException("Could not update protection sign back side");
            var backOwner = BlockLockerLiveEvidence.observe(backChest);
            if (backOwner.owner() != null || !backOwner.everyoneProtected())
                throw new IOException("BlockLocker sign back-side Everyone override was ignored");

            verifyRealCapturedMoney(world, baseY, true);

            Files.createDirectories(getDataFolder().toPath());
            Files.writeString(testWitness(), world.getUID() + "\n" + baseY + "\n");
            getLogger().info("PAPER_BLOCKLOCKER_115_OWNER_PASSED: unprotected Free,"
                    + " exact UUID owner, Everyone priority, verified legacy unknown UUID Free,"
                    + " cross-chunk double chest and back-side Everyone;"
                    + " saved NBT and BAN are not verified");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "PAPER_BLOCKLOCKER_115_OWNER_FAILED", e);
            Bukkit.shutdown();
            throw new IllegalStateException("BlockLocker live ownership probe failed", e);
        }
    }

    /**
     * Real Paper Block inventories, real CoinItemCodec ItemStacks and real
     * BlockLocker v1.15 sign protection; only the BAN source is an explicit
     * isolated test observation, NOT proof of a real provider-complete epoch.
     * On restart, never reconstruct the original physical money.
     */
    private void verifyRealCapturedMoney(World world, int y, boolean seed)
            throws IOException {
        var codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        int[] xs = {3, 6, 9, 12};
        int[] denominations = {1000, 10000, 5000, 100};
        if (seed) {
            for (int i = 0; i < xs.length; i++) {
                var state = world.getBlockAt(xs[i], y, 4).getState();
                if (!(state instanceof org.bukkit.block.Chest chest))
                    throw new IOException("Real BlockLocker fixture is not a chest");
                chest.getBlockInventory().setItem(
                        0, codec.create(CoinDenomination.fromValue(denominations[i]), 1));
            }
        }
        var slice = PaperLoadedChunkAssets.scan(world.getChunkAt(0, 0),
                1024, 16384, 100000, 8 * 1024 * 1024, 16 * 1024 * 1024);
        var original = slice.entries().stream()
                .map(entry -> new HybridLiveSourceStage.Value(
                        entry.physicalKey(), entry.world(), entry.amount()))
                .toList();
        var source = new HybridLiveSourceStage.ChunkSource(
                new SavedLiveSourcePartition.Chunk(world.getName(), 0, 0), original);
        BanSourceCoverage declaredFixtureBans = BanSourceCoverage.capture(
                Set.of("isolated-provider-observation"),
                Map.of(OWNER, List.of(new BanEvidence.Source(
                        "isolated-provider-observation", BanEvidence.Status.CLEAR))));
        BigInteger[] values = {
                BigInteger.valueOf(1000), BigInteger.valueOf(10000),
                BigInteger.valueOf(5000), BigInteger.valueOf(100)};
        for (int i = 0; i < xs.length; i++) {
            String key = "world/" + world.getName() + "/" + xs[i] + "," + y + ",4:0";
            var candidates = original.stream()
                    .filter(value -> key.equals(value.physicalKey())).toList();
            if (candidates.size() != 1 || !values[i].equals(candidates.getFirst().amount()))
                throw new IOException("Original Paper monetary scan omitted real chest coin: " + key);
            var v = candidates.getFirst();
            var asset = PaperBlockLockerLiveClassifier.classify(
                    source, v, world, declaredFixtureBans, Set.of());
            if (!source.id().equals(asset.source())
                    || !key.equals(asset.physicalKey())
                    || !values[i].equals(asset.value())
                    || !world.getName().equals(asset.world())
                    || asset.currentlyBanned() || asset.hasPlayerdata())
                throw new IOException("Real original coin/source/owner status changed: " + key);
            if (i == 1) {
                // Owner exists, but no real playerdata was created in this
                // isolated Paper test: this source is Dead, not Personal/Free.
                if (!OWNER.equals(asset.owner()) || asset.everyoneProtected())
                    throw new IOException("Known protected owner lost");
            } else if (asset.owner() != null
                    || asset.everyoneProtected() != (i == 2))
                throw new IOException("Real Free/Everyone priority incorrect");
        }
        getLogger().info("PAPER_A04_LIVE_BLOCKLOCKER_NATIVE_CURRENCY_CLASSIFIED"
                + " phase=" + (seed ? "create" : "reload")
                + " privateDead=10000 everyoneFree=5000 unprotectedFree=1000"
                + " legacyFree=100 originalValueUnchanged=true"
                + " (BAN observation is test-only; no G0 publication)");
    }

    private Path testWitness() {
        return getDataFolder().toPath().resolve("a04-probe-world-and-y.txt");
    }

    /** Bounded post-restart reader: no fixture rebuilding or monetary effect. */
    private void verifyRestartedOwnerEvidence(World world) throws IOException {
        List<String> witness = Files.readAllLines(testWitness());
        if (witness.size() != 2 || !witness.getFirst().equals(world.getUID().toString()))
            throw new IOException("The second Paper process did not load the same world identity");
        int y;
        try { y = Integer.parseInt(witness.get(1)); }
        catch (NumberFormatException bad) { throw new IOException("Malformed original fixture height", bad); }
        world.getChunkAt(0, 0);
        world.getChunkAt(1, 0);

        var free = BlockLockerLiveEvidence.observe(world.getBlockAt(3, y, 4));
        if (free.owner() != null || free.everyoneProtected())
            throw new IOException("Unprotected chest changed after saved world reload");
        var owned = BlockLockerLiveEvidence.observe(world.getBlockAt(6, y, 4));
        if (!OWNER.equals(owned.owner()) || owned.everyoneProtected())
            throw new IOException("BlockLocker saved primary owner UUID was not restored");
        var frontEveryone = BlockLockerLiveEvidence.observe(world.getBlockAt(9, y, 4));
        if (frontEveryone.owner() != null || !frontEveryone.everyoneProtected())
            throw new IOException("BlockLocker saved front Everyone was not restored");
        var legacy = BlockLockerLiveEvidence.observe(world.getBlockAt(12, y, 4));
        if (legacy.owner() != null || legacy.everyoneProtected())
            throw new IOException("Legacy UUID-unidentifiable sign is not Free after reload");
        Block west = world.getBlockAt(15, y, 4);
        Block east = world.getBlockAt(16, y, 4);
        if (!(west.getState() instanceof org.bukkit.block.Chest westChest)
                || !(east.getState() instanceof org.bukkit.block.Chest eastChest)
                || westChest.getInventory().getSize() != 54
                || eastChest.getInventory().getSize() != 54)
            throw new IOException("Saved double chest lost physical 54-slot topology");
        var westOwner = BlockLockerLiveEvidence.observe(west);
        var eastOwner = BlockLockerLiveEvidence.observe(east);
        if (!OWNER.equals(westOwner.owner()) || !OWNER.equals(eastOwner.owner())
                || westOwner.everyoneProtected() || eastOwner.everyoneProtected())
            throw new IOException("Saved double chest owner differs across Chunk boundary");
        Sign backSign = (Sign) world.getBlockAt(21, y, 5).getState();
        if (!backSign.getSide(Side.BACK).getLine(0).contains("[More Users]")
                || !backSign.getSide(Side.BACK).getLine(1).contains("[Everyone]"))
            throw new IOException("BlockLocker back-side protection display not restored");
        var backEveryone = BlockLockerLiveEvidence.observe(world.getBlockAt(21, y, 4));
        if (backEveryone.owner() != null || !backEveryone.everyoneProtected())
            throw new IOException("Saved back-side Everyone was not restored");
        verifyRealCapturedMoney(world, y, false);
        getLogger().info("PAPER_BLOCKLOCKER_115_SAVED_OWNER_AND_EVERYONE_UUID_PASSED"
                + " world=" + world.getUID() + " y=" + y);
    }

    private static void configureChestHalf(Block block,
                                           org.bukkit.block.data.type.Chest.Type half) {
        var chestData = (org.bukkit.block.data.type.Chest) block.getBlockData();
        chestData.setFacing(BlockFace.NORTH);
        chestData.setType(half);
        block.setBlockData(chestData);
    }

    private static Block placeChest(World world, int x, int y, int z,
                                    String owner, String secondary) {
        Block chest = world.getBlockAt(x, y, z);
        chest.setType(Material.CHEST);
        if (owner == null) return chest;
        Block signBlock = world.getBlockAt(x, y, z + 1);
        signBlock.setType(Material.OAK_WALL_SIGN);
        WallSign signData = (WallSign) signBlock.getBlockData();
        signData.setFacing(BlockFace.SOUTH); // sign is south; attached chest to its north
        signBlock.setBlockData(signData);
        Sign sign = (Sign) signBlock.getState();
        sign.getSide(Side.FRONT).setLine(0, "[Private]");
        sign.getSide(Side.FRONT).setLine(1, owner);
        if (secondary != null) sign.getSide(Side.FRONT).setLine(2, secondary);
        if (!sign.update(true, false))
            throw new IllegalStateException("Failed to place BlockLocker probe sign");
        return chest;
    }
}
