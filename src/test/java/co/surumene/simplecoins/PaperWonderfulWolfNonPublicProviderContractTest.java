package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.surumene.simplecoins.domain.ExternalOwnerDecision;
import co.surumene.simplecoins.fixtures.WolfVisibilityFixtures;
import org.junit.jupiter.api.Test;

/**
 * TASK-20261101-004: test-only proposed compatibility with non-public
 * implementations of a public WWW API. Red is not permission for a production fix.
 */
class PaperWonderfulWolfNonPublicProviderContractTest {
    @Test void publicProviderAndIndividualRemainAWorkingControl() {
        var result = PaperWonderfulWolfOwnerAdapter.readPlugin(
                new WolfVisibilityFixtures.PublicPlugin(), WolfVisibilityFixtures.WOLF);
        assertEquals(ExternalOwnerDecision.Kind.PERSONAL, result.kind());
        assertEquals(WolfVisibilityFixtures.COMMANDER, result.owner());
    }

    @Test void nonPublicLoadedImplementationShouldNotHidePublicMethodEvidence() {
        var result = PaperWonderfulWolfOwnerAdapter.readPlugin(
                new WolfVisibilityFixtures.PluginWithHiddenLoaded(), WolfVisibilityFixtures.WOLF);
        assertEquals(ExternalOwnerDecision.Kind.PERSONAL, result.kind());
        assertEquals(WolfVisibilityFixtures.COMMANDER, result.owner());
    }

    @Test void nonPublicIndividualImplementationShouldNotHidePublicMethodEvidence() {
        var result = PaperWonderfulWolfOwnerAdapter.readPlugin(
                new WolfVisibilityFixtures.PluginWithHiddenIndividual(), WolfVisibilityFixtures.WOLF);
        assertEquals(ExternalOwnerDecision.Kind.PERSONAL, result.kind());
        assertEquals(WolfVisibilityFixtures.COMMANDER, result.owner());
    }

    @Test void missingIndividualStillCannotProveFreeOwnership() {
        var result = PaperWonderfulWolfOwnerAdapter.readPlugin(
                new WolfVisibilityFixtures.PublicPlugin(), WolfVisibilityFixtures.COMMANDER);
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN, result.kind());
    }
}
