package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.ExternalOwnerDecision;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaperWonderfulWolfOwnerAdapterTest {
    private static final UUID WOLF = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID COMMANDER = UUID.fromString("00000000-0000-0000-0000-000000000012");

    public record Individual(Optional<UUID> commanderId, Optional<UUID> ownerId) {}
    public static final class Loaded {
        private final Map<UUID, Individual> values = new HashMap<>();
        public Optional<Individual> find(UUID id) { return Optional.ofNullable(values.get(id)); }
    }
    public static final class PluginFixture {
        private final Loaded loaded = new Loaded();
        public Loaded loadedIndividuals() { return loaded; }
    }
    public static final class MissingApi {}
    public static final class MalformedIndividual {
        public Optional<String> commanderId() { return Optional.of("not-uuid"); }
        public Optional<UUID> ownerId() { return Optional.of(OWNER); }
    }
    public static final class MalformedLoaded {
        public Optional<MalformedIndividual> find(UUID id) {
            return Optional.of(new MalformedIndividual());
        }
    }
    public static final class MalformedPlugin {
        public MalformedLoaded loadedIndividuals() { return new MalformedLoaded(); }
    }

    @Test void commanderTakesPrecedenceOverLoadedWwwOwner() {
        var plugin = new PluginFixture();
        plugin.loaded.values.put(WOLF, new Individual(
                Optional.of(COMMANDER), Optional.of(OWNER)));
        var evidence = PaperWonderfulWolfOwnerAdapter.readPlugin(plugin, WOLF);
        assertEquals(ExternalOwnerDecision.Kind.PERSONAL, evidence.kind());
        assertEquals(COMMANDER, evidence.owner());
    }

    @Test void verifiedAbsenceOfCommanderFallsBackToOwner() {
        var plugin = new PluginFixture();
        plugin.loaded.values.put(WOLF, new Individual(Optional.empty(), Optional.of(OWNER)));
        assertEquals(OWNER, PaperWonderfulWolfOwnerAdapter.readPlugin(plugin, WOLF).owner());
        plugin.loaded.values.put(WOLF, new Individual(Optional.empty(), Optional.empty()));
        assertEquals(ExternalOwnerDecision.Kind.FREE,
                PaperWonderfulWolfOwnerAdapter.readPlugin(plugin, WOLF).kind());
    }

    @Test void unloadedOrUnavailableApiNeverBecomesFree() {
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                PaperWonderfulWolfOwnerAdapter.readPlugin(new PluginFixture(), WOLF).kind());
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                PaperWonderfulWolfOwnerAdapter.readPlugin(new MissingApi(), WOLF).kind());
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                PaperWonderfulWolfOwnerAdapter.readPlugin(null, WOLF).kind());
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                PaperWonderfulWolfOwnerAdapter.readPlugin(new PluginFixture(), null).kind());
    }

    @Test void malformedOptionalIdentityIsUnknown() {
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                PaperWonderfulWolfOwnerAdapter.readPlugin(new MalformedPlugin(), WOLF).kind());
    }
}
