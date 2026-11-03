package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** CAT Draft BL-ORDER-01..04: characterization of pinned saved-sign aggregation. */
class BlockLockerSavedProtectionOrderTest {
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000067");
    private static final BlockLockerSignAttachment.Position CHEST =
            new BlockLockerSignAttachment.Position(0, 64, 0);
    private static final String PRIMARY = "0,64,-1";
    private static final String MORE_USERS = "1,64,0";
    private static final String DETACHED = "20,64,20";

    private static BlockLockerSavedSignCandidates.Evidence sign(
            String header, String line2) {
        return new BlockLockerSavedSignCandidates.Evidence(
                Optional.empty(), Optional.of(new BlockLockerSavedSignText.Evidence(
                        List.of(header, line2, "", ""), Optional.empty(), true)));
    }

    private static Map<String, Object> wall(String facing) {
        return Map.of("Name", "minecraft:oak_wall_sign",
                "Properties", Map.of("facing", facing, "waterlogged", "false"));
    }

    private static ExternalOwnerDecision.Evidence resolve(
            Map<String, BlockLockerSavedSignCandidates.Evidence> candidates,
            Map<String, Map<String, Object>> palette,
            boolean knownOwner, String... order) throws IOException {
        var orderedSigns = new LinkedHashMap<String, BlockLockerSavedSignCandidates.Evidence>();
        var orderedStates = new LinkedHashMap<String, Map<String, Object>>();
        for (String key : order) {
            orderedSigns.put(key, candidates.get(key));
            orderedStates.put(key, palette.get(key));
        }
        return BlockLockerSavedProtectionResolver.single(
                CHEST, orderedSigns, orderedStates, raw -> raw,
                Set.of("[Private]"), Set.of("[More Users]"), Set.of("Everyone"),
                name -> knownOwner
                        ? ExternalOwnerDecision.personal(OWNER)
                        : ExternalOwnerDecision.unknown());
    }

    @Test void attachedEveryoneIsFreeInEitherSavedMapOrder() throws Exception {
        var signs = Map.of(
                PRIMARY, sign("[Private]", "Alice"),
                MORE_USERS, sign("[More Users]", "[Everyone]"));
        var states = Map.of(PRIMARY, wall("north"), MORE_USERS, wall("east"));
        assertEquals(ExternalOwnerDecision.Kind.FREE,
                resolve(signs, states, true, PRIMARY, MORE_USERS).kind());
        assertEquals(ExternalOwnerDecision.Kind.FREE,
                resolve(signs, states, true, MORE_USERS, PRIMARY).kind());
    }

    @Test void unresolvedPrimaryRemainsUnknownInEitherOrder() throws Exception {
        var signs = Map.of(
                PRIMARY, sign("[Private]", "Alice"),
                MORE_USERS, sign("[More Users]", "[Everyone]"));
        var states = Map.of(PRIMARY, wall("north"), MORE_USERS, wall("east"));
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                resolve(signs, states, false, PRIMARY, MORE_USERS).kind());
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                resolve(signs, states, false, MORE_USERS, PRIMARY).kind());
    }

    @Test void twoAttachedPrivateSignsAreRejectedInEitherOrder() {
        var signs = Map.of(
                PRIMARY, sign("[Private]", "Alice"),
                MORE_USERS, sign("[Private]", "Bob"));
        var states = Map.of(PRIMARY, wall("north"), MORE_USERS, wall("east"));
        assertThrows(IOException.class,
                () -> resolve(signs, states, true, PRIMARY, MORE_USERS));
        assertThrows(IOException.class,
                () -> resolve(signs, states, true, MORE_USERS, PRIMARY));
    }

    @Test void detachedEveryoneCannotOverrideVerifiedOwnerInEitherOrder()
            throws Exception {
        var signs = Map.of(
                PRIMARY, sign("[Private]", "Alice"),
                DETACHED, sign("[More Users]", "[Everyone]"));
        var states = Map.of(PRIMARY, wall("north"), DETACHED, wall("north"));
        assertEquals(OWNER, resolve(signs, states, true, PRIMARY, DETACHED).owner());
        assertEquals(OWNER, resolve(signs, states, true, DETACHED, PRIMARY).owner());
    }
}
