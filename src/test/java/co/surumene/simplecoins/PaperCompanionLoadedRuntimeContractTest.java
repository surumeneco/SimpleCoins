package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.surumene.simplecoins.domain.CompanionOwnerResolution;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Tameable;
import org.junit.jupiter.api.Test;

/** TASK-20261101-004: Paper interface boundary, not a live Paper/WWC/WWW test. */
class PaperCompanionLoadedRuntimeContractTest {
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-0000000000c4");

    private static AnimalTamer tamer(Supplier<UUID> uuid) {
        return (AnimalTamer) Proxy.newProxyInstance(
                AnimalTamer.class.getClassLoader(), new Class<?>[]{AnimalTamer.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return uuid.get();
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Tameable entity(Supplier<Boolean> tamed, Supplier<AnimalTamer> owner) {
        return (Tameable) Proxy.newProxyInstance(
                Tameable.class.getClassLoader(), new Class<?>[]{Tameable.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isTamed" -> tamed.get();
                    case "getOwner" -> owner.get();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test void tamedLoadedEntityReturnsKnownUuid() {
        assertEquals(CompanionOwnerResolution.known(OWNER),
                PaperCompanionOwnerAdapter.readPaper(entity(() -> true,
                        () -> tamer(() -> OWNER))));
    }

    @Test void untamedEntityDoesNotReadOwner() {
        assertEquals(CompanionOwnerResolution.absent(),
                PaperCompanionOwnerAdapter.readPaper(entity(() -> false,
                        () -> { throw new AssertionError("getOwner must not be called"); })));
    }

    @Test void missingOrThrowingOwnerCannotBecomeVerifiedAbsent() {
        assertEquals(CompanionOwnerResolution.unknown(),
                PaperCompanionOwnerAdapter.readPaper(entity(() -> true, () -> null)));
        assertEquals(CompanionOwnerResolution.unknown(),
                PaperCompanionOwnerAdapter.readPaper(entity(() -> true,
                        () -> { throw new IllegalStateException("unloaded"); })));
    }

    @Test void TameableOrAnimalTamerFailureIsUnknown() {
        assertEquals(CompanionOwnerResolution.unknown(),
                PaperCompanionOwnerAdapter.readPaper(entity(
                        () -> { throw new IllegalStateException("entity stale"); }, () -> null)));
        assertEquals(CompanionOwnerResolution.unknown(),
                PaperCompanionOwnerAdapter.readPaper(entity(() -> true,
                        () -> tamer(() -> { throw new IllegalStateException("owner stale"); }))));
    }

    @Test void nullEntityIsUnknown() {
        assertEquals(CompanionOwnerResolution.unknown(),
                PaperCompanionOwnerAdapter.readPaper(null));
    }
}
