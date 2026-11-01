package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.CompanionOwnerResolution;
import co.surumene.simplecoins.domain.ExternalOwnerDecision;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaperWonderfulChickenOwnerAdapterTest {
    private static final UUID COMMANDER =
            UUID.fromString("00000000-0000-0000-0000-0000000000af");

    @Test void canonicalLoadedWwcFollowTargetSuppliesPositiveCommanderEvidence() throws Exception {
        var commander = PaperWonderfulChickenOwnerAdapter.read(
                "wonderful_chicken", COMMANDER.toString(), 4);
        assertEquals(CompanionOwnerResolution.State.KNOWN, commander.state());
        assertEquals(COMMANDER, commander.uuid());
        assertEquals(COMMANDER, CompanionOwnerResolution.resolve(
                commander, CompanionOwnerResolution.unknown()).owner());
    }

    @Test void missingOrUnsupportedWwcDataNeverProvesFree() throws Exception {
        assertEquals(CompanionOwnerResolution.State.UNKNOWN,
                PaperWonderfulChickenOwnerAdapter.read("wonderful_chicken", null, 4).state());
        assertEquals(CompanionOwnerResolution.State.UNKNOWN,
                PaperWonderfulChickenOwnerAdapter.read("other", COMMANDER.toString(), 4).state());
        assertEquals(CompanionOwnerResolution.State.UNKNOWN,
                PaperWonderfulChickenOwnerAdapter.read("wonderful_chicken", COMMANDER.toString(), 5).state());
        assertEquals(CompanionOwnerResolution.State.UNKNOWN,
                PaperWonderfulChickenOwnerAdapter.read("wonderful_chicken", COMMANDER.toString(), null).state());
        assertEquals(CompanionOwnerResolution.State.UNKNOWN,
                PaperWonderfulChickenOwnerAdapter.read("wonderful_chicken", COMMANDER.toString().toUpperCase(), 4).state());
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN, CompanionOwnerResolution.resolve(
                PaperWonderfulChickenOwnerAdapter.read("wonderful_chicken", null, 4),
                CompanionOwnerResolution.unknown()).kind());
    }
}
