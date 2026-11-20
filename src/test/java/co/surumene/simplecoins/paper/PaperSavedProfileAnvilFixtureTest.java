package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;
import co.surumene.simplecoins.domain.AnvilRegionReader;
import co.surumene.simplecoins.domain.BlockLockerHeaderMatcher;
import co.surumene.simplecoins.domain.BlockLockerSavedProfileResolution;
import co.surumene.simplecoins.domain.SavedTerrainBlockPalette;
import co.surumene.simplecoins.domain.BlockLockerSignAttachment;
import co.surumene.simplecoins.domain.BlockLockerChestLinkage;
import co.surumene.simplecoins.domain.SavedBlockLockerChestSigns;
import co.surumene.simplecoins.domain.SavedBlockLockerSignSummary;
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
        Map<Integer, Map<String, Object>> terrain = new HashMap<>();
        Map<Integer, Integer> savedHeights = new HashMap<>();
        AnvilRegionReader.scan(Path.of(path), 2 * 1024 * 1024, 16 * 1024 * 1024, chunk -> {
            if (chunk.z() != 0 || (chunk.x() != 0 && chunk.x() != 1)) return;
            if (terrain.putIfAbsent(chunk.x(),chunk.nbt()) != null)
                throw new IOException("Duplicate saved fixture terrain Chunk");
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
                savedHeights.put(x,y);
            }
        });
        assertEquals(EXPECTED,result.keySet(),"Actual stopped Paper source has incomplete signs");
        assertEquals(EXPECTED,savedHeights.keySet());
        assertEquals(1,Set.copyOf(savedHeights.values()).size(),
                "Fixture sign heights diverged across the selected Chunks");
        final int fixtureY=savedHeights.values().iterator().next();
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
        assertEquals(Set.of(0,1),terrain.keySet(),"Selected real fixture Chunks incomplete");
        for (int x : EXPECTED) {
            var signState=SavedTerrainBlockPalette.read(terrain.get(Math.floorDiv(x,16)),x,fixtureY,5);
            assertEquals("minecraft:oak_wall_sign",signState.name(),"Missing actual saved sign x="+x);
            assertEquals("south",signState.properties().get("facing"));
            assertTrue(BlockLockerSignAttachment.attaches(
                new BlockLockerSignAttachment.Position(x,fixtureY,5),
                BlockLockerSignAttachment.Mount.WALL_SOUTH,
                new BlockLockerSignAttachment.Position(x,fixtureY,4)),
                "Sign not physically attached to saved chest at "+x);
        }
        for (int x : new int[]{3,6,9,12,15,16,21}) {
            var chest=SavedTerrainBlockPalette.read(terrain.get(Math.floorDiv(x,16)),x,fixtureY,4);
            assertEquals("minecraft:chest",chest.name(),"Missing saved physical chest at "+x);
        }
        var first=SavedTerrainBlockPalette.read(terrain.get(0),15,fixtureY,4);
        var second=SavedTerrainBlockPalette.read(terrain.get(1),16,fixtureY,4);
        assertEquals("north",first.properties().get("facing"));
        assertEquals("left",first.properties().get("type"));
        assertEquals("north",second.properties().get("facing"));
        assertEquals("right",second.properties().get("type"));
        var left=new BlockLockerChestLinkage.Part(
            new BlockLockerChestLinkage.Position(15,fixtureY,4),BlockLockerChestLinkage.Facing.NORTH,
            BlockLockerChestLinkage.Half.LEFT,BlockLockerChestLinkage.Kind.CHEST);
        var right=new BlockLockerChestLinkage.Part(
            new BlockLockerChestLinkage.Position(16,fixtureY,4),BlockLockerChestLinkage.Facing.NORTH,
            BlockLockerChestLinkage.Half.RIGHT,BlockLockerChestLinkage.Kind.CHEST);
        assertTrue(BlockLockerChestLinkage.linked(left,right),
                "Saved double-chest NBT states do not form a reciprocal pair");
        assertTrue(BlockLockerChestLinkage.signAttachedToPair(
            new BlockLockerSignAttachment.Position(15,fixtureY,5),
            BlockLockerSignAttachment.Mount.WALL_SOUTH,left,right));
        var observedChunks=Map.of(
            new SavedBlockLockerChestSigns.ChunkPosition(0,0),terrain.get(0),
            new SavedBlockLockerChestSigns.ChunkPosition(1,0),terrain.get(1));
        for (int x : EXPECTED) {
            var chestObservation=SavedBlockLockerChestSigns.observe(
                observedChunks,new SavedBlockLockerChestSigns.Position(x,fixtureY,4),
                Set.of("[Private]"),Set.of("[More Users]"),Set.of("Everyone"),
                PaperSavedBlockLockerSignText.nativeText());
            assertEquals(1,chestObservation.attachedSigns().size(),
                "The real saved chest must have precisely one applicable sign at x="+x);
            var candidate=chestObservation.attachedSigns().get(0);
            assertEquals(new SavedBlockLockerChestSigns.Position(x,fixtureY,5),
                candidate.position(),"Actual saved sign did not attach to its chest");
            assertEquals(result.get(x),candidate.profiles(),
                "Physical attachment observer changed the saved profile at x="+x);
            var aggregated=SavedBlockLockerSignSummary.summarize(chestObservation);
            assertTrue(aggregated.protectionSignObserved());
            assertEquals(x == 9 || x == 21,aggregated.everyoneEffective(),
                "Saved Everyone aggregation mismatched at x="+x);
            assertEquals(!(x == 9 || x == 21),aggregated.everyoneAbsent());
            assertEquals(x == 6 || x == 15 ? OWNER : null,
                aggregated.verifiedMainOwner().orElse(null));
            assertFalse(aggregated.mainOwnerConflict());
            assertEquals(x==15 ? 2 : 1,chestObservation.storageMembers().size(),
                "The saved double chest must be one physical storage pair");
        }
        var reversePair=SavedBlockLockerChestSigns.observe(observedChunks,
            new SavedBlockLockerChestSigns.Position(16,fixtureY,4),
            Set.of("[Private]"),Set.of("[More Users]"),Set.of("Everyone"),
            PaperSavedBlockLockerSignText.nativeText());
        assertEquals(2,reversePair.storageMembers().size());
        assertEquals(1,reversePair.attachedSigns().size());
        assertEquals(new SavedBlockLockerChestSigns.Position(15,fixtureY,5),
            reversePair.attachedSigns().get(0).position());
        var unprotected=SavedBlockLockerChestSigns.observe(observedChunks,
            new SavedBlockLockerChestSigns.Position(3,fixtureY,4),
            Set.of("[Private]"),Set.of("[More Users]"),Set.of("Everyone"),
            PaperSavedBlockLockerSignText.nativeText());
        assertTrue(unprotected.attachedSigns().isEmpty(),
            "A chest with no applicable saved signs must have an empty local observation");
        var noProtectionSummary=SavedBlockLockerSignSummary.summarize(unprotected);
        assertFalse(noProtectionSummary.protectionSignObserved());
        assertFalse(noProtectionSummary.everyoneAbsent());
        System.out.println("PAPER_A04_SAVED_ATTACHED_SIGN_SUMMARY_PASSED"
            + " owner=2 everyone=2 unresolved=1 unprotected=1"
            + " (fixture-only; no full-source and writer proof)");
        System.out.println("PAPER_A04_SAVED_ATTACHED_CHEST_SIGNS_PASSED"
            + " protectedChests=5 unprotectedChests=1 reciprocalPair=1"
            + " chunks=2 (test fixture; not writer/global-source proof)");
        System.out.println("PAPER_A04_PRODUCT_SAVED_TERRAIN_PALETTE_PASSED signs=5 chests=7"
                + " chunks=2 doubleChest=true (fixture-only; G0 writer epoch unproved)");
        System.out.println("PAPER_A04_SAVED_EFFECTIVE_PROFILES_PASSED signs=5"
                + " knownOwners=2 everyone=2 unresolved=1 chunks=2"
                + " (fixture-only; no all-writer epoch or settlement publication)");
    }
}
