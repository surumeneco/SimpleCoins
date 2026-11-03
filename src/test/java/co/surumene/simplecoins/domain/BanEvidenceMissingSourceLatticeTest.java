package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Source-exact code-to-spec verification of all required-provider presence/status
 * combinations. This does not attest discovery of a complete provider inventory.
 */
class BanEvidenceMissingSourceLatticeTest {
    private static final String[] IDS = {"paper", "ip", "external", "moderation"};
    private static final Set<String> REQUIRED = Set.of(IDS);
    private static final BanEvidence.Status[] STATES = {
        null, BanEvidence.Status.CLEAR,
        BanEvidence.Status.UNKNOWN, BanEvidence.Status.BANNED
    };

    @Test
    void everyMissingAndObservedProviderCombinationPreservesBanPrecedence() throws Exception {
        int verified = 0;
        for (int packed = 0; packed < 256; packed++) {
            int remaining = packed;
            boolean hasBan = false;
            boolean hasUnknownOrMissing = false;
            List<BanEvidence.Source> observations = new ArrayList<>();
            for (String sourceId : IDS) {
                BanEvidence.Status status = STATES[remaining % STATES.length];
                remaining /= STATES.length;
                if (status == null) {
                    hasUnknownOrMissing = true;
                } else {
                    observations.add(new BanEvidence.Source(sourceId, status));
                    hasBan |= status == BanEvidence.Status.BANNED;
                    hasUnknownOrMissing |= status == BanEvidence.Status.UNKNOWN;
                }
            }
            BanEvidence.Status expected = hasBan
                    ? BanEvidence.Status.BANNED
                    : hasUnknownOrMissing
                    ? BanEvidence.Status.UNKNOWN : BanEvidence.Status.CLEAR;
            assertEquals(expected, BanEvidence.evaluate(REQUIRED, observations).status(),
                    "presence/status vector " + packed);
            verified++;
        }
        assertEquals(256, verified);
    }
}
