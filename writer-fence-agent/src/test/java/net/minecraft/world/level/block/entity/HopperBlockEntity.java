package net.minecraft.world.level.block.entity;

/** Exact bytecode descriptor and return-value fixture for actual Paper native Hopper pull. */
public final class HopperBlockEntity {
    private HopperBlockEntity() {}

    private static int admittedBodies;

    public static int bodyCount() { return admittedBodies; }

    public static boolean hopperPull(net.minecraft.world.level.Level level,
            Hopper hopper, net.minecraft.world.Container source,
            net.minecraft.world.item.ItemStack item, int slot) {
        admittedBodies++;
        return true;
    }
}
