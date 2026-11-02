package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExternalOwnerDecisionTest {
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-00000000002a");

    private static BanEvidence.Decision ban(BanEvidence.Status status) throws IOException {
        return BanEvidence.evaluate(Set.of("paper"),
                List.of(new BanEvidence.Source("paper", status)));
    }

    @Test void unavailableExternalOwnerCannotBecomeFreeOrPersonal() {
        var unknown = ExternalOwnerDecision.unknown();
        assertThrows(IOException.class, () -> ExternalOwnerDecision.toBlock(
                unknown, ban(BanEvidence.Status.CLEAR), true));
        assertThrows(IOException.class, () -> ExternalOwnerDecision.toEntity(
                unknown, ban(BanEvidence.Status.CLEAR), true));
    }

    @Test void verifiedFreeAndPersonalDecisionsConvertWithoutInventingOwner() throws Exception {
        var free = ExternalOwnerDecision.toBlock(ExternalOwnerDecision.free(),
                ban(BanEvidence.Status.UNKNOWN), false);
        assertNull(free.owner());
        assertFalse(free.currentlyBanned());
        assertFalse(free.hasPlayerdata());

        var personal = ExternalOwnerDecision.toEntity(
                ExternalOwnerDecision.personal(OWNER), ban(BanEvidence.Status.BANNED), true);
        assertEquals(OWNER, personal.owner());
        assertTrue(personal.currentlyBanned());
        assertTrue(personal.hasPlayerdata());
    }

    @Test void unknownBanStatusCannotBeCoercedToClearForPersonalOwner() {
        assertThrows(IOException.class, () -> ExternalOwnerDecision.toBlock(
                ExternalOwnerDecision.personal(OWNER), ban(BanEvidence.Status.UNKNOWN), true));
        assertThrows(IOException.class, () -> ExternalOwnerDecision.toEntity(
                ExternalOwnerDecision.personal(OWNER), null, true));
    }

    @Test void malformedOwnerEvidenceIsRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () ->
                new ExternalOwnerDecision.Evidence(ExternalOwnerDecision.Kind.PERSONAL, null));
        assertThrows(IllegalArgumentException.class, () ->
                new ExternalOwnerDecision.Evidence(ExternalOwnerDecision.Kind.FREE, OWNER));
    }
    @Test void unknownOwnerOfRealCoinInventoryAbortsSettlementInsteadOfCountingZero() throws Exception {
        Map<String,Object> coin = Map.of("Slot", (byte) 0,
                "id", "minecraft:paper", "count", 1,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        Map<String,Object> chest = Map.of("id", "minecraft:chest",
                "x", 1, "y", 64, "z", 2, "Items", List.of(coin));
        Map<String,Object> chunk = Map.of("block_entities", List.of(chest));

        var unknownSettlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("region:file"));
        assertThrows(IOException.class, () -> BlockEntitySettlementImport.stageChunk(
                unknownSettlement, "region:file", "world", chunk,
                position -> ExternalOwnerDecision.toBlock(ExternalOwnerDecision.unknown(),
                        ban(BanEvidence.Status.UNKNOWN), false)));
        assertThrows(IllegalStateException.class, unknownSettlement::commit);

        var freeSettlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("region:file"));
        BlockEntitySettlementImport.stageChunk(freeSettlement, "region:file", "world", chunk,
                position -> ExternalOwnerDecision.toBlock(ExternalOwnerDecision.free(),
                        ban(BanEvidence.Status.UNKNOWN), false));
        freeSettlement.completeSource("region:file");
        assertEquals(BigInteger.valueOf(100), freeSettlement.commit().free());
    }

    @Test void missingRequiredExternalBanSourceCannotPublishPersonalAssets() throws Exception {
        var incomplete = BanEvidence.evaluate(Set.of("paper", "external"), List.of(
                new BanEvidence.Source("paper", BanEvidence.Status.CLEAR)));
        assertEquals(BanEvidence.Status.UNKNOWN, incomplete.status());
        assertThrows(IOException.class, () -> ExternalOwnerDecision.toEntity(
                ExternalOwnerDecision.personal(OWNER), incomplete, true));

        var complete = BanEvidence.evaluate(Set.of("paper", "external"), List.of(
                new BanEvidence.Source("paper", BanEvidence.Status.CLEAR),
                new BanEvidence.Source("external", BanEvidence.Status.CLEAR)));
        var resolved = ExternalOwnerDecision.toEntity(
                ExternalOwnerDecision.personal(OWNER), complete, true);
        assertEquals(OWNER, resolved.owner());
        assertFalse(resolved.currentlyBanned());
    }

    @Test void rawStatusCannotServeAsSettlementProof() {
        assertThrows(NoSuchMethodException.class, () -> ExternalOwnerDecision.class.getMethod(
                "toBlock", ExternalOwnerDecision.Evidence.class,
                BanEvidence.Status.class, boolean.class));
        assertThrows(NoSuchMethodException.class, () -> ExternalOwnerDecision.class.getMethod(
                "toEntity", ExternalOwnerDecision.Evidence.class,
                BanEvidence.Status.class, boolean.class));
    }


}
