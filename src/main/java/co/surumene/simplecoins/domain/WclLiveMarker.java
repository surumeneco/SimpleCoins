package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Classifies only the pinned WWC/WWW Bukkit PDC namespace/type scheme.
 * An absent namespace means "no verified marker", never verified monetary
 * absence, reader availability, all-Entity coverage or G0 source closure.
 */
public final class WclLiveMarker {
    private static final String CHICKEN_NAMESPACE = "whatawonderfulchicken:";
    private static final String WOLF_NAMESPACE = "whatawonderfulwolf:";

    private WclLiveMarker() {}

    /**
     * The caller supplies the actual Paper EntityType NamespacedKey and exact
     * PDC getKeys()/typed type values. All partial or contradictory provider
     * evidence aborts the source generation rather than becoming zero money.
     */
    public static Optional<String> provider(
            String species, Set<String> pdcKeys,
            String chickenType, String wolfType) throws IOException {
        Objects.requireNonNull(species, "loaded Bukkit Entity species");
        Objects.requireNonNull(pdcKeys, "loaded Bukkit PDC key set");

        boolean chicken = false;
        boolean wolf = false;
        for (String key : pdcKeys) {
            if (key == null)
                throw new IOException("Null loaded Entity PDC identity key");
            chicken |= key.startsWith(CHICKEN_NAMESPACE);
            wolf |= key.startsWith(WOLF_NAMESPACE);
        }
        if (chicken && wolf)
            throw new IOException("Contradictory WWC/WWW loaded Entity PDC identities");
        if (!chicken && !wolf) {
            if (chickenType != null || wolfType != null)
                throw new IOException("WCL type observed without a WCL namespace identity");
            return Optional.empty();
        }
        if (chicken) {
            if (!"minecraft:chicken".equals(species)
                    || !"wonderful_chicken".equals(chickenType) || wolfType != null)
                throw new IOException("Loaded WWC marker/species is incomplete or contradictory");
            return Optional.of("wwc");
        }
        if (!"minecraft:wolf".equals(species)
                || !"wonderful_wolf".equals(wolfType) || chickenType != null)
            throw new IOException("Loaded WWW marker/species is incomplete or contradictory");
        return Optional.of("www");
    }
}
