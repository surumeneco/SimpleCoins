package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Objects;

/**
 * Source-bound coordinate of a positive currency slot that came from
 * PaperLoadedChunkAssets.scan() of an actual loaded BlockState inventory.
 *
 * This is only a physical source-key validator. It cannot establish that
 * Paper memory and saved files are frozen in a G0 accounting epoch.
 */
public record LiveContainerPhysicalSlot(int x, int y, int z, int slot) {

    public static LiveContainerPhysicalSlot parse(
            HybridLiveSourceStage.Source source,
            HybridLiveSourceStage.Value value) throws IOException {
        if (!(source instanceof HybridLiveSourceStage.ChunkSource chunk) || value == null)
            throw new IOException("LIVE container needs an actual selected Chunk source");
        String world = chunk.chunk().world();
        if (value.world() == null || !value.world().equals(world))
            throw new IOException("LIVE container World differs from selected source");
        String key = Objects.requireNonNull(value.physicalKey());
        String prefix = "world/" + world + "/";
        if (!key.startsWith(prefix))
            throw new IOException("LIVE container has no matching world-qualified key");
        String suffix = key.substring(prefix.length());
        int separator = suffix.indexOf(':');
        if (separator < 0 || separator != suffix.lastIndexOf(':'))
            throw new IOException("LIVE Block inventory key has no unique slot separator");
        String position = suffix.substring(0, separator);
        String[] coords = position.split(",", -1);
        if (coords.length != 3)
            throw new IOException("LIVE Block inventory position must be x,y,z");
        int x = parseInt(coords[0]), y = parseInt(coords[1]), z = parseInt(coords[2]);
        int slot = parseInt(suffix.substring(separator + 1));
        if (slot < 0)
            throw new IOException("Negative LIVE Block inventory slot");
        if (Math.floorDiv(x, 16) != chunk.chunk().x()
                || Math.floorDiv(z, 16) != chunk.chunk().z())
            throw new IOException("LIVE Block inventory outside its captured Chunk");
        return new LiveContainerPhysicalSlot(x, y, z, slot);
    }

    private static int parseInt(String raw) throws IOException {
        try {
            int number = Integer.parseInt(raw);
            if (!Integer.toString(number).equals(raw))
                throw new NumberFormatException("Noncanonical integer");
            return number;
        } catch (NumberFormatException invalid) {
            throw new IOException("Malformed LIVE container coordinate or slot", invalid);
        }
    }
}
