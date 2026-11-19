package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;
import co.surumene.simplecoins.domain.BanEvidence;
import co.surumene.simplecoins.domain.BanSourceCoverage;
import co.surumene.simplecoins.domain.HybridLiveSourceStage;
import co.surumene.simplecoins.domain.SavedLiveSourcePartition;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaperBlockLockerLiveClassifierTest {
    private static final UUID OWNER =
            UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final String SOURCE_KEY = "world/world/6,70,4:0";
    private static HybridLiveSourceStage.ChunkSource source() {
        return new HybridLiveSourceStage.ChunkSource(
                new SavedLiveSourcePartition.Chunk("world", 0, 0), List.of());
    }
    private static HybridLiveSourceStage.Value value() {
        return new HybridLiveSourceStage.Value(SOURCE_KEY, "world",
                BigInteger.valueOf(10000));
    }
    private static BanSourceCoverage bans(BanEvidence.Status status) throws IOException {
        return BanSourceCoverage.capture(Set.of("observed-provider"),
                Map.of(OWNER, List.of(new BanEvidence.Source("observed-provider", status))));
    }

    @Test void ownerAndOriginalMoneyIdentityArePreserved() throws Exception {
        var classified = PaperBlockLockerLiveClassifier.classifyEvidence(
                source(), value(), new BlockLockerLiveEvidence.Ownership(OWNER, false),
                bans(BanEvidence.Status.CLEAR), Set.of(OWNER));
        assertEquals(source().id(), classified.source());
        assertEquals(SOURCE_KEY, classified.physicalKey());
        assertEquals("world", classified.world());
        assertEquals(BigInteger.valueOf(10000), classified.value());
        assertEquals(OWNER, classified.owner());
        assertFalse(classified.everyoneProtected());
        assertFalse(classified.currentlyBanned());
        assertTrue(classified.hasPlayerdata());
    }

    @Test void anyoneOrMissingProtectionIsFreeWithoutInventedOwner() throws Exception {
        for (var evidence : List.of(new BlockLockerLiveEvidence.Ownership(null, true),
                                    new BlockLockerLiveEvidence.Ownership(null, false))) {
            var asset = PaperBlockLockerLiveClassifier.classifyEvidence(
                    source(), value(), evidence,
                    BanSourceCoverage.capture(Set.of("observed-provider"), Map.of()),
                    Set.of());
            assertNull(asset.owner());
            assertFalse(asset.currentlyBanned());
            assertFalse(asset.hasPlayerdata());
        }
    }

    @Test void bannedOrMissingPlayerdataIsDeadNotFree() throws Exception {
        var banned = PaperBlockLockerLiveClassifier.classifyEvidence(
                source(), value(), new BlockLockerLiveEvidence.Ownership(OWNER, false),
                bans(BanEvidence.Status.BANNED), Set.of(OWNER));
        assertTrue(banned.currentlyBanned());
        var absentData = PaperBlockLockerLiveClassifier.classifyEvidence(
                source(), value(), new BlockLockerLiveEvidence.Ownership(OWNER, false),
                bans(BanEvidence.Status.CLEAR), Set.of());
        assertFalse(absentData.hasPlayerdata());
        assertEquals(OWNER, absentData.owner());
    }

    @Test void incompleteBanMustNotTurnProtectedCurrencyIntoFree() throws Exception {
        var missing = BanSourceCoverage.capture(Set.of("observed-provider"), Map.of());
        assertThrows(IOException.class, () -> PaperBlockLockerLiveClassifier.classifyEvidence(
                source(), value(), new BlockLockerLiveEvidence.Ownership(OWNER, false),
                missing, Set.of()));
        assertThrows(IOException.class, () -> bans(BanEvidence.Status.UNKNOWN));
    }
}
