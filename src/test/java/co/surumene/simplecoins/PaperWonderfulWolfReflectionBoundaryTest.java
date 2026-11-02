package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.ExternalOwnerDecision;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** TASK-20261101-004: WWW optional reflection boundary fails closed on API drift. */
class PaperWonderfulWolfReflectionBoundaryTest {
    private static final UUID WOLF = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID COMMANDER = UUID.fromString("00000000-0000-0000-0000-000000000012");

    public record Individual(Optional<UUID> commanderId, Optional<UUID> ownerId) {}
    public static final class Loaded {
        public Optional<Individual> find(UUID id) {
            return WOLF.equals(id) ? Optional.of(new Individual(
                    Optional.of(COMMANDER), Optional.of(OWNER))) : Optional.empty();
        }
    }
    public static final class PluginFixture {
        public Loaded loadedIndividuals() { return new Loaded(); }
    }
    public static final class NullLoadedPlugin {
        public Object loadedIndividuals() { return null; }
    }
    public static final class ThrowingLoadedPlugin {
        public Object loadedIndividuals() { throw new IllegalStateException("unavailable"); }
    }
    public static final class InvalidFindLoaded {
        public Object find(UUID id) { return WOLF.equals(id) ? OWNER : null; }
    }
    public static final class InvalidFindPlugin {
        public InvalidFindLoaded loadedIndividuals() { return new InvalidFindLoaded(); }
    }
    public static final class ThrowingFindLoaded {
        public Optional<Individual> find(UUID id) { throw new IllegalStateException("unloaded"); }
    }
    public static final class ThrowingFindPlugin {
        public ThrowingFindLoaded loadedIndividuals() { return new ThrowingFindLoaded(); }
    }
    public static final class NullIdentity {
        public Optional<UUID> commanderId() { return null; }
        public Optional<UUID> ownerId() { return Optional.of(OWNER); }
    }
    public static final class NullIdentityLoaded {
        public Optional<NullIdentity> find(UUID id) { return Optional.of(new NullIdentity()); }
    }
    public static final class NullIdentityPlugin {
        public NullIdentityLoaded loadedIndividuals() { return new NullIdentityLoaded(); }
    }

    @Test void actualPublicMethodShapeReturnsCommanderBeforeOwner() {
        var evidence = PaperWonderfulWolfOwnerAdapter.readPlugin(new PluginFixture(), WOLF);
        assertEquals(ExternalOwnerDecision.Kind.PERSONAL, evidence.kind());
        assertEquals(COMMANDER, evidence.owner());
    }

    @Test void unloadedWolfAndNullProviderRemainUnknown() {
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                PaperWonderfulWolfOwnerAdapter.readPlugin(new PluginFixture(), OWNER).kind());
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                PaperWonderfulWolfOwnerAdapter.readPlugin(null, WOLF).kind());
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                PaperWonderfulWolfOwnerAdapter.readPlugin(new PluginFixture(), null).kind());
    }

    @Test void missingOrChangedRuntimeApiCannotProduceFree() {
        for (Object plugin : new Object[]{new NullLoadedPlugin(), new InvalidFindPlugin(),
                new NullIdentityPlugin()})
            assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                    PaperWonderfulWolfOwnerAdapter.readPlugin(plugin, WOLF).kind());
    }

    @Test void providerInvocationFailuresRemainUnknown() {
        for (Object plugin : new Object[]{new ThrowingLoadedPlugin(), new ThrowingFindPlugin()})
            assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                    PaperWonderfulWolfOwnerAdapter.readPlugin(plugin, WOLF).kind());
    }
}
