package org.bukkit.craftbukkit.entity;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.entity.player.Player;
/** Isolated bytecode fixture matching the exact Paper Craft signature. */
public final class CraftPlayer {
    public void saveData() { new PlayerDataStorage().save(new Player()); }
}
