package net.minecraft.world.level.storage;
import net.minecraft.world.entity.player.Player;
import co.surumene.simplecoins.writeragent.NativeSaveFixture;
/** Isolated bytecode fixture matching the exact Paper NMS signature. */
public final class PlayerDataStorage {
    public void save(Player player) {
        NativeSaveFixture.onNmsBody();
    }
}
