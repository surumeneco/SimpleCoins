package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;
import co.surumene.simplecoins.domain.AnvilRegionReader;
import co.surumene.simplecoins.domain.BlockLockerHeaderMatcher;
import co.surumene.simplecoins.domain.BlockLockerSavedProfileResolution;
import co.surumene.simplecoins.domain.SavedTerrainBlockPalette;
import co.surumene.simplecoins.domain.SavedBlockLockerAttachedProtection;
import co.surumene.simplecoins.domain.BlockLockerSignAttachment;
import co.surumene.simplecoins.domain.BlockLockerChestLinkage;
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
        var selected=Map.of(
                new SavedBlockLockerAttachedProtection.ChunkKey(0,0),terrain.get(0),
                new SavedBlockLockerAttachedProtection.ChunkKey(1,0),terrain.get(1));
        var plain=SavedBlockLockerAttachedProtection.observe(
                selected,3,fixtureY,4,Set.of("[Private]"),Set.of("[More Users]"),
                Set.of("Everyone"),PaperSavedBlockLockerSignText.nativeText());
        assertFalse(plain.hasProtectionSign());
        assertTrue(plain.primaryOwner().isEmpty());
        assertFalse(plain.everyoneEffective());
        assertEquals(1,plain.parts().size());

        var personal=SavedBlockLockerAttachedProtection.observe(
                selected,6,fixtureY,4,Set.of("[Private]"),Set.of("[More Users]"),
                Set.of("Everyone"),PaperSavedBlockLockerSignText.nativeText());
        assertTrue(personal.hasProtectionSign());
        assertEquals(OWNER,personal.primaryOwner().orElseThrow());
        assertFalse(personal.everyoneEffective());
        assertEquals(1,personal.appliedSigns().size());

        var shared=SavedBlockLockerAttachedProtection.observe(
                selected,9,fixtureY,4,Set.of("[Private]"),Set.of("[More Users]"),
                Set.of("Everyone"),PaperSavedBlockLockerSignText.nativeText());
        assertTrue(shared.everyoneEffective());
        assertTrue(shared.primaryOwner().isEmpty());

        var unresolved=SavedBlockLockerAttachedProtection.observe(
                selected,12,fixtureY,4,Set.of("[Private]"),Set.of("[More Users]"),
                Set.of("Everyone"),PaperSavedBlockLockerSignText.nativeText());
        assertTrue(unresolved.hasProtectionSign());
        assertTrue(unresolved.primaryOwner().isEmpty());
        assertFalse(unresolved.everyoneEffective());

        var leftPair=SavedBlockLockerAttachedProtection.observe(
                selected,15,fixtureY,4,Set.of("[Private]"),Set.of("[More Users]"),
                Set.of("Everyone"),PaperSavedBlockLockerSignText.nativeText());
        var rightPair=SavedBlockLockerAttachedProtection.observe(
                selected,16,fixtureY,4,Set.of("[Private]"),Set.of("[More Users]"),
                Set.of("Everyone"),PaperSavedBlockLockerSignText.nativeText());
        assertEquals(2,leftPair.parts().size());
        assertEquals(leftPair.parts(),rightPair.parts());
        assertEquals(leftPair.appliedSigns(),rightPair.appliedSigns());
        assertEquals(OWNER,leftPair.primaryOwner().orElseThrow());
        assertEquals(OWNER,rightPair.primaryOwner().orElseThrow());

        var sharedBack=SavedBlockLockerAttachedProtection.observe(
                selected,21,fixtureY,4,Set.of("[Private]"),Set.of("[More Users]"),
                Set.of("Everyone"),PaperSavedBlockLockerSignText.nativeText());
        assertTrue(sharedBack.everyoneEffective());
        assertEquals(1,sharedBack.appliedSigns().size());
        System.out.println("PAPER_A04_SAVED_PHYSICAL_ATTACHMENT_OBSERVED_PASSED"
                + " parts=7 signs=5 groupCount=6 ownerGroups=2 everyoneGroups=2"
                + " unresolvedGroups=1 unprotectedGroups=1 doubleChestReciprocal=true"
                + " (fixture-only; all-world writer generation NOT verified)");
        System.out.println("PAPER_A04_PRODUCT_SAVED_TERRAIN_PALETTE_PASSED signs=5 chests=7"
                + " chunks=2 doubleChest=true (fixture-only; G0 writer epoch unproved)");
        System.out.println("PAPER_A04_SAVED_EFFECTIVE_PROFILES_PASSED signs=5"
                + " knownOwners=2 everyone=2 unresolved=1 chunks=2"
                + " (fixture-only; no all-writer epoch or settlement publication)");
    }
}
