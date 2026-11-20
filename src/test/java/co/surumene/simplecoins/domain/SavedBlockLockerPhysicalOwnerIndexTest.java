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
}
