package co.surumene.simplecoins.paper;

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
            try {
                BlockLockerLiveEvidence.observe(unknown);
                throw new IOException("Missing UUID unexpectedly accepted as Free");
            } catch (IOException expected) {
                if (!expected.getMessage().contains("no verified UUID"))
                    throw expected;
            }
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

            Files.createDirectories(getDataFolder().toPath());
            Files.writeString(testWitness(), world.getUID() + "\n" + baseY + "\n");
            getLogger().info("PAPER_BLOCKLOCKER_115_OWNER_PASSED: unprotected Free,"
                    + " exact UUID owner, Everyone priority, unknown UUID refusal,"
                    + " cross-chunk double chest and back-side Everyone;"
                    + " saved NBT and BAN are not verified");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "PAPER_BLOCKLOCKER_115_OWNER_FAILED", e);
            Bukkit.shutdown();
            throw new IllegalStateException("BlockLocker live ownership probe failed", e);
        }
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
        try {
            BlockLockerLiveEvidence.observe(world.getBlockAt(12, y, 4));
            throw new IOException("Unresolved BlockLocker owner unexpectedly accepted after reload");
        } catch (IOException expected) {
            if (!expected.getMessage().contains("no verified UUID"))
                throw expected;
        }
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
