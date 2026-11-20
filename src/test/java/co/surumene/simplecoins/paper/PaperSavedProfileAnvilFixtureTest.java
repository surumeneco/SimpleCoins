package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;
import co.surumene.simplecoins.domain.AnvilRegionReader;
import co.surumene.simplecoins.domain.BlockLockerHeaderMatcher;
import co.surumene.simplecoins.domain.BlockLockerSavedProfileResolution;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Real Paper 26.2 + BlockLocker 1.15 stopped-world Anvil proof.
 * Runs only with the explicit CI fixture path; never a live economy source.
 */
class PaperSavedProfileAnvilFixtureTest {
    private static final UUID OWNER = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Set<Integer> EXPECTED = Set.of(6,9,12,15,21);

    @Test void savedBlockLockerProfilesMatchObservedLiveOwnerAndEveryoneAcrossBothChunks() throws Exception {
        String path = System.getProperty("a04.saved.fixture");
        Assumptions.assumeTrue(path != null && !path.isBlank(),
                "Real stopped-Paper saved fixture not selected");
        Map<Integer, BlockLockerSavedProfileResolution.SignProfiles> result = new HashMap<>();
        AnvilRegionReader.scan(Path.of(path), 2 * 1024 * 1024, 16 * 1024 * 1024, chunk -> {
            if (chunk.z() != 0 || (chunk.x() != 0 && chunk.x() != 1)) return;
            Object blocks = chunk.nbt().get("block_entities");
            if (!(blocks instanceof List<?> entities))
                throw new IOException("Saved terrain lacks block entities");
            for (Object raw : entities) {
                if (!(raw instanceof Map<?,?> entity)
                        || !(entity.get("id") instanceof String id)
                        || !id.equals("minecraft:sign"))
                    continue;
                if (!(entity.get("x") instanceof Integer x)
                        || !(entity.get("z") instanceof Integer z)
                        || !(entity.get("y") instanceof Integer y))
                    throw new IOException("Malformed saved sign coordinate");
                if (z != 5 || !EXPECTED.contains(x)) continue;
                if (Math.floorDiv(x,16) != chunk.x())
                    throw new IOException("Saved sign located outside declared Chunk");
                @SuppressWarnings("unchecked")
                Map<String,Object> sign = (Map<String,Object>) entity;
                var resolved = BlockLockerSavedProfileResolution.read(
                        sign, Set.of("[Private]"), Set.of("[More Users]"), Set.of("Everyone"),
                        PaperSavedBlockLockerSignText.nativeText())
                        .orElseThrow(() -> new IOException("Actual BlockLocker protection header missing"));
                assertEquals(BlockLockerHeaderMatcher.Type.PRIVATE, resolved.type());
                if (result.putIfAbsent(x,resolved) != null)
                    throw new IOException("Duplicate saved BlockLocker sign at X=" + x);
            }
        });
        assertEquals(EXPECTED,result.keySet(),"Actual stopped Paper source has incomplete signs");
        for (int x : new int[]{6,15}) {
            assertEquals(OWNER,result.get(x).primaryOwner().orElseThrow(),
                    "Stored UUID or LockettePro display identity lost at x=" + x);
            assertTrue(result.get(x).everyoneAbsenceVerified());
        }
        for (int x : new int[]{9,21}) {
            assertTrue(result.get(x).primaryOwner().isEmpty());
            assertTrue(result.get(x).everyoneEffective(),
                    "Front/back Everyone profile not reproduced at x=" + x);
        }
        assertTrue(result.get(12).primaryOwner().isEmpty(),
                "Unresolved saved text owner cannot be guessed");
        assertTrue(result.get(12).everyoneAbsenceVerified());
        System.out.println("PAPER_A04_SAVED_EFFECTIVE_PROFILES_PASSED signs=5"
                + " knownOwners=2 everyone=2 unresolved=1 chunks=2"
                + " (fixture-only; no all-writer epoch or settlement publication)");
    }
}
