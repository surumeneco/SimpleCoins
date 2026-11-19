package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;
import co.surumene.simplecoins.domain.WclOwnerEvidence;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaperWonderfulSavedCandidateOwnerTest {
    private static final UUID ENTITY = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID COMMANDER = UUID.fromString("00000000-0000-0000-0000-000000000111");

    @Test void savedCandidateExposesOnlyUnpublishedVerifiedOwnerEvidence() throws Exception {
        var candidate = new PaperWonderfulSavedChunkCandidates.Candidate(
                new WorldStorageDiscovery.SourceFile("world", Path.of("r.0.0.mca"),
                        WorldStorageDiscovery.Kind.ENTITY),
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                0, 0, ENTITY, "www", Map.of("commander", COMMANDER),
                Map.of("cargo:7", BigInteger.valueOf(3000)));
        assertEquals(WclOwnerEvidence.Kind.HOLD,
                candidate.ownerEvidence(false, WclOwnerEvidence.TameOwner.unknown()).kind());
        var positive = candidate.ownerEvidence(true, WclOwnerEvidence.TameOwner.unknown());
        assertEquals(WclOwnerEvidence.Kind.OWNER, positive.kind());
        assertEquals(COMMANDER, positive.owner());
        assertEquals(Map.of("cargo:7", BigInteger.valueOf(3000)), candidate.slotValues());
    }

    @Test void unknownChickenOwnerMustNotTurnIntoVerifiedFree() throws Exception {
        var candidate = new PaperWonderfulSavedChunkCandidates.Candidate(
                new WorldStorageDiscovery.SourceFile("world", Path.of("r.0.0.mca"),
                        WorldStorageDiscovery.Kind.ENTITY),
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                0, 0, ENTITY, "wwc", Map.of(),
                Map.of("shulker", BigInteger.valueOf(10000)));
        assertEquals(WclOwnerEvidence.Kind.HOLD,
                candidate.ownerEvidence(true, WclOwnerEvidence.TameOwner.unknown()).kind());
        assertEquals(WclOwnerEvidence.Kind.FREE,
                candidate.ownerEvidence(true, WclOwnerEvidence.TameOwner.verifiedAbsent()).kind());
    }

    @Test void savedCandidateReadsVanillaWolfOwnerWithoutGuessing() throws Exception {
        UUID owner = new UUID(0,42);
        var candidate = new PaperWonderfulSavedChunkCandidates.Candidate(
                new WorldStorageDiscovery.SourceFile("world", Path.of("r.0.0.mca"),
                        WorldStorageDiscovery.Kind.ENTITY),
                new UUID(0,10), 0,0, ENTITY, "www", Map.of("owner", owner),
                Map.of("cargo:7", BigInteger.valueOf(3000)));
        var chunk = new co.surumene.simplecoins.domain.AnvilRegionReader.Chunk(0,0,
                Map.of("Entities", java.util.List.of(
                        Map.of("id", "minecraft:wolf", "UUID", new int[]{0,0,0,153},
                               "Owner", new int[]{0,0,0,42}))));
        assertEquals(owner, candidate.savedOwnerEvidence(true, chunk).owner());
        assertEquals(WclOwnerEvidence.Kind.HOLD,
                candidate.savedOwnerEvidence(false, chunk).kind());
    }

    @Test void candidateCannotUseWrongChunkAsAffirmativeAbsence() throws Exception {
        var candidate = new PaperWonderfulSavedChunkCandidates.Candidate(
                new WorldStorageDiscovery.SourceFile("world", Path.of("r.0.0.mca"),
                        WorldStorageDiscovery.Kind.ENTITY),
                new UUID(0,10), 0,0, ENTITY, "wwc", Map.of(),
                Map.of("shulker", BigInteger.valueOf(10000)));
        var chunk = new co.surumene.simplecoins.domain.AnvilRegionReader.Chunk(1,0,
                Map.of("Entities", java.util.List.of(
                        Map.of("id", "minecraft:chicken", "UUID", new int[]{0,0,0,153}))));
        assertThrows(java.io.IOException.class,
                () -> candidate.savedOwnerEvidence(true, chunk));
    }
}
