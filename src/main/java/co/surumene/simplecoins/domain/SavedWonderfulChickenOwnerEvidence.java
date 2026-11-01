package co.surumene.simplecoins.domain;

import java.util.Map;

/** Read-only positive-only WWC commander evidence from saved Java Entity NBT. */
public final class SavedWonderfulChickenOwnerEvidence {
    private SavedWonderfulChickenOwnerEvidence() {}

    public static CompanionOwnerResolution.Identity read(Map<String, Object> savedEntity) {
        return CompanionOwnerResolution.unknown();
    }
}
