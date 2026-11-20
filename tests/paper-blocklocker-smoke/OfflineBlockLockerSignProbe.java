package co.surumene.simplecoins.a04probe;

import co.surumene.simplecoins.domain.AnvilRegionReader;
import co.surumene.simplecoins.domain.BlockLockerSavedSignCandidates;
import co.surumene.simplecoins.domain.BlockLockerSavedSignText;
import co.surumene.simplecoins.domain.BlockLockerSignAttachment;
import co.surumene.simplecoins.domain.BlockLockerChestLinkage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Test-only non-Bukkit inspection of real Paper/BlockLocker saved Anvil text.
 * Raw sign text/hints are not authoritative block ownership evidence.
 */
public final class OfflineBlockLockerSignProbe {
    private static final UUID OWNER = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Set<Integer> EXPECTED_X = Set.of(6, 9, 12, 15, 21);
    private OfflineBlockLockerSignProbe() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected exact Anvil path");
        Path region = Path.of(args[0]).toAbsolutePath().normalize();
        if (!Files.isRegularFile(region, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Saved terrain Anvil does not exist: " + region);
        Set<Integer> found = new HashSet<>();
        Map<Integer, Integer> heights = new HashMap<>();
        Map<Integer, Map<String, Object>> terrainChunks = new HashMap<>();
        int[] visited = {0};
        AnvilRegionReader.scan(region, 2 * 1024 * 1024, 16 * 1024 * 1024, chunk -> {
            visited[0]++;
            if (chunk.x() != 0 && chunk.x() != 1) return;
            if (chunk.z() != 0) return;
            if (terrainChunks.putIfAbsent(chunk.x(), chunk.nbt()) != null)
                throw new IOException("Duplicate saved terrain Chunk: " + chunk.x());
            var candidates = BlockLockerSavedSignCandidates.extractEvidence(chunk.nbt());
            for (var entry : candidates.entrySet()) {
                String[] coordinates = entry.getKey().split(",");
                if (coordinates.length != 3)
                    throw new IOException("Invalid saved sign coordinate key: " + entry.getKey());
                int x = Integer.parseInt(coordinates[0]);
                int y = Integer.parseInt(coordinates[1]);
                int z = Integer.parseInt(coordinates[2]);
                if (z != 5 || !EXPECTED_X.contains(x)) continue;
                if (Math.floorDiv(x, 16) != chunk.x() || Math.floorDiv(z, 16) != chunk.z())
                    throw new IOException("Saved sign declared in incorrect terrain Chunk");
                if (!found.add(x))
                    throw new IOException("Duplicate saved BlockLocker sign at X=" + x);
                heights.put(x, y);
                var evidence = entry.getValue();
                BlockLockerSavedSignText.Evidence text = evidence.displayText()
                        .orElseThrow(() -> new IOException("Expected saved BlockLocker sign text at X=" + x));
                if (!contains(text.front().get(0), "[Private]"))
                    throw new IOException("Saved BlockLocker [Private] header missing X=" + x
                            + " observed=" + text.front());
                switch (x) {
                    case 6, 15 -> {
                        if (!text.front().get(1).contains(OWNER.toString()))
                            throw new IOException("Original verified-UUID display text lost X=" + x);
                    }
                    case 9 -> {
                        if (!contains(text.front().get(2), "[Everyone]"))
                            throw new IOException("Saved front Everyone marker lost");
                    }
                    case 12 -> {
                        if (!text.front().get(1).contains("UnknownNoUUID"))
                            throw new IOException("Saved unresolved owner text lost");
                    }
                    case 21 -> {
                        var back = text.back().orElseThrow(
                                () -> new IOException("Saved modern sign lacks BlockLocker back side"));
                        if (!contains(back.get(0), "[More Users]")
                                || !contains(back.get(1), "[Everyone]"))
                            throw new IOException("Saved BlockLocker back-side Everyone markers lost");
                    }
                    default -> throw new IOException("Unexpected fixture source");
                }
                System.out.println("PAPER_A04_SAVED_SIGN_RAW_SOURCE_VALIDATED"
                        + " x=" + x + " y=" + y + " chunk=" + chunk.x() + "," + chunk.z()
                        + " pdcHintsPresent=" + evidence.pdcHints().isPresent());
            }
        });
        if (!found.equals(EXPECTED_X))
            throw new IOException("Missing saved BlockLocker source signs: expected="
                    + EXPECTED_X + " observed=" + found + " chunks=" + visited[0]);
        if (new HashSet<>(heights.values()).size() != 1)
            throw new IOException("BlockLocker test signs changed vertical source alignment");
        System.out.println("PAPER_A04_FIVE_REAL_SAVED_BLOCKLOCKER_SIGNS_ACROSS_CHUNKS_PASSED");
        verifySavedTerrainTopology(terrainChunks, heights.values().iterator().next());
    }

    /**
     * Test-only read-only topology witness from the SAME saved terrain Anvil as
     * the sign text scan; neither owner nor writer epoch is inferred.
     */
    private static void verifySavedTerrainTopology(
            Map<Integer, Map<String, Object>> chunks, int y) throws IOException {
        if (!chunks.keySet().equals(Set.of(0, 1)))
            throw new IOException("Saved fixture terrain is missing cross-Chunk evidence");
        for (int x : new int[]{3, 6, 9, 12, 15, 16, 21}) {
            SavedBlock chest = readSavedBlock(chunks, x, y, 4);
            if (!"minecraft:chest".equals(chest.name()))
                throw new IOException("Missing actual saved chest block at X=" + x
                        + ": " + chest.name());
        }
        for (int x : EXPECTED_X) {
            SavedBlock wallSign = readSavedBlock(chunks, x, y, 5);
            if (!"minecraft:oak_wall_sign".equals(wallSign.name())
                    || !"south".equals(wallSign.properties().get("facing")))
                throw new IOException("Saved BlockLocker sign is not a south-facing wall sign X=" + x);
            if (!BlockLockerSignAttachment.attaches(
                    new BlockLockerSignAttachment.Position(x, y, 5),
                    BlockLockerSignAttachment.Mount.WALL_SOUTH,
                    new BlockLockerSignAttachment.Position(x, y, 4)))
                throw new IOException("Saved BlockLocker sign does not attach to observed chest X=" + x);
        }
        SavedBlock west = readSavedBlock(chunks, 15, y, 4);
        SavedBlock east = readSavedBlock(chunks, 16, y, 4);
        if (!"north".equals(west.properties().get("facing"))
                || !"left".equals(west.properties().get("type"))
                || !"north".equals(east.properties().get("facing"))
                || !"right".equals(east.properties().get("type")))
            throw new IOException("Saved cross-Chunk double chest is not a reciprocal pair");
        var first = new BlockLockerChestLinkage.Part(
                new BlockLockerChestLinkage.Position(15, y, 4),
                BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.LEFT,
                BlockLockerChestLinkage.Kind.CHEST);
        var second = new BlockLockerChestLinkage.Part(
                new BlockLockerChestLinkage.Position(16, y, 4),
                BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.RIGHT,
                BlockLockerChestLinkage.Kind.CHEST);
        if (!BlockLockerChestLinkage.linked(first, second)
                || !BlockLockerChestLinkage.signAttachedToPair(
                        new BlockLockerSignAttachment.Position(15, y, 5),
                        BlockLockerSignAttachment.Mount.WALL_SOUTH, first, second))
            throw new IOException("Saved double chest sign-to-pair evidence is not reciprocal");
        System.out.println("PAPER_A04_REAL_SAVED_SIGN_CHEST_TOPOLOGY_PASSED"
                + " signs=5 chests=7 chunks=2 crossChunkPair=true"
                + " (TEST-ONLY; owner and writer epoch not proved)");
    }

    private record SavedBlock(String name, Map<String, String> properties) {}

    /**
     * Decode the palette-indexed modern section at an actual saved coordinate.
     * Paper 26.2 uses non-spanning packed longs; malformed/missing means HOLD.
     */
    private static SavedBlock readSavedBlock(Map<Integer, Map<String, Object>> chunks,
                                             int x, int y, int z) throws IOException {
        if (Math.floorDiv(z, 16) != 0)
            throw new IOException("Saved topology probe left its selected terrain Z Chunk");
        Map<String, Object> chunk = chunks.get(Math.floorDiv(x, 16));
        if (chunk == null || !(chunk.get("sections") instanceof List<?> sections))
            throw new IOException("Saved terrain Chunk/sections unavailable for X=" + x);
        Map<?, ?> section = null;
        for (Object candidate : sections) {
            if (!(candidate instanceof Map<?, ?> named)
                    || !(named.get("Y") instanceof Number index))
                throw new IOException("Malformed saved terrain section identity");
            if (index.intValue() == Math.floorDiv(y, 16)) {
                if (section != null)
                    throw new IOException("Duplicate saved terrain Y section");
                section = named;
            }
        }
        if (section == null || !(section.get("block_states") instanceof Map<?, ?> states)
                || !(states.get("palette") instanceof List<?> palette) || palette.isEmpty())
            throw new IOException("Saved terrain section block palette unavailable");
        int position = (Math.floorMod(y, 16) << 8)
                | (Math.floorMod(z, 16) << 4) | Math.floorMod(x, 16);
        int paletteIndex;
        if (palette.size() == 1 && !states.containsKey("data")) {
            paletteIndex = 0;
        } else {
            if (!(states.get("data") instanceof long[] data))
                throw new IOException("Missing packed saved terrain palette indices");
            int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
            int perWord = 64 / bits;
            int expectedWords = (4096 + perWord - 1) / perWord;
            if (bits > 32 || data.length != expectedWords)
                throw new IOException("Malformed saved terrain palette packing");
            int word = position / perWord;
            int shift = (position % perWord) * bits;
            paletteIndex = (int) ((data[word] >>> shift) & ((1L << bits) - 1));
            if (paletteIndex >= palette.size())
                throw new IOException("Saved terrain palette index out of range");
        }
        Object raw = palette.get(paletteIndex);
        if (!(raw instanceof Map<?, ?> block) || !(block.get("Name") instanceof String name))
            throw new IOException("Malformed saved terrain palette block identity");
        Map<String, String> properties = new HashMap<>();
        if (block.containsKey("Properties")) {
            if (!(block.get("Properties") instanceof Map<?, ?> props))
                throw new IOException("Malformed saved terrain block Properties");
            for (var entry : props.entrySet()) {
                if (!(entry.getKey() instanceof String key)
                        || !(entry.getValue() instanceof String value))
                    throw new IOException("Non-string saved terrain block property");
                properties.put(key, value);
            }
        }
        return new SavedBlock(name, Map.copyOf(properties));
    }

    private static boolean contains(String text, String token) {
        return text != null && text.toLowerCase(java.util.Locale.ROOT)
                .contains(token.toLowerCase(java.util.Locale.ROOT));
    }
}
