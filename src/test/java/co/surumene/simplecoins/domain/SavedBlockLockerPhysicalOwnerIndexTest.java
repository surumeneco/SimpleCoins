package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SavedBlockLockerPhysicalOwnerIndexTest {
    private static final UUID OWNER=UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final SavedBlockLockerOwnerCandidate.ExternalProof COMPLETE =
        new SavedBlockLockerOwnerCandidate.ExternalProof(true,true,true,true,false);

    private static SavedBlockLockerChestSigns.Observation protectedChest(int x) {
        return new SavedBlockLockerChestSigns.Observation(
            List.of(new SavedBlockLockerChestSigns.Position(x,65,4)),
            List.of(new SavedBlockLockerChestSigns.AttachedSign(
                new SavedBlockLockerChestSigns.Position(x,65,5),
                new BlockLockerSavedProfileResolution.SignProfiles(
                    BlockLockerHeaderMatcher.Type.PRIVATE,Optional.of(OWNER),false,true))));
    }

    @Test void confirmedOwnerIsIndexedByWorldAndPhysicalBlockPosition() throws Exception {
        var pending=new SavedBlockLockerPhysicalOwnerIndex.Builder();
        pending.accept("world",protectedChest(6),COMPLETE);
        var index=pending.finish();
        var found=index.lookup("world","6,65,4");
        assertEquals(OWNER,found.verifiedOwner().orElseThrow());
        assertFalse(found.everyoneEffective());
        assertFalse(found.verifiedNonProtection());
        assertThrows(java.io.IOException.class,()->index.lookup("world","9,65,4"),
            "An unobserved position is not evidence for Free");
        assertThrows(java.io.IOException.class,()->index.lookup("other-world","6,65,4"),
            "Matching coordinates in another world never inherit ownership");
    }

    @Test void reciprocalDoubleChestUsesOnePhysicalOwnerForBothHalves() throws Exception {
        var a=new SavedBlockLockerChestSigns.Position(15,65,4);
        var b=new SavedBlockLockerChestSigns.Position(16,65,4);
        var observed=new SavedBlockLockerChestSigns.Observation(List.of(a,b),
            protectedChest(15).attachedSigns());
        var pending=new SavedBlockLockerPhysicalOwnerIndex.Builder();
        pending.accept("world",observed,COMPLETE);
        var index=pending.finish();
        assertEquals(index.lookup("world","15,65,4"),index.lookup("world","16,65,4"));
        assertEquals(OWNER,index.lookup("world","16,65,4").verifiedOwner().orElseThrow());
        assertThrows(java.io.IOException.class,()->pending.accept("world",observed,COMPLETE),
            "A completed source index cannot be mutated");
    }

    @Test void repeatedPhysicalChestHalfInvalidatesWholePendingIndex() throws Exception {
        var a=new SavedBlockLockerChestSigns.Position(15,65,4);
        var b=new SavedBlockLockerChestSigns.Position(16,65,4);
        var first=new SavedBlockLockerChestSigns.Observation(List.of(a,b),
            protectedChest(15).attachedSigns());
        var second=new SavedBlockLockerChestSigns.Observation(List.of(b,a),
            protectedChest(15).attachedSigns());
        var pending=new SavedBlockLockerPhysicalOwnerIndex.Builder();
        pending.accept("world",first,COMPLETE);
        assertThrows(java.io.IOException.class,()->pending.accept("world",second,COMPLETE),
            "Opposite-half re-traversal is a duplicate physical source");
        assertThrows(java.io.IOException.class,pending::finish,
            "The previously staged half must not be exposed after failure");
    }

    @Test void equalCoordinatesInDifferentWorldsAreIndependentPhysicalKeys() throws Exception {
        var pending=new SavedBlockLockerPhysicalOwnerIndex.Builder();
        pending.accept("world",protectedChest(6),COMPLETE);
        pending.accept("another-world",protectedChest(6),COMPLETE);
        var index=pending.finish();
        assertEquals(OWNER,index.lookup("world","6,65,4").verifiedOwner().orElseThrow());
        assertEquals(OWNER,index.lookup("another-world","6,65,4").verifiedOwner().orElseThrow());
        assertThrows(java.io.IOException.class,()->index.lookup("world","06,65,4"));
    }

    @Test void everyoneAndProvenNonProtectionRemainDistinctFreeReasons() throws Exception {
        var pending=new SavedBlockLockerPhysicalOwnerIndex.Builder();
        var shared=new SavedBlockLockerChestSigns.Observation(
            List.of(new SavedBlockLockerChestSigns.Position(9,65,4)),
            List.of(new SavedBlockLockerChestSigns.AttachedSign(
                new SavedBlockLockerChestSigns.Position(9,65,5),
                new BlockLockerSavedProfileResolution.SignProfiles(
                    BlockLockerHeaderMatcher.Type.PRIVATE,Optional.empty(),true,false))));
        pending.accept("world",shared,COMPLETE);
        var empty=new SavedBlockLockerChestSigns.Observation(
            List.of(new SavedBlockLockerChestSigns.Position(12,65,4)),List.of());
        var nonProtection=new SavedBlockLockerOwnerCandidate.ExternalProof(true,true,true,true,true);
        pending.accept("world",empty,nonProtection);
        var index=pending.finish();
        var everyone=index.lookup("world","9,65,4");
        assertTrue(everyone.everyoneEffective());
        assertFalse(everyone.verifiedNonProtection());
        assertTrue(everyone.verifiedOwner().isEmpty());
        var unprotected=index.lookup("world","12,65,4");
        assertFalse(unprotected.everyoneEffective());
        assertTrue(unprotected.verifiedNonProtection());
        assertTrue(unprotected.verifiedOwner().isEmpty());
    }

    @Test void anyUnknownExternalSourceProofCannotEnterAnOwnerIndex() {
        var pending=new SavedBlockLockerPhysicalOwnerIndex.Builder();
        assertThrows(java.io.IOException.class,()->pending.accept("world",protectedChest(6),
            SavedBlockLockerOwnerCandidate.ExternalProof.unknown()));
        assertThrows(java.io.IOException.class,pending::finish);
        assertThrows(java.io.IOException.class,()->pending.accept("world",protectedChest(6),COMPLETE),
            "A failed transaction must not be resurrected");
    }
}
