package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.surumene.simplecoins.domain.ExternalOwnerDecision;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * TASK-20261101-004 / WWW-ACCESS-01..04.
 * Candidate regression for known commander vs lower-priority owner accessor failure.
 * No provider completeness or live Paper claim is implied.
 */
class PaperWonderfulWolfOwnerAccessorFailureTest {
    private static final UUID WOLF =
            UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID COMMANDER =
            UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000011");

    public static final class Loaded {
        private final Object individual;
        public Loaded(Object individual) { this.individual = individual; }
        public Optional<Object> find(UUID id) {
            return WOLF.equals(id) ? Optional.of(individual) : Optional.empty();
        }
    }
    public static final class PluginFixture {
        private final Loaded loaded;
        public PluginFixture(Object individual) { this.loaded = new Loaded(individual); }
        public Loaded loadedIndividuals() { return loaded; }
    }
    public static final class ThrowingOwner {
        public Optional<UUID> commanderId() { return Optional.of(COMMANDER); }
        public Optional<UUID> ownerId() { throw new IllegalStateException("owner lookup unavailable"); }
    }
    public static final class MissingOwner {
        public Optional<UUID> commanderId() { return Optional.of(COMMANDER); }
    }
    public static final class AbsentCommanderThrowingOwner {
        public Optional<UUID> commanderId() { return Optional.empty(); }
        public Optional<UUID> ownerId() { throw new IllegalStateException("owner lookup unavailable"); }
    }
    public static final class ReadableOwner {
        public Optional<UUID> commanderId() { return Optional.of(COMMANDER); }
        public Optional<UUID> ownerId() { return Optional.of(OWNER); }
    }

    @Test void knownCommanderSurvivesThrowingOwnerAccessor() {
        var evidence = PaperWonderfulWolfOwnerAdapter.readPlugin(
                new PluginFixture(new ThrowingOwner()), WOLF);
        assertEquals(ExternalOwnerDecision.Kind.PERSONAL, evidence.kind());
        assertEquals(COMMANDER, evidence.owner());
    }

    @Test void knownCommanderSurvivesMissingOwnerAccessor() {
        var evidence = PaperWonderfulWolfOwnerAdapter.readPlugin(
                new PluginFixture(new MissingOwner()), WOLF);
        assertEquals(ExternalOwnerDecision.Kind.PERSONAL, evidence.kind());
        assertEquals(COMMANDER, evidence.owner());
    }

    @Test void absentCommanderAndFailingOwnerRemainUnknown() {
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                PaperWonderfulWolfOwnerAdapter.readPlugin(
                        new PluginFixture(new AbsentCommanderThrowingOwner()), WOLF).kind());
    }

    @Test void knownCommanderStillWinsWithReadableOwner() {
        var evidence = PaperWonderfulWolfOwnerAdapter.readPlugin(
                new PluginFixture(new ReadableOwner()), WOLF);
        assertEquals(ExternalOwnerDecision.Kind.PERSONAL, evidence.kind());
        assertEquals(COMMANDER, evidence.owner());
    }
}
