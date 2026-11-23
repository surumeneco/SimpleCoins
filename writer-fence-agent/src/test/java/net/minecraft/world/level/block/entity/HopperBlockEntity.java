package net.minecraft.world.level.block.entity;

/** Exact bytecode descriptor and return-value fixtures for Paper native Hopper pull and output. */
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
    public static boolean hopperPush(net.minecraft.world.level.Level level,
            net.minecraft.world.Container destination, net.minecraft.core.Direction direction,
            HopperBlockEntity hopper) {
        admittedBodies++;
        return true;
    }
}
