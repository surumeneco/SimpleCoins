package co.surumene.simplecoins.paper;

import java.io.IOException;
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
            getLogger().info("PAPER_BLOCKLOCKER_115_OWNER_PASSED: unprotected Free,"
                    + " exact UUID owner, Everyone priority, unknown UUID refusal;"
                    + " saved NBT and BAN are not verified");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "PAPER_BLOCKLOCKER_115_OWNER_FAILED", e);
            Bukkit.shutdown();
            throw new IllegalStateException("BlockLocker live ownership probe failed", e);
        }
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
