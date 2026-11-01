package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.BlockLockerProtectionResolution;
import co.surumene.simplecoins.domain.BlockLockerSignAttachment;
import co.surumene.simplecoins.domain.BlockLockerVisibleSignResolver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;

/**
 * Read-only live Paper sign evidence for BlockLocker ownership resolution.
 * Call only on the server's appropriate thread with a coherent world state.
 * The caller supplies verified runtime localization and identity lookup; a
 * visible sign alone does not prove attachment to a protected inventory.
 */
public final class PaperBlockLockerSignAdapter {
    private PaperBlockLockerSignAdapter() {}

    public static Optional<BlockLockerProtectionResolution.VerifiedSign> readPaper(
            Sign sign,
            Set<String> privateAliases,
            Set<String> moreUsersAliases,
            Set<String> everyoneAliases,
            BlockLockerVisibleSignResolver.PrimaryIdentity primaryIdentity)
            throws IOException {
        if (sign == null)
            throw new IOException("Missing live Paper sign state");
        final BlockLockerSignAttachment.Mount mount;
        final BlockLockerSignAttachment.Position position;
        final List<String> visible = new ArrayList<>(4);
        try {
            mount = mount(sign.getBlockData());
            position = new BlockLockerSignAttachment.Position(
                    sign.getX(), sign.getY(), sign.getZ());
            var components = sign.getSide(Side.FRONT).lines();
            if (components == null || components.size() != 4)
                throw new IOException("Incomplete live Paper sign front text");
            for (var component : components) {
                if (component == null)
                    throw new IOException("Null live Paper sign text component");
                visible.add(PlainTextComponentSerializer.plainText().serialize(component));
            }
        } catch (RuntimeException unavailable) {
            throw new IOException("Cannot read live Paper sign evidence", unavailable);
        }
        return BlockLockerVisibleSignResolver.verify(
                position, mount, visible, Optional.empty(),
                privateAliases, moreUsersAliases, everyoneAliases, primaryIdentity);
    }

    private static BlockLockerSignAttachment.Mount mount(BlockData state) throws IOException {
        if (state instanceof WallSign wall) {
            return switch (wall.getFacing()) {
                case NORTH -> BlockLockerSignAttachment.Mount.WALL_NORTH;
                case EAST -> BlockLockerSignAttachment.Mount.WALL_EAST;
                case SOUTH -> BlockLockerSignAttachment.Mount.WALL_SOUTH;
                case WEST -> BlockLockerSignAttachment.Mount.WALL_WEST;
                default -> throw new IOException("Unsupported live wall sign facing");
            };
        }
        if (state instanceof org.bukkit.block.data.type.Sign)
            return BlockLockerSignAttachment.Mount.STANDING;
        throw new IOException("Unsupported live sign block state");
    }
}
