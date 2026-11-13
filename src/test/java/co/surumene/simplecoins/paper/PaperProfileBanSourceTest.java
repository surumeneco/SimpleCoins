package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.BanEvidence;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;

class PaperProfileBanSourceTest {
    private static final UUID PLAYER =
            UUID.fromString("00000000-0000-0000-0000-0000000000a5");

    private static OfflinePlayer profile(UUID id, Boolean banned) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> id;
            case "isBanned" -> {
                if (banned == null) throw new IllegalStateException("Provider failure");
                yield banned;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        };
        return (OfflinePlayer) Proxy.newProxyInstance(
                OfflinePlayer.class.getClassLoader(),
                new Class<?>[]{OfflinePlayer.class}, handler);
    }

    @Test void paperProfileBanIsReportedUnderExactlyOneProviderIdentity() throws Exception {
        var observation = PaperProfileBanSource.fromProfile(PLAYER, profile(PLAYER, true));
        assertEquals("paper-profile", observation.id());
        assertEquals(BanEvidence.Status.BANNED, observation.status());
    }

    @Test void paperProfileClearIsNotIPOrExternalBanClear() throws Exception {
        var observation = PaperProfileBanSource.fromProfile(PLAYER, profile(PLAYER, false));
        assertEquals("paper-profile", observation.id());
        assertEquals(BanEvidence.Status.CLEAR, observation.status());
    }

    @Test void providerFailureCannotBeReportedAsClear() {
        assertThrows(IOException.class,
                () -> PaperProfileBanSource.fromProfile(PLAYER, profile(PLAYER, null)));
    }

    @Test void mismatchedProfileUuidCannotProveAnotherPlayerIsClear() {
        assertThrows(IOException.class, () -> PaperProfileBanSource.fromProfile(
                PLAYER, profile(UUID.randomUUID(), false)));
    }

    @Test void missingPlayerOrProfileDoesNotProduceEvidence() {
        assertThrows(IOException.class, () ->
                PaperProfileBanSource.fromProfile(null, profile(PLAYER, false)));
        assertThrows(IOException.class, () ->
                PaperProfileBanSource.fromProfile(PLAYER, null));
    }
}
