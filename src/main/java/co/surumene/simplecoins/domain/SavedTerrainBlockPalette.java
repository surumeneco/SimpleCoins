package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Read-only Paper 26.2 terrain block-state decoding from one explicitly
 * selected SAVED Chunk. Its caller must establish source/epoch stability.
 * A block state alone never proves attachment, BlockLocker ownership, or
 * an all-world settlement source.
 */
public final class SavedTerrainBlockPalette {
    private SavedTerrainBlockPalette() {}

    public record BlockState(String name, Map<String, String> properties) {
        public BlockState {
            Objects.requireNonNull(name);
            Objects.requireNonNull(properties);
            if (name.isBlank())
                throw new IllegalArgumentException("Blank saved block name");
            properties = Map.copyOf(properties);
        }
    }

    public static BlockState read(Map<String, Object> chunk,
                                  int x, int y, int z) throws IOException {
        if (chunk == null
                || !(chunk.get("xPos") instanceof Integer chunkX)
                || !(chunk.get("zPos") instanceof Integer chunkZ)
                || chunkX != Math.floorDiv(x, 16)
                || chunkZ != Math.floorDiv(z, 16))
            throw new IOException("Saved terrain Chunk identity does not match requested block");

        if (!(chunk.get("sections") instanceof List<?> sections))
            throw new IOException("Saved terrain Chunk sections missing");
        int expectedSectionY = Math.floorDiv(y, 16);
        Map<?, ?> section = null;
        for (Object raw : sections) {
            if (!(raw instanceof Map<?, ?> candidate)
                    || !(candidate.get("Y") instanceof Number sectionIndex))
                throw new IOException("Malformed saved terrain section header");
            if (sectionIndex.intValue() == expectedSectionY) {
                if (section != null)
                    throw new IOException("Duplicate saved terrain section Y=" + expectedSectionY);
                section = candidate;
            }
        }
        if (section == null
                || !(section.get("block_states") instanceof Map<?, ?> states)
                || !(states.get("palette") instanceof List<?> palette)
                || palette.isEmpty() || palette.size() > 4096)
            throw new IOException("Missing or invalid saved terrain block palette");

        int localIndex = (Math.floorMod(y, 16) << 8)
                | (Math.floorMod(z, 16) << 4) | Math.floorMod(x, 16);
        int index;
        if (palette.size() == 1 && !states.containsKey("data")) {
            index = 0;
        } else {
            if (!(states.get("data") instanceof long[] packed))
                throw new IOException("Missing saved terrain packed palette data");
            int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
            int perWord = 64 / bits;
            int expectedWords = (4096 + perWord - 1) / perWord;
            if (packed.length != expectedWords)
                throw new IOException("Malformed saved terrain packed palette length");
            long word = packed[localIndex / perWord];
            int shift = (localIndex % perWord) * bits;
            long mask = (1L << bits) - 1L;
            index = (int) ((word >>> shift) & mask);
            if (index >= palette.size())
                throw new IOException("Saved terrain palette index is out of range");
        }

        Object rawBlock = palette.get(index);
        if (!(rawBlock instanceof Map<?, ?> block)
                || !(block.get("Name") instanceof String name)
                || name.isBlank())
            throw new IOException("Invalid saved terrain block identity");
        Map<String, String> properties = new HashMap<>();
        if (block.containsKey("Properties")) {
            if (!(block.get("Properties") instanceof Map<?, ?> props))
                throw new IOException("Malformed saved terrain block Properties");
            for (var entry : props.entrySet()) {
                if (!(entry.getKey() instanceof String key)
                        || !(entry.getValue() instanceof String value))
                    throw new IOException("Non-string saved block property");
                properties.put(key, value);
            }
        }
        return new BlockState(name, properties);
    }
}
