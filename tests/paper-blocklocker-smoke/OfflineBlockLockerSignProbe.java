package co.surumene.simplecoins.a04probe;

import co.surumene.simplecoins.domain.AnvilRegionReader;
import co.surumene.simplecoins.domain.BlockLockerSavedSignCandidates;
import co.surumene.simplecoins.domain.BlockLockerSavedSignText;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
        int[] visited = {0};
        AnvilRegionReader.scan(region, 2 * 1024 * 1024, 16 * 1024 * 1024, chunk -> {
            visited[0]++;
            if (chunk.x() != 0 && chunk.x() != 1) return;
            if (chunk.z() != 0) return;
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
    }

    private static boolean contains(String text, String token) {
        return text != null && text.toLowerCase(java.util.Locale.ROOT)
                .contains(token.toLowerCase(java.util.Locale.ROOT));
    }
}
