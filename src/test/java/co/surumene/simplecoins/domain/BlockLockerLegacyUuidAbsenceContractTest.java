package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * CAT Draft BL-LEGACY-01/02. These are contract-discovery tests, not an
 * authorization to map generic lookup failures to FREE. The first test is
 * deliberately Red against PR #8 until a proof-bearing identity path exists.
 */
class BlockLockerLegacyUuidAbsenceContractTest {
    private static final BlockLockerSignAttachment.Position SIGN =
            new BlockLockerSignAttachment.Position(0, 64, -1);
    private static final BlockLockerSignAttachment.Position CHEST =
            new BlockLockerSignAttachment.Position(0, 64, 0);

    private static BlockLockerProtectionResolution.VerifiedSign legacy(
            BlockLockerVisibleSignResolver.PrimaryIdentity identity) throws Exception {
        // The caller is assumed to have verified this old-format sign's
        // physical attachment and coherent saved-text snapshot.
        var text = new BlockLockerSavedSignText.Evidence(
                List.of("[Private]", "Alice", "", ""), Optional.empty(), false);
        var saved = new BlockLockerSavedSignCandidates.Evidence(
                Optional.empty(), Optional.of(text));
        return BlockLockerVisibleSignResolver.fromSaved(
                SIGN, BlockLockerSignAttachment.Mount.WALL_NORTH,
                saved, raw -> raw, Set.of("[Private]"), Set.of("[More Users]"),
                Set.of("Everyone"), identity).orElseThrow();
    }

    @Test
    void verifiedLegacyUuidAbsenceNeedsDistinctFreeOutcome() throws Exception {
        // Candidate input: verified absence from an authoritative identity
        // source, not an unavailable API. Current API has no proof tag.
        var sign = legacy(name -> {
            assertEquals("Alice", name);
            return ExternalOwnerDecision.free();
        });
        assertEquals(ExternalOwnerDecision.Kind.FREE,
                BlockLockerProtectionResolution.single(CHEST, List.of(sign)).kind(),
                "02B legacy verified UUID-unresolvable rule is not represented");
    }

    @Test
    void unavailableIdentityMustNeverBeCoercedToFree() throws Exception {
        var sign = legacy(name -> ExternalOwnerDecision.unknown());
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                BlockLockerProtectionResolution.single(CHEST, List.of(sign)).kind());
    }
}
