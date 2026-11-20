package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SavedBlockLockerOwnerCandidateTest {
    private static final UUID OWNER = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static SavedBlockLockerChestSigns.Observation observed(
            BlockLockerHeaderMatcher.Type type, UUID owner, boolean everyone) {
        var signs = type == null ? List.<SavedBlockLockerChestSigns.AttachedSign>of()
            : List.of(new SavedBlockLockerChestSigns.AttachedSign(
                new SavedBlockLockerChestSigns.Position(6,65,5),
                new BlockLockerSavedProfileResolution.SignProfiles(type,
                    Optional.ofNullable(owner),everyone,!everyone)));
        return new SavedBlockLockerChestSigns.Observation(
            List.of(new SavedBlockLockerChestSigns.Position(6,65,4)),signs);
    }

    @Test void ownerCandidateRequiresAllIndependentSourceProofs() throws Exception {
        var source=observed(BlockLockerHeaderMatcher.Type.PRIVATE,OWNER,false);
        var unverified=SavedBlockLockerOwnerCandidate.fromObservedSigns(source,
            SavedBlockLockerOwnerCandidate.ExternalProof.unknown());
        assertEquals(BlockLockerSavedOwnerVerdict.Verdict.HOLD,unverified.verdict());
        assertTrue(unverified.verifiedOwner().isEmpty());

        var complete=new SavedBlockLockerOwnerCandidate.ExternalProof(true,true,true,true,false);
        var candidate=SavedBlockLockerOwnerCandidate.fromObservedSigns(source,complete);
        assertEquals(BlockLockerSavedOwnerVerdict.Verdict.OWNER_UNPUBLISHED,candidate.verdict());
        assertEquals(OWNER,candidate.verifiedOwner().orElseThrow());

        for (int missing=0;missing<4;missing++) {
            var incomplete=new SavedBlockLockerOwnerCandidate.ExternalProof(
                missing!=0, missing!=1, missing!=2, missing!=3, false);
            assertEquals(BlockLockerSavedOwnerVerdict.Verdict.HOLD,
                SavedBlockLockerOwnerCandidate.fromObservedSigns(source,incomplete).verdict(),
                "One missing independent proof must block promotion at predicate "+missing);
        }
    }
}
