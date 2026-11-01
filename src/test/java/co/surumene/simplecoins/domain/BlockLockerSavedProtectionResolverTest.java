package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlockLockerSavedProtectionResolverTest {
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000067");
    private static final Set<String> PRIVATE = Set.of("[Private]");
    private static final Set<String> MORE = Set.of("[More Users]");
    private static final Set<String> EVERYONE = Set.of("Everyone");
    private static final BlockLockerSignAttachment.Position CHEST =
            new BlockLockerSignAttachment.Position(0, 64, 0);

    private static BlockLockerSavedSignCandidates.Evidence sign(String... lines) {
        return new BlockLockerSavedSignCandidates.Evidence(
                Optional.empty(), Optional.of(new BlockLockerSavedSignText.Evidence(
                        List.of(lines), Optional.empty(), true)));
    }

    private static Map<String, Object> wall(String facing) {
        return Map.of("Name", "minecraft:oak_wall_sign",
                "Properties", Map.of("facing", facing, "waterlogged", "false"));
    }

    private static ExternalOwnerDecision.Evidence single(
            Map<String, BlockLockerSavedSignCandidates.Evidence> signs,
            Map<String, Map<String, Object>> states) throws IOException {
        return BlockLockerSavedProtectionResolver.single(
                CHEST, signs, states, raw -> raw, PRIVATE, MORE, EVERYONE,
                name -> ExternalOwnerDecision.personal(OWNER));
    }

    @Test void attachedPrivateSignAndEveryoneAreResolvedFromMatchingSavedStates()
            throws Exception {
        var privateSign = sign("[Private]", "Alice", "", "");
        var more = sign("[More Users]", "[Everyone]", "", "");
        assertEquals(OWNER, single(
                Map.of("0,64,-1", privateSign),
                Map.of("0,64,-1", wall("north"))).owner());
        assertEquals(ExternalOwnerDecision.Kind.FREE, single(
                Map.of("0,64,-1", privateSign, "1,64,0", more),
                Map.of("0,64,-1", wall("north"), "1,64,0", wall("east"))).kind());
    }

    @Test void missingPalettePdcOnlyAndNoncanonicalPositionsFailClosed() {
        var privateSign = sign("[Private]", "Alice", "", "");
        assertThrows(IOException.class, () -> single(
                Map.of("0,64,-1", privateSign), Map.of()));
        var pdcOnly = new BlockLockerSavedSignCandidates.Evidence(
                Optional.of(new BlockLockerSavedHints.Hints(
                        "PRIVATE", Optional.of(OWNER), false)), Optional.empty());
        assertThrows(IOException.class, () -> single(
                Map.of("0,64,-1", pdcOnly), Map.of("0,64,-1", wall("north"))));
        assertThrows(IOException.class, () -> single(
                Map.of("00,64,-1", privateSign), Map.of("00,64,-1", wall("north"))));
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                assertDoesNotThrow(() -> single(Map.of(), Map.of())).kind());
    }

    @Test void reciprocalDoubleChestAcceptsSignOnEitherHalf() throws Exception {
        var first = new BlockLockerChestLinkage.Part(
                new BlockLockerChestLinkage.Position(0, 64, 0),
                BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.LEFT, BlockLockerChestLinkage.Kind.CHEST);
        var second = new BlockLockerChestLinkage.Part(
                new BlockLockerChestLinkage.Position(1, 64, 0),
                BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.RIGHT, BlockLockerChestLinkage.Kind.CHEST);
        assertEquals(OWNER, BlockLockerSavedProtectionResolver.doubleChest(
                first, second,
                Map.of("2,64,0", sign("[Private]", "Alice", "", "")),
                Map.of("2,64,0", wall("east")), raw -> raw,
                PRIVATE, MORE, EVERYONE,
                name -> ExternalOwnerDecision.personal(OWNER)).owner());
    }
}
