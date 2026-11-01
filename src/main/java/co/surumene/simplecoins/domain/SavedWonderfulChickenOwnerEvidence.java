package co.surumene.simplecoins.domain;

import java.util.Map;
import java.util.UUID;

/**
 * Read-only, positive-only commander evidence from a saved Java chicken
 * Entity's Bukkit PDC compound. Missing or malformed evidence is UNKNOWN,
 * never a proof of FREE ownership. The caller must still validate Entity UUID,
 * coherent snapshot, loaded-vs-disk state, BAN and WWC precedence.
 */
public final class SavedWonderfulChickenOwnerEvidence {
    private SavedWonderfulChickenOwnerEvidence() {}

    private static final String NAMESPACE = "whatawonderfulchicken:";

    public static CompanionOwnerResolution.Identity read(Map<String, Object> savedEntity) {
        if (savedEntity == null || !"minecraft:chicken".equals(savedEntity.get("id")))
            return CompanionOwnerResolution.unknown();
        if (!(savedEntity.get("BukkitValues") instanceof Map<?, ?> pdc))
            return CompanionOwnerResolution.unknown();

        if (!"wonderful_chicken".equals(pdc.get(NAMESPACE + "type")))
            return CompanionOwnerResolution.unknown();
        Object rawVersion = pdc.get(NAMESPACE + "data_version");
        Object rawTarget = pdc.get(NAMESPACE + "follow_target");
        if (!(rawVersion instanceof Integer version) || version < 2 || version > 4 ||
                !(rawTarget instanceof String target))
            return CompanionOwnerResolution.unknown();
        try {
            UUID commander = UUID.fromString(target);
            return commander.toString().equals(target)
                    ? CompanionOwnerResolution.known(commander)
                    : CompanionOwnerResolution.unknown();
        } catch (IllegalArgumentException invalid) {
            return CompanionOwnerResolution.unknown();
        }
    }
}
