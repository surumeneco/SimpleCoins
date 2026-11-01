package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** TASK-20261101-004: visible legacy formatting is not a profile identity. */
class BlockLockerPrimaryNameNormalizationTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final BlockLockerSignAttachment.Position SIGN =
            new BlockLockerSignAttachment.Position(0, 64, -1);
    private static final BlockLockerSignAttachment.Position CHEST =
            new BlockLockerSignAttachment.Position(0, 64, 0);

    private static Optional<BlockLockerProtectionResolution.VerifiedSign> verify(
            String name, BlockLockerVisibleSignResolver.PrimaryIdentity lookup)
            throws java.io.IOException {
        return BlockLockerVisibleSignResolver.verify(
                SIGN, BlockLockerSignAttachment.Mount.WALL_NORTH,
                List.of("[Private]", name, "[Everyone]", ""),
                Optional.empty(), Set.of("[Private]"), Set.of("[More Users]"),
                Set.of("Everyone"), lookup);
    }

    @Test void legacyColorCodesDoNotBecomePartOfProfileLookupKey() throws Exception {
        var sign = verify("§a Alice §r", name -> {
            assertEquals("Alice", name);
            return ExternalOwnerDecision.personal(OWNER);
        }).orElseThrow();
        assertEquals(ExternalOwnerDecision.Kind.FREE,
                BlockLockerProtectionResolution.single(CHEST, List.of(sign)).kind());
    }

    @Test void formattingOnlyProfileRemainsUnknownDespiteEveryone() throws Exception {
        var sign = verify("§a §r", name -> {
            throw new AssertionError("Formatting-only profile must not resolve an owner");
        }).orElseThrow();
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                BlockLockerProtectionResolution.single(CHEST, List.of(sign)).kind());
    }

    @Test void unformattedProfileStillResolves() throws Exception {
        var sign = verify("Alice", name -> {
            assertEquals("Alice", name);
            return ExternalOwnerDecision.personal(OWNER);
        }).orElseThrow();
        assertEquals(OWNER, sign.primary().owner());
    }
}
