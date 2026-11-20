package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SavedBlockLockerSignSummaryTest {
    private static final UUID A=UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID B=UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static SavedBlockLockerChestSigns.AttachedSign sign(
            int x, BlockLockerHeaderMatcher.Type type, UUID owner, boolean everyone) {
        return new SavedBlockLockerChestSigns.AttachedSign(
            new SavedBlockLockerChestSigns.Position(x,65,5),
            new BlockLockerSavedProfileResolution.SignProfiles(type,
                Optional.ofNullable(owner),everyone,!everyone));
    }
    private static SavedBlockLockerChestSigns.Observation observation(
            SavedBlockLockerChestSigns.AttachedSign... signs) {
        return new SavedBlockLockerChestSigns.Observation(
            List.of(new SavedBlockLockerChestSigns.Position(6,65,4)),
            List.of(signs));
    }
    @Test void oneVerifiedMainOwnerAndNoEveryone() throws Exception {
        var s=SavedBlockLockerSignSummary.summarize(observation(
            sign(6,BlockLockerHeaderMatcher.Type.PRIVATE,A,false)));
        assertTrue(s.protectionSignObserved());
        assertEquals(A,s.verifiedMainOwner().orElseThrow());
        assertFalse(s.everyoneEffective());
        assertTrue(s.everyoneAbsent());
        assertFalse(s.mainOwnerConflict());
    }
    @Test void everyoneOnAdditionalSignAppliesAcrossTheWholeProtection() throws Exception {
        var s=SavedBlockLockerSignSummary.summarize(observation(
            sign(6,BlockLockerHeaderMatcher.Type.PRIVATE,A,false),
            sign(7,BlockLockerHeaderMatcher.Type.MORE_USERS,null,true)));
        assertEquals(A,s.verifiedMainOwner().orElseThrow());
        assertTrue(s.everyoneEffective());
        assertFalse(s.everyoneAbsent());
        assertFalse(s.mainOwnerConflict());
    }
    @Test void conflictingPrivateOwnerUuidsMustNotBeArbitrarilyChosen() throws Exception {
        var s=SavedBlockLockerSignSummary.summarize(observation(
            sign(6,BlockLockerHeaderMatcher.Type.PRIVATE,A,false),
            sign(7,BlockLockerHeaderMatcher.Type.PRIVATE,B,false)));
        assertTrue(s.mainOwnerConflict());
        assertTrue(s.verifiedMainOwner().isEmpty());
        assertTrue(s.everyoneAbsent());
    }
    @Test void unknownMainAlongsideVerifiedOwnerIsNotAProvedOwner() throws Exception {
        var s=SavedBlockLockerSignSummary.summarize(observation(
            sign(6,BlockLockerHeaderMatcher.Type.PRIVATE,null,false),
            sign(7,BlockLockerHeaderMatcher.Type.PRIVATE,A,false)));
        assertTrue(s.verifiedMainOwner().isEmpty());
        assertTrue(s.protectionSignObserved());
    }
    @Test void extraUserOnlyIsNotAValidatedPrimaryOwner() throws Exception {
        var s=SavedBlockLockerSignSummary.summarize(observation(
            sign(6,BlockLockerHeaderMatcher.Type.MORE_USERS,A,false)));
        assertTrue(s.protectionSignObserved());
        assertTrue(s.verifiedMainOwner().isEmpty());
        assertTrue(s.everyoneAbsent());
    }
    @Test void emptyAttachedSignObservationDoesNotProveUnprotectedWorld() throws Exception {
        var s=SavedBlockLockerSignSummary.summarize(observation());
        assertFalse(s.protectionSignObserved());
        assertFalse(s.everyoneEffective());
        assertFalse(s.everyoneAbsent());
        assertTrue(s.verifiedMainOwner().isEmpty());
    }
    @Test void duplicatedAttachedSignCoordinatesFailClosed() {
        var item=sign(6,BlockLockerHeaderMatcher.Type.PRIVATE,A,false);
        assertThrows(IOException.class,()->SavedBlockLockerSignSummary.summarize(
            observation(item,item)));
    }
}
