package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Source-exact code-to-spec Candidate; no external BAN inventory is inferred. */
class BanEvidencePermutationAndMissingSourceTest {
    private static final String[] IDS = {"paper","ip","external","moderation"};
    private static final Set<String> REQUIRED = Set.of(IDS);

    @Test void fourSourceTruthTableIsOrderIndependent() throws Exception {
        var states = BanEvidence.Status.values();
        int count = 0;
        for (var a:states) for (var b:states) for (var c:states) for (var d:states) {
            var values = new BanEvidence.Status[]{a,b,c,d};
            var expected = Arrays.asList(values).contains(BanEvidence.Status.BANNED)
                    ? BanEvidence.Status.BANNED
                    : Arrays.asList(values).contains(BanEvidence.Status.UNKNOWN)
                    ? BanEvidence.Status.UNKNOWN : BanEvidence.Status.CLEAR;
            for(int i=0;i<4;i++) for(int j=0;j<4;j++)
                for(int k=0;k<4;k++) for(int l=0;l<4;l++) {
                    if (i==j||i==k||i==l||j==k||j==l||k==l) continue;
                    assertEquals(expected, BanEvidence.evaluate(REQUIRED,List.of(
                        new BanEvidence.Source(IDS[i],values[i]),
                        new BanEvidence.Source(IDS[j],values[j]),
                        new BanEvidence.Source(IDS[k],values[k]),
                        new BanEvidence.Source(IDS[l],values[l]))).status());
                    count++;
                }
        }
        assertEquals(1944,count);
    }

    @Test void missingFourthSourceNeverProvesClear() throws Exception {
        var states = BanEvidence.Status.values();
        int count = 0;
        for(var a:states) for(var b:states) for(var c:states) {
            var values = new BanEvidence.Status[]{a,b,c};
            var expected = Arrays.asList(values).contains(BanEvidence.Status.BANNED)
                    ? BanEvidence.Status.BANNED : BanEvidence.Status.UNKNOWN;
            for(int i=0;i<3;i++) for(int j=0;j<3;j++) for(int k=0;k<3;k++) {
                if(i==j||i==k||j==k) continue;
                assertEquals(expected, BanEvidence.evaluate(REQUIRED,List.of(
                    new BanEvidence.Source(IDS[i],values[i]),
                    new BanEvidence.Source(IDS[j],values[j]),
                    new BanEvidence.Source(IDS[k],values[k]))).status());
                count++;
            }
        }
        assertEquals(162,count);
    }

    @Test void invalidSourceEvidenceNeverOverridesValidation() {
        var banned = new BanEvidence.Source("paper", BanEvidence.Status.BANNED);
        var undeclared = new BanEvidence.Source("unlisted", BanEvidence.Status.CLEAR);
        var duplicate = new BanEvidence.Source(" PAPER ", BanEvidence.Status.CLEAR);
        var nullStatus = new BanEvidence.Source("ip", null);
        for (var invalid : List.of(undeclared, duplicate, nullStatus)) {
            assertThrows(java.io.IOException.class, () -> BanEvidence.evaluate(REQUIRED, List.of(banned, invalid)));
            assertThrows(java.io.IOException.class, () -> BanEvidence.evaluate(REQUIRED, List.of(invalid, banned)));
        }
    }

}
