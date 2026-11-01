package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlockLockerProtectionResolutionTest {
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final BlockLockerSignAttachment.Position CHEST =
            new BlockLockerSignAttachment.Position(0, 64, 0);

    private static BlockLockerProtectionResolution.VerifiedSign primary(
            ExternalOwnerDecision.Evidence owner) {
        return new BlockLockerProtectionResolution.VerifiedSign(
                new BlockLockerSignAttachment.Position(0, 64, -1),
                BlockLockerSignAttachment.Mount.WALL_NORTH,
                BlockLockerHeaderMatcher.Type.PRIVATE, owner, false);
    }

    private static BlockLockerProtectionResolution.VerifiedSign everyone(int x, int z) {
        return new BlockLockerProtectionResolution.VerifiedSign(
                new BlockLockerSignAttachment.Position(x, 64, z),
                BlockLockerSignAttachment.Mount.WALL_EAST,
                BlockLockerHeaderMatcher.Type.MORE_USERS, null, true);
    }

    @Test void attachedEveryoneOverridesPrimaryButUnattachedSignDoesNot() throws Exception {
        var owner = primary(ExternalOwnerDecision.personal(OWNER));
        assertEquals(ExternalOwnerDecision.Kind.FREE,
                BlockLockerProtectionResolution.single(
                        CHEST, List.of(owner, everyone(1, 0))).kind());
        assertEquals(OWNER, BlockLockerProtectionResolution.single(
                CHEST, List.of(owner, everyone(20, 20))).owner());
    }

    @Test void missingPrimaryOrUnresolvedIdentityCannotProveFree() throws Exception {
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                BlockLockerProtectionResolution.single(CHEST, List.of(everyone(1, 0))).kind());
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                BlockLockerProtectionResolution.single(CHEST, List.of()).kind());
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                BlockLockerProtectionResolution.single(
                        CHEST, List.of(primary(ExternalOwnerDecision.unknown()))).kind());
        assertThrows(IllegalArgumentException.class, () ->
                primary(ExternalOwnerDecision.free()));
    }

    @Test void duplicateAttachedPrivateSignsAndMalformedEvidenceFailClosed() {
        var owner = primary(ExternalOwnerDecision.personal(OWNER));
        assertThrows(IOException.class, () -> BlockLockerProtectionResolution.single(
                CHEST, List.of(owner, owner)));
        assertThrows(IOException.class, () -> BlockLockerProtectionResolution.single(
                null, List.of(owner)));
        assertThrows(IOException.class, () -> BlockLockerProtectionResolution.single(
                CHEST, null));
        assertThrows(IllegalArgumentException.class, () ->
                new BlockLockerProtectionResolution.VerifiedSign(
                        owner.position(), owner.mount(),
                        BlockLockerHeaderMatcher.Type.MORE_USERS,
                        ExternalOwnerDecision.personal(OWNER), false));
    }

    @Test void reciprocalDoubleChestSharesOneOwnershipDecision() throws Exception {
        var left = new BlockLockerChestLinkage.Part(
                new BlockLockerChestLinkage.Position(0, 64, 0),
                BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.LEFT,
                BlockLockerChestLinkage.Kind.CHEST);
        var right = new BlockLockerChestLinkage.Part(
                new BlockLockerChestLinkage.Position(1, 64, 0),
                BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.RIGHT,
                BlockLockerChestLinkage.Kind.CHEST);
        var signOnRight = new BlockLockerProtectionResolution.VerifiedSign(
                new BlockLockerSignAttachment.Position(2, 64, 0),
                BlockLockerSignAttachment.Mount.WALL_EAST,
                BlockLockerHeaderMatcher.Type.PRIVATE,
                ExternalOwnerDecision.personal(OWNER), false);
        assertEquals(OWNER, BlockLockerProtectionResolution.doubleChest(
                left, right, List.of(signOnRight)).owner());
        var notAdjacent = new BlockLockerChestLinkage.Part(
                new BlockLockerChestLinkage.Position(3, 64, 0),
                BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.RIGHT,
                BlockLockerChestLinkage.Kind.CHEST);
        assertThrows(IOException.class, () -> BlockLockerProtectionResolution.doubleChest(
                left, notAdjacent, List.of(signOnRight)));
    }
    @Test void resolvedEveryoneStagesRealCoinAsFreeButMissingPrimaryBlocksCommit()
            throws Exception {
        var coin = java.util.Map.of("Slot", (byte) 0,
                "id", "minecraft:paper", "count", 1,
                "components", java.util.Map.of("minecraft:custom_data",
                        java.util.Map.of("PublicBukkitValues",
                                java.util.Map.of("simplecoins:denomination", 100))));
        var chest = java.util.Map.of("id", "minecraft:chest",
                "x", 0, "y", 64, "z", 0, "Items", java.util.List.of(coin));
        java.util.Map<String, Object> chunk = java.util.Map.of(
                "block_entities", java.util.List.of(chest));
        var verifiedFree = BlockLockerProtectionResolution.single(CHEST,
                List.of(primary(ExternalOwnerDecision.personal(OWNER)), everyone(1, 0)));
        var accumulator = new SettlementAccumulator(
                new WorldScope(true, java.util.Set.of(), java.util.Set.of()),
                java.util.Set.of("region:file"));
        BlockEntitySettlementImport.stageChunk(accumulator, "region:file", "world", chunk,
                position -> ExternalOwnerDecision.toBlock(verifiedFree, null, false));
        accumulator.completeSource("region:file");
        assertEquals(java.math.BigInteger.valueOf(100), accumulator.commit().free());

        var unknown = BlockLockerProtectionResolution.single(CHEST,
                List.of(everyone(1, 0)));
        var incomplete = new SettlementAccumulator(
                new WorldScope(true, java.util.Set.of(), java.util.Set.of()),
                java.util.Set.of("region:file"));
        assertThrows(IOException.class, () -> BlockEntitySettlementImport.stageChunk(
                incomplete, "region:file", "world", chunk,
                position -> ExternalOwnerDecision.toBlock(unknown, null, false)));
        assertThrows(IllegalStateException.class, incomplete::commit);
    }

}
