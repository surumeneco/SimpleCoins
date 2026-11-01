package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlockLockerVisibleSignResolverTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000008");
    private static final BlockLockerSignAttachment.Position SIGN =
            new BlockLockerSignAttachment.Position(0, 64, -1);
    private static final BlockLockerSignAttachment.Position CHEST =
            new BlockLockerSignAttachment.Position(0, 64, 0);
    private static final Set<String> PRIVATE = Set.of("[Private]");
    private static final Set<String> MORE = Set.of("[More Users]");
    private static final Set<String> EVERYONE = Set.of("Everyone");

    private static BlockLockerProtectionResolution.VerifiedSign verify(
            List<String> lines, Optional<BlockLockerSavedHints.Hints> hints,
            BlockLockerVisibleSignResolver.PrimaryIdentity identity) throws IOException {
        return BlockLockerVisibleSignResolver.verify(
                SIGN, BlockLockerSignAttachment.Mount.WALL_NORTH,
                lines, hints, PRIVATE, MORE, EVERYONE, identity).orElseThrow();
    }

    @Test void verifiedPrivateIdentityAndMatchingSavedHintResolvePersonal() throws Exception {
        var hint = new BlockLockerSavedHints.Hints("PRIVATE", Optional.of(OWNER), false);
        var sign = verify(List.of("[Private]", "Alice", "", ""),
                Optional.of(hint), name -> ExternalOwnerDecision.personal(OWNER));
        assertEquals(OWNER, BlockLockerProtectionResolution.single(CHEST, List.of(sign)).owner());
    }

    @Test void unknownPrimaryRemainsUnknownEvenWithVisibleEveryone() throws Exception {
        var sign = verify(List.of("[Private]", "Alice", "[Everyone]", ""),
                Optional.empty(), name -> ExternalOwnerDecision.unknown());
        assertTrue(sign.everyone());
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                BlockLockerProtectionResolution.single(CHEST, List.of(sign)).kind());
    }

    @Test void savedUuidConflictCannotPromoteUnverifiedOwner() throws Exception {
        var hint = new BlockLockerSavedHints.Hints("PRIVATE", Optional.of(OTHER), false);
        var sign = verify(List.of("[Private]", "Alice", "", ""),
                Optional.of(hint), name -> ExternalOwnerDecision.personal(OWNER));
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                BlockLockerProtectionResolution.single(CHEST, List.of(sign)).kind());
    }

    @Test void legacyTextOnlySignMayUseAuthoritativeIdentityLookup() throws Exception {
        var sign = verify(List.of("[Private]", "Alice", "", ""),
                Optional.empty(), name -> {
                    assertEquals("Alice", name);
                    return ExternalOwnerDecision.personal(OWNER);
                });
        assertEquals(OWNER, BlockLockerProtectionResolution.single(CHEST, List.of(sign)).owner());
    }

    @Test void visibleEveryoneControlsMoreUsersRatherThanSavedPdcMarker() throws Exception {
        var primary = verify(List.of("[Private]", "Alice", "", ""),
                Optional.empty(), name -> ExternalOwnerDecision.personal(OWNER));
        var marker = new BlockLockerSavedHints.Hints("MORE_USERS", Optional.empty(), true);
        var ignored = verify(List.of("[More Users]", "Bob", "", ""),
                Optional.of(marker), name -> { throw new AssertionError("MORE_USERS has no primary"); });
        assertFalse(ignored.everyone());
        assertEquals(OWNER, BlockLockerProtectionResolution.single(
                CHEST, List.of(primary, ignored)).owner());
        var publicSign = verify(List.of("[More Users]", "[Everyone]", "", ""),
                Optional.of(marker), name -> { throw new AssertionError("MORE_USERS has no primary"); });
        assertTrue(publicSign.everyone());
        assertEquals(ExternalOwnerDecision.Kind.FREE,
                BlockLockerProtectionResolution.single(CHEST, List.of(primary, publicSign)).kind());
    }

    @Test void mismatchedHeadersAndMalformedVisibleLinesFailClosed() {
        var hint = new BlockLockerSavedHints.Hints("MORE_USERS", Optional.empty(), false);
        assertThrows(IOException.class, () -> verify(
                List.of("[Private]", "Alice", "", ""), Optional.of(hint),
                name -> ExternalOwnerDecision.personal(OWNER)));
        assertThrows(IOException.class, () -> verify(
                List.of("[Private]", "Alice"), Optional.empty(),
                name -> ExternalOwnerDecision.personal(OWNER)));
        assertThrows(IOException.class, () -> verify(
                Arrays.asList("[Private]", null, "", ""), Optional.empty(),
                name -> ExternalOwnerDecision.personal(OWNER)));
        assertThrows(IOException.class, () -> verify(
                List.of("[Private]", "Alice", "", ""), Optional.empty(),
                name -> { throw new IOException("Identity source unavailable"); }));
    }

    @Test void nonBlockLockerSignIsIgnoredUnlessSavedPdcClaimsProtection() throws Exception {
        assertTrue(BlockLockerVisibleSignResolver.verify(
                SIGN, BlockLockerSignAttachment.Mount.WALL_NORTH,
                List.of("Hello", "Alice", "", ""), Optional.empty(),
                PRIVATE, MORE, EVERYONE, name -> ExternalOwnerDecision.personal(OWNER)).isEmpty());
        var hint = new BlockLockerSavedHints.Hints("PRIVATE", Optional.of(OWNER), false);
        assertThrows(IOException.class, () -> verify(
                List.of("Hello", "Alice", "", ""), Optional.of(hint),
                name -> ExternalOwnerDecision.personal(OWNER)));
    }
}
