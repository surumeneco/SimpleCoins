package co.surumene.simplecoins.fixtures;

import java.util.Optional;
import java.util.UUID;

/** Public method contracts with implementation classes of differing Java visibility. */
public final class WolfVisibilityFixtures {
    private WolfVisibilityFixtures() {}

    public static final UUID WOLF = UUID.fromString("00000000-0000-0000-0000-000000000091");
    public static final UUID COMMANDER = UUID.fromString("00000000-0000-0000-0000-000000000092");

    public static final class PublicIndividual {
        public Optional<UUID> commanderId() { return Optional.of(COMMANDER); }
        public Optional<UUID> ownerId() { return Optional.empty(); }
    }

    public static final class PublicLoaded {
        public Optional<PublicIndividual> find(UUID id) {
            return WOLF.equals(id) ? Optional.of(new PublicIndividual()) : Optional.empty();
        }
    }

    public static final class PublicPlugin {
        public PublicLoaded loadedIndividuals() { return new PublicLoaded(); }
    }

    // The methods are public, but their declaring implementation class is not.
    static final class HiddenLoaded {
        public Optional<PublicIndividual> find(UUID id) {
            return WOLF.equals(id) ? Optional.of(new PublicIndividual()) : Optional.empty();
        }
    }

    public static final class PluginWithHiddenLoaded {
        public Object loadedIndividuals() { return new HiddenLoaded(); }
    }

    static final class HiddenIndividual {
        public Optional<UUID> commanderId() { return Optional.of(COMMANDER); }
        public Optional<UUID> ownerId() { return Optional.empty(); }
    }

    public static final class PublicLoadedWithHiddenIndividual {
        public Optional<?> find(UUID id) {
            return WOLF.equals(id) ? Optional.of(new HiddenIndividual()) : Optional.empty();
        }
    }

    public static final class PluginWithHiddenIndividual {
        public PublicLoadedWithHiddenIndividual loadedIndividuals() {
            return new PublicLoadedWithHiddenIndividual();
        }
    }
}
